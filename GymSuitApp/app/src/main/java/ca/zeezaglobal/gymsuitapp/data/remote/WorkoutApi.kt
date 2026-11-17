package ca.zeezaglobal.gymsuitapp.data.remote

import android.content.Context
import android.util.Log
import ca.zeezaglobal.gymsuitapp.data.local.AuthManager
import ca.zeezaglobal.gymsuitapp.data.model.LoggedWorkout
import ca.zeezaglobal.gymsuitapp.data.model.WorkoutSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Pulls workouts from the backend (GET /api/mobile/workout) so the app can show
 * sessions logged elsewhere — another device, or an MCP client ("I did 2 sets
 * of bench press…"). Each row maps back to a [LoggedWorkout] by its externalId,
 * so the caller merges by id and never duplicates a local one. Mirrors
 * [FoodIntakeApi.fetch]. Best-effort: a failure returns an empty list.
 */
class WorkoutApi(private val context: Context) {

    private val authManager = AuthManager(context.applicationContext)

    companion object {
        private const val TAG = "WorkoutApi"
        private const val BASE_URL = "https://api.gymsuit.app/api/mobile/workout"
        private const val TIMEOUT_MS = 20000
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    suspend fun fetch(from: LocalDate = LocalDate.now().minusDays(30)): Result<List<LoggedWorkout>> =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL("$BASE_URL?from=${from.format(DATE_FORMATTER)}")
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                }
                val token = authManager.getAccessToken()
                if (!token.isNullOrBlank()) {
                    connection.setRequestProperty("Authorization", "Bearer $token")
                }
                val code = connection.responseCode
                if (code == 401) {
                    AuthManager.unauthorizedEvent.tryEmit(Unit)
                }
                if (code !in 200..299) {
                    return@withContext Result.failure(Exception("Workout fetch failed ($code)"))
                }
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(text)
                val zone = ZoneId.systemDefault()
                val workouts = ArrayList<LoggedWorkout>(arr.length())
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    workouts.add(toLoggedWorkout(o, zone))
                }
                Result.success(workouts)
            } catch (e: Exception) {
                Log.w(TAG, "Workout fetch failed: ${e.message}")
                Result.failure(e)
            } finally {
                connection?.disconnect()
            }
        }

    private fun toLoggedWorkout(o: JSONObject, zone: ZoneId): LoggedWorkout {
        val startMillis = runCatching {
            LocalDateTime.parse(o.optString("startTime"))
                .atZone(zone).toInstant().toEpochMilli()
        }.getOrDefault(System.currentTimeMillis())

        val setCount = if (o.isNull("setCount")) 0 else o.optInt("setCount")
        val totalReps = if (o.isNull("totalReps")) 0 else o.optInt("totalReps")
        val totalVolume = if (o.isNull("totalVolumeKg")) 0.0 else o.optDouble("totalVolumeKg", 0.0)
        // Reconstruct uniform sets from the stored aggregates (a session logs as
        // setCount sets of totalReps/setCount reps at totalVolume/totalReps kg).
        val sets: List<WorkoutSet> = if (setCount > 0 && totalReps > 0) {
            val repsPerSet = (totalReps.toDouble() / setCount).roundToInt().coerceAtLeast(1)
            val weightPerRep = if (totalVolume > 0) totalVolume / totalReps else 0.0
            List(setCount) { WorkoutSet(reps = repsPerSet, weightKg = weightPerRep) }
        } else {
            emptyList()
        }

        val calories = if (o.isNull("caloriesBurned")) null else o.optDouble("caloriesBurned")
        val notes = calories?.let { "≈ ${it.roundToInt()} kcal" } ?: ""

        return LoggedWorkout(
            id = o.optString("externalId"),
            exerciseId = "",
            exerciseName = o.optString("title"),
            timestampMillis = startMillis,
            sets = sets,
            durationMinutes = if (o.isNull("durationMinutes")) 0 else o.optInt("durationMinutes"),
            notes = notes,
            syncedToHealth = true,
            caloriesBurned = calories?.roundToInt()
        )
    }
}
