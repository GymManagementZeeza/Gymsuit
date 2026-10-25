package ca.zeezaglobal.gymsuitapp.data.remote

import android.content.Context
import android.util.Log
import ca.zeezaglobal.gymsuitapp.data.local.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Reads the workout the member's trainer sent for today.
 * GET /api/mobile/trainer-workout/today — 404 means no workout was sent today.
 */
class TrainerWorkoutApi(context: Context) {

    private val authManager = AuthManager(context.applicationContext)

    data class SuggestedExercise(
        val exerciseName: String,
        val sets: Int,
        val reps: Int,
        val weightKg: Double
    )

    data class TrainerWorkoutSuggestion(
        val id: Long,
        val date: String,
        val trainerName: String,
        val message: String?,
        val exercises: List<SuggestedExercise>
    )

    suspend fun getTodaySuggestion(): Result<TrainerWorkoutSuggestion> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/today")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Accept", "application/json")
                authManager.getAccessToken()?.let { token ->
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }
            val responseCode = connection.responseCode
            if (responseCode == 404) {
                return@withContext Result.failure(NoSuchElementException("No trainer workout for today"))
            }
            if (responseCode !in 200..299) {
                return@withContext Result.failure(RuntimeException("Trainer workout request failed: $responseCode"))
            }
            val responseText = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use {
                it.readText()
            }
            val json = JSONObject(responseText)
            val exercisesJson = json.getJSONArray("exercises")
            val exercises = mutableListOf<SuggestedExercise>()
            for (i in 0 until exercisesJson.length()) {
                val exercise = exercisesJson.getJSONObject(i)
                exercises.add(
                    SuggestedExercise(
                        exerciseName = exercise.getString("exerciseName"),
                        sets = exercise.getInt("sets"),
                        reps = exercise.getInt("reps"),
                        weightKg = exercise.getDouble("weightKg")
                    )
                )
            }
            Result.success(
                TrainerWorkoutSuggestion(
                    id = json.getLong("id"),
                    date = json.getString("date"),
                    trainerName = json.getString("trainerName"),
                    message = if (json.isNull("message")) null else json.getString("message"),
                    exercises = exercises
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load today's trainer workout", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "TrainerWorkoutApi"
        private const val BASE_URL = "https://api.gymsuit.app/api/mobile/trainer-workout"
    }
}
