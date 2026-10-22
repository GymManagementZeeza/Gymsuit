package ca.zeezaglobal.gymsuitapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ca.zeezaglobal.gymsuitapp.MainActivity
import ca.zeezaglobal.gymsuitapp.R
import ca.zeezaglobal.gymsuitapp.data.local.AuthManager
import ca.zeezaglobal.gymsuitapp.data.remote.HealthSyncApi
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GymSuitFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "GymSuitFCM"
        const val CHANNEL_ID_CHALLENGES = "gymsuit_challenges"
        private const val CHANNEL_NAME_CHALLENGES = "Challenge & Leaderboard Updates"

        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = NotificationChannel(
                    CHANNEL_ID_CHALLENGES,
                    CHANNEL_NAME_CHALLENGES,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications when you or friends rank up or overtake each other in challenges."
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "New FCM registration token received: $token")

        val authManager = AuthManager(applicationContext)
        authManager.saveFcmToken(token)

        if (authManager.isLoggedIn()) {
            CoroutineScope(Dispatchers.IO).launch {
                HealthSyncApi(applicationContext).updateFcmToken(token)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val title = remoteMessage.notification?.title ?: data["title"] ?: "GymSuit Update"
        val body = remoteMessage.notification?.body ?: data["body"] ?: "Check out your challenge leaderboard!"
        val type = data["type"] ?: "GENERAL"
        val challengeId = data["challengeId"]

        showNotification(title, body, type, challengeId)
    }

    private fun showNotification(title: String, body: String, type: String, challengeId: String?) {
        createNotificationChannels(applicationContext)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", type)
            if (challengeId != null) {
                putExtra("challenge_id", challengeId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID_CHALLENGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(
                System.currentTimeMillis().toInt(),
                notification
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted: ${e.message}")
        }
    }
}
