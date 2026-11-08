import Foundation
import Combine
import HealthKit

public enum SyncStatus: Equatable {
    case idle
    case syncing
    case success(message: String)
    case error(message: String)
}

@MainActor
public final class HealthSyncManager: ObservableObject {
    public static let shared = HealthSyncManager()

    @Published public private(set) var isSyncing: Bool = false
    @Published public private(set) var syncStatus: SyncStatus = .idle
    @Published public private(set) var lastSyncDate: Date? = nil
    @Published public private(set) var lastSyncError: String? = nil

    private var autoResetTask: Task<Void, Never>?

    private let healthKitManager = HealthKitManager.shared
    private let api = HealthSyncApi.shared
    private let foodApi = FoodIntakeApi.shared

    // The food work currently running. New work waits for it: two overlapping
    // uploads would otherwise insert the same entry twice.
    private var foodSyncTask: Task<Bool, Never>?

    /// Sends queued deletes, then uploads the food entries and meal photos
    /// the backend hasn't confirmed yet (idempotent by entry id). Best-effort:
    /// a failure never fails health sync or the food UI, and whatever didn't
    /// go through is retried by the next call. Runs automatically at the end
    /// of [sync] and right after a food entry is added.
    @discardableResult
    public func syncFoodIntake() async -> Bool {
        guard AuthManager.shared.isLoggedIn else { return false }
        return await runFoodWorkInOrder { [self] in
            let deletesSent = await sendFoodDeletes()
            let uploaded = await uploadPendingFood()
            return deletesSent && uploaded
        }
    }

    /// Queues the backend delete for a local entry, so it's retried if this attempt fails.
    @discardableResult
    public func deleteFoodEntry(id: String) async -> Bool {
        FoodPhotoStore.shared.delete(id: id)
        FoodStore.shared.queueRemoteDelete(id: id)
        guard AuthManager.shared.isLoggedIn else { return false }
        return await runFoodWorkInOrder { [self] in await sendFoodDeletes() }
    }

    private func runFoodWorkInOrder(_ work: @escaping @MainActor () async -> Bool) async -> Bool {
        let previous = foodSyncTask
        let task = Task { @MainActor in
            _ = await previous?.value
            return await work()
        }
        foodSyncTask = task
        return await task.value
    }

    private func sendFoodDeletes() async -> Bool {
        for id in FoodStore.shared.pendingRemoteDeletes() {
            guard await foodApi.delete(externalId: id) else { return false }
            FoodStore.shared.clearRemoteDelete(id: id)
        }
        return true
    }

    /// Uploads in small batches, recording each one as it succeeds so a backlog makes progress.
    private func uploadPendingFood() async -> Bool {
        let uploaded = FoodStore.shared.uploadedIds()
        let photoSizes = FoodPhotoStore.shared.pendingPhotoSizes()
        let pending = FoodStore.shared.entries.filter { !uploaded.contains($0.id) || photoSizes[$0.id] != nil }
        for batch in batchFoodUploads(pending, photoSizes: photoSizes) {
            var photos: [String: Data] = [:]
            for entry in batch where photoSizes[entry.id] != nil {
                photos[entry.id] = FoodPhotoStore.shared.read(id: entry.id)
            }
            guard await foodApi.sync(entries: batch, photos: photos) else { return false }
            FoodStore.shared.markUploaded(ids: batch.map(\.id))
            // Only photos this batch actually sent, so none is marked done without being uploaded.
            FoodPhotoStore.shared.markUploaded(ids: Array(photos.keys))
        }
        return true
    }

    private let defaults = UserDefaults.standard
    private let keyLastSync = "health_last_sync_iso"
    private let keyCachedDailyRecords = "health_cached_daily_records"

    @Published public private(set) var cachedDailyRecords: [String: DailyHealthSyncDto] = [:]

    public func cachedRecord(for date: Date) -> DailyHealthSyncDto? {
        let key = dateFormatter.string(from: date)
        return cachedDailyRecords[key]
    }

    public func latestCachedHeartRate() -> (bpm: Int, dateStr: String)? {
        let sortedKeys = cachedDailyRecords.keys.sorted(by: >)
        for key in sortedKeys {
            if let rec = cachedDailyRecords[key], let bpm = rec.latestHeartRateBpm, bpm > 0 {
                return (bpm, key)
            }
        }
        return nil
    }

    public static func parseDate(_ string: String) -> Date? {
        let iso = ISO8601DateFormatter()
        if let d = iso.date(from: string) { return d }
        if let d = iso.date(from: string + "Z") { return d }

        let df = DateFormatter()
        df.locale = Locale(identifier: "en_US_POSIX")
        df.timeZone = TimeZone(secondsFromGMT: 0)

        let formats = [
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        ]
        for f in formats {
            df.dateFormat = f
            if let d = df.date(from: string) { return d }
        }
        return nil
    }

