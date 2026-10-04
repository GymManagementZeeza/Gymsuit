import Foundation
import Combine
import HealthKit

@MainActor
public final class HealthSyncManager: ObservableObject {
    public static let shared = HealthSyncManager()

    @Published public private(set) var isSyncing: Bool = false
    @Published public private(set) var lastSyncDate: Date? = nil
    @Published public private(set) var lastSyncError: String? = nil

    private let healthKitManager = HealthKitManager.shared
    private let api = HealthSyncApi.shared
    private let defaults = UserDefaults.standard
    private let keyLastSync = "health_last_sync_iso"

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
        lastSyncError = nil
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
            var dailyDtos: [DailyHealthSyncDto] = []
            for offset in 0..<7 {
                guard let targetDate = calendar.date(byAdding: .day, value: -offset, to: today) else { continue }
                let dateStr = dateFormatter.string(from: targetDate)

                let steps = await healthKitManager.fetchSteps(for: targetDate)
                let calories = await healthKitManager.fetchCaloriesBreakdown(for: targetDate)
                let distanceMeters = await healthKitManager.fetchDistanceMeters(for: targetDate)
                let hr = await healthKitManager.fetchHeartRateSummary(for: targetDate)
                let sleep = await healthKitManager.fetchSleepSession(for: targetDate)

                let hasData = steps > 0 || calories.hasData || (distanceMeters != nil && distanceMeters! > 0) || hr != nil || sleep != nil
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
                        latestHeartRateBpm: hr?.latestBpm,
                        restingHeartRateBpm: nil,
                        minHeartRateBpm: hr?.minBpm,
                        maxHeartRateBpm: hr?.maxBpm,
                        sleepDurationMinutes: sleep?.durationMinutes,
                        sleepStartTime: sleepStartIso,
                        sleepEndTime: sleepEndIso,
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

            // 3. Post to backend
            let request = HealthSyncRequest(
                lastSyncTime: lastSyncIso,
                clientDevice: "IOS_APPLE_HEALTH",
                dailyRecords: dailyDtos,
                workouts: workoutDtos
            )

            let response = try await api.sync(request: request)

            // 4. Ingest incoming remote daily records from backend into Apple Health
            for remote in response.remoteDailyRecords {
                // If the record came from another device (e.g. Android Health Connect), write it to Apple Health
                if remote.sourceDevice != "IOS_APPLE_HEALTH" {
                    guard let recordDate = dateFormatter.date(from: remote.recordDate) else { continue }
                    let startOfDay = calendar.startOfDay(for: recordDate)
                    let endOfDay = calendar.date(byAdding: .day, value: 1, to: startOfDay) ?? recordDate

                    if let steps = remote.steps, steps > 0 {
                        _ = await healthKitManager.writeSteps(count: steps, start: startOfDay, end: endOfDay, externalId: "sync_steps_\(remote.recordDate)")
                    }
                    if let calories = remote.activeCalories, calories > 0 {
                        _ = await healthKitManager.writeActiveCalories(kcal: calories, start: startOfDay, end: endOfDay, externalId: "sync_cal_\(remote.recordDate)")
                    }
                    if let dist = remote.distanceMeters, dist > 0 {
                        _ = await healthKitManager.writeDistance(meters: dist, start: startOfDay, end: endOfDay, externalId: "sync_dist_\(remote.recordDate)")
                    }
                    if let hr = remote.latestHeartRateBpm, hr > 0 {
                        _ = await healthKitManager.writeHeartRate(bpm: hr, date: recordDate, externalId: "sync_hr_\(remote.recordDate)")
                    }
                    if let sStart = remote.sleepStartTime, let sEnd = remote.sleepEndTime,
                       let startDate = isoFormatter.date(from: sStart),
                       let endDate = isoFormatter.date(from: sEnd) {
                        _ = await healthKitManager.writeSleep(start: startDate, end: endDate, externalId: "sync_sleep_\(remote.recordDate)")
                    }
                    if let weight = remote.weightKg, weight > 0 {
                        _ = await healthKitManager.writeWeight(kg: weight, date: recordDate)
                    }
                }
            }

            // 5. Ingest incoming remote workouts from backend into Apple Health and local store
            for rw in response.remoteWorkouts {
                if rw.sourceDevice != "IOS_APPLE_HEALTH" {
                    let start = isoFormatter.date(from: rw.startTime) ?? today
                    let end = isoFormatter.date(from: rw.endTime) ?? today
                    let duration = rw.durationMinutes ?? max(1, Int(end.timeIntervalSince(start) / 60))

                    // Write to Apple Health
                    _ = await healthKitManager.writeSyncedWorkout(
                        title: rw.title,
                        start: start,
                        end: end,
                        caloriesBurned: rw.caloriesBurned,
                        setCount: rw.setCount,
                        totalReps: rw.totalReps,
                        totalVolumeKg: rw.totalVolumeKg,
                        externalId: rw.externalId
                    )

                    // Write to local workout log if not present
                    let exists = WorkoutStore.shared.loggedWorkouts.contains { $0.id.uuidString == rw.externalId }
                    if !exists {
                        let parsedUuid = UUID(uuidString: rw.externalId) ?? UUID()
                        let newWorkout = LoggedWorkout(
                            id: parsedUuid,
                            exerciseId: rw.title.lowercased().replacingOccurrences(of: " ", with: "_"),
                            exerciseName: rw.title,
                            date: start,
                            sets: (1...(rw.setCount ?? 1)).map { _ in
                                WorkoutSet(
                                    reps: rw.totalReps ?? 10,
                                    weightKg: (rw.totalVolumeKg ?? 0) / Double(max(1, rw.totalReps ?? 10))
                                )
                            },
                            durationMinutes: duration,
                            notes: "Synced from \(rw.sourceDevice ?? "Android")",
                            syncedToHealthKit: true
                        )
                        WorkoutStore.shared.logWorkout(newWorkout)
                    }
                }
            }

            // Save new sync checkpoint
            let nowIso = isoFormatter.string(from: today)
            defaults.set(nowIso, forKey: keyLastSync)
            self.lastSyncDate = today
            return true
        } catch {
            print("HealthSyncManager error: \(error.localizedDescription)")
            self.lastSyncError = error.localizedDescription
            return false
        }
    }
}
