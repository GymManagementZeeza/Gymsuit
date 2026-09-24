package ca.zeezaglobal.gymsuitapp.data.repository

import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import ca.zeezaglobal.gymsuitapp.data.model.AiSummarizeResponse
import ca.zeezaglobal.gymsuitapp.data.remote.AiSummarizeService
interface AiSummaryRepository {
    suspend fun fetchAiSummary(): Result<AiSummarizeResponse>
}

class AiSummaryRepositoryImpl(
    private val healthConnectManager: HealthConnectManager,
    private val aiSummarizeService: AiSummarizeService
) : AiSummaryRepository {

    override suspend fun fetchAiSummary(): Result<AiSummarizeResponse> {
        return try {
            val payload = healthConnectManager.buildAiSummarizeRequest()
            aiSummarizeService.getSummary(payload)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
