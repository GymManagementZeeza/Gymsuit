package ca.zeezaglobal.gymsuitapp.di

import android.content.Context
import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import ca.zeezaglobal.gymsuitapp.data.local.HybridLocalAiEngine
import ca.zeezaglobal.gymsuitapp.data.local.LocalAiEngine
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

    val authManager: ca.zeezaglobal.gymsuitapp.data.local.AuthManager by lazy {
        ca.zeezaglobal.gymsuitapp.data.local.AuthManager(appContext)
    }

    val mobileAuthApi: ca.zeezaglobal.gymsuitapp.data.remote.MobileAuthApi by lazy {
        ca.zeezaglobal.gymsuitapp.data.remote.MobileAuthApi()
    }

    val localAiEngine: LocalAiEngine by lazy {
        HybridLocalAiEngine(appContext)
    }

    val aiSummaryRepository: AiSummaryRepository by lazy {
        AiSummaryRepositoryImpl(
            context = appContext,
            healthConnectManager = healthConnectManager,
            localAiEngine = localAiEngine
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
