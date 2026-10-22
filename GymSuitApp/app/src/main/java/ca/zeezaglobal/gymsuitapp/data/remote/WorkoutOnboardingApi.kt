package ca.zeezaglobal.gymsuitapp.data.remote

import android.content.Context
import android.util.Log
import ca.zeezaglobal.gymsuitapp.data.WorkoutOnboardingStore
import ca.zeezaglobal.gymsuitapp.data.WorkoutStore
import ca.zeezaglobal.gymsuitapp.data.local.AuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * Response payload of the workout onboarding endpoints.
 * Mirror of the iOS `WorkoutOnboardingData` (every field optional).
 */
data class WorkoutOnboardingData(
    val memberId: Long?,
    val workoutGoal: String?,
    val heightCm: Double?,
    val weightKg: Double?,
    val targetWeightKg: Double?,
    val birthYear: Int?,
    val dateOfBirth: String?,
    val workoutDaysPerWeek: Int?,
    val targetDate: String?,
    val message: String?
) {
    companion object {
        fun fromJson(json: JSONObject): WorkoutOnboardingData = WorkoutOnboardingData(
            memberId = if (json.isNull("memberId")) null else json.optLong("memberId"),
            workoutGoal = if (json.isNull("workoutGoal")) null else json.optString("workoutGoal"),
            heightCm = if (json.isNull("heightCm")) null else json.optDouble("heightCm"),
            weightKg = if (json.isNull("weightKg")) null else json.optDouble("weightKg"),
            targetWeightKg = if (json.isNull("targetWeightKg")) null else json.optDouble("targetWeightKg"),
            birthYear = if (json.isNull("birthYear")) null else json.optInt("birthYear"),
            dateOfBirth = if (json.isNull("dateOfBirth")) null else json.optString("dateOfBirth"),
            workoutDaysPerWeek = if (json.isNull("workoutDaysPerWeek")) null else json.optInt("workoutDaysPerWeek"),
            targetDate = if (json.isNull("targetDate")) null else json.optString("targetDate"),
            message = if (json.isNull("message")) null else json.optString("message")
        )
    }
}

/**
 * Client for the workout onboarding endpoints:
 *   POST   https://api.gymsuit.app/api/mobile/onboarding/sync
 *   GET    https://api.gymsuit.app/api/mobile/onboarding?email=
 *   DELETE https://api.gymsuit.app/api/mobile/onboarding?email=
 *
 * Mirror of the iOS `WorkoutOnboardingApi` (Services/WorkoutOnboardingApi.swift):
 * same endpoints, same JSON payload, same call timing, same fallbacks.
 * Follows the app's existing networking pattern (see StepBadgeApi):
 * HttpURLConnection + JSONObject on Dispatchers.IO, bearer token from
 * AuthManager, 15s timeouts, a 401 feeds AuthManager.unauthorizedEvent.
 */
class WorkoutOnboardingApi(context: Context) {

    private val appContext = context.applicationContext
    private val authManager = AuthManager(appContext)
    private val store = WorkoutOnboardingStore(appContext)

    companion object {
        private const val TAG = "WorkoutOnboardingApi"
        private const val BASE_URL = "https://api.gymsuit.app/api/mobile/onboarding"
        private const val TIMEOUT_MS = 15_000
    }

