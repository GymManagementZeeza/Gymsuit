package ca.zeezaglobal.gymsuitapp.data.model

import org.json.JSONArray
import org.json.JSONObject

data class WeightRecordItem(
    val time: String,
    val weightKg: Double
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("time", time)
        put("weight_kg", weightKg)
    }
}

data class SleepSessionItem(
    val startTime: String,
    val endTime: String,
    val durationMinutes: Long,
    val title: String
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("start_time", startTime)
        put("end_time", endTime)
        put("duration_minutes", durationMinutes)
        put("title", title)
    }
}

data class ExerciseSessionItem(
    val startTime: String,
    val endTime: String,
    val title: String,
    val exerciseType: Int
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("start_time", startTime)
        put("end_time", endTime)
        put("title", title)
        put("exercise_type", exerciseType)
    }
}

data class HealthDataPayload(
    val weightRecords: List<WeightRecordItem>,
    val latestWeightKg: Double,
    val sleepSessions: List<SleepSessionItem>,
    val latestSleepMinutes: Long,
    val latestSleepFormatted: String,
    val todaySteps: Long,
    val latestHeartRateBpm: Int,
    val todayActiveCaloriesKcal: Double,
    val todayDistanceMeters: Double,
    val exerciseSessions: List<ExerciseSessionItem>
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("weight_records", JSONArray().apply {
            weightRecords.forEach { put(it.toJsonObject()) }
        })
        put("latest_weight_kg", latestWeightKg)
        put("sleep_sessions", JSONArray().apply {
            sleepSessions.forEach { put(it.toJsonObject()) }
        })
        put("latest_sleep_minutes", latestSleepMinutes)
        put("latest_sleep_formatted", latestSleepFormatted)
        put("today_steps", todaySteps)
        put("latest_heart_rate_bpm", latestHeartRateBpm)
        put("today_active_calories_kcal", todayActiveCaloriesKcal)
        put("today_distance_meters", todayDistanceMeters)
        put("exercise_sessions", JSONArray().apply {
            exerciseSessions.forEach { put(it.toJsonObject()) }
        })
    }
}

data class AiSummarizeRequest(
    val timestamp: String,
    val deviceSdkAvailable: Boolean,
    val healthData: HealthDataPayload
) {
    fun toJsonString(): String {
        val root = JSONObject().apply {
            put("timestamp", timestamp)
            put("device_sdk_available", deviceSdkAvailable)
            put("health_data", healthData.toJsonObject())
        }
        return root.toString()
    }
}

data class AiSummarizeResponse(
    val summary: String
)
