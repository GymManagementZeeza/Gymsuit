package ca.zeezaglobal.gymsuitapp.di

import android.content.Context
import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import ca.zeezaglobal.gymsuitapp.data.summary.WellnessSummaryGenerator
import ca.zeezaglobal.gymsuitapp.ui.screens.DashboardViewModelFactory

/**
 * Dependency Injection container providing singleton application-level
 * and data-layer dependencies.
 */
class AppComponent private constructor(private val appContext: Context) {

    val healthConnectManager: HealthConnectManager by lazy {
        HealthConnectManager(appContext)
    }

    val wellnessSummaryGenerator: WellnessSummaryGenerator by lazy {
        WellnessSummaryGenerator(healthConnectManager)
    }

    val dashboardViewModelFactory: DashboardViewModelFactory by lazy {
        DashboardViewModelFactory(wellnessSummaryGenerator)
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
