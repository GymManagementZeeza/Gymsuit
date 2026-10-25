package ca.zeezaglobal.gymsuitapp.ui.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import ca.zeezaglobal.gymsuitapp.MainActivity
import ca.zeezaglobal.gymsuitapp.R
import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * User preference for the floating steps bubble. No step target is stored
 * anywhere else in the app (badges use fixed 5k/10k/20k thresholds), so the
 * bubble rings toward [DEFAULT_TARGET_STEPS] (10,000 — the Step Champion
 * threshold) until a proper step-goal setting exists.
 */
object StepsBubblePrefs {
    private const val PREFS = "gymsuit_steps_bubble"
    private const val KEY_ENABLED = "steps_bubble_enabled"
    private const val KEY_TARGET_STEPS = "steps_bubble_target_steps"

    const val DEFAULT_TARGET_STEPS = 10_000

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun targetSteps(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_TARGET_STEPS, DEFAULT_TARGET_STEPS)
            .takeIf { it > 0 } ?: DEFAULT_TARGET_STEPS
}

/**
 * Floating bubble showing today's steps with a circular progress ring toward
 * the step target. Runs as a foreground service so the overlay survives while
 * the app is in the background; the service is only ever started when the
 * user enables the toggle in Settings and grants "Display over other apps".
 *
 * - Steps refresh every 5 minutes from Health Connect (same source the sync
 *   uses, including the on-device pedometer fallback when Health Connect
 *   has no step source for today).
 * - Drag the bubble to move it. Tap it to open GymSuit (which syncs on
 *   launch). Turn it off from Settings → Floating steps bubble.
 * - Never crashes the app: every refresh is guarded, and the service stops
 *   itself if overlay permission is revoked.
 */
class StepsBubbleService : Service() {

    companion object {
        private const val TAG = "StepsBubbleService"
        private const val CHANNEL_ID = "gymsuit_steps_bubble"
        private const val NOTIFICATION_ID = 1002

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, StepsBubbleService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, StepsBubbleService::class.java))
        }

        /** Starts the bubble on app launch / boot when the user left it on. */
        fun startIfEnabled(context: Context) {
            if (StepsBubblePrefs.isEnabled(context)) start(context)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var windowManager: WindowManager? = null
    private var bubbleView: StepsBubbleView? = null
    private var params: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm
        val sizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 76f, resources.displayMetrics
        ).toInt()
        val view = StepsBubbleView(
            this,
            onDrag = { dx, dy ->
                val p = params ?: return@StepsBubbleView
                val v = bubbleView ?: return@StepsBubbleView
                p.x += dx.toInt()
                p.y += dy.toInt()
                try {
                    wm.updateViewLayout(v, p)
                } catch (e: Exception) {
                    Log.w(TAG, "Bubble move failed", e)
                }
            },
            onTap = {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
            }
        )
        view.setTarget(StepsBubblePrefs.targetSteps(this))
        val metrics = resources.displayMetrics
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = metrics.widthPixels - sizePx - 32
            y = 400
        }
        bubbleView = view
        try {
            wm.addView(view, params)
        } catch (e: Exception) {
            Log.e(TAG, "Could not add bubble overlay", e)
            stopSelf()
            return
        }
        serviceScope.launch {
            while (isActive) {
                refreshSteps()
                delay(5 * 60 * 1000L)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY: if the system kills the service, it restarts and the
        // bubble comes back (onCreate re-adds the overlay).
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        try {
            bubbleView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Bubble remove failed", e)
        }
        bubbleView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun refreshSteps() {
        try {
            val today = LocalDate.now()
            val steps = withContext(Dispatchers.IO) {
                HealthConnectManager(applicationContext)
                    .readProgressTotals(today, today).steps
            }
            bubbleView?.setSteps(steps)
        } catch (e: Exception) {
            Log.w(TAG, "Step refresh failed", e)
        }
    }

    private fun buildNotification(): android.app.Notification {
        ensureChannel()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("GymSuit steps bubble")
            .setContentText("Showing today's steps over other apps.")
            .setContentIntent(pending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun ensureChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Steps bubble",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the floating steps bubble running."
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}

/** Restarts the bubble after a reboot when the user left it enabled. */
class StepsBubbleBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            StepsBubbleService.startIfEnabled(context)
        }
    }
}

/**
 * Plain-View bubble (no Compose in a Service — avoids the ViewTree
 * lifecycle owner a ComposeView would need here). Draws a dark circle, a
 * progress ring toward the step target, and the compact step count.
 */
private class StepsBubbleView(
    context: Context,
    private val onDrag: (dx: Float, dy: Float) -> Unit,
    private val onTap: () -> Unit
) : View(context) {

    private var steps: Long = 0L
    private var target: Int = StepsBubblePrefs.DEFAULT_TARGET_STEPS

    private val sizePx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 76f, resources.displayMetrics
    ).toInt()
    private val ringWidthPx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 7f, resources.displayMetrics
    )
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#111827") }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#374151")
        style = Paint.Style.STROKE
        strokeWidth = ringWidthPx
        strokeCap = Paint.Cap.ROUND
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0062FF")
        style = Paint.Style.STROKE
        strokeWidth = ringWidthPx
        strokeCap = Paint.Cap.ROUND
    }
    private val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics
        )
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9CA3AF")
        textAlign = Paint.Align.CENTER
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 9f, resources.displayMetrics
        )
    }
    private val arcRect = RectF()

    private var downX = 0f
    private var downY = 0f
    private var moved = false

    fun setSteps(value: Long) {
        if (steps != value) {
            steps = value
            postInvalidate()
        }
    }

    fun setTarget(value: Int) {
        target = value
        postInvalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(sizePx, sizePx)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        canvas.drawCircle(cx, cy, width / 2f, bgPaint)
        val inset = ringWidthPx / 2f + 2f
        arcRect.set(inset, inset, width - inset, height - inset)
        canvas.drawArc(arcRect, 0f, 360f, false, trackPaint)
        val progress = if (target > 0) (steps.toFloat() / target).coerceIn(0f, 1f) else 0f
        if (progress > 0f) {
            canvas.drawArc(arcRect, -90f, 360f * progress, false, progressPaint)
        }
        val compact = if (steps >= 1000) {
            String.format(Locale.US, "%.1fk", steps / 1000f)
        } else {
            steps.toString()
        }
        canvas.drawText(compact, cx, cy + 2f, countPaint)
        canvas.drawText("steps", cx, cy + 20f, labelPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                moved = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                if (abs(dx) > 8 || abs(dy) > 8) moved = true
                onDrag(dx, dy)
                downX = event.rawX
                downY = event.rawY
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!moved) onTap()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
