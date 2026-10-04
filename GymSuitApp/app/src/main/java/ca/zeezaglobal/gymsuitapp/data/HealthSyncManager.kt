package ca.zeezaglobal.gymsuitapp.data

import android.content.Context
import android.util.Log
import ca.zeezaglobal.gymsuitapp.data.local.AuthManager
import ca.zeezaglobal.gymsuitapp.data.model.DailyHealthSyncDto
import ca.zeezaglobal.gymsuitapp.data.model.HealthSyncRequest
import ca.zeezaglobal.gymsuitapp.data.model.LoggedWorkout
import ca.zeezaglobal.gymsuitapp.data.model.WorkoutSet
import ca.zeezaglobal.gymsuitapp.data.model.WorkoutSyncDto
import ca.zeezaglobal.gymsuitapp.data.remote.HealthSyncApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class HealthSyncManager(private val context: Context) {

    private val authManager = AuthManager(context.applicationContext)
    private val healthConnectManager = HealthConnectManager(context.applicationContext)
    private val workoutStore = WorkoutStore(context.applicationContext)
    private val api = HealthSyncApi(context.applicationContext)
    private val prefs = context.getSharedPreferences("health_sync_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "HealthSyncManager"
        private const val KEY_LAST_SYNC_ISO = "last_sync_iso"
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT
    }

    suspend fun sync(): Boolean = withContext(Dispatchers.IO) {
        if (!authManager.isLoggedIn()) {
            return@withContext false
        }

        try {
            val lastSyncIso = prefs.getString(KEY_LAST_SYNC_ISO, null)
            val today = LocalDate.now()

            // 1. Gather local Health Connect daily metrics for past 7 days
            val dailyDtos = mutableListOf<DailyHealthSyncDto>()
            for (offset in 0 until 7) {
                val targetDate = today.minusDays(offset.toLong())
                val dateStr = targetDate.format(DATE_FORMATTER)

                val progress = healthConnectManager.readProgressTotals(targetDate, targetDate)
                val cal = healthConnectManager.readCaloriesBreakdownForDate(targetDate)
                val hr = healthConnectManager.tryReadHeartRateDataForDate(targetDate)
                val sleep = healthConnectManager.readDetailedSleepForDate(targetDate)

                val hasData = progress.steps > 0 || cal.hasData || hr?.hasData == true || sleep?.hasData == true
                if (hasData) {
                    var sStartIso: String? = null
                    var sEndIso: String? = null
                    if (sleep?.hasData == true && sleep.startTime != null && sleep.endTime != null) {
                        sStartIso = ISO_FORMATTER.format(sleep.startTime)
                        sEndIso = ISO_FORMATTER.format(sleep.endTime)
                    }

                    dailyDtos.add(
                        DailyHealthSyncDto(
                            recordDate = dateStr,
                            steps = if (progress.steps > 0) progress.steps else null,
                            activeCalories = if (cal.hasData && cal.workoutKcal + cal.moveKcal > 0) cal.workoutKcal + cal.moveKcal else null,
                            totalCalories = if (cal.hasData && cal.totalKcal > 0) cal.totalKcal else null,
                            distanceMeters = if (progress.distanceKm > 0) progress.distanceKm * 1000.0 else null,
                            latestHeartRateBpm = if (hr?.hasData == true && hr.latestBpm > 0) hr.latestBpm else null,
                            restingHeartRateBpm = null,
                            minHeartRateBpm = if (hr?.hasData == true && hr.minBpm > 0) hr.minBpm else null,
                            maxHeartRateBpm = if (hr?.hasData == true && hr.maxBpm > 0) hr.maxBpm else null,
                            sleepDurationMinutes = if (sleep?.hasData == true && sleep.totalSleepMinutes > 0) sleep.totalSleepMinutes else null,
                            sleepStartTime = sStartIso,
                            sleepEndTime = sEndIso,
                            sourceDevice = "ANDROID_HEALTH_CONNECT"
                        )
                    )
                }
            }

            // 2. Gather logged workouts
            val loggedWorkouts = workoutStore.getLoggedWorkouts()
            val workoutDtos = loggedWorkouts.map { w ->
                val startInstant = Instant.ofEpochMilli(w.timestampMillis)
                val endInstant = startInstant.plusSeconds((w.durationMinutes.coerceAtLeast(1) * 60).toLong())
                val totalReps = w.sets.sumOf { it.reps }
                val totalVolume = w.sets.sumOf { it.reps * it.weightKg }

                WorkoutSyncDto(
                    externalId = w.id,
                    title = w.exerciseName,
                    startTime = ISO_FORMATTER.format(startInstant),
                    endTime = ISO_FORMATTER.format(endInstant),
                    durationMinutes = w.durationMinutes,
                    caloriesBurned = null,
                    setCount = w.sets.size,
                    totalReps = totalReps,
                    totalVolumeKg = totalVolume,
                    sourceDevice = "ANDROID_HEALTH_CONNECT"
                )
            }

            // 3. Post to backend
            val request = HealthSyncRequest(
                lastSyncTime = lastSyncIso,
                clientDevice = "ANDROID_HEALTH_CONNECT",
                dailyRecords = dailyDtos,
                workouts = workoutDtos
            )

            val result = api.sync(request)
            result.onSuccess { response ->
                // 4. Ingest remote daily records from backend into Health Connect
                for (remote in response.remoteDailyRecords) {
                    if (remote.sourceDevice != "ANDROID_HEALTH_CONNECT") {
                        try {
                            val recordDate = LocalDate.parse(remote.recordDate, DATE_FORMATTER)
                            val startOfDay = recordDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                            val endOfDay = recordDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

                            remote.steps?.let { steps ->
                                if (steps > 0) healthConnectManager.insertSteps(steps, startOfDay, endOfDay)
                            }
                            remote.activeCalories?.let { cal ->
                                if (cal > 0) healthConnectManager.insertActiveCalories(cal, startOfDay, endOfDay)
                            }
                            remote.distanceMeters?.let { dist ->
                                if (dist > 0) healthConnectManager.insertDistance(dist, startOfDay, endOfDay)
                            }
                            remote.latestHeartRateBpm?.let { bpm ->
                                if (bpm > 0) healthConnectManager.insertHeartRate(bpm, endOfDay.minusSeconds(60))
                            }
                            if (!remote.sleepStartTime.isNullOrBlank() && !remote.sleepEndTime.isNullOrBlank()) {
                                val sStart = Instant.parse(remote.sleepStartTime)
                                val sEnd = Instant.parse(remote.sleepEndTime)
                                healthConnectManager.insertSleepSession(sStart, sEnd)
                            }
                            remote.weightKg?.let { wt ->
                                if (wt > 0) healthConnectManager.insertWeight(wt, endOfDay)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error writing remote daily record to Health Connect", e)
                        }
                    }
                }

                // 5. Ingest remote workouts into Health Connect & WorkoutStore
                for (rw in response.remoteWorkouts) {
                    if (rw.sourceDevice != "ANDROID_HEALTH_CONNECT") {
                        try {
                            val startInstant = Instant.parse(rw.startTime)
                            val endInstant = Instant.parse(rw.endTime)
                            val duration = rw.durationMinutes ?: 30
                            val sets = rw.setCount ?: 3
                            val reps = rw.totalReps ?: 30
                            val volume = rw.totalVolumeKg ?: 0.0

                            healthConnectManager.insertExerciseSession(
                                exerciseName = rw.title,
                                start = startInstant,
                                end = endInstant,
                                setCount = sets,
                                totalReps = reps,
                                totalVolumeKg = volume
                            )

                            val exists = loggedWorkouts.any { it.id == rw.externalId }
                            if (!exists) {
                                val newLogged = LoggedWorkout(
                                    id = rw.externalId,
                                    exerciseId = rw.title.lowercase().replace(" ", "_"),
                                    exerciseName = rw.title,
                                    timestampMillis = startInstant.toEpochMilli(),
                                    sets = List(sets) {
                                        WorkoutSet(reps = reps / sets.coerceAtLeast(1), weightKg = volume / reps.coerceAtLeast(1))
                                    },
                                    durationMinutes = duration,
                                    notes = "Synced from ${rw.sourceDevice ?: "iOS"}",
                                    syncedToHealth = true
                                )
                                workoutStore.logWorkout(newLogged)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error writing remote workout", e)
                        }
                    }
                }

                // Update sync checkpoint
                prefs.edit().putString(KEY_LAST_SYNC_ISO, ISO_FORMATTER.format(Instant.now())).apply()
                Log.i(TAG, "Sync complete: uploaded ${response.uploadedDailyCount} daily, ${response.uploadedWorkoutCount} workouts")
            }.onFailure { err ->
                Log.e(TAG, "Sync failed: ${err.message}", err)
                return@withContext false
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "HealthSyncManager exception: ${e.message}", e)
            false
        }
    }
}
