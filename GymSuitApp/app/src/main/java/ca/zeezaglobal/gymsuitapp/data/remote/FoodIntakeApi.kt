package ca.zeezaglobal.gymsuitapp.data.remote

import android.content.Context
import android.util.Log
import ca.zeezaglobal.gymsuitapp.data.FoodEntry
import ca.zeezaglobal.gymsuitapp.data.local.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Uploads food log entries (food_log.json) to the backend so the trainer
 * progress page can read calorie intake from the database. Uploads are
 * idempotent: the backend upserts by the entry id, so re-sending an entry
 * never double-counts. Failures are logged only — a food upload must never
 * break health sync or the food UI.
 */
class FoodIntakeApi(private val context: Context) {

    private val authManager = AuthManager(context.applicationContext)

    companion object {
        private const val TAG = "FoodIntakeApi"
        private const val BASE_URL = "https://api.gymsuit.app/api/mobile/food"
        private const val TIMEOUT_MS = 20000
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    /**
     * @param photos compressed JPEG bytes keyed by entry id. Only photos in
     * this map are sent; entries without a photo entry keep whatever photo
     * the backend already has.
     */
    suspend fun sync(entries: List<FoodEntry>, photos: Map<String, ByteArray> = emptyMap()): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val entriesJson = JSONArray()
            for (entry in entries) {
                val recordDate = Instant.ofEpochMilli(entry.timestampMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(DATE_FORMATTER)
                entriesJson.put(JSONObject().apply {
                    put("externalId", entry.id)
                    put("recordDate", recordDate)
                    put("meal", entry.meal.name)
                    put("dishName", entry.name)
                    put("calories", entry.calories)
                    put("proteinG", entry.proteinG)
                    put("carbsG", entry.carbsG)
                    put("fatG", entry.fatG)
                    photos[entry.id]?.let { bytes ->
                        put("photoBase64", android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP))
                    }
                })
            }
            val payload = JSONObject().apply { put("entries", entriesJson) }
            val response = postJson("$BASE_URL/sync", payload)
            Result.success(response.optInt("synced", entries.size))
        } catch (e: Exception) {
            Log.w(TAG, "Food intake sync failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun delete(externalId: String): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/$externalId")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("Accept", "application/json")
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }
            val token = authManager.getAccessToken()
            if (!token.isNullOrBlank()) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            connection.responseCode in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "Food intake delete failed: ${e.message}")
            false
        } finally {
            connection?.disconnect()
        }
    }

    private fun postJson(urlString: String, payload: JSONObject): JSONObject {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doInput = true
                doOutput = true
            }
            val token = authManager.getAccessToken()
            if (!token.isNullOrBlank()) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                return JSONObject(responseText)
            } else {
                val errorText = connection.errorStream?.let { stream ->
                    BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
                } ?: "HTTP $responseCode"
                if (responseCode == 401) {
                    AuthManager.unauthorizedEvent.tryEmit(Unit)
                }
                throw Exception("Food intake sync failed ($responseCode): $errorText")
            }
        } finally {
            connection?.disconnect()
        }
    }
}
