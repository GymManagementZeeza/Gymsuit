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
 * Reads the meal plan the member's nutritionist sent: today's, or the most recent earlier one.
 * GET /api/mobile/meal-plan/current — 404 means no plan has been sent.
 */
class MealPlanApi(context: Context) {

    private val authManager = AuthManager(context.applicationContext)

    data class MealPlan(
        val id: Long,
        val date: String,
        val nutritionistName: String,
        val breakfast: String?,
        val lunch: String?,
        val eveningSnack: String?,
        val dinner: String?,
        val note: String?
    )

    suspend fun getCurrent(): Result<MealPlan> = withContext(Dispatchers.IO) {
        try {
            val connection = (URL("$BASE_URL/current").openConnection() as HttpURLConnection).apply {
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
                return@withContext Result.failure(NoSuchElementException("No meal plan"))
            }
            if (responseCode !in 200..299) {
                return@withContext Result.failure(RuntimeException("Meal plan request failed: $responseCode"))
            }
            val text = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { it.readText() }
            val json = JSONObject(text)
            fun opt(key: String) = if (json.isNull(key)) null else json.optString(key).ifBlank { null }
            Result.success(
                MealPlan(
                    id = json.getLong("id"),
                    date = json.getString("date"),
                    nutritionistName = json.getString("nutritionistName"),
                    breakfast = opt("breakfast"),
                    lunch = opt("lunch"),
                    eveningSnack = opt("eveningSnack"),
                    dinner = opt("dinner"),
                    note = opt("note")
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load meal plan", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "MealPlanApi"
        private const val BASE_URL = "https://api.gymsuit.app/api/mobile/meal-plan"
    }
}
