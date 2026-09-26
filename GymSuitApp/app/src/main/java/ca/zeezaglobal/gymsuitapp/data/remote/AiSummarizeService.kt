package ca.zeezaglobal.gymsuitapp.data.remote

import android.util.Log
import ca.zeezaglobal.gymsuitapp.data.model.AiSummarizeRequest
import ca.zeezaglobal.gymsuitapp.data.model.AiSummarizeResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

interface AiSummarizeService {
    suspend fun getSummary(request: AiSummarizeRequest): Result<AiSummarizeResponse>
}

class AiSummarizeServiceImpl : AiSummarizeService {

    companion object {
        private const val TAG = "GymSuitApi"
        private const val API_URL = "https://api.gymsuit.app/api/ai/summarize"
        private const val TIMEOUT_MS = 20000
    }

    override suspend fun getSummary(request: AiSummarizeRequest): Result<AiSummarizeResponse> =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                Log.d(TAG, "--> POST $API_URL")
                val url = URL(API_URL)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    doInput = true
                    doOutput = true
                }

                // Write request JSON payload
                val requestJson = request.toJsonString()
                Log.d(TAG, "--> Payload: $requestJson")
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(requestJson)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                Log.d(TAG, "<-- Response Code: $responseCode")

                if (responseCode in 200..299) {
                    val responseText = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use {
                        it.readText()
                    }
                    Log.d(TAG, "<-- Response Body: $responseText")
                    val jsonObject = JSONObject(responseText)
                    val summary = jsonObject.optString("summary", "")
                    Result.success(AiSummarizeResponse(summary = summary))
                } else {
                    val errorText = connection.errorStream?.let { stream ->
                        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
                    } ?: "HTTP $responseCode"
                    Log.e(TAG, "<-- Error Response ($responseCode): $errorText")
                    Result.failure(Exception("AI Summary failed ($responseCode): $errorText"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network/API exception occurred: ${e.message}", e)
                Result.failure(e)
            } finally {
                connection?.disconnect()
            }
        }
}