    private let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f
    }()

    private let isoFormatter = ISO8601DateFormatter()

    private init() {
        if let saved = defaults.string(forKey: keyLastSync), let d = isoFormatter.date(from: saved) {
            self.lastSyncDate = d
        }
        if let data = defaults.data(forKey: keyCachedDailyRecords),
           let decoded = try? JSONDecoder().decode([String: DailyHealthSyncDto].self, from: data) {
            self.cachedDailyRecords = decoded
        }
    }

    /// Performs a two-way synchronization:
    /// 1. Reads local Apple Health metrics for the last 7 days and local logged workouts.
    /// 2. Sends local records to the Gymsuit backend.
    /// 3. Receives remote records updated on other devices (e.g. Android Health Connect).
    /// 4. Writes new remote records into Apple Health and local store.
    @discardableResult
    public func sync() async -> Bool {
        guard AuthManager.shared.isLoggedIn else { return false }
        guard !isSyncing else { return false }

        isSyncing = true
        syncStatus = .syncing
        lastSyncError = nil
        autoResetTask?.cancel()
        defer { isSyncing = false }

        // Ensure HealthKit is authorized
        if !healthKitManager.isAuthorized {
            _ = await healthKitManager.requestAuthorization()
        }

        do {
            let lastSyncIso = defaults.string(forKey: keyLastSync)
            let calendar = Calendar.current
            let today = Date()

            // 1. Collect local daily records for the past 7 days
            // Weight: the latest Apple Health sample on or before each day,
            // excluding samples this app wrote back from the server (those
            // are tagged fromBackend — uploading them would loop the server
            // value back as a new weigh-in). fetchWeightSamples was only
            // used by the weight history screen until now, so weight never
            // reached the server — the report averaged whatever stale rows
            // the backend still held, which is how a phantom 5.7 kg drop
            // appeared in one week.
            let weightSamples = await healthKitManager.fetchWeightSamples(days: 90)
            var dailyDtos: [DailyHealthSyncDto] = []
            for offset in 0..<7 {
                guard let targetDate = calendar.date(byAdding: .day, value: -offset, to: today) else { continue }
                let dateStr = dateFormatter.string(from: targetDate)

                let steps = await healthKitManager.fetchSteps(for: targetDate)
                let calories = await healthKitManager.fetchCaloriesBreakdown(for: targetDate)
                let distanceMeters = await healthKitManager.fetchDistanceMeters(for: targetDate)
                let hr = await healthKitManager.fetchHeartRateSummary(for: targetDate)
                let sleep = await healthKitManager.fetchSleepSession(for: targetDate)
                let dayEnd = calendar.date(byAdding: .day, value: 1, to: calendar.startOfDay(for: targetDate)) ?? targetDate
                let weightForDay = weightSamples
                    .filter { !$0.fromBackend && $0.date <= dayEnd }
                    .max(by: { $0.date < $1.date })?.kg

                let hasData = steps > 0 || calories.hasData || (distanceMeters != nil && distanceMeters! > 0) || (hr?.hasData == true && (hr?.latestBpm ?? 0) > 0) || sleep != nil || weightForDay != nil
                if hasData {
                    var sleepStartIso: String? = nil
                    var sleepEndIso: String? = nil
                    if let s = sleep {
                        sleepStartIso = isoFormatter.string(from: s.startTime)
                        sleepEndIso = isoFormatter.string(from: s.endTime)
                    }

                    dailyDtos.append(DailyHealthSyncDto(
                        recordDate: dateStr,
                        steps: steps > 0 ? steps : nil,
                        activeCalories: calories.hasData && calories.workoutKcal + calories.moveKcal > 0 ? calories.workoutKcal + calories.moveKcal : nil,
                        totalCalories: calories.hasData && calories.totalKcal > 0 ? calories.totalKcal : nil,
                        distanceMeters: distanceMeters,
                        latestHeartRateBpm: (hr?.hasData == true && (hr?.latestBpm ?? 0) > 0) ? hr?.latestBpm : nil,
                        restingHeartRateBpm: nil,
                        minHeartRateBpm: (hr?.hasData == true && (hr?.minBpm ?? 0) > 0) ? hr?.minBpm : nil,
                        maxHeartRateBpm: (hr?.hasData == true && (hr?.maxBpm ?? 0) > 0) ? hr?.maxBpm : nil,
                        sleepDurationMinutes: sleep?.durationMinutes,
                        sleepStartTime: sleepStartIso,
                        sleepEndTime: sleepEndIso,
                        weightKg: weightForDay,
                        sourceDevice: "IOS_APPLE_HEALTH"
                    ))
                }
            }

            // 2. Collect logged workouts
            let workouts = WorkoutStore.shared.loggedWorkouts
            let workoutDtos = workouts.map { w in
                let end = calendar.date(byAdding: .minute, value: max(1, w.durationMinutes), to: w.date) ?? w.date
                return WorkoutSyncDto(
                    externalId: w.id.uuidString,
                    title: w.exerciseName,
                    startTime: isoFormatter.string(from: w.date),
                    endTime: isoFormatter.string(from: end),
                    durationMinutes: w.durationMinutes,
                    caloriesBurned: nil,
                    setCount: w.sets.count,
                    totalReps: w.totalReps,
                    totalVolumeKg: w.totalVolumeKg,
                    sourceDevice: "IOS_APPLE_HEALTH"
                )
            }

            // 3. Collect points history
            let localPoints = PointsStore.shared.history.map { p in
                PointsTransactionDto(
                    id: p.id.uuidString,
                    points: p.points,
                    reason: p.reason,
                    rupeeValue: p.rupeeValue,
                    createdAt: isoFormatter.string(from: p.date),
                    deviceId: "IOS_APP"
                )
            }

            // 4. Post to backend
            let request = HealthSyncRequest(
                lastSyncTime: lastSyncIso,
                clientDevice: "IOS_APPLE_HEALTH",
                dailyRecords: dailyDtos,
                workouts: workoutDtos,
                pointsTransactions: localPoints
            )

            let response = try await api.sync(request: request)

            // 4. Ingest incoming remote daily records from backend into Apple Health (larger data wins)
            for remote in response.remoteDailyRecords {
                self.cachedDailyRecords[remote.recordDate] = remote

                guard let recordDate = dateFormatter.date(from: remote.recordDate) else { continue }
                let startOfDay = calendar.startOfDay(for: recordDate)
                let endOfDay = calendar.date(byAdding: .day, value: 1, to: startOfDay) ?? recordDate

                // Query existing local values to ensure larger data wins
                let localSteps = await healthKitManager.fetchSteps(for: recordDate)
                let localCalories = await healthKitManager.fetchCaloriesBreakdown(for: recordDate)
                let localDistance = await healthKitManager.fetchDistanceMeters(for: recordDate) ?? 0.0
                let localSleep = await healthKitManager.fetchSleepSession(for: recordDate)
                let localHr = await healthKitManager.fetchHeartRateSummary(for: recordDate)

                let localHasAnyData = localSteps > 0 || localCalories.hasData || (localHr?.hasData ?? false) || localSleep != nil
                if remote.sourceDevice == "IOS_APPLE_HEALTH" && localHasAnyData {
                    continue
                }

                if let steps = remote.steps, steps > localSteps {
                    // Write back only the gap: HealthKit readers sum all samples, so
                    // writing the full remote total on top of local samples would
                    // double-count the day (mirrors Android's gap-only write-back).
                    _ = await healthKitManager.writeSteps(count: steps - localSteps, start: startOfDay, end: endOfDay, externalId: "sync_steps_\(remote.recordDate)")
                }

                let localActiveKcal = (localCalories.hasData ? (localCalories.workoutKcal + localCalories.moveKcal) : 0.0)
                if let calories = remote.activeCalories, calories > localActiveKcal {
                    // Gap only (see steps above).
                    _ = await healthKitManager.writeActiveCalories(kcal: calories - localActiveKcal, start: startOfDay, end: endOfDay, externalId: "sync_cal_\(remote.recordDate)")
                }

                if let dist = remote.distanceMeters, dist > localDistance {
                    // Gap only (see steps above).
                    _ = await healthKitManager.writeDistance(meters: dist - localDistance, start: startOfDay, end: endOfDay, externalId: "sync_dist_\(remote.recordDate)")
                }

                if let hr = remote.latestHeartRateBpm, hr > 0 {
                    if localHr == nil || !(localHr?.hasData ?? false) || (localHr?.latestBpm ?? 0) == 0 {
                        _ = await healthKitManager.writeHeartRate(bpm: hr, date: recordDate, externalId: "sync_hr_\(remote.recordDate)")
                    }
                }

                let localSleepMins = localSleep?.durationMinutes ?? 0
                if let sStart = remote.sleepStartTime, let sEnd = remote.sleepEndTime,
                   let startDate = HealthSyncManager.parseDate(sStart),
                   let endDate = HealthSyncManager.parseDate(sEnd),
                   let rSleepMins = remote.sleepDurationMinutes,
                   rSleepMins > localSleepMins {
                    _ = await healthKitManager.writeSleep(start: startDate, end: endDate, externalId: "sync_sleep_\(remote.recordDate)")
                }

                if let weight = remote.weightKg, weight > 0 {
                    // Only fill the gap: if Apple Health already has a
                    // sample that day, writing the server value again
                    // would plant a duplicate sample.
                    let hasLocalWeightThatDay = weightSamples.contains {
                        calendar.isDate($0.date, inSameDayAs: recordDate)
                    }
                    if !hasLocalWeightThatDay {
                        _ = await healthKitManager.writeWeight(kg: weight, date: recordDate)
                    }
                }
            }

            if let encoded = try? JSONEncoder().encode(self.cachedDailyRecords) {
                defaults.set(encoded, forKey: keyCachedDailyRecords)
            }

            // 5. Ingest incoming remote workouts from backend into Apple Health and local store (larger data wins)
            for rw in response.remoteWorkouts {
                if rw.sourceDevice != "IOS_APPLE_HEALTH" {
                    let start = HealthSyncManager.parseDate(rw.startTime) ?? today
                    let end = HealthSyncManager.parseDate(rw.endTime) ?? today
                    let duration = rw.durationMinutes ?? max(1, Int(end.timeIntervalSince(start) / 60))
                    let setCount = rw.setCount ?? 1
                    let totalReps = rw.totalReps ?? 10
                    let totalVolume = rw.totalVolumeKg ?? 0.0

                    // Calculate remote data score
                    let remoteScore = totalVolume + Double(totalReps * 10) + Double(duration * 5)

                    // Check for overlapping local workout (same external ID or within 45 mins)
                    let overlappingLocal = WorkoutStore.shared.loggedWorkouts.first { local in
                        local.id.uuidString == rw.externalId ||
                        abs(local.date.timeIntervalSince(start)) < 45 * 60
                    }

                    if let existing = overlappingLocal {
                        let existingScore = existing.totalVolumeKg + Double(existing.totalReps * 10) + Double(existing.durationMinutes * 5)
                        // If local has larger or equal data, keep local
                        if existingScore >= remoteScore {
                            continue
                        }

                        // Remote has larger data: update existing workout in WorkoutStore
                        let updatedWorkout = LoggedWorkout(
                            id: existing.id,
                            exerciseId: rw.title.lowercased().replacingOccurrences(of: " ", with: "_"),
                            exerciseName: rw.title,
                            date: start,
                            sets: (1...setCount).map { _ in
                                WorkoutSet(
                                    reps: totalReps / max(1, setCount),
                                    weightKg: totalVolume / Double(max(1, totalReps))
                                )
                            },
                            durationMinutes: duration,
                            notes: "Updated from \(rw.sourceDevice ?? "Android") (richer data)",
                            syncedToHealthKit: true
                        )
                        WorkoutStore.shared.upsertWorkout(updatedWorkout)
                    } else {
                        // Insert new workout
                        let parsedUuid = UUID(uuidString: rw.externalId) ?? UUID()
                        let newWorkout = LoggedWorkout(
                            id: parsedUuid,
                            exerciseId: rw.title.lowercased().replacingOccurrences(of: " ", with: "_"),
                            exerciseName: rw.title,
                            date: start,
                            sets: (1...setCount).map { _ in
                                WorkoutSet(
                                    reps: totalReps / max(1, setCount),
                                    weightKg: totalVolume / Double(max(1, totalReps))
                                )
                            },
                            durationMinutes: duration,
                            notes: "Synced from \(rw.sourceDevice ?? "Android")",
                            syncedToHealthKit: true
                        )
                        WorkoutStore.shared.upsertWorkout(newWorkout)
                    }

                    // Write to Apple Health
                    _ = await healthKitManager.writeSyncedWorkout(
                        title: rw.title,
                        start: start,
                        end: end,
                        caloriesBurned: rw.caloriesBurned,
                        setCount: setCount,
                        totalReps: totalReps,
                        totalVolumeKg: totalVolume,
                        externalId: rw.externalId
                    )
                }
            }

            // 6. Ingest remote points balance & transactions
            PointsStore.shared.mergeRemote(remoteBalance: response.pointsBalance, remoteTransactions: response.pointsTransactions)

            // 7. Upload the food log too, so trainers can see calorie
            // intake. Best-effort: never fails the health sync above.
            await syncFoodIntake()

            // Save new sync checkpoint
            let nowIso = isoFormatter.string(from: today)
            defaults.set(nowIso, forKey: keyLastSync)
            self.lastSyncDate = today
            self.syncStatus = .success(message: "All data synced")
            autoResetTask = Task {
                try? await Task.sleep(nanoseconds: 3_500_000_000)
                if !Task.isCancelled {
                    self.syncStatus = .idle
                }
            }
            return true
        } catch {
            print("HealthSyncManager error: \(error.localizedDescription)")
            self.lastSyncError = error.localizedDescription
            self.syncStatus = .error(message: error.localizedDescription)
            autoResetTask = Task {
                try? await Task.sleep(nanoseconds: 3_500_000_000)
                if !Task.isCancelled {
                    self.syncStatus = .idle
                }
            }
            return false
        }
    }
}
