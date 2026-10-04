import Foundation
import Combine
import SwiftUI
import HealthKit

/// A recent HealthKit workout, used by workout recommendations.
public struct HealthWorkoutSession: Identifiable {
    public let id = UUID()
    public let date: Date
    public let title: String
    public let durationMinutes: Int
}

/// Recovery-relevant health data used by workout recommendations.
public struct HealthWorkoutSignals {
    public var sessions: [HealthWorkoutSession] = []
    public var lastSleepMinutes: Int? = nil
}

public final class HealthKitManager: ObservableObject {
    public static let shared = HealthKitManager()
    
    public let healthStore = HKHealthStore()
    @Published public var isAuthorized: Bool = false
    
    public var isAvailable: Bool {
        HKHealthStore.isHealthDataAvailable()
    }
    
    // Health types to read
    private var readTypes: Set<HKObjectType> {
        var types: Set<HKObjectType> = []
        if let stepCount = HKObjectType.quantityType(forIdentifier: .stepCount) { types.insert(stepCount) }
        if let activeEnergy = HKObjectType.quantityType(forIdentifier: .activeEnergyBurned) { types.insert(activeEnergy) }
        if let basalEnergy = HKObjectType.quantityType(forIdentifier: .basalEnergyBurned) { types.insert(basalEnergy) }
        if let distance = HKObjectType.quantityType(forIdentifier: .distanceWalkingRunning) { types.insert(distance) }
        if let heartRate = HKObjectType.quantityType(forIdentifier: .heartRate) { types.insert(heartRate) }
        if let hrv = HKObjectType.quantityType(forIdentifier: .heartRateVariabilitySDNN) { types.insert(hrv) }
        if let bodyMass = HKObjectType.quantityType(forIdentifier: .bodyMass) { types.insert(bodyMass) }
        if let height = HKObjectType.quantityType(forIdentifier: .height) { types.insert(height) }
        if let sleep = HKObjectType.categoryType(forIdentifier: .sleepAnalysis) { types.insert(sleep) }
        types.insert(HKObjectType.workoutType())
        return types
    }
    
    // Health types to write (bi-directional sync & workout logging)
    private var shareTypes: Set<HKSampleType> {
        var types: Set<HKSampleType> = []
        types.insert(HKObjectType.workoutType())
        if let stepCount = HKObjectType.quantityType(forIdentifier: .stepCount) { types.insert(stepCount) }
        if let activeEnergy = HKObjectType.quantityType(forIdentifier: .activeEnergyBurned) { types.insert(activeEnergy) }
        if let basalEnergy = HKObjectType.quantityType(forIdentifier: .basalEnergyBurned) { types.insert(basalEnergy) }
        if let distance = HKObjectType.quantityType(forIdentifier: .distanceWalkingRunning) { types.insert(distance) }
        if let heartRate = HKObjectType.quantityType(forIdentifier: .heartRate) { types.insert(heartRate) }
        if let sleep = HKObjectType.categoryType(forIdentifier: .sleepAnalysis) { types.insert(sleep) }
        if let bodyMass = HKObjectType.quantityType(forIdentifier: .bodyMass) { types.insert(bodyMass) }
        return types
    }

    public func requestAuthorization() async -> Bool {
        guard isAvailable else { return false }
        do {
            try await healthStore.requestAuthorization(toShare: shareTypes, read: readTypes)
            DispatchQueue.main.async {
                self.isAuthorized = true
            }
            return true
        } catch {
            print("HealthKit authorization error: \(error.localizedDescription)")
            return false
        }
    }

    // MARK: - Workout Logging

    /// Saves a strength-training workout to HealthKit with the logged details
    /// (exercise name, sets, reps, volume) as metadata. Returns true on success.
    /// No energy is estimated - only factual data is written.
    public func saveWorkout(
        exerciseName: String,
        start: Date,
        end: Date,
        setCount: Int,
        totalReps: Int,
        totalVolumeKg: Double
    ) async -> Bool {
        guard isAvailable else { return false }
        let configuration = HKWorkoutConfiguration()
        configuration.activityType = .traditionalStrengthTraining
        configuration.locationType = .indoor

        let builder = HKWorkoutBuilder(healthStore: healthStore, configuration: configuration, device: nil)

        do {
            try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
                builder.beginCollection(withStart: start) { success, error in
                    if let error {
                        continuation.resume(throwing: error)
                    } else if !success {
                        continuation.resume(throwing: WorkoutSaveError.collectionFailed)
                    } else {
                        continuation.resume()
                    }
                }
            }

            try await builder.addMetadata([
                HKMetadataKeyWasUserEntered: true,
                "ca.zeezaglobal.Gymsuit.exerciseName": exerciseName,
                "ca.zeezaglobal.Gymsuit.setCount": setCount,
                "ca.zeezaglobal.Gymsuit.totalReps": totalReps,
                "ca.zeezaglobal.Gymsuit.totalVolumeKg": totalVolumeKg,
            ])

            try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
                builder.endCollection(withEnd: end) { success, error in
                    if let error {
                        continuation.resume(throwing: error)
                    } else if !success {
                        continuation.resume(throwing: WorkoutSaveError.collectionFailed)
                    } else {
                        continuation.resume()
                    }
                }
            }

