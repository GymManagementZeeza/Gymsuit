import Foundation

public struct WeightRecordItem: Codable {
    public let time: String
    public let weightKg: Double
}

public struct SleepSessionItem: Codable {
    public let startTime: String
    public let endTime: String
    public let durationMinutes: Int64
    public let title: String
}

public struct ExerciseSessionItem: Codable {
    public let startTime: String
    public let endTime: String
    public let title: String
    public let exerciseType: Int
}

public struct HealthDataPayload: Codable {
    public let weightRecords: [WeightRecordItem]
    public let latestWeightKg: Double?
    public let sleepSessions: [SleepSessionItem]
    public let latestSleepMinutes: Int64?
    public let latestSleepFormatted: String?
    public let todaySteps: Int64?
    public let latestHeartRateBpm: Int?
    public let todayActiveCaloriesKcal: Double?
    public let todayDistanceMeters: Double?
    public let exerciseSessions: [ExerciseSessionItem]
    public let previousSteps: Int64?
    public let previousSleepMinutes: Int64?
    public let previousActiveCaloriesKcal: Double?

    public init(
        weightRecords: [WeightRecordItem] = [],
        latestWeightKg: Double? = nil,
        sleepSessions: [SleepSessionItem] = [],
        latestSleepMinutes: Int64? = nil,
        latestSleepFormatted: String? = nil,
        todaySteps: Int64? = nil,
        latestHeartRateBpm: Int? = nil,
        todayActiveCaloriesKcal: Double? = nil,
        todayDistanceMeters: Double? = nil,
        exerciseSessions: [ExerciseSessionItem] = [],
        previousSteps: Int64? = nil,
        previousSleepMinutes: Int64? = nil,
        previousActiveCaloriesKcal: Double? = nil
    ) {
        self.weightRecords = weightRecords
        self.latestWeightKg = latestWeightKg
        self.sleepSessions = sleepSessions
        self.latestSleepMinutes = latestSleepMinutes
        self.latestSleepFormatted = latestSleepFormatted
        self.todaySteps = todaySteps
        self.latestHeartRateBpm = latestHeartRateBpm
        self.todayActiveCaloriesKcal = todayActiveCaloriesKcal
        self.todayDistanceMeters = todayDistanceMeters
        self.exerciseSessions = exerciseSessions
        self.previousSteps = previousSteps
        self.previousSleepMinutes = previousSleepMinutes
        self.previousActiveCaloriesKcal = previousActiveCaloriesKcal
    }
}

public struct AiSummarizeRequest: Codable {
    public let timestamp: String
    public let deviceSdkAvailable: Bool
    public let healthData: HealthDataPayload
}

public struct AiSummarizeResponse: Codable {
    public let summary: String
}

public struct SleepSessionData: Identifiable {
    public var id = UUID()
    public let startTime: Date
    public let endTime: Date
    public let durationMinutes: Int64
    public let startTimeFormatted: String
    public let endTimeFormatted: String
    public let durationFormatted: String
}

public enum SleepStageType: String, CaseIterable {
    case awake = "AWAKE"
    case rem = "REM"
    case light = "LIGHT"
    case deep = "DEEP"
}

public struct SleepStageSegment: Identifiable {
    public var id = UUID()
    public let stage: SleepStageType
    public let startFraction: Double // 0.0 to 1.0
    public let endFraction: Double   // 0.0 to 1.0
    public let durationMinutes: Int64
}

public struct DetailedSleepData {
    public let sessionDate: Date
    public let startTime: Date
    public let endTime: Date
    public let startTimeFormatted: String
    public let midTimeFormatted: String
    public let endTimeFormatted: String
    public let totalSleepMinutes: Int64
    public let awakeMinutes: Int64
    public let remMinutes: Int64
    public let lightMinutes: Int64
    public let deepMinutes: Int64
    public let stages: [SleepStageSegment]
    public let interruptionsCount: Int
    public let hasData: Bool
}

public struct CaloriesBreakdown {
    public let totalKcal: Double
    public let stepsKcal: Double
    public let workoutKcal: Double
    public let moveKcal: Double
    public let hasData: Bool
}

public struct CalorieActivityItem: Identifiable {
    public var id = UUID()
    public let name: String
    public let caloriesKcal: Double
    public let percentage: Int
    public let durationOrCount: String
    public let colorHex: UInt32
}

public struct DetailedCaloriesData {
    public let date: Date
    public let totalCaloriesKcal: Double
    public let targetKcal: Double
    public let activities: [CalorieActivityItem]
    public let stepsCount: Int64
    public let workoutMinutes: Int64
    public let hasData: Bool
}

public struct HeartRatePoint: Identifiable {
    public var id = UUID()
    public let time: Date
    public let bpm: Int
}

public struct HeartRateSummaryData {
    public let latestBpm: Int
    public let minBpm: Int
    public let maxBpm: Int
    public let timeRangeFormatted: String
    public let points: [HeartRatePoint]
    public let hasData: Bool
}

public struct HrvBucket: Identifiable {
    public var id = UUID()
    public let hourLabel: String
    public let hourOfDay: Int
    public let minMs: Int
    public let maxMs: Int
    public let avgMs: Int
    public let samples: [Int]
    public let isHighlighted: Bool
}

public struct DetailedHrvData {
    public let date: Date
    public let latestHrvMs: Int
    public let aveVariabilityMs: Int
    public let stressLevel: String
    public let minBpm: Int
    public let maxBpm: Int
    public let latestBpm: Int
    public let buckets: [HrvBucket]
    public let hasData: Bool
}
