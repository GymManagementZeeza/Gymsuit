package ca.zeezaglobal.gymsuitapp

import android.app.Application
import android.util.Log
import ca.zeezaglobal.gymsuitapp.data.BackgroundHealthSync
import ca.zeezaglobal.gymsuitapp.ui.overlay.StepsBubbleService
import ca.zeezaglobal.gymsuitapp.data.local.AuthManager
import ca.zeezaglobal.gymsuitapp.data.remote.HealthSyncApi
import ca.zeezaglobal.gymsuitapp.di.AppComponent
import ca.zeezaglobal.gymsuitapp.notifications.GymSuitFirebaseMessagingService
import ca.zeezaglobal.gymsuitapp.notifications.MealReminders
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GymSuitApplication : Application() {

    lateinit var appComponent: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        appComponent = AppComponent.from(this)
        MealReminders.restore(this)
        // Setup notification channel for challenges & leaderboards
        GymSuitFirebaseMessagingService.createNotificationChannels(this)
        // Periodic background health sync (WorkManager; no-op when logged out).
        BackgroundHealthSync.schedule(this)
        // Restart the floating steps bubble if the user left it enabled.
        StepsBubbleService.startIfEnabled(this)

        // Retrieve FCM token and sync to backend if logged in
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val token = task.result
                    val authManager = AuthManager(this)
                    authManager.saveFcmToken(token)
                    if (authManager.isLoggedIn()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            HealthSyncApi(applicationContext).updateFcmToken(token)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("GymSuitApplication", "Firebase not yet initialized: ${e.message}")
        }
    }
}
