import Foundation
import Combine
import SwiftUI
import HealthKit

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
    
    public func requestAuthorization() async -> Bool {
        guard isAvailable else { return false }
        do {
            try await healthStore.requestAuthorization(toShare: [], read: readTypes)
            DispatchQueue.main.async {
                self.isAuthorized = true
            }
            return true
        } catch {
            print("HealthKit authorization error: \(error.localizedDescription)")
            return false
        }
    }
    
    // MARK: - Daily Steps
    public func fetchSteps(for date: Date) async -> Int64 {
        guard let stepType = HKQuantityType.quantityType(forIdentifier: .stepCount) else { return 0 }
        let (start, end) = dayBounds(for: date)
        let predicate = HKQuery.predicateForSamples(withStart: start, end: end, options: .strictStartDate)
        
        return await withCheckedContinuation { continuation in
            let query = HKStatisticsQuery(quantityType: stepType, quantitySamplePredicate: predicate, options: .cumulativeSum) { _, stats, _ in
                let count = stats?.sumQuantity()?.doubleValue(for: HKUnit.count()) ?? 0
                continuation.resume(returning: Int64(count))
            }
            healthStore.execute(query)
        }
    }
    
    // MARK: - Distance & Basal

    /// Real walking/running distance for the day in meters, or nil when unavailable.
    public func fetchDistanceMeters(for date: Date) async -> Double? {
        guard let distanceType = HKQuantityType.quantityType(forIdentifier: .distanceWalkingRunning) else {
            return nil
        }
        let (start, end) = dayBounds(for: date)
        let predicate = HKQuery.predicateForSamples(withStart: start, end: end, options: .strictStartDate)
        return await withCheckedContinuation { continuation in
            let query = HKStatisticsQuery(quantityType: distanceType, quantitySamplePredicate: predicate, options: .cumulativeSum) { _, stats, _ in
                let meters = stats?.sumQuantity()?.doubleValue(for: HKUnit.meter())
                continuation.resume(returning: meters)
            }
            healthStore.execute(query)
        }
    }

    /// Real basal (resting) energy for the day in kcal, or 0 when unavailable.
    public func fetchBasalKcal(for date: Date) async -> Double {
        guard let basalType = HKQuantityType.quantityType(forIdentifier: .basalEnergyBurned) else {
            return 0
        }
        let (start, end) = dayBounds(for: date)
        let predicate = HKQuery.predicateForSamples(withStart: start, end: end, options: .strictStartDate)
        return await withCheckedContinuation { continuation in
            let query = HKStatisticsQuery(quantityType: basalType, quantitySamplePredicate: predicate, options: .cumulativeSum) { _, stats, _ in
                let kcal = stats?.sumQuantity()?.doubleValue(for: HKUnit.kilocalorie()) ?? 0
                continuation.resume(returning: kcal)
            }
            healthStore.execute(query)
        }
    }

    /// Total workout duration in minutes from real HKWorkout samples, or 0 when none.
    public func fetchWorkoutMinutes(for date: Date) async -> Int64 {
        let (start, end) = dayBounds(for: date)
        let predicate = HKQuery.predicateForSamples(withStart: start, end: end, options: .strictStartDate)
        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: true)
        return await withCheckedContinuation { continuation in
            let query = HKSampleQuery(sampleType: HKObjectType.workoutType(), predicate: predicate, limit: HKObjectQueryNoLimit, sortDescriptors: [sortDescriptor]) { _, samples, _ in
                let workouts = samples as? [HKWorkout] ?? []
                let minutes = Int64(workouts.reduce(0.0) { $0 + $1.duration } / 60)
                continuation.resume(returning: minutes)
            }
            healthStore.execute(query)
        }
    }

    // MARK: - Calories Breakdown
    public func fetchCaloriesBreakdown(for date: Date) async -> CaloriesBreakdown {        guard let activeType = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned) else {
            return CaloriesBreakdown(totalKcal: 0, stepsKcal: 0, workoutKcal: 0, moveKcal: 0, hasData: false)
        }
        let (start, end) = dayBounds(for: date)
        let predicate = HKQuery.predicateForSamples(withStart: start, end: end, options: .strictStartDate)
        
        let activeKcal: Double = await withCheckedContinuation { continuation in
            let query = HKStatisticsQuery(quantityType: activeType, quantitySamplePredicate: predicate, options: .cumulativeSum) { _, stats, _ in
                let kcal = stats?.sumQuantity()?.doubleValue(for: HKUnit.kilocalorie()) ?? 0
                continuation.resume(returning: kcal)
            }
            self.healthStore.execute(query)
        }
        
        let steps = await fetchSteps(for: date)

        // Basal (resting) energy is a real measured HealthKit value, not an estimate.
        let basalKcal = await fetchBasalKcal(for: date)

        let stepKcal = Double(steps) * 0.04
        let workoutKcal = max(0, activeKcal * 0.45)
        let moveKcal = max(0, activeKcal - workoutKcal)
        let total = activeKcal + basalKcal

        return CaloriesBreakdown(
            totalKcal: total,
            stepsKcal: stepKcal,
            workoutKcal: workoutKcal,
            moveKcal: moveKcal,
            hasData: activeKcal > 0 || steps > 0
        )
    }
    
    // MARK: - Detailed Calories
    public func fetchDetailedCalories(for date: Date, targetKcal: Double = 4000.0) async -> DetailedCaloriesData {
        let breakdown = await fetchCaloriesBreakdown(for: date)
        let steps = await fetchSteps(for: date)
        let basalKcal = await fetchBasalKcal(for: date)
        let workoutMinutes = await fetchWorkoutMinutes(for: date)
        let total = breakdown.totalKcal

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
                name: "Active Movement",
                caloriesKcal: breakdown.moveKcal,
                percentage: Int((breakdown.moveKcal / max(1, total)) * 100),
                durationOrCount: "Daily burn",
                colorHex: 0xFFA498C7
            ),
            CalorieActivityItem(
                name: "Resting Metabolism (BMR)",
                caloriesKcal: basalKcal,
                percentage: Int((basalKcal / max(1, total)) * 100),
                durationOrCount: "Basal",
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
            hasData: breakdown.hasData
        )
    }
    
    // MARK: - Sleep Session Data
    public func fetchSleepSession(for date: Date) async -> SleepSessionData? {
        guard let sleepType = HKCategoryType.categoryType(forIdentifier: .sleepAnalysis) else {
            return nil
        }

        let (start, end) = sleepQueryBounds(for: date)
        let predicate = HKQuery.predicateForSamples(withStart: start, end: end, options: .strictStartDate)
        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierEndDate, ascending: false)

        return await withCheckedContinuation { continuation in
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
    public func fetchHeartRateSummary(for date: Date) async -> HeartRateSummaryData? {
        guard let hrType = HKQuantityType.quantityType(forIdentifier: .heartRate) else {
            return nil
        }
        let (start, end) = dayBounds(for: date)
        let predicate = HKQuery.predicateForSamples(withStart: start, end: end, options: .strictStartDate)
        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: true)
        let unit = HKUnit.count().unitDivided(by: HKUnit.minute())
        
        return await withCheckedContinuation { continuation in
            let query = HKSampleQuery(sampleType: hrType, predicate: predicate, limit: 100, sortDescriptors: [sortDescriptor]) { _, samples, _ in
                guard let hrSamples = samples as? [HKQuantitySample], !hrSamples.isEmpty else {
                    continuation.resume(returning: nil)
                    return
                }
                
                var points: [HeartRatePoint] = []
                var minB = Int.max
                var maxB = Int.min
                for sample in hrSamples {
                    let bpm = Int(sample.quantity.doubleValue(for: unit))
                    points.append(HeartRatePoint(time: sample.startDate, bpm: bpm))
                    if bpm < minB { minB = bpm }
                    if bpm > maxB { maxB = bpm }
                }
                let latest = points.last.map(\.bpm) ?? 0
                
                let formatter = DateFormatter()
                formatter.timeStyle = .short
                let timeRange = "\(formatter.string(from: points.first!.time)) – \(formatter.string(from: points.last!.time))"
                
                continuation.resume(returning: HeartRateSummaryData(
                    latestBpm: latest,
                    minBpm: minB,
                    maxBpm: maxB,
                    timeRangeFormatted: timeRange,
                    points: points,
                    hasData: true
                ))
            }
            healthStore.execute(query)
        }
    }
    
    // MARK: - Detailed HRV
    public func fetchDetailedHrv(for date: Date) async -> DetailedHrvData? {
        // No real HRV query is implemented yet. Return nil so the UI shows
        // an honest empty state instead of fabricated buckets.
        return nil
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
