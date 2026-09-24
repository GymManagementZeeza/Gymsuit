package ca.zeezaglobal.gymsuitapp.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import android.util.Log
import org.json.JSONObject
import org.json.JSONArray

data class SleepSessionData(
    val startTime: Instant,
    val endTime: Instant,
    val durationMinutes: Long,
    val startTimeFormatted: String, // e.g. "4:02 AM"
    val endTimeFormatted: String,   // e.g. "6:54 AM"
    val durationFormatted: String   // e.g. "2h 52m"
)

data class CaloriesBreakdown(
    val totalKcal: Double,
    val stepsKcal: Double,
    val workoutKcal: Double,
    val moveKcal: Double,
    val hasData: Boolean
)

class HealthConnectManager(private val context: Context) {

    private val healthConnectClient: HealthConnectClient? by lazy {
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    // Comprehensive permissions covering all health & fitness categories
    val permissions = setOf(
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class)
    )

    fun isAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    suspend fun hasPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    suspend fun hasAnyPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.any { it in permissions }
    }

    suspend fun hasWeightPermission(): Boolean {
        val client = healthConnectClient ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.contains(HealthPermission.getReadPermission(WeightRecord::class))
    }

    suspend fun hasSleepPermission(): Boolean {
        val client = healthConnectClient ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.contains(HealthPermission.getReadPermission(SleepSessionRecord::class))
    }

    suspend fun hasExercisePermission(): Boolean {
        val client = healthConnectClient ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.contains(HealthPermission.getReadPermission(ExerciseSessionRecord::class))
    }

    suspend fun hasCaloriesPermission(): Boolean {
        val client = healthConnectClient ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.contains(HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)) ||
               granted.contains(HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)) ||
               granted.contains(HealthPermission.getReadPermission(StepsRecord::class))
    }

    /**
     * Reads active calories burned for a specific [date] (local timezone).
     * Checks ActiveCaloriesBurnedRecord, TotalCaloriesBurnedRecord, and falls back to
     * steps-derived calories (~0.04 kcal/step) if the connected wearable only logs steps.
     */
    suspend fun readCaloriesForDate(date: LocalDate): Double? {
        val client = healthConnectClient ?: return null
        val zoneId = ZoneId.systemDefault()
        val startOfDay = date.atStartOfDay(zoneId).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant()

        return try {
            // 1. Try reading explicit ActiveCaloriesBurnedRecord
            val activeResponse = try {
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = ActiveCaloriesBurnedRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                    )
                )
            } catch (e: Exception) {
                null
            }

            val activeKcal = activeResponse?.records?.sumOf { it.energy.inKilocalories } ?: 0.0
            if (activeKcal > 0.0) {
                Log.d("CALORIES_DEBUG", "Found active calories for $date: $activeKcal kcal")
                return activeKcal
            }

            // 2. Try reading TotalCaloriesBurnedRecord
            val totalResponse = try {
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = TotalCaloriesBurnedRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                    )
                )
            } catch (e: Exception) {
                null
            }

            val totalKcal = totalResponse?.records?.sumOf { it.energy.inKilocalories } ?: 0.0
            if (totalKcal > 0.0) {
                Log.d("CALORIES_DEBUG", "Found total calories for $date: $totalKcal kcal")
                return totalKcal
            }

            // 3. If wearable only synced StepsRecord, estimate active calories from steps (~0.04 kcal/step)
            val stepsResponse = try {
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = StepsRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                    )
                )
            } catch (e: Exception) {
                null
            }

            val totalSteps = stepsResponse?.records?.sumOf { it.count } ?: 0L
            if (totalSteps > 0) {
                val estimatedKcal = totalSteps * 0.04
                Log.d("CALORIES_DEBUG", "Derived calories from $totalSteps steps for $date: $estimatedKcal kcal")
                return estimatedKcal
            }

            Log.d("CALORIES_DEBUG", "No calories or steps found for $date")
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Reads and computes a dynamic category breakdown of calories burned on [date]:
     * - Steps (calories from walking / steps)
     * - Workout (calories from exercise sessions)
     * - Move (general active daily movement)
     */
    suspend fun readCaloriesBreakdownForDate(date: LocalDate): CaloriesBreakdown {
        val client = healthConnectClient ?: return CaloriesBreakdown(0.0, 0.0, 0.0, 0.0, false)
        val zoneId = ZoneId.systemDefault()
        val startOfDay = date.atStartOfDay(zoneId).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant()

        return try {
            // 1. Query Steps
            val stepsResponse = try {
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = StepsRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                    )
                )
            } catch (e: Exception) { null }
            val totalSteps = stepsResponse?.records?.sumOf { it.count } ?: 0L
            val rawStepsKcal = totalSteps * 0.04 // ~0.04 kcal/step

            // 2. Query Workouts / Exercise sessions
            val exerciseResponse = try {
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = ExerciseSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                    )
                )
            } catch (e: Exception) { null }
            val exerciseMins = exerciseResponse?.records?.sumOf {
                java.time.Duration.between(it.startTime, it.endTime).toMinutes()
            } ?: 0L
            val rawWorkoutKcal = exerciseMins * 7.5 // ~7.5 kcal/min for workout

            // 3. Query recorded ActiveCaloriesBurnedRecord
            val activeResponse = try {
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = ActiveCaloriesBurnedRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                    )
                )
            } catch (e: Exception) { null }
            val recordedActiveKcal = activeResponse?.records?.sumOf { it.energy.inKilocalories } ?: 0.0

            // 4. Query TotalCaloriesBurnedRecord
            val totalCalResponse = try {
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = TotalCaloriesBurnedRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                    )
                )
            } catch (e: Exception) { null }
            val recordedTotalKcal = totalCalResponse?.records?.sumOf { it.energy.inKilocalories } ?: 0.0

            val baseTotal = when {
                recordedActiveKcal > 0.0 -> recordedActiveKcal
                rawStepsKcal + rawWorkoutKcal > 0.0 -> (rawStepsKcal + rawWorkoutKcal) * 1.15
                recordedTotalKcal > 0.0 -> recordedTotalKcal
                else -> 0.0
            }

            if (baseTotal <= 0.0) {
                return CaloriesBreakdown(0.0, 0.0, 0.0, 0.0, false)
            }

            // Distribute into the 3 categories dynamically based on actual steps and workouts
            val (stepsShare, workoutShare, moveShare) = when {
                rawWorkoutKcal > 0 && rawStepsKcal > 0 -> {
                    val sRatio = rawStepsKcal / (rawStepsKcal + rawWorkoutKcal)
                    val wRatio = rawWorkoutKcal / (rawStepsKcal + rawWorkoutKcal)
                    Triple(baseTotal * 0.85 * sRatio, baseTotal * 0.85 * wRatio, baseTotal * 0.15)
                }
                rawWorkoutKcal > 0 -> {
                    Triple(baseTotal * 0.20, baseTotal * 0.65, baseTotal * 0.15)
                }
                rawStepsKcal > 0 -> {
                    Triple(baseTotal * 0.75, baseTotal * 0.10, baseTotal * 0.15)
                }
                else -> {
                    Triple(baseTotal * 0.50, baseTotal * 0.30, baseTotal * 0.20)
                }
            }

            CaloriesBreakdown(
                totalKcal = baseTotal,
                stepsKcal = stepsShare,
                workoutKcal = workoutShare,
                moveKcal = moveShare,
                hasData = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            CaloriesBreakdown(0.0, 0.0, 0.0, 0.0, false)
        }
    }

    /**
     * Reads all dates within the last [daysBack] days on which an exercise / workout session occurred.
     */
    suspend fun readWorkoutDays(daysBack: Long = 35): Set<LocalDate> {
        val client = healthConnectClient ?: return emptySet()
        val zoneId = ZoneId.systemDefault()
        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(Instant.now().minus(daysBack, ChronoUnit.DAYS))
                )
            )
            response.records.map { record ->
                record.startTime.atZone(zoneId).toLocalDate()
            }.toSet()
        } catch (e: Exception) {
            e.printStackTrace()
            emptySet()
        }
    }

    /**
     * Reads the latest weight record in kilograms (recorded within the last 90 days).
     * Returns null if no record found, permission not granted, or error occurs.
     */
    suspend fun readLatestWeight(): Double? {
        val client = healthConnectClient ?: return null
        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(Instant.now().minus(90, ChronoUnit.DAYS))
                )
            )
            // Pick the most recent weight record
            val latestRecord = response.records.maxByOrNull { it.time }
            latestRecord?.weight?.inKilograms
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Reads the most recent sleep session record (within the last 30+ days) and returns total minutes.
     * Returns null if no record found or permission not granted.
     */
    suspend fun readLatestSleepDurationMinutes(): Long? {
        val client = healthConnectClient ?: return null
        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(Instant.now().minus(35, ChronoUnit.DAYS))
                )
            )
            val latestRecord = response.records.maxByOrNull { it.endTime }
            latestRecord?.let {
                java.time.Duration.between(it.startTime, it.endTime).toMinutes()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Reads all sleep sessions for the past 30+ days, localized to the user's timezone.
     */
    suspend fun readRecentSleepSessions(daysBack: Long = 35): List<SleepSessionData> {
        val client = healthConnectClient ?: return emptyList()
        val zoneId = ZoneId.systemDefault()
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(Instant.now().minus(daysBack, ChronoUnit.DAYS))
                )
            )
            response.records.map { record ->
                val durationMins = java.time.Duration.between(record.startTime, record.endTime).toMinutes()
                val startLocal = record.startTime.atZone(zoneId).format(timeFormatter)
                val endLocal = record.endTime.atZone(zoneId).format(timeFormatter)
                val hours = durationMins / 60
                val mins = durationMins % 60
                val durationFormatted = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

                SleepSessionData(
                    startTime = record.startTime,
                    endTime = record.endTime,
                    durationMinutes = durationMins,
                    startTimeFormatted = startLocal,
                    endTimeFormatted = endLocal,
                    durationFormatted = durationFormatted
                )
            }.sortedBy { it.startTime }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Reads all available health records from Health Connect (or fallback sample data)
     * and logs the complete health data payload as structured, formatted JSON in Android Logcat.
     */
    suspend fun logAllHealthDataAsJson(): String {
        val rootJson = JSONObject()
        val now = Instant.now()
        rootJson.put("timestamp", now.toString())
        rootJson.put("device_sdk_available", isAvailable())

        val client = healthConnectClient
        if (client == null) {
            rootJson.put("status", "Health Connect unavailable on device")
            val sampleData = JSONObject().apply {
                put("weight_kg", 75.0)
                put("sleep_minutes", 465)
                put("sleep_formatted", "7h 45m")
                put("steps", 8420)
                put("heart_rate_bpm", 72)
                put("active_calories_kcal", 450)
                put("distance_meters", 5230.0)
            }
            rootJson.put("sample_health_data", sampleData)
            val jsonString = rootJson.toString(2)
            Log.d("HEALTH_DATA_JSON", jsonString)
            return jsonString
        }

        val dataJson = JSONObject()
        try {
            // 1. Weight Records
            try {
                val weightResponse = client.readRecords(
                    ReadRecordsRequest(
                        recordType = WeightRecord::class,
                        timeRangeFilter = TimeRangeFilter.after(now.minus(90, ChronoUnit.DAYS))
                    )
                )
                val weightsArray = JSONArray()
                weightResponse.records.forEach { record ->
                    weightsArray.put(JSONObject().apply {
                        put("time", record.time.toString())
                        put("weight_kg", record.weight.inKilograms)
                    })
                }
                dataJson.put("weight_records", weightsArray)
                dataJson.put("latest_weight_kg", weightResponse.records.maxByOrNull { it.time }?.weight?.inKilograms ?: 75.0)
            } catch (e: Exception) {
                dataJson.put("weight_error", e.message)
                dataJson.put("latest_weight_kg", 75.0)
            }

            // 2. Sleep Sessions
            try {
                val sleepResponse = client.readRecords(
                    ReadRecordsRequest(
                        recordType = SleepSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.after(now.minus(35, ChronoUnit.DAYS))
                    )
                )
                val sleepArray = JSONArray()
                sleepResponse.records.forEach { record ->
                    val durationMins = java.time.Duration.between(record.startTime, record.endTime).toMinutes()
                    sleepArray.put(JSONObject().apply {
                        put("start_time", record.startTime.toString())
                        put("end_time", record.endTime.toString())
                        put("duration_minutes", durationMins)
                        put("title", record.title ?: "Sleep Session")
                    })
                }
                dataJson.put("sleep_sessions", sleepArray)
                val latestSleep = sleepResponse.records.maxByOrNull { it.endTime }
                val latestMins = latestSleep?.let { java.time.Duration.between(it.startTime, it.endTime).toMinutes() } ?: 465L
                dataJson.put("latest_sleep_minutes", latestMins)
                dataJson.put("latest_sleep_formatted", "${latestMins / 60}h ${latestMins % 60}m")
            } catch (e: Exception) {
                dataJson.put("sleep_error", e.message)
                dataJson.put("latest_sleep_minutes", 465)
                dataJson.put("latest_sleep_formatted", "7h 45m")
            }

            // 3. Steps Records
            try {
                val stepsResponse = client.readRecords(
                    ReadRecordsRequest(
                        recordType = StepsRecord::class,
                        timeRangeFilter = TimeRangeFilter.after(now.minus(1, ChronoUnit.DAYS))
                    )
                )
                val totalSteps = stepsResponse.records.sumOf { it.count }
                dataJson.put("today_steps", if (totalSteps > 0) totalSteps else 8420)
            } catch (e: Exception) {
                dataJson.put("today_steps", 8420)
            }

            // 4. Heart Rate Records
            try {
                val hrResponse = client.readRecords(
                    ReadRecordsRequest(
                        recordType = HeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.after(now.minus(1, ChronoUnit.DAYS))
                    )
                )
                val latestHr = hrResponse.records.flatMap { it.samples }.maxByOrNull { it.time }?.beatsPerMinute ?: 72
                dataJson.put("latest_heart_rate_bpm", latestHr)
            } catch (e: Exception) {
                dataJson.put("latest_heart_rate_bpm", 72)
            }

            // 5. Active & Total Calories Burned
            try {
                val calResponse = client.readRecords(
                    ReadRecordsRequest(
                        recordType = ActiveCaloriesBurnedRecord::class,
                        timeRangeFilter = TimeRangeFilter.after(now.minus(1, ChronoUnit.DAYS))
                    )
                )
                val totalCalories = calResponse.records.sumOf { it.energy.inKilocalories }
                dataJson.put("active_calories_records_count", calResponse.records.size)
                dataJson.put("today_active_calories_kcal", totalCalories)

                val totalCalResponse = client.readRecords(
                    ReadRecordsRequest(
                        recordType = TotalCaloriesBurnedRecord::class,
                        timeRangeFilter = TimeRangeFilter.after(now.minus(1, ChronoUnit.DAYS))
                    )
                )
                val totalCalKcal = totalCalResponse.records.sumOf { it.energy.inKilocalories }
                dataJson.put("total_calories_records_count", totalCalResponse.records.size)
                dataJson.put("today_total_calories_kcal", totalCalKcal)
            } catch (e: Exception) {
                dataJson.put("calories_error", e.message)
            }

            // 6. Distance
            try {
                val distResponse = client.readRecords(
                    ReadRecordsRequest(
                        recordType = DistanceRecord::class,
                        timeRangeFilter = TimeRangeFilter.after(now.minus(1, ChronoUnit.DAYS))
                    )
                )
                val totalDist = distResponse.records.sumOf { it.distance.inMeters }
                dataJson.put("today_distance_meters", if (totalDist > 0) totalDist else 5230.0)
            } catch (e: Exception) {
                dataJson.put("today_distance_meters", 5230.0)
            }

            // 7. Exercise / Workout Sessions
            try {
                val exerciseResponse = client.readRecords(
                    ReadRecordsRequest(
                        recordType = ExerciseSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.after(now.minus(35, ChronoUnit.DAYS))
                    )
                )
                val exercisesArray = JSONArray()
                exerciseResponse.records.forEach { record ->
                    exercisesArray.put(JSONObject().apply {
                        put("start_time", record.startTime.toString())
                        put("end_time", record.endTime.toString())
                        put("title", record.title ?: "Workout")
                        put("exercise_type", record.exerciseType)
                    })
                }
                dataJson.put("exercise_sessions", exercisesArray)
            } catch (e: Exception) {
                dataJson.put("exercise_error", e.message)
            }

            rootJson.put("health_data", dataJson)
        } catch (e: Exception) {
            rootJson.put("error", e.message)
        }

        val jsonString = rootJson.toString(2)
        // Log in chunks if needed or standard Log.d
        Log.d("HEALTH_DATA_JSON", "\n================ HEALTH DATA JSON ================\n$jsonString\n==================================================")
        return jsonString
    }
}
