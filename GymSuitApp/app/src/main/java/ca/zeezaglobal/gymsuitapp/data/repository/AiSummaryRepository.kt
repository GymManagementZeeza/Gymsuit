package ca.zeezaglobal.gymsuitapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import ca.zeezaglobal.gymsuitapp.data.model.AiSummarizeResponse
import ca.zeezaglobal.gymsuitapp.data.remote.AiSummarizeService
import java.time.LocalDate

interface AiSummaryRepository {
    fun getCachedSummary(date: LocalDate): String?
    suspend fun getSummaryForDate(date: LocalDate, forceRefresh: Boolean = false): Result<AiSummarizeResponse>
}

class AiSummaryRepositoryImpl(
    private val context: Context,
    private val healthConnectManager: HealthConnectManager,
    private val aiSummarizeService: AiSummarizeService
) : AiSummaryRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ai_summaries_cache", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SUMMARY_PREFIX = "summary_"
        private const val KEY_TIMESTAMP_PREFIX = "timestamp_"
        private const val ONE_HOUR_MILLIS = 60 * 60 * 1000L
    }

    override fun getCachedSummary(date: LocalDate): String? {
        val today = LocalDate.now()
        val cachedText = prefs.getString("$KEY_SUMMARY_PREFIX$date", null) ?: return null

        if (date == today) {
            val lastFetchTime = prefs.getLong("$KEY_TIMESTAMP_PREFIX$date", 0L)
            val now = System.currentTimeMillis()
            // If more than 1 hour old, consider cache expired for today
            if (now - lastFetchTime > ONE_HOUR_MILLIS) {
                return null
            }
        }
        return cachedText
    }

    override suspend fun getSummaryForDate(
        date: LocalDate,
        forceRefresh: Boolean
    ): Result<AiSummarizeResponse> {
        val today = LocalDate.now()

        // 1. Check local cache first (unless force refresh requested)
        if (!forceRefresh) {
            val cached = getCachedSummary(date)
            if (cached != null) {
                return Result.success(AiSummarizeResponse(summary = cached))
            }
        }

        // 2. Build health data payload for selected date
        return try {
            val payload = if (date == today) {
                healthConnectManager.buildAiSummarizeRequest()
            } else {
                healthConnectManager.buildAiSummarizeRequestForDate(date)
            }

            val apiResult = aiSummarizeService.getSummary(payload)
            apiResult.onSuccess { response ->
                if (response.summary.isNotBlank()) {
                    // Cache the successful summary
                    prefs.edit()
                        .putString("$KEY_SUMMARY_PREFIX$date", response.summary)
                        .putLong("$KEY_TIMESTAMP_PREFIX$date", System.currentTimeMillis())
                        .apply()
                }
            }
            apiResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
