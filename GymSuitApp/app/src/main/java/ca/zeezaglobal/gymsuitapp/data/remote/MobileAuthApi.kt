package ca.zeezaglobal.gymsuitapp.data.remote

import android.util.Log
import ca.zeezaglobal.gymsuitapp.data.local.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MobileAuthApi {

    companion object {
        private const val TAG = "MobileAuthApi"
        private const val BASE_URL = "https://api.gymsuit.app/api/mobile/auth"
        private const val TIMEOUT_MS = 15000
    }

    suspend fun sendOtp(email: String, mode: String = "login"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("mode", mode)
            }
            val responseJson = postJson("$BASE_URL/send-otp", payload)
            Result.success(responseJson.optString("message", "A 6-digit code has been sent to your email"))
        } catch (e: Exception) {
            Log.e(TAG, "sendOtp failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun verifyLoginOtp(email: String, otp: String): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("otp", otp.trim())
            }
            val responseJson = postJson("$BASE_URL/verify-login", payload)
            Result.success(parseSession(responseJson))
        } catch (e: Exception) {
            Log.e(TAG, "verifyLoginOtp failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun registerWithOtp(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        otp: String
    ): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("firstName", firstName.trim())
                put("lastName", lastName.trim())
                put("email", email.trim().lowercase())
                put("phone", phone.trim())
                put("otp", otp.trim())
            }
            val responseJson = postJson("$BASE_URL/register-otp", payload)
            Result.success(parseSession(responseJson))
        } catch (e: Exception) {
            Log.e(TAG, "registerWithOtp failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("password", password)
            }
            val responseJson = postJson("$BASE_URL/login", payload)
            Result.success(parseSession(responseJson))
        } catch (e: Exception) {
            Log.e(TAG, "Login failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        password: String
    ): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("firstName", firstName.trim())
                put("lastName", lastName.trim())
                put("email", email.trim().lowercase())
                put("phone", phone.trim())
                put("password", password)
            }
            val responseJson = postJson("$BASE_URL/register", payload)
            Result.success(parseSession(responseJson))
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun forgotPassword(email: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("email", email.trim().lowercase())
            }
            val responseJson = postJson("$BASE_URL/forgot-password", payload)
            Result.success(responseJson.optString("message", "Verification code sent to your email"))
        } catch (e: Exception) {
            Log.e(TAG, "Forgot password request failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String, otp: String, newPassword: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("otp", otp.trim())
                put("newPassword", newPassword)
            }
            val responseJson = postJson("$BASE_URL/reset-password", payload)
            Result.success(responseJson.optString("message", "Password reset successfully"))
        } catch (e: Exception) {
            Log.e(TAG, "Reset password failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun refreshToken(refreshToken: String): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("refreshToken", refreshToken)
            }
            val responseJson = postJson("$BASE_URL/refresh", payload)
            Result.success(parseSession(responseJson))
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        email: String,
        firstName: String,
        lastName: String
    ): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("firstName", firstName.trim())
                put("lastName", lastName.trim())
            }
            val encodedEmail = java.net.URLEncoder.encode(email.trim().lowercase(), "UTF-8")
            val responseJson = postJson("$BASE_URL/update-profile?email=$encodedEmail", payload)
            Result.success(parseSession(responseJson))
        } catch (e: Exception) {
            Log.e(TAG, "updateProfile failed: ${e.message}", e)
            Result.failure(e)
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

            val payloadStr = payload.toString()
            Log.d(TAG, "--> Body: $payloadStr")
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payloadStr)
                writer.flush()
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "<-- Response Code: $responseCode from $urlString")
            if (responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use {
                    it.readText()
                }
                Log.d(TAG, "<-- Response Body: $responseText")
                return JSONObject(responseText)
            } else {
                val errorText = connection.errorStream?.let { stream ->
                    BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
                } ?: "HTTP $responseCode"

                Log.e(TAG, "<-- Error Response ($responseCode): $errorText")
                val message = try {
                    JSONObject(errorText).optString("message", errorText)
                } catch (e: Exception) {
                    errorText
                }
                throw Exception(message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network call failed for $urlString: ${e.message}", e)
            throw e
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseSession(json: JSONObject): UserSession {
        return UserSession(
            token = json.getString("token"),
            refreshToken = json.optString("refreshToken", ""),
            email = json.optString("email", ""),
            firstName = json.optString("firstName", ""),
            lastName = json.optString("lastName", ""),
            role = json.optString("role", "MEMBER"),
            memberId = if (json.has("memberId") && !json.isNull("memberId")) json.getLong("memberId") else null,
            gymId = if (json.has("gymId") && !json.isNull("gymId")) json.getLong("gymId") else null,
            gymName = if (json.has("gymName") && !json.isNull("gymName")) json.getString("gymName") else null
        )
    }
}
