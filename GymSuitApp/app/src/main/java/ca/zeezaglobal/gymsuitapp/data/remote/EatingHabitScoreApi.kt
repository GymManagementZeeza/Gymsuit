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
 * Reads the overall eating-habit score (0–100) the backend computes from
 * the food the member logged: GET /api/mobile/food/score?days=7.
 * A null score means nothing was logged in the window — the UI shows a
 * "log food to see your score" state instead of a made-up number.
 * Failures return a failed Result and are logged only; a score failure
 * must never break the food UI.
 */
class EatingHabitScoreApi(context: Context) {

    private val authManager = AuthManager(context.applicationContext)

    data class Components(
        val protein: Double,
        val calories: Double,
        val balance: Double,
        val pattern: Double
    )

    data class EatingHabitScore(
        val score: Int?,
        val label: String,
        val daysLogged: Int,
        val windowDays: Int,
        val avgCaloriesKcal: Double,
        val avgProteinG: Double,
        val proteinTargetG: Double,
        val calorieTargetKcal: Int,
        val components: Components,
        val foodQuality: Double
    )

    suspend fun getScore(days: Int = 7, calorieTargetKcal: Int = 2000): Result<EatingHabitScore> =
        withContext(Dispatchers.IO) {
            try {
                val connection = (URL("$BASE_URL?days=$days&calorieTargetKcal=$calorieTargetKcal")
                    .openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                    setRequestProperty("Accept", "application/json")
                    authManager.getAccessToken()?.let { token ->
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                }
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    return@withContext Result.failure(RuntimeException("Eating habit score request failed: $responseCode"))
                }
                val text = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8))
                    .use { it.readText() }
                val json = JSONObject(text)
                val components = json.getJSONObject("components")
                Result.success(
                    EatingHabitScore(
                        score = if (json.isNull("score")) null else json.getInt("score"),
                        label = json.getString("label"),
                        daysLogged = json.getInt("daysLogged"),
                        windowDays = json.getInt("windowDays"),
                        avgCaloriesKcal = json.optDouble("avgCaloriesKcal", 0.0),
                        avgProteinG = json.optDouble("avgProteinG", 0.0),
                        proteinTargetG = json.optDouble("proteinTargetG", 0.0),
                        calorieTargetKcal = json.optInt("calorieTargetKcal", calorieTargetKcal),
                        components = Components(
                            protein = components.optDouble("protein", 0.0),
                            calories = components.optDouble("calories", 0.0),
                            balance = components.optDouble("balance", 0.0),
                            pattern = components.optDouble("pattern", 0.0)
                        ),
                        foodQuality = json.optDouble("foodQuality", 0.0)
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load eating habit score", e)
                Result.failure(e)
            }
        }

    companion object {
        private const val TAG = "EatingHabitScoreApi"
        private const val BASE_URL = "https://api.gymsuit.app/api/mobile/food/score"
    }
}
