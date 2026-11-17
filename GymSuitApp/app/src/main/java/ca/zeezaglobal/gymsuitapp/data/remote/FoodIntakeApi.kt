package ca.zeezaglobal.gymsuitapp.data.remote

import android.content.Context
import android.util.Log
import ca.zeezaglobal.gymsuitapp.data.FoodEntry
import ca.zeezaglobal.gymsuitapp.data.MealType
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
import java.time.LocalDate
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

    /**
     * Fetches the member's food entries from [from] (default: 30 days back)
     * through today, so the app can show meals logged on another device, the
     * diet website or an MCP client. Entries map back to [FoodEntry] by their
     * externalId, so the caller merges by id and never duplicates a local one.
     * The within-day time is derived from the meal slot (breakfast/lunch/…),
     * since the server stores only the date. Best-effort: a failure is logged
     * and returns an empty list rather than disturbing the local log.
     */
    suspend fun fetch(from: LocalDate = LocalDate.now().minusDays(30)): Result<List<FoodEntry>> =
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
                    return@withContext Result.failure(Exception("Food fetch failed ($code)"))
                }
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(text)
                val zone = ZoneId.systemDefault()
                val entries = ArrayList<FoodEntry>(arr.length())
                for (i in 0 until arr.length()) {
                    entries.add(toFoodEntry(arr.getJSONObject(i), zone))
                }
                Result.success(entries)
            } catch (e: Exception) {
                Log.w(TAG, "Food fetch failed: ${e.message}")
                Result.failure(e)
            } finally {
                connection?.disconnect()
            }
        }

    private fun toFoodEntry(o: JSONObject, zone: ZoneId): FoodEntry {
        val meal = runCatching { MealType.valueOf(o.optString("meal")) }
            .getOrDefault(MealType.SNACK)
        val date = runCatching { LocalDate.parse(o.optString("recordDate")) }
            .getOrDefault(LocalDate.now())
        val millis = date.atTime(mealHour(meal), 0).atZone(zone).toInstant().toEpochMilli()
        return FoodEntry(
            id = o.optString("externalId"),
            name = o.optString("dishName"),
            meal = meal,
            calories = o.optInt("calories"),
            proteinG = o.optDouble("proteinG", 0.0),
            carbsG = o.optDouble("carbsG", 0.0),
            fatG = o.optDouble("fatG", 0.0),
            timestampMillis = millis
        )
    }

    /**
     * Sends free text ("2 idlis for breakfast, biryani for lunch") to the backend,
     * where the AI turns it into meals with estimated nutrition that are saved.
     * Returns the saved entries for the caller to merge into the local log.
     */
    suspend fun aiLog(text: String, date: LocalDate): Result<List<FoodEntry>> =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL("$BASE_URL/ai-log").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = TIMEOUT_MS
                    readTimeout = 60000 // the AI call can take a while
                }
                val token = authManager.getAccessToken()
                if (!token.isNullOrBlank()) {
                    connection.setRequestProperty("Authorization", "Bearer $token")
                }
                val body = JSONObject().put("text", text).put("date", date.format(DATE_FORMATTER))
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
                val code = connection.responseCode
                if (code == 401) AuthManager.unauthorizedEvent.tryEmit(Unit)
                if (code !in 200..299) {
                    val err = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    val msg = runCatching { JSONObject(err).optString("message") }.getOrNull()
                    return@withContext Result.failure(
                        Exception(msg?.takeIf { it.isNotBlank() } ?: "Couldn't log meals ($code)")
                    )
                }
                val arr = JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
                val zone = ZoneId.systemDefault()
                Result.success(List(arr.length()) { toFoodEntry(arr.getJSONObject(it), zone) })
            } catch (e: Exception) {
                Log.w(TAG, "AI food log failed: ${e.message}")
                Result.failure(Exception("Couldn't reach GymSuit. Check your connection."))
            } finally {
                connection?.disconnect()
            }
        }

    /** A sensible within-day hour per meal, so pulled entries sort naturally. */
    private fun mealHour(meal: MealType): Int = when (meal) {
        MealType.BREAKFAST -> 8
        MealType.LUNCH -> 13
        MealType.SNACK -> 16
        MealType.DINNER -> 20
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
