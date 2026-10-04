package ca.zeezaglobal.gymsuitapp.data.model

import org.json.JSONArray
import org.json.JSONObject

data class DailyHealthSyncDto(
    val recordDate: String,
    val steps: Long? = null,
    val activeCalories: Double? = null,
    val totalCalories: Double? = null,
    val distanceMeters: Double? = null,
    val latestHeartRateBpm: Int? = null,
    val restingHeartRateBpm: Int? = null,
    val minHeartRateBpm: Int? = null,
    val maxHeartRateBpm: Int? = null,
    val heartRateSamplesJson: String? = null,
    val sleepDurationMinutes: Long? = null,
    val sleepStartTime: String? = null,
    val sleepEndTime: String? = null,
    val sleepStagesJson: String? = null,
    val weightKg: Double? = null,
    val sourceDevice: String? = null,
    val updatedAt: String? = null
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("recordDate", recordDate)
        steps?.let { put("steps", it) }
        activeCalories?.let { put("activeCalories", it) }
        totalCalories?.let { put("totalCalories", it) }
        distanceMeters?.let { put("distanceMeters", it) }
        latestHeartRateBpm?.let { put("latestHeartRateBpm", it) }
        restingHeartRateBpm?.let { put("restingHeartRateBpm", it) }
        minHeartRateBpm?.let { put("minHeartRateBpm", it) }
        maxHeartRateBpm?.let { put("maxHeartRateBpm", it) }
        heartRateSamplesJson?.let { put("heartRateSamplesJson", it) }
        sleepDurationMinutes?.let { put("sleepDurationMinutes", it) }
        sleepStartTime?.let { put("sleepStartTime", it) }
        sleepEndTime?.let { put("sleepEndTime", it) }
        sleepStagesJson?.let { put("sleepStagesJson", it) }
        weightKg?.let { put("weightKg", it) }
        sourceDevice?.let { put("sourceDevice", it) }
        updatedAt?.let { put("updatedAt", it) }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): DailyHealthSyncDto {
            return DailyHealthSyncDto(
                recordDate = json.getString("recordDate"),
                steps = if (json.has("steps") && !json.isNull("steps")) json.getLong("steps") else null,
                activeCalories = if (json.has("activeCalories") && !json.isNull("activeCalories")) json.getDouble("activeCalories") else null,
                totalCalories = if (json.has("totalCalories") && !json.isNull("totalCalories")) json.getDouble("totalCalories") else null,
                distanceMeters = if (json.has("distanceMeters") && !json.isNull("distanceMeters")) json.getDouble("distanceMeters") else null,
                latestHeartRateBpm = if (json.has("latestHeartRateBpm") && !json.isNull("latestHeartRateBpm")) json.getInt("latestHeartRateBpm") else null,
                restingHeartRateBpm = if (json.has("restingHeartRateBpm") && !json.isNull("restingHeartRateBpm")) json.getInt("restingHeartRateBpm") else null,
                minHeartRateBpm = if (json.has("minHeartRateBpm") && !json.isNull("minHeartRateBpm")) json.getInt("minHeartRateBpm") else null,
                maxHeartRateBpm = if (json.has("maxHeartRateBpm") && !json.isNull("maxHeartRateBpm")) json.getInt("maxHeartRateBpm") else null,
                heartRateSamplesJson = if (json.has("heartRateSamplesJson") && !json.isNull("heartRateSamplesJson")) json.getString("heartRateSamplesJson") else null,
                sleepDurationMinutes = if (json.has("sleepDurationMinutes") && !json.isNull("sleepDurationMinutes")) json.getLong("sleepDurationMinutes") else null,
                sleepStartTime = if (json.has("sleepStartTime") && !json.isNull("sleepStartTime")) json.getString("sleepStartTime") else null,
                sleepEndTime = if (json.has("sleepEndTime") && !json.isNull("sleepEndTime")) json.getString("sleepEndTime") else null,
                sleepStagesJson = if (json.has("sleepStagesJson") && !json.isNull("sleepStagesJson")) json.getString("sleepStagesJson") else null,
                weightKg = if (json.has("weightKg") && !json.isNull("weightKg")) json.getDouble("weightKg") else null,
                sourceDevice = if (json.has("sourceDevice") && !json.isNull("sourceDevice")) json.getString("sourceDevice") else null,
                updatedAt = if (json.has("updatedAt") && !json.isNull("updatedAt")) json.getString("updatedAt") else null
            )
        }
    }
}

