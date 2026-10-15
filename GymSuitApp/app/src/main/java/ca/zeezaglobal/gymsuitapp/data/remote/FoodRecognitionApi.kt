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

/** Thrown when the backend answers 503 — its inference server is down. */
class FoodRecognitionUnavailableException(message: String) : Exception(message)

data class FoodRecognitionResult(
    val dish: String,
    val confidence: Double,
    val probabilities: Map<String, Double>
)

/**
 * Meal recognition via the backend (`POST /api/mobile/food/recognize`), which
 * proxies to the trained FastAPI inference server. Replaces the old on-device
 * ONNX Food-101 classifier: the model now lives on the server, the app only
 * uploads the photo. Multipart body, field `image`.
 */
class FoodRecognitionApi(private val context: Context) {

    private val authManager = AuthManager(context.applicationContext)

    companion object {
        private const val TAG = "FoodRecognitionApi"
        private const val BASE_URL = "https://api.gymsuit.app/api/mobile/food/recognize"
        private const val TIMEOUT_MS = 20000
        private const val BOUNDARY = "GymSuitFoodBoundary"
    }

    suspend fun recognize(jpegBytes: ByteArray): FoodRecognitionResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            Log.d(TAG, "--> POST $BASE_URL (${jpegBytes.size} bytes)")
            val url = URL(BASE_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$BOUNDARY")
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

            connection.outputStream.use { out ->
                val header = "--$BOUNDARY\r\n" +
                    "Content-Disposition: form-data; name=\"image\"; filename=\"dish.jpg\"\r\n" +
                    "Content-Type: image/jpeg\r\n\r\n"
                out.write(header.toByteArray(Charsets.UTF_8))
                out.write(jpegBytes)
                out.write("\r\n--$BOUNDARY--\r\n".toByteArray(Charsets.UTF_8))
                out.flush()
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "<-- Response Code: $responseCode")
            if (responseCode in 200..299) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                return@withContext parseResult(JSONObject(responseText))
            }
            val errorText = connection.errorStream?.let { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            } ?: "HTTP $responseCode"
            Log.e(TAG, "<-- Error: $errorText")
            if (responseCode == 401) {
                AuthManager.unauthorizedEvent.tryEmit(Unit)
            }
            if (responseCode == 503) {
                throw FoodRecognitionUnavailableException("Food recognition server is unavailable")
            }
            throw Exception("Food recognition failed ($responseCode): $errorText")
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseResult(json: JSONObject): FoodRecognitionResult {
        val probabilities = mutableMapOf<String, Double>()
        val probsJson = json.optJSONObject("probabilities")
        if (probsJson != null) {
            val keys = probsJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                probabilities[key] = probsJson.optDouble(key)
            }
        }
        return FoodRecognitionResult(
            dish = json.optString("dish"),
            confidence = json.optDouble("confidence"),
            probabilities = probabilities
        )
    }
}