    /**
     * POSTs the collected answers. Called when the onboarding finishes
     * (WorkoutOnboardingApi.syncOnboarding in iOS). A non-2xx response fails
     * with the server's "message" when present, mirroring the iOS error.
     */
    suspend fun syncOnboarding(
        workoutGoal: String,
        heightCm: Double,
        weightKg: Double,
        targetWeightKg: Double,
        birthYear: Int,
        workoutDaysPerWeek: Int,
        targetDate: Date
    ): Result<WorkoutOnboardingData> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("workoutGoal", workoutGoal)
                put("heightCm", heightCm)
                put("weightKg", weightKg)
                put("targetWeightKg", targetWeightKg)
                put("birthYear", birthYear)
                put("workoutDaysPerWeek", workoutDaysPerWeek)
                put("targetDate", isoDateString(targetDate))
                sessionEmail()?.let { put("email", it) }
            }
            val json = requestJson("POST", "$BASE_URL/sync", payload)
            Result.success(WorkoutOnboardingData.fromJson(json))
        } catch (e: Exception) {
            Log.e(TAG, "syncOnboarding failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /** GETs the onboarding data previously synced for the signed-in user. */
    suspend fun fetchOnboarding(): Result<WorkoutOnboardingData> = withContext(Dispatchers.IO) {
        try {
            val json = requestJson("GET", urlWithEmail(BASE_URL), null)
            Result.success(WorkoutOnboardingData.fromJson(json))
        } catch (e: Exception) {
            Log.e(TAG, "fetchOnboarding failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Mirror of iOS `deleteOnboardingData()`: DELETEs the cloud record
     * (network failures are swallowed, exactly like iOS's `try?`), then
     * clears the local onboarding answers and all logged workouts so the
     * onboarding wizard restarts.
     */
    suspend fun deleteOnboardingData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            try {
                requestRaw("DELETE", urlWithEmail(BASE_URL))
            } catch (e: Exception) {
                Log.w(TAG, "deleteOnboardingData request failed (ignored, mirroring iOS): ${e.message}")
            }
            store.clearOnboardingData()
            WorkoutStore(appContext).clearAllWorkouts()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteOnboardingData failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Mirror of iOS `syncPendingOnboardingIfNeeded()`, called by iOS from
     * ContentView.onAppear when logged in and after every login. Android
     * calls it from the dashboard on appear.
     *
     * Only runs when setup is complete and a height and weight are stored.
     * Unknown fields fall back exactly like iOS: target weight → current
     * weight, birth year → 2003, days per week → 2. The target date is
     * recomputed with the accelerated formula from the projection step
     * (one week per kg of difference, minimum 14 days out). Fire-and-forget.
     */
    fun syncPendingOnboardingIfNeeded(scope: CoroutineScope) {
        if (!store.setupCompleted) return
        val workoutGoal = store.selectedGoalRaw
        val heightCm = store.heightCm
        val weightKg = store.weightKg
        val targetWeightKg = store.targetWeightKg
        val birthYear = store.birthYear
        val workoutDaysPerWeek = store.daysPerWeek
        if (heightCm <= 0 || weightKg <= 0) return

        val acceleratedDays = max(ceil(max(abs(weightKg - targetWeightKg), 1.0) * 7.0).toInt(), 14)
        val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, acceleratedDays) }

        scope.launch {
            syncOnboarding(
                workoutGoal = workoutGoal,
                heightCm = heightCm,
                weightKg = weightKg,
                targetWeightKg = if (targetWeightKg > 0) targetWeightKg else weightKg,
                birthYear = if (birthYear > 1900) birthYear else 2003,
                workoutDaysPerWeek = if (workoutDaysPerWeek > 0) workoutDaysPerWeek else 2,
                targetDate = calendar.time
            )
        }
    }

    // region HTTP helpers (StepBadgeApi pattern)

    private fun sessionEmail(): String? =
        authManager.getSession()?.email?.trim()?.takeIf { it.isNotEmpty() }

    private fun urlWithEmail(base: String): String {
        val email = sessionEmail() ?: return base
        return "$base?email=${URLEncoder.encode(email, "UTF-8")}"
    }

    private fun openConnection(method: String, urlString: String, hasBody: Boolean): HttpURLConnection =
        (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            if (hasBody) setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            val token = authManager.getAccessToken()
            if (!token.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
            if (hasBody) {
                doInput = true
                doOutput = true
            }
        }

    private fun requestJson(method: String, urlString: String, payload: JSONObject?): JSONObject {
        var connection: HttpURLConnection? = null
        try {
            connection = openConnection(method, urlString, payload != null)
            if (payload != null) {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }
            }
            val responseCode = connection.responseCode
            if (responseCode == 401) {
                AuthManager.unauthorizedEvent.tryEmit(Unit)
            }
            if (responseCode in 200..299) {
                val responseText = BufferedReader(
                    InputStreamReader(connection.inputStream, Charsets.UTF_8)
                ).use { it.readText() }
                return JSONObject(responseText)
            }
            val errorText = connection.errorStream?.let { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            } ?: "HTTP $responseCode"
            val message = try {
                JSONObject(errorText).optString("message", errorText).ifEmpty { errorText }
            } catch (e: Exception) {
                errorText
            }
            throw Exception(message)
        } finally {
            connection?.disconnect()
        }
    }

    private fun requestRaw(method: String, urlString: String) {
        var connection: HttpURLConnection? = null
        try {
            connection = openConnection(method, urlString, false)
            val responseCode = connection.responseCode
            if (responseCode == 401) {
                AuthManager.unauthorizedEvent.tryEmit(Unit)
            }
            if (responseCode !in 200..299) {
                val errorText = connection.errorStream?.let { stream ->
                    BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
                } ?: "HTTP $responseCode"
                throw Exception(errorText)
            }
        } finally {
            connection?.disconnect()
        }
    }

    /** "yyyy-MM-dd" in GMT with a US locale — mirror of the iOS formatter. */
    private fun isoDateString(date: Date): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(date)
    }

    // endregion
}