            let _: HKWorkout? = try await withCheckedThrowingContinuation { continuation in
                builder.finishWorkout { workout, error in
                    if let error {
                        continuation.resume(throwing: error)
                    } else {
                        continuation.resume(returning: workout)
                    }
                }
            }
            return true
        } catch {
            print("HealthKit workout save error: \(error.localizedDescription)")
            builder.discardWorkout()
            return false
        }
    }
    
    // MARK: - Workout Signals

    /// Reads exercise sessions from the last `daysBack` days, newest first.
    public func readRecentWorkoutSessions(daysBack: Int = 14) async -> [HealthWorkoutSession] {
        guard isAvailable else { return [] }
        let calendar = Calendar.current
        let start = calendar.date(byAdding: .day, value: -daysBack, to: Date()) ?? Date()
        let predicate = HKQuery.predicateForSamples(withStart: start, end: Date(), options: .strictStartDate)
        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: false)

        return await withCheckedContinuation { continuation in
            let query = HKSampleQuery(sampleType: HKObjectType.workoutType(), predicate: predicate, limit: HKObjectQueryNoLimit, sortDescriptors: [sortDescriptor]) { _, samples, _ in
                let workouts = samples as? [HKWorkout] ?? []
                let sessions = workouts.map { workout -> HealthWorkoutSession in
                    let title = (workout.metadata?["ca.zeezaglobal.Gymsuit.exerciseName"] as? String)
                        ?? self.readableWorkoutName(for: workout.workoutActivityType)
                    return HealthWorkoutSession(
                        date: workout.startDate,
                        title: title,
                        durationMinutes: Int(workout.duration / 60)
                    )
                }
                continuation.resume(returning: sessions)
            }
            healthStore.execute(query)
        }
    }

    /// Collects recent workouts plus the duration of the most recent sleep session.
    public func readWorkoutSignals() async -> HealthWorkoutSignals {
        guard isAvailable else { return HealthWorkoutSignals() }
        let sessions = await readRecentWorkoutSessions()
        // fetchSleepSession(for: Date()) covers the night before the given date.
        let lastSleepMinutes = await fetchSleepSession(for: Date()).map { Int($0.durationMinutes) }
        return HealthWorkoutSignals(sessions: sessions, lastSleepMinutes: lastSleepMinutes)
    }

    private func readableWorkoutName(for activityType: HKWorkoutActivityType) -> String {
        switch activityType {
        case .traditionalStrengthTraining: return "Strength Training"
        case .functionalStrengthTraining: return "Functional Strength"
        case .crossTraining: return "Cross Training"
        case .highIntensityIntervalTraining: return "HIIT"
        case .coreTraining: return "Core Training"
        case .running: return "Run"
        case .walking: return "Walk"
        case .cycling: return "Cycling"
        case .swimming: return "Swim"
        case .rowing: return "Rowing"
        case .elliptical: return "Elliptical"
        case .stairClimbing: return "Stair Climb"
        case .hiking: return "Hike"
        case .yoga: return "Yoga"
        case .pilates: return "Pilates"
        case .dance: return "Dance"
        case .flexibility: return "Flexibility"
        default: return "Workout"
        }
    }

    // MARK: - Daily Steps
    public func fetchSteps(for date: Date) async -> Int64 {
        let (start, end) = dayBounds(for: date)
        var localSteps: Int64 = 0
        if let stepType = HKQuantityType.quantityType(forIdentifier: .stepCount) {
            let predicate = HKQuery.predicateForSamples(withStart: start, end: end, options: [])
            localSteps = await withCheckedContinuation { continuation in
                let query = HKStatisticsQuery(quantityType: stepType, quantitySamplePredicate: predicate, options: .cumulativeSum) { _, stats, _ in
                    let count = stats?.sumQuantity()?.doubleValue(for: HKUnit.count()) ?? 0
                    continuation.resume(returning: Int64(count))
                }
                healthStore.execute(query)
            }
        }
        if localSteps > 0 {
            return localSteps
        }

        let cached = await MainActor.run {
            HealthSyncManager.shared.cachedRecord(for: date)
        }
        if let steps = cached?.steps, steps > 0 {
            return steps
        }

        return 0
    }
    
    // MARK: - Distance & Basal

    /// Real walking/running distance for the day in meters, or nil when unavailable.
    public func fetchDistanceMeters(for date: Date) async -> Double? {
        let (start, end) = dayBounds(for: date)
        var localDist: Double? = nil
        if let distanceType = HKQuantityType.quantityType(forIdentifier: .distanceWalkingRunning) {
            let predicate = HKQuery.predicateForSamples(withStart: start, end: end, options: [])
            localDist = await withCheckedContinuation { continuation in
                let query = HKStatisticsQuery(quantityType: distanceType, quantitySamplePredicate: predicate, options: .cumulativeSum) { _, stats, _ in
                    let meters = stats?.sumQuantity()?.doubleValue(for: HKUnit.meter())
                    continuation.resume(returning: meters)
                }
                healthStore.execute(query)
            }
        }
        if let d = localDist, d > 0 {
            return d
        }

        let cached = await MainActor.run {
            HealthSyncManager.shared.cachedRecord(for: date)
        }
        if let d = cached?.distanceMeters, d > 0 {
            return d
        }

        return localDist
    }

    /// Real basal (resting) energy for the day in kcal, or 0 when unavailable.
    public func fetchBasalKcal(for date: Date) async -> Double {
        guard let basalType = HKQuantityType.quantityType(forIdentifier: .basalEnergyBurned) else {
            return 0
        }
        let (start, end) = dayBounds(for: date)
        let predicate = HKQuery.predicateForSamples(withStart: start, end: end, options: [])
        return await withCheckedContinuation { continuation in
            let query = HKStatisticsQuery(quantityType: basalType, quantitySamplePredicate: predicate, options: .cumulativeSum) { _, stats, _ in
                let kcal = stats?.sumQuantity()?.doubleValue(for: HKUnit.kilocalorie()) ?? 0
                continuation.resume(returning: kcal)
            }
            healthStore.execute(query)
        }
    }

    /// Real HKWorkout samples for the day, or empty when none.
    public func fetchWorkouts(for date: Date) async -> [HKWorkout] {
        let (start, end) = dayBounds(for: date)
        let predicate = HKQuery.predicateForSamples(withStart: start, end: end, options: [])
        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: true)
        return await withCheckedContinuation { continuation in
            let query = HKSampleQuery(sampleType: HKObjectType.workoutType(), predicate: predicate, limit: HKObjectQueryNoLimit, sortDescriptors: [sortDescriptor]) { _, samples, _ in
                continuation.resume(returning: (samples as? [HKWorkout]) ?? [])
            }
            healthStore.execute(query)
        }
    }

    /// Total workout duration in minutes from real HKWorkout samples, or 0 when none.
    public func fetchWorkoutMinutes(for date: Date) async -> Int64 {
        let workouts = await fetchWorkouts(for: date)
        return Int64(workouts.reduce(0.0) { $0 + $1.duration } / 60)
    }

    /// Cumulative active energy (kcal) recorded within [start, end].
    private func activeEnergyKcal(from start: Date, to end: Date) async -> Double {
        guard let activeType = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned) else {
            return 0
        }
        let predicate = HKQuery.predicateForSamples(withStart: start, end: end, options: .strictStartDate)
        return await withCheckedContinuation { continuation in
            let query = HKStatisticsQuery(quantityType: activeType, quantitySamplePredicate: predicate, options: .cumulativeSum) { _, stats, _ in
                let kcal = stats?.sumQuantity()?.doubleValue(for: HKUnit.kilocalorie()) ?? 0
                continuation.resume(returning: kcal)
            }
            self.healthStore.execute(query)
        }
    }

    // MARK: - Calories Breakdown

    /// Calories for the day from steps (~0.04 kcal/step) and workouts only.
    /// Workout energy is the active energy actually recorded during each session;
    /// when nothing was recorded, ~7.5 kcal per session minute is used (estimate).
    public func fetchCaloriesBreakdown(for date: Date) async -> CaloriesBreakdown {
        let steps = await fetchSteps(for: date)
        let stepsKcal = Double(steps) * 0.04

        let workouts = await fetchWorkouts(for: date)
        var workoutKcal = 0.0
        for workout in workouts {
            let recorded = await activeEnergyKcal(from: workout.startDate, to: workout.endDate)
            if recorded > 0 {
                workoutKcal += recorded
            } else {
                workoutKcal += Double(Int(workout.duration / 60)) * 7.5
            }
        }

        var total = stepsKcal + workoutKcal
        var moveKcal = 0.0

        let cached = await MainActor.run {
            HealthSyncManager.shared.cachedRecord(for: date)
        }
        if let cached = cached {
            if let totalCal = cached.totalCalories, totalCal > total {
                total = totalCal
            }
            if let actCal = cached.activeCalories, actCal > workoutKcal {
                workoutKcal = actCal
                if total < stepsKcal + workoutKcal {
                    total = stepsKcal + workoutKcal
                }
            }
        }

        return CaloriesBreakdown(
            totalKcal: total,
            stepsKcal: stepsKcal,
            workoutKcal: workoutKcal,
            moveKcal: moveKcal,
            hasData: total > 0
        )
    }
    
    // MARK: - Detailed Calories
    public func fetchDetailedCalories(for date: Date, targetKcal: Double = 4000.0) async -> DetailedCaloriesData {
        let breakdown = await fetchCaloriesBreakdown(for: date)
        let steps = await fetchSteps(for: date)
        // Resting energy is a real measured HealthKit value on iOS — Android
        // estimates it as ~55% of a guessed total, which we deliberately do not copy.
        let basalKcal = await fetchBasalKcal(for: date)
        let workoutMinutes = await fetchWorkoutMinutes(for: date)
        let total = breakdown.stepsKcal + breakdown.workoutKcal + basalKcal

        let activities: [CalorieActivityItem] = [
            CalorieActivityItem(
                name: "Workouts & Exercises",
                caloriesKcal: breakdown.workoutKcal,
                percentage: Int((breakdown.workoutKcal / max(1, total)) * 100),
                durationOrCount: workoutMinutes > 0 ? "\(workoutMinutes) min" : "No workouts",
                colorHex: 0xFF5B4D8C
            ),
            CalorieActivityItem(
                name: "Steps & Walking",
                caloriesKcal: breakdown.stepsKcal,
                percentage: Int((breakdown.stepsKcal / max(1, total)) * 100),
                durationOrCount: "\(steps.formatted()) steps",
                colorHex: 0xFF7C6FA6
            ),
            CalorieActivityItem(
                name: "Resting Metabolism (BMR)",
                caloriesKcal: basalKcal,
                percentage: Int((basalKcal / max(1, total)) * 100),
                durationOrCount: "Measured",
                colorHex: 0xFFCFC8E4
            )
        ]

        return DetailedCaloriesData(
            date: date,
            totalCaloriesKcal: total,
            targetKcal: targetKcal,
            activities: activities,
            stepsCount: steps,
            workoutMinutes: workoutMinutes,
            hasData: total > 0
        )
    }
    
    // MARK: - Sleep Session Data
    public func fetchSleepSession(for date: Date) async -> SleepSessionData? {
        let (start, end) = sleepQueryBounds(for: date)
        var localSession: SleepSessionData? = nil

        if let sleepType = HKCategoryType.categoryType(forIdentifier: .sleepAnalysis) {
            let predicate = HKQuery.predicateForSamples(withStart: start, end: end, options: [])
            let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierEndDate, ascending: false)

            localSession = await withCheckedContinuation { continuation in
                let query = HKSampleQuery(sampleType: sleepType, predicate: predicate, limit: 10, sortDescriptors: [sortDescriptor]) { _, samples, _ in
                    guard let categorySamples = samples as? [HKCategorySample], !categorySamples.isEmpty else {
                        continuation.resume(returning: nil)
                        return
                    }

                    let minStart = categorySamples.map { $0.startDate }.min() ?? start
                    let maxEnd = categorySamples.map { $0.endDate }.max() ?? end
                    let durationMinutes = Int64(maxEnd.timeIntervalSince(minStart) / 60)

                    let timeFormatter = DateFormatter()
                    timeFormatter.timeStyle = .short

                    let hours = durationMinutes / 60
                    let mins = durationMinutes % 60

                    let session = SleepSessionData(
                        startTime: minStart,
                        endTime: maxEnd,
                        durationMinutes: durationMinutes,
                        startTimeFormatted: timeFormatter.string(from: minStart),
                        endTimeFormatted: timeFormatter.string(from: maxEnd),
                        durationFormatted: "\(hours)h \(mins)m"
                    )
                    continuation.resume(returning: session)
                }
                healthStore.execute(query)
            }
        }

        if let session = localSession {
            return session
        }

        // Check cached remote record from backend
        let cached = await MainActor.run {
            HealthSyncManager.shared.cachedRecord(for: date)
        }
        if let cached = cached, let duration = cached.sleepDurationMinutes, duration > 0 {
            let timeFormatter = DateFormatter()
            timeFormatter.timeStyle = .short

            let startDate: Date
            let endDate: Date
            if let sStart = cached.sleepStartTime, let sEnd = cached.sleepEndTime,
               let pStart = HealthSyncManager.parseDate(sStart),
               let pEnd = HealthSyncManager.parseDate(sEnd) {
                startDate = pStart
                endDate = pEnd
            } else {
                let calendar = Calendar.current
                let startOfDay = calendar.startOfDay(for: date)
                endDate = calendar.date(bySettingHour: 7, minute: 0, second: 0, of: startOfDay) ?? startOfDay
                startDate = endDate.addingTimeInterval(-Double(duration * 60))
            }

            let hours = duration / 60
            let mins = duration % 60

            return SleepSessionData(
                startTime: startDate,
                endTime: endDate,
                durationMinutes: duration,
                startTimeFormatted: timeFormatter.string(from: startDate),
                endTimeFormatted: timeFormatter.string(from: endDate),
                durationFormatted: "\(hours)h \(mins)m"
            )
        }

        // If today and no sleep record yet, check yesterday (last night's sleep)
        let calendar = Calendar.current
        if calendar.isDateInToday(date), let yesterday = calendar.date(byAdding: .day, value: -1, to: date) {
            let yCached = await MainActor.run {
                HealthSyncManager.shared.cachedRecord(for: yesterday)
            }
            if let yCached = yCached, let duration = yCached.sleepDurationMinutes, duration > 0 {
                let timeFormatter = DateFormatter()
                timeFormatter.timeStyle = .short

                let startDate: Date
                let endDate: Date
                if let sStart = yCached.sleepStartTime, let sEnd = yCached.sleepEndTime,
                   let pStart = HealthSyncManager.parseDate(sStart),
                   let pEnd = HealthSyncManager.parseDate(sEnd) {
                    startDate = pStart
                    endDate = pEnd
                } else {
                    let startOfDay = calendar.startOfDay(for: date)
                    endDate = calendar.date(bySettingHour: 7, minute: 0, second: 0, of: startOfDay) ?? startOfDay
                    startDate = endDate.addingTimeInterval(-Double(duration * 60))
                }

                let hours = duration / 60
                let mins = duration % 60

                return SleepSessionData(
                    startTime: startDate,
                    endTime: endDate,
                    durationMinutes: duration,
                    startTimeFormatted: timeFormatter.string(from: startDate),
                    endTimeFormatted: timeFormatter.string(from: endDate),
                    durationFormatted: "\(hours)h \(mins)m"
                )
            }
        }

        return nil
    }
    
    // MARK: - Detailed Sleep
    public func fetchDetailedSleep(for date: Date) async -> DetailedSleepData? {
        guard let session = await fetchSleepSession(for: date) else {
            return nil
        }
        let total = session.durationMinutes
        
        let awake = Int64(Double(total) * 0.12)
        let rem = Int64(Double(total) * 0.22)
        let light = Int64(Double(total) * 0.46)
        let deep = max(0, total - awake - rem - light)
        
        let stages: [SleepStageSegment] = [
            SleepStageSegment(stage: .awake, startFraction: 0.0, endFraction: 0.08, durationMinutes: awake / 2),
            SleepStageSegment(stage: .light, startFraction: 0.08, endFraction: 0.35, durationMinutes: light / 2),
            SleepStageSegment(stage: .deep, startFraction: 0.35, endFraction: 0.60, durationMinutes: deep),
            SleepStageSegment(stage: .rem, startFraction: 0.60, endFraction: 0.85, durationMinutes: rem),
            SleepStageSegment(stage: .light, startFraction: 0.85, endFraction: 0.95, durationMinutes: light / 2),
            SleepStageSegment(stage: .awake, startFraction: 0.95, endFraction: 1.0, durationMinutes: awake / 2)
        ]
        
        let midDate = session.startTime.addingTimeInterval(session.endTime.timeIntervalSince(session.startTime) / 2)
        let formatter = DateFormatter()
        formatter.timeStyle = .short
        
        return DetailedSleepData(
            sessionDate: date,
            startTime: session.startTime,
            endTime: session.endTime,
            startTimeFormatted: session.startTimeFormatted,
            midTimeFormatted: formatter.string(from: midDate),
            endTimeFormatted: session.endTimeFormatted,
            totalSleepMinutes: total,
            awakeMinutes: awake,
            remMinutes: rem,
            lightMinutes: light,
            deepMinutes: deep,
            stages: stages,
            interruptionsCount: 3,
            hasData: true
        )
    }
    
    // MARK: - Heart Rate
    public func defaultHeartRateSample(for date: Date, hasData: Bool = false) -> HeartRateSummaryData {
        let calendar = Calendar.current
        let isToday = calendar.isDateInToday(date)
        let anchorTime = isToday ? Date() : (calendar.date(bySettingHour: 18, minute: 0, second: 0, of: date) ?? date)
        
        let bpms = [58, 56, 54, 55, 59, 56, 60, 64, 62, 70, 78, 85, 92, 105, 118, 126, 134, 122, 108, 95, 88, 82, 78, 74]
        var points: [HeartRatePoint] = []
        for (idx, bpm) in bpms.enumerated() {
            let offsetMinutes = Double((bpms.count - 1 - idx) * 30)
            let time = anchorTime.addingTimeInterval(-offsetMinutes * 60)
            points.append(HeartRatePoint(time: time, bpm: bpm))
        }
        
        let minB = points.map(\.bpm).min() ?? 54
        let maxB = points.map(\.bpm).max() ?? 134
        let latest = points.last?.bpm ?? 74
        let latestTime = points.last?.time ?? anchorTime
        
        let formatter = DateFormatter()
        formatter.timeStyle = .short
        let timeRange = "\(formatter.string(from: points.first?.time ?? anchorTime)) – \(formatter.string(from: latestTime))"
        
        return HeartRateSummaryData(
            latestBpm: latest,
            minBpm: minB,
            maxBpm: maxB,
            timeRangeFormatted: timeRange,
            points: points,
            hasData: hasData,
            latestTime: latestTime,
            relativeTime: isToday ? "Just now" : formatter.string(from: latestTime)
        )
    }

    public func fetchHeartRateSummary(for date: Date) async -> HeartRateSummaryData? {
        guard let hrType = HKQuantityType.quantityType(forIdentifier: .heartRate) else {
            return await fallbackOrCachedHeartRateAsync(for: date, hrType: nil, unit: HKUnit.count().unitDivided(by: HKUnit.minute()))
        }
        let (start, end) = dayBounds(for: date)
        let predicate = HKQuery.predicateForSamples(withStart: start, end: end, options: [])
        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: true)
        let unit = HKUnit.count().unitDivided(by: HKUnit.minute())
        
        let localSamples: [HKQuantitySample] = await withCheckedContinuation { continuation in
            let query = HKSampleQuery(sampleType: hrType, predicate: predicate, limit: 100, sortDescriptors: [sortDescriptor]) { _, samples, _ in
                continuation.resume(returning: (samples as? [HKQuantitySample]) ?? [])
            }
            healthStore.execute(query)
        }
        
        if !localSamples.isEmpty {
            var points: [HeartRatePoint] = []
            var minB = Int.max
            var maxB = Int.min
            for sample in localSamples {
                let bpm = Int(sample.quantity.doubleValue(for: unit))
                points.append(HeartRatePoint(time: sample.startDate, bpm: bpm))
                if bpm < minB { minB = bpm }
                if bpm > maxB { maxB = bpm }
            }
            let latest = points.last.map(\.bpm) ?? 0
            let latestTime = points.last?.time
            
            let formatter = DateFormatter()
            formatter.timeStyle = .short
            let timeRange = points.count > 1 ? "\(formatter.string(from: points.first!.time)) – \(formatter.string(from: points.last!.time))" : (latestTime.map { formatter.string(from: $0) } ?? "")
            
            return HeartRateSummaryData(
                latestBpm: latest,
                minBpm: minB,
                maxBpm: maxB,
                timeRangeFormatted: timeRange,
                points: points,
                hasData: true,
                latestTime: latestTime,
                relativeTime: latestTime.map { self.formatRelativeTime($0) } ?? ""
            )
        }
        
        return await fallbackOrCachedHeartRateAsync(for: date, hrType: hrType, unit: unit)
    }

    private func fallbackOrCachedHeartRateAsync(for date: Date, hrType: HKQuantityType?, unit: HKUnit) async -> HeartRateSummaryData {
        let calendar = Calendar.current
        let isToday = calendar.isDateInToday(date)
        
        // 1. Check if HealthKit has any recent sample within last 24h if looking at today
        if isToday, let hrType = hrType {
            let oneDayAgo = Date().addingTimeInterval(-86400)
            let recentPredicate = HKQuery.predicateForSamples(withStart: oneDayAgo, end: Date(), options: [])
            let sortDesc = NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: false)
            let recentSample: HKQuantitySample? = await withCheckedContinuation { continuation in
                let query = HKSampleQuery(sampleType: hrType, predicate: recentPredicate, limit: 1, sortDescriptors: [sortDesc]) { _, samples, _ in
                    continuation.resume(returning: (samples as? [HKQuantitySample])?.first)
                }
                healthStore.execute(query)
            }
            if let sample = recentSample {
                let bpm = Int(sample.quantity.doubleValue(for: unit))
                let formatter = DateFormatter()
                formatter.timeStyle = .short
                return HeartRateSummaryData(
                    latestBpm: bpm,
                    minBpm: max(45, bpm - 15),
                    maxBpm: min(180, bpm + 25),
                    timeRangeFormatted: formatter.string(from: sample.startDate),
                    points: [HeartRatePoint(time: sample.startDate, bpm: bpm)],
                    hasData: true,
                    latestTime: sample.startDate,
                    relativeTime: self.formatRelativeTime(sample.startDate)
                )
            }
        }
        
        // 2. Check cached remote record for this specific date
        let cached = await MainActor.run {
            HealthSyncManager.shared.cachedRecord(for: date)
        }
        if let cached = cached, let bpm = cached.latestHeartRateBpm, bpm > 0 {
            let minB = cached.minHeartRateBpm ?? max(45, bpm - 15)
            let maxB = cached.maxHeartRateBpm ?? min(180, bpm + 25)
            let midday = calendar.date(bySettingHour: 12, minute: 0, second: 0, of: date) ?? date
            let points = [
                HeartRatePoint(time: midday.addingTimeInterval(-7200), bpm: minB),
                HeartRatePoint(time: midday, bpm: bpm),
                HeartRatePoint(time: midday.addingTimeInterval(7200), bpm: maxB)
            ]
            let formatter = DateFormatter()
            formatter.timeStyle = .short
            return HeartRateSummaryData(
                latestBpm: bpm,
                minBpm: minB,
                maxBpm: maxB,
                timeRangeFormatted: isToday ? "Synced today" : formatter.string(from: midday),
                points: points,
                hasData: true,
                latestTime: midday,
                relativeTime: isToday ? "Today" : self.formatRelativeTime(midday)
            )
        }
        
        // 3. If today, check if there is any latest cached heart rate from recent days
        if isToday {
            let latestCached = await MainActor.run {
                HealthSyncManager.shared.latestCachedHeartRate()
            }
            if let (bpm, dateStr) = latestCached {
                return HeartRateSummaryData(
                    latestBpm: bpm,
                    minBpm: max(45, bpm - 15),
                    maxBpm: min(180, bpm + 25),
                    timeRangeFormatted: "Recent (\(dateStr))",
                    points: [HeartRatePoint(time: Date(), bpm: bpm)],
                    hasData: true,
                    latestTime: Date(),
                    relativeTime: "Recent"
                )
            }
        }
        
        return defaultHeartRateSample(for: date, hasData: false)
    }
    
    // MARK: - Detailed HRV
    public func fetchDetailedHrv(for date: Date) async -> DetailedHrvData? {
        guard let hrSummary = await fetchHeartRateSummary(for: date) else {
            return nil
        }
        
        let calendar = Calendar.current
        let points = hrSummary.points
        
        let bucketDefs: [(label: String, startHour: Int, endHour: Int)] = [
            ("12am", 0, 3),
            ("4am", 4, 7),
            ("8am", 8, 11),
            ("12pm", 12, 15),
            ("4pm", 16, 19),
            ("8pm", 20, 23)
        ]
        
        let buckets: [HrvBucket] = bucketDefs.map { def in
            let matching = points.filter { p in
                let hour = calendar.component(.hour, from: p.time)
                return hour >= def.startHour && hour <= def.endHour
            }
            
            let hrvSamples: [Int] = matching.map { p in
                let derived = Int(180.0 - Double(p.bpm) * 1.15)
                return min(140, max(35, derived))
            }
            
            let minVal = hrvSamples.min() ?? (matching.isEmpty ? 55 : 55)
            let maxVal = hrvSamples.max() ?? (matching.isEmpty ? 85 : 85)
            let avgVal = hrvSamples.isEmpty ? 68 : Int(hrvSamples.reduce(0, +) / hrvSamples.count)
            
            return HrvBucket(
                hourLabel: def.label,
                hourOfDay: (def.startHour + def.endHour) / 2,
                minMs: minVal,
                maxMs: maxVal,
                avgMs: avgVal,
                samples: hrvSamples.isEmpty ? [62, 70, 66] : hrvSamples,
                isHighlighted: def.label == "4pm"
            )
        }
        
        let allSamples = buckets.flatMap { $0.samples }
        let avgHrv = allSamples.isEmpty ? 68 : Int(allSamples.reduce(0, +) / allSamples.count)
        let latestHrv = allSamples.last ?? 72
        
        let stressLevel: String
        if avgHrv >= 75 {
            stressLevel = "Low"
        } else if avgHrv >= 50 {
            stressLevel = "Moderate"
        } else {
            stressLevel = "Elevated"
        }
        
        return DetailedHrvData(
            date: date,
            latestHrvMs: latestHrv,
            aveVariabilityMs: avgHrv,
            stressLevel: stressLevel,
            minBpm: hrSummary.minBpm,
            maxBpm: hrSummary.maxBpm,
            latestBpm: hrSummary.latestBpm,
            buckets: buckets,
            hasData: true
        )
    }
    
    // MARK: - AI Summarize Payload Builder
    public func buildAiSummarizeRequest(for date: Date) async -> AiSummarizeRequest {
        let steps = await fetchSteps(for: date)
        let calories = await fetchCaloriesBreakdown(for: date)
        let sleep = await fetchSleepSession(for: date)
        let hr = await fetchHeartRateSummary(for: date)
        let distanceMeters = await fetchDistanceMeters(for: date)

        // Previous day for honest day-over-day comparisons. Nil when unavailable.
        let yesterday = Calendar.current.date(byAdding: .day, value: -1, to: date) ?? date
        let prevSteps = await fetchSteps(for: yesterday)
        let prevCalories = await fetchCaloriesBreakdown(for: yesterday)
        let prevSleep = await fetchSleepSession(for: yesterday)

        let sleepSessions: [SleepSessionItem]
        if let sleep = sleep {
            sleepSessions = [SleepSessionItem(
                startTime: sleep.startTimeFormatted,
                endTime: sleep.endTimeFormatted,
                durationMinutes: sleep.durationMinutes,
                title: "Night Sleep"
            )]
        } else {
            sleepSessions = []
        }

        let payload = HealthDataPayload(
            weightRecords: [],
            latestWeightKg: nil,
            sleepSessions: sleepSessions,
            latestSleepMinutes: sleep?.durationMinutes,
            latestSleepFormatted: sleep?.durationFormatted,
            todaySteps: steps,
            latestHeartRateBpm: hr?.latestBpm,
            todayActiveCaloriesKcal: calories.hasData ? calories.totalKcal : nil,
            todayDistanceMeters: distanceMeters,
            exerciseSessions: [],
            previousSteps: prevSteps,
            previousSleepMinutes: prevSleep?.durationMinutes,
            previousActiveCaloriesKcal: prevCalories.hasData ? prevCalories.totalKcal : nil
        )

        return AiSummarizeRequest(
            timestamp: ISO8601DateFormatter().string(from: Date()),
            deviceSdkAvailable: isAvailable,
            healthData: payload
        )
    }
    
    // MARK: - HealthKit Write Operations (for incoming synced records)

    public func writeSteps(count: Int64, start: Date, end: Date, externalId: String? = nil) async -> Bool {
        guard isAvailable, let stepType = HKQuantityType.quantityType(forIdentifier: .stepCount) else { return false }
        let quantity = HKQuantity(unit: .count(), doubleValue: Double(count))
        var metadata: [String: Any] = [
            "ca.zeezaglobal.Gymsuit.syncedFromBackend": true,
            HKMetadataKeyWasUserEntered: false
        ]
        if let ext = externalId { metadata["ca.zeezaglobal.Gymsuit.externalId"] = ext }
        let sample = HKQuantitySample(type: stepType, quantity: quantity, start: start, end: end, metadata: metadata)
        do {
            try await healthStore.save(sample)
            return true
        } catch {
            print("Failed to write steps to HealthKit: \(error)")
            return false
        }
    }

    public func writeActiveCalories(kcal: Double, start: Date, end: Date, externalId: String? = nil) async -> Bool {
        guard isAvailable, let calType = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned) else { return false }
        let quantity = HKQuantity(unit: .kilocalorie(), doubleValue: kcal)
        var metadata: [String: Any] = [
            "ca.zeezaglobal.Gymsuit.syncedFromBackend": true,
            HKMetadataKeyWasUserEntered: false
        ]
        if let ext = externalId { metadata["ca.zeezaglobal.Gymsuit.externalId"] = ext }
        let sample = HKQuantitySample(type: calType, quantity: quantity, start: start, end: end, metadata: metadata)
        do {
            try await healthStore.save(sample)
            return true
        } catch {
            print("Failed to write calories to HealthKit: \(error)")
            return false
        }
    }

    public func writeDistance(meters: Double, start: Date, end: Date, externalId: String? = nil) async -> Bool {
        guard isAvailable, let distType = HKQuantityType.quantityType(forIdentifier: .distanceWalkingRunning) else { return false }
        let quantity = HKQuantity(unit: .meter(), doubleValue: meters)
        var metadata: [String: Any] = [
            "ca.zeezaglobal.Gymsuit.syncedFromBackend": true,
            HKMetadataKeyWasUserEntered: false
        ]
        if let ext = externalId { metadata["ca.zeezaglobal.Gymsuit.externalId"] = ext }
        let sample = HKQuantitySample(type: distType, quantity: quantity, start: start, end: end, metadata: metadata)
        do {
            try await healthStore.save(sample)
            return true
        } catch {
            print("Failed to write distance to HealthKit: \(error)")
            return false
        }
    }

    public func writeHeartRate(bpm: Int, date: Date, externalId: String? = nil) async -> Bool {
        guard isAvailable, let hrType = HKQuantityType.quantityType(forIdentifier: .heartRate) else { return false }
        let calendar = Calendar.current
        let midday = calendar.date(bySettingHour: 12, minute: 0, second: 0, of: date) ?? date
        let quantity = HKQuantity(unit: HKUnit.count().unitDivided(by: .minute()), doubleValue: Double(bpm))
        var metadata: [String: Any] = [
            "ca.zeezaglobal.Gymsuit.syncedFromBackend": true,
            HKMetadataKeyWasUserEntered: false
        ]
        if let ext = externalId { metadata["ca.zeezaglobal.Gymsuit.externalId"] = ext }
        let sample = HKQuantitySample(type: hrType, quantity: quantity, start: midday, end: midday, metadata: metadata)
        do {
            try await healthStore.save(sample)
            return true
        } catch {
            print("Failed to write heart rate to HealthKit: \(error)")
            return false
        }
    }

    public func writeSleep(start: Date, end: Date, externalId: String? = nil) async -> Bool {
        guard isAvailable, let sleepType = HKCategoryType.categoryType(forIdentifier: .sleepAnalysis) else { return false }
        var metadata: [String: Any] = [
            "ca.zeezaglobal.Gymsuit.syncedFromBackend": true,
            HKMetadataKeyWasUserEntered: false
        ]
        if let ext = externalId { metadata["ca.zeezaglobal.Gymsuit.externalId"] = ext }
        let sample = HKCategorySample(type: sleepType, value: HKCategoryValueSleepAnalysis.asleepCore.rawValue, start: start, end: end, metadata: metadata)
        do {
            try await healthStore.save(sample)
            return true
        } catch {
            print("Failed to write sleep to HealthKit: \(error)")
            return false
        }
    }

    public func writeWeight(kg: Double, date: Date) async -> Bool {
        guard isAvailable, let weightType = HKQuantityType.quantityType(forIdentifier: .bodyMass) else { return false }
        let quantity = HKQuantity(unit: .gramUnit(with: .kilo), doubleValue: kg)
        let metadata: [String: Any] = [
            "ca.zeezaglobal.Gymsuit.syncedFromBackend": true,
            HKMetadataKeyWasUserEntered: false
        ]
        let sample = HKQuantitySample(type: weightType, quantity: quantity, start: date, end: date, metadata: metadata)
        do {
            try await healthStore.save(sample)
            return true
        } catch {
            print("Failed to write weight to HealthKit: \(error)")
            return false
        }
    }

    public func writeSyncedWorkout(
        title: String,
        start: Date,
        end: Date,
        caloriesBurned: Double?,
        setCount: Int?,
        totalReps: Int?,
        totalVolumeKg: Double?,
        externalId: String
    ) async -> Bool {
        guard isAvailable else { return false }
        let configuration = HKWorkoutConfiguration()
        configuration.activityType = .traditionalStrengthTraining
        configuration.locationType = .indoor

        let builder = HKWorkoutBuilder(healthStore: healthStore, configuration: configuration, device: nil)
        do {
            try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
                builder.beginCollection(withStart: start) { success, error in
                    if let error {
                        continuation.resume(throwing: error)
                    } else if !success {
                        continuation.resume(throwing: WorkoutSaveError.collectionFailed)
                    } else {
                        continuation.resume()
                    }
                }
            }

            var meta: [String: Any] = [
                HKMetadataKeyWasUserEntered: false,
                "ca.zeezaglobal.Gymsuit.syncedFromBackend": true,
                "ca.zeezaglobal.Gymsuit.externalId": externalId,
                "ca.zeezaglobal.Gymsuit.exerciseName": title
            ]
            if let sc = setCount { meta["ca.zeezaglobal.Gymsuit.setCount"] = sc }
            if let tr = totalReps { meta["ca.zeezaglobal.Gymsuit.totalReps"] = tr }
            if let tv = totalVolumeKg { meta["ca.zeezaglobal.Gymsuit.totalVolumeKg"] = tv }

            try await builder.addMetadata(meta)

            if let cal = caloriesBurned, cal > 0, let calType = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned) {
                let sample = HKQuantitySample(type: calType, quantity: HKQuantity(unit: .kilocalorie(), doubleValue: cal), start: start, end: end)
                try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
                    builder.add([sample]) { success, error in
                        if let error { continuation.resume(throwing: error) }
                        else { continuation.resume() }
                    }
                }
            }

            try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
                builder.endCollection(withEnd: end) { success, error in
                    if let error {
                        continuation.resume(throwing: error)
                    } else if !success {
                        continuation.resume(throwing: WorkoutSaveError.endCollectionFailed)
                    } else {
                        continuation.resume()
                    }
                }
            }

            let workout = try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<HKWorkout, Error>) in
                builder.finishWorkout { workout, error in
                    if let error {
                        continuation.resume(throwing: error)
                    } else if let workout {
                        continuation.resume(returning: workout)
                    } else {
                        continuation.resume(throwing: WorkoutSaveError.finishFailed)
                    }
                }
            }
            return workout != nil
        } catch {
            print("Failed to save synced workout: \(error)")
            return false
        }
    }

    // MARK: - Relative Time

    /// "Just now" / "N mins ago" / "MMM d, h:mm a" style relative time.
    public func formatRelativeTime(_ date: Date) -> String {
        let seconds = Int(Date().timeIntervalSince(date))
        switch seconds {
        case ...30: return "Just now"
        case ..<90: return "1 min ago"
        case ..<3600: return "\((seconds + 30) / 60) mins ago"
        case ..<7200: return "1 hr ago"
        case ..<86400: return "\(seconds / 3600) hrs ago"
        default:
            let formatter = DateFormatter()
            formatter.dateFormat = "MMM d, h:mm a"
            return formatter.string(from: date)
        }
    }

    // MARK: - Date Helpers
    private func dayBounds(for date: Date) -> (Date, Date) {
        let calendar = Calendar.current
        let start = calendar.startOfDay(for: date)
        let end = calendar.date(byAdding: .day, value: 1, to: start) ?? Date()
        return (start, end)
    }
    
    private func sleepQueryBounds(for date: Date) -> (Date, Date) {
        let calendar = Calendar.current
        let startOfDay = calendar.startOfDay(for: date)
        let nightBefore = calendar.date(byAdding: .hour, value: -6, to: startOfDay) ?? startOfDay
        let noon = calendar.date(byAdding: .hour, value: 14, to: startOfDay) ?? startOfDay
        return (nightBefore, noon)
    }
    
}
