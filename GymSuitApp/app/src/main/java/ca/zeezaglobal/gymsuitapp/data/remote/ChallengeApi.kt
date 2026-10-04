package ca.zeezaglobal.gymsuitapp.data.remote

import android.content.Context
import android.util.Log
import ca.zeezaglobal.gymsuitapp.data.local.AuthManager
import ca.zeezaglobal.gymsuitapp.data.model.ChallengeDetail
import ca.zeezaglobal.gymsuitapp.data.model.ChallengeSummary
import ca.zeezaglobal.gymsuitapp.data.model.ProgressTotals
import ca.zeezaglobal.gymsuitapp.data.model.parseChallengeDetail
import ca.zeezaglobal.gymsuitapp.data.model.parseChallengeSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Group fitness challenges backend client.
 * Base: https://api.gymsuit.app/api/mobile/challenges — JWT Bearer auth on every call.
 * Follows the same HttpURLConnection + JSONObject pattern as MobileAuthApi.
 */
class ChallengeApi(context: Context) {

    private val authManager = AuthManager(context.applicationContext)

    companion object {
        private const val TAG = "ChallengeApi"
        private const val BASE_URL = "https://api.gymsuit.app/api/mobile/challenges"
        private const val TIMEOUT_MS = 15000
    }

    suspend fun createChallenge(
        name: String,
        description: String,
        metricType: String,
        startDate: String,
        endDate: String
    ): Result<ChallengeDetail> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("name", name.trim())
                put("metricType", metricType)
                put("startDate", startDate)
                put("endDate", endDate)
                if (description.isNotBlank()) put("description", description.trim())
            }
            val responseJson = postJson(BASE_URL, payload)
            Result.success(parseChallengeDetail(responseJson))
        } catch (e: Exception) {
            Log.e(TAG, "createChallenge failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun listChallenges(): Result<List<ChallengeSummary>> = withContext(Dispatchers.IO) {
        try {
            // getJson wraps a bare JSON array response as {"challenges": [...]}.
            val responseJson = getJson(BASE_URL)
            val list = mutableListOf<ChallengeSummary>()
            val array = responseJson.optJSONArray("challenges")
            if (array != null) {
                for (i in 0 until array.length()) {
                    array.optJSONObject(i)?.let { list.add(parseChallengeSummary(it)) }
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "listChallenges failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getDetail(id: String): Result<ChallengeDetail> = withContext(Dispatchers.IO) {
        try {
            val responseJson = getJson("$BASE_URL/${URLEncoder.encode(id, "UTF-8")}")
            Result.success(parseChallengeDetail(responseJson))
        } catch (e: Exception) {
            Log.e(TAG, "getDetail failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun joinByCode(code: String): Result<ChallengeDetail> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply { put("code", code.trim()) }
            val responseJson = postJson("$BASE_URL/join", payload)
            Result.success(parseChallengeDetail(responseJson))
        } catch (e: Exception) {
            Log.e(TAG, "joinByCode failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun inviteEmail(id: String, email: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply { put("email", email.trim()) }
            val responseJson = postJson("$BASE_URL/${URLEncoder.encode(id, "UTF-8")}/invites", payload)
            Result.success(responseJson.optString("message", "Invite sent"))
        } catch (e: Exception) {
            Log.e(TAG, "inviteEmail failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun postProgress(id: String, totals: ProgressTotals): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("steps", totals.steps)
                put("workouts", totals.workouts)
                put("calories", totals.calories)
                put("distanceKm", totals.distanceKm)
            }
            val responseJson = postJson("$BASE_URL/${URLEncoder.encode(id, "UTF-8")}/progress", payload)
            Result.success(responseJson.optString("message", "Progress synced"))
        } catch (e: Exception) {
            Log.e(TAG, "postProgress failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun leaveChallenge(id: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val responseJson = postJson("$BASE_URL/${URLEncoder.encode(id, "UTF-8")}/leave", JSONObject())
            Result.success(responseJson.optString("message", "You left the challenge"))
        } catch (e: Exception) {
            Log.e(TAG, "leaveChallenge failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ---------- HTTP plumbing (mirrors MobileAuthApi) ----------

    private fun applyAuth(connection: HttpURLConnection) {
        authManager.getAccessToken()?.let { token ->
            connection.setRequestProperty("Authorization", "Bearer $token")
        }
    }

    private fun readResponse(connection: HttpURLConnection, urlString: String): JSONObject {
        val responseCode = connection.responseCode
        Log.d(TAG, "<-- Response Code: $responseCode from $urlString")
        if (responseCode in 200..299) {
            val responseText = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use {
                it.readText()
            }
            Log.d(TAG, "<-- Response Body: $responseText")
            val trimmed = responseText.trim()
            // Backend list endpoint returns a bare JSON array; wrap it so callers
            // always deal with a JSONObject.
            return if (trimmed.startsWith("[")) {
                JSONObject().put("challenges", JSONArray(trimmed))
            } else {
                JSONObject(trimmed)
            }
        } else {
            val errorText = connection.errorStream?.let { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            } ?: "HTTP $responseCode"
            Log.e(TAG, "<-- Error Response ($responseCode): $errorText")
            
            if (responseCode == 401) {
                AuthManager.unauthorizedEvent.tryEmit(Unit)
            }

            val message = try {
                JSONObject(errorText).optString("message", errorText)
            } catch (e: Exception) {
                errorText
            }
            throw Exception(message)
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
            Log.d(TAG, "--> Body: $payloadStr")
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payloadStr)
                writer.flush()
            }
            return readResponse(connection, urlString)
        } catch (e: Exception) {
            Log.e(TAG, "Network call failed for $urlString: ${e.message}", e)
            throw e
        } finally {
            connection?.disconnect()
        }
    }

    private fun getJson(urlString: String): JSONObject {
        var connection: HttpURLConnection? = null
        try {
            Log.d(TAG, "--> GET $urlString")
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doInput = true
            }
            applyAuth(connection)
            return readResponse(connection, urlString)
        } catch (e: Exception) {
            Log.e(TAG, "Network call failed for $urlString: ${e.message}", e)
            throw e
        } finally {
            connection?.disconnect()
        }
    }
}
