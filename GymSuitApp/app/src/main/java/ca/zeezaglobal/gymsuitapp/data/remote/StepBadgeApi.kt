package ca.zeezaglobal.gymsuitapp.data.remote

import android.content.Context
import android.util.Log
import ca.zeezaglobal.gymsuitapp.data.local.AuthManager
import ca.zeezaglobal.gymsuitapp.data.model.StepBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * Step milestone badges backend client.
 * Base: https://api.gymsuit.app/api/mobile/badges — JWT Bearer <redacted> on every call.
 * Follows the same HttpURLConnection + JSONObject pattern as ChallengeApi.
 */
class StepBadgeApi(private val context: Context) {

    private val authManager = AuthManager(context.applicationContext)

    companion object {
        private const val TAG = "StepBadgeApi"
        private const val BASE_URL = "https://api.gymsuit.app/api/mobile/badges"
        private const val TIMEOUT_MS = 15000
    }

    suspend fun listBadges(): Result<List<StepBadge>> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(BASE_URL)
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

            val responseCode = connection.responseCode
            if (responseCode == 401) {
                AuthManager.unauthorizedEvent.tryEmit(Unit)
                throw Exception("Unauthorized")
            }
            if (responseCode !in 200..299) {
                throw Exception("Failed to load badges (HTTP $responseCode)")
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val array = JSONArray(responseText)
            val badges = List(array.length()) { i -> StepBadge.fromJson(array.getJSONObject(i)) }
            Result.success(badges)
        } catch (e: Exception) {
            Log.e(TAG, "listBadges failed: ${e.message}", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
}
