package ca.zeezaglobal.gymsuitapp.data.remote

import android.content.Context
import android.util.Log
import ca.zeezaglobal.gymsuitapp.data.local.AuthManager
import ca.zeezaglobal.gymsuitapp.data.model.HealthSyncRequest
import ca.zeezaglobal.gymsuitapp.data.model.HealthSyncResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class HealthSyncApi(context: Context) {

    private val authManager = AuthManager(context.applicationContext)

    companion object {
        private const val TAG = "HealthSyncApi"
        private const val BASE_URL = "https://api.gymsuit.app/api/mobile/health/sync"
        private const val TIMEOUT_MS = 20000
    }

    suspend fun sync(request: HealthSyncRequest): Result<HealthSyncResponse> = withContext(Dispatchers.IO) {
        try {
            val payload = request.toJsonObject()
            val responseJson = postJson(BASE_URL, payload)
            Result.success(HealthSyncResponse.fromJsonObject(responseJson))
        } catch (e: Exception) {
            Log.e(TAG, "Health sync failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun applyAuth(connection: HttpURLConnection) {
        val token = authManager.getAccessToken()
        if (!token.isNullOrBlank()) {
            connection.setRequestProperty("Authorization", "Bearer $token")
        }
    }

    private fun postJson(urlString: String, payload: JSONObject): JSONObject {
        var connection: HttpURLConnection? = null
        try {
            Log.d(TAG, "--> POST $urlString")
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
            applyAuth(connection)

            val payloadStr = payload.toString()
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payloadStr)
                writer.flush()
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "<-- Response Code: $responseCode")

            if (responseCode in 200..299) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                return JSONObject(responseText)
            } else {
                val errorText = connection.errorStream?.let { stream ->
                    BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
                } ?: "HTTP $responseCode"
                Log.e(TAG, "<-- Error: $errorText")
                if (responseCode == 401) {
                    AuthManager.unauthorizedEvent.tryEmit(Unit)
                }
                throw Exception("Sync failed ($responseCode): $errorText")
            }
        } finally {
            connection?.disconnect()
        }
    }
}
