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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    data class Success(val message: String = "All data synced") : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

class HealthSyncManager(private val context: Context) {

    private val authManager = AuthManager(context.applicationContext)
    private val healthConnectManager = HealthConnectManager(context.applicationContext)
    private val workoutStore = WorkoutStore(context.applicationContext)
    private val api = HealthSyncApi(context.applicationContext)
    private val prefs = context.getSharedPreferences("health_sync_prefs", Context.MODE_PRIVATE)

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private var autoResetJob: Job? = null

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
        if (_syncStatus.value is SyncStatus.Syncing) {
            return@withContext true
        }

        _syncStatus.value = SyncStatus.Syncing
        autoResetJob?.cancel()

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

            // 3. Gather local points history
            val localPoints = PointsStore.history.value.map { p ->
                val iso = ISO_FORMATTER.format(Instant.ofEpochMilli(p.timestampMillis))
                ca.zeezaglobal.gymsuitapp.data.model.PointsTransactionDto(
                    id = p.id,
                    points = p.points,
                    reason = p.reason,
                    rupeeValue = p.rupeeValue,
                    createdAt = iso,
                    deviceId = "ANDROID_APP"
                )
            }

            // 4. Post to backend
            val request = HealthSyncRequest(
                lastSyncTime = lastSyncIso,
                clientDevice = "ANDROID_HEALTH_CONNECT",
                dailyRecords = dailyDtos,
                workouts = workoutDtos,
                pointsTransactions = localPoints
            )

