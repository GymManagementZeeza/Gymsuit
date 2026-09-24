package ca.zeezaglobal.gymsuitapp.di

import android.content.Context
import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import ca.zeezaglobal.gymsuitapp.data.remote.AiSummarizeService
import ca.zeezaglobal.gymsuitapp.data.remote.AiSummarizeServiceImpl
import ca.zeezaglobal.gymsuitapp.data.repository.AiSummaryRepository
import ca.zeezaglobal.gymsuitapp.data.repository.AiSummaryRepositoryImpl
import ca.zeezaglobal.gymsuitapp.ui.screens.DashboardViewModelFactory

/**
 * Dependency Injection container providing singleton application-level
 * and data-layer dependencies.
 */
class AppComponent private constructor(private val appContext: Context) {

    val healthConnectManager: HealthConnectManager by lazy {
        HealthConnectManager(appContext)
    }

    val aiSummarizeService: AiSummarizeService by lazy {
        AiSummarizeServiceImpl()
    }

    val aiSummaryRepository: AiSummaryRepository by lazy {
        AiSummaryRepositoryImpl(
            healthConnectManager = healthConnectManager,
            aiSummarizeService = aiSummarizeService
        )
    }

    val dashboardViewModelFactory: DashboardViewModelFactory by lazy {
        DashboardViewModelFactory(aiSummaryRepository)
    }

    companion object {
        @Volatile
        private var INSTANCE: AppComponent? = null

        fun from(context: Context): AppComponent {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppComponent(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
