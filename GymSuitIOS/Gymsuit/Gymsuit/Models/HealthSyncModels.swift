import Foundation

public struct DailyHealthSyncDto: Codable {
    public let recordDate: String
    public let steps: Int64?
    public let activeCalories: Double?
    public let totalCalories: Double?
    public let distanceMeters: Double?
    public let latestHeartRateBpm: Int?
    public let restingHeartRateBpm: Int?
    public let minHeartRateBpm: Int?
    public let maxHeartRateBpm: Int?
    public let heartRateSamplesJson: String?
    public let sleepDurationMinutes: Int64?
    public let sleepStartTime: String?
    public let sleepEndTime: String?
    public let sleepStagesJson: String?
    public let weightKg: Double?
    public let sourceDevice: String?
    public let updatedAt: String?

    public init(
        recordDate: String,
        steps: Int64? = nil,
        activeCalories: Double? = nil,
        totalCalories: Double? = nil,
        distanceMeters: Double? = nil,
        latestHeartRateBpm: Int? = nil,
        restingHeartRateBpm: Int? = nil,
        minHeartRateBpm: Int? = nil,
        maxHeartRateBpm: Int? = nil,
        heartRateSamplesJson: String? = nil,
        sleepDurationMinutes: Int64? = nil,
        sleepStartTime: String? = nil,
        sleepEndTime: String? = nil,
        sleepStagesJson: String? = nil,
        weightKg: Double? = nil,
        sourceDevice: String? = nil,
        updatedAt: String? = nil
    ) {
        self.recordDate = recordDate
        self.steps = steps
        self.activeCalories = activeCalories
        self.totalCalories = totalCalories
        self.distanceMeters = distanceMeters
        self.latestHeartRateBpm = latestHeartRateBpm
        self.restingHeartRateBpm = restingHeartRateBpm
        self.minHeartRateBpm = minHeartRateBpm
        self.maxHeartRateBpm = maxHeartRateBpm
        self.heartRateSamplesJson = heartRateSamplesJson
        self.sleepDurationMinutes = sleepDurationMinutes
        self.sleepStartTime = sleepStartTime
        self.sleepEndTime = sleepEndTime
        self.sleepStagesJson = sleepStagesJson
        self.weightKg = weightKg
        self.sourceDevice = sourceDevice
        self.updatedAt = updatedAt
    }
}

public struct WorkoutSyncDto: Codable {
    public let externalId: String
    public let title: String
    public let startTime: String
    public let endTime: String
    public let durationMinutes: Int?
    public let caloriesBurned: Double?
    public let setCount: Int?
    public let totalReps: Int?
    public let totalVolumeKg: Double?
    public let sourceDevice: String?
    public let updatedAt: String?

    public init(
        externalId: String,
        title: String,
        startTime: String,
        endTime: String,
        durationMinutes: Int? = nil,
        caloriesBurned: Double? = nil,
        setCount: Int? = nil,
        totalReps: Int? = nil,
        totalVolumeKg: Double? = nil,
        sourceDevice: String? = nil,
        updatedAt: String? = nil
    ) {
        self.externalId = externalId
        self.title = title
        self.startTime = startTime
        self.endTime = endTime
        self.durationMinutes = durationMinutes
        self.caloriesBurned = caloriesBurned
        self.setCount = setCount
        self.totalReps = totalReps
        self.totalVolumeKg = totalVolumeKg
        self.sourceDevice = sourceDevice
        self.updatedAt = updatedAt
    }
}

public struct HealthSyncRequest: Codable {
    public let lastSyncTime: String?
    public let clientDevice: String
    public let dailyRecords: [DailyHealthSyncDto]
    public let workouts: [WorkoutSyncDto]

    public init(
        lastSyncTime: String?,
        clientDevice: String = "IOS_APPLE_HEALTH",
        dailyRecords: [DailyHealthSyncDto],
        workouts: [WorkoutSyncDto]
    ) {
        self.lastSyncTime = lastSyncTime
        self.clientDevice = clientDevice
        self.dailyRecords = dailyRecords
        self.workouts = workouts
    }
}

public struct HealthSyncResponse: Codable {
    public let serverSyncTime: String
    public let uploadedDailyCount: Int
    public let uploadedWorkoutCount: Int
    public let remoteDailyRecords: [DailyHealthSyncDto]
    public let remoteWorkouts: [WorkoutSyncDto]
}