data class WorkoutSyncDto(
    val externalId: String,
    val title: String,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Int? = null,
    val caloriesBurned: Double? = null,
    val setCount: Int? = null,
    val totalReps: Int? = null,
    val totalVolumeKg: Double? = null,
    val sourceDevice: String? = null,
    val updatedAt: String? = null
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("externalId", externalId)
        put("title", title)
        put("startTime", startTime)
        put("endTime", endTime)
        durationMinutes?.let { put("durationMinutes", it) }
        caloriesBurned?.let { put("caloriesBurned", it) }
        setCount?.let { put("setCount", it) }
        totalReps?.let { put("totalReps", it) }
        totalVolumeKg?.let { put("totalVolumeKg", it) }
        sourceDevice?.let { put("sourceDevice", it) }
        updatedAt?.let { put("updatedAt", it) }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): WorkoutSyncDto {
            return WorkoutSyncDto(
                externalId = json.getString("externalId"),
                title = json.getString("title"),
                startTime = json.getString("startTime"),
                endTime = json.getString("endTime"),
                durationMinutes = if (json.has("durationMinutes") && !json.isNull("durationMinutes")) json.getInt("durationMinutes") else null,
                caloriesBurned = if (json.has("caloriesBurned") && !json.isNull("caloriesBurned")) json.getDouble("caloriesBurned") else null,
                setCount = if (json.has("setCount") && !json.isNull("setCount")) json.getInt("setCount") else null,
                totalReps = if (json.has("totalReps") && !json.isNull("totalReps")) json.getInt("totalReps") else null,
                totalVolumeKg = if (json.has("totalVolumeKg") && !json.isNull("totalVolumeKg")) json.getDouble("totalVolumeKg") else null,
                sourceDevice = if (json.has("sourceDevice") && !json.isNull("sourceDevice")) json.getString("sourceDevice") else null,
                updatedAt = if (json.has("updatedAt") && !json.isNull("updatedAt")) json.getString("updatedAt") else null
            )
        }
    }
}

data class HealthSyncRequest(
    val lastSyncTime: String?,
    val clientDevice: String = "ANDROID_HEALTH_CONNECT",
    val dailyRecords: List<DailyHealthSyncDto>,
    val workouts: List<WorkoutSyncDto>
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        lastSyncTime?.let { put("lastSyncTime", it) }
        put("clientDevice", clientDevice)
        put("dailyRecords", JSONArray().apply { dailyRecords.forEach { put(it.toJsonObject()) } })
        put("workouts", JSONArray().apply { workouts.forEach { put(it.toJsonObject()) } })
    }
}

data class HealthSyncResponse(
    val serverSyncTime: String,
    val uploadedDailyCount: Int,
    val uploadedWorkoutCount: Int,
    val remoteDailyRecords: List<DailyHealthSyncDto>,
    val remoteWorkouts: List<WorkoutSyncDto>
) {
    companion object {
        fun fromJsonObject(json: JSONObject): HealthSyncResponse {
            val dailyArr = json.optJSONArray("remoteDailyRecords") ?: JSONArray()
            val dailyList = mutableListOf<DailyHealthSyncDto>()
            for (i in 0 until dailyArr.length()) {
                val item = dailyArr.optJSONObject(i) ?: continue
                dailyList.add(DailyHealthSyncDto.fromJsonObject(item))
            }

            val workoutArr = json.optJSONArray("remoteWorkouts") ?: JSONArray()
            val workoutList = mutableListOf<WorkoutSyncDto>()
            for (i in 0 until workoutArr.length()) {
                val item = workoutArr.optJSONObject(i) ?: continue
                workoutList.add(WorkoutSyncDto.fromJsonObject(item))
            }

            return HealthSyncResponse(
                serverSyncTime = json.optString("serverSyncTime", ""),
                uploadedDailyCount = json.optInt("uploadedDailyCount", 0),
                uploadedWorkoutCount = json.optInt("uploadedWorkoutCount", 0),
                remoteDailyRecords = dailyList,
                remoteWorkouts = workoutList
            )
        }
    }
}