            val result = api.sync(request)
            result.onSuccess { response ->
                // 4. Ingest remote daily records from backend into Health Connect (larger data wins)
                for (remote in response.remoteDailyRecords) {
                    if (remote.sourceDevice != "ANDROID_HEALTH_CONNECT") {
                        try {
                            val recordDate = LocalDate.parse(remote.recordDate, DATE_FORMATTER)
                            val startOfDay = recordDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                            val endOfDay = recordDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

                            val progress = healthConnectManager.readProgressTotals(recordDate, recordDate)
                            val cal = healthConnectManager.readCaloriesBreakdownForDate(recordDate)
                            val hr = healthConnectManager.tryReadHeartRateDataForDate(recordDate)
                            val sleep = healthConnectManager.readDetailedSleepForDate(recordDate)

                            // Prioritize device with larger data
                            remote.steps?.let { steps ->
                                if (steps > progress.steps) {
                                    healthConnectManager.insertSteps(steps, startOfDay, endOfDay)
                                }
                            }

                            val localActiveCal = if (cal.hasData) (cal.workoutKcal + cal.moveKcal) else 0.0
                            remote.activeCalories?.let { rCal ->
                                if (rCal > localActiveCal) {
                                    healthConnectManager.insertActiveCalories(rCal, startOfDay, endOfDay)
                                }
                            }

                            val localDistanceMeters = progress.distanceKm * 1000.0
                            remote.distanceMeters?.let { dist ->
                                if (dist > localDistanceMeters) {
                                    healthConnectManager.insertDistance(dist, startOfDay, endOfDay)
                                }
                            }

                            remote.latestHeartRateBpm?.let { bpm ->
                                if (bpm > 0 && (hr == null || !hr.hasData || hr.latestBpm == 0)) {
                                    healthConnectManager.insertHeartRate(bpm, endOfDay.minusSeconds(60))
                                }
                            }

                            val localSleepMinutes = if (sleep?.hasData == true) sleep.totalSleepMinutes else 0
                            val remoteSleepMinutes = remote.sleepDurationMinutes ?: 0
                            if (remoteSleepMinutes > localSleepMinutes &&
                                !remote.sleepStartTime.isNullOrBlank() &&
                                !remote.sleepEndTime.isNullOrBlank()) {
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

                // 5. Ingest remote workouts into Health Connect & WorkoutStore (larger data wins)
                for (rw in response.remoteWorkouts) {
                    if (rw.sourceDevice != "ANDROID_HEALTH_CONNECT") {
                        try {
                            val startInstant = Instant.parse(rw.startTime)
                            val endInstant = Instant.parse(rw.endTime)
                            val duration = rw.durationMinutes ?: 30
                            val sets = rw.setCount ?: 3
                            val reps = rw.totalReps ?: 30
                            val volume = rw.totalVolumeKg ?: 0.0

                            // Calculate data volume score for remote workout
                            val remoteScore = volume + (reps * 10.0) + (duration * 5.0)

                            // Check for overlapping local workout (same id or start time within 45 mins)
                            val overlappingLocal = loggedWorkouts.firstOrNull { local ->
                                local.id == rw.externalId ||
                                Math.abs(local.timestampMillis - startInstant.toEpochMilli()) < 45 * 60 * 1000L
                            }

                            if (overlappingLocal != null) {
                                val localReps = overlappingLocal.sets.sumOf { it.reps }
                                val localVolume = overlappingLocal.sets.sumOf { it.reps * it.weightKg }
                                val localScore = localVolume + (localReps * 10.0) + (overlappingLocal.durationMinutes * 5.0)

                                // If local already has larger or equal data, keep local
                                if (localScore >= remoteScore) {
                                    continue
                                }

                                // Remote has larger data: update existing workout in WorkoutStore
                                val updatedLogged = LoggedWorkout(
                                    id = overlappingLocal.id,
                                    exerciseId = rw.title.lowercase().replace(" ", "_"),
                                    exerciseName = rw.title,
                                    timestampMillis = startInstant.toEpochMilli(),
                                    sets = List(sets) {
                                        WorkoutSet(reps = reps / sets.coerceAtLeast(1), weightKg = volume / reps.coerceAtLeast(1))
                                    },
                                    durationMinutes = duration,
                                    notes = "Updated from ${rw.sourceDevice ?: "iOS"} (richer data)",
                                    syncedToHealth = true
                                )
                                workoutStore.upsertWorkout(updatedLogged)
                            } else {
                                // New workout from remote
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
                                workoutStore.upsertWorkout(newLogged)
                            }

                            // Write to Health Connect
                            healthConnectManager.insertExerciseSession(
                                exerciseName = rw.title,
                                start = startInstant,
                                end = endInstant,
                                setCount = sets,
                                totalReps = reps,
                                totalVolumeKg = volume
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "Error writing remote workout", e)
                        }
                    }
                }

                // 6. Ingest remote points balance & transactions
                PointsStore.mergeRemote(response.pointsBalance, response.pointsTransactions)

                // Update sync checkpoint
                prefs.edit().putString(KEY_LAST_SYNC_ISO, ISO_FORMATTER.format(Instant.now())).apply()
                Log.i(TAG, "Sync complete: uploaded ${response.uploadedDailyCount} daily, ${response.uploadedWorkoutCount} workouts")

                _syncStatus.value = SyncStatus.Success("All data synced")
                autoResetJob = CoroutineScope(Dispatchers.Default).launch {
                    delay(3500)
                    if (_syncStatus.value is SyncStatus.Success) {
                        _syncStatus.value = SyncStatus.Idle
                    }
                }
            }.onFailure { err ->
                Log.e(TAG, "Sync failed: ${err.message}", err)
                _syncStatus.value = SyncStatus.Error(err.message ?: "Sync failed")
                autoResetJob = CoroutineScope(Dispatchers.Default).launch {
                    delay(3500)
                    if (_syncStatus.value is SyncStatus.Error) {
                        _syncStatus.value = SyncStatus.Idle
                    }
                }
                return@withContext false
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "HealthSyncManager exception: ${e.message}", e)
            _syncStatus.value = SyncStatus.Error(e.message ?: "Sync failed")
            autoResetJob = CoroutineScope(Dispatchers.Default).launch {
                delay(3500)
                if (_syncStatus.value is SyncStatus.Error) {
                    _syncStatus.value = SyncStatus.Idle
                }
            }
            false
        }
    }
}
