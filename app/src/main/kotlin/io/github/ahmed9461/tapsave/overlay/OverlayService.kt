package io.github.ahmed9461.tapsave.overlay

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import io.github.ahmed9461.tapsave.MainActivity
import io.github.ahmed9461.tapsave.R
import io.github.ahmed9461.tapsave.platform.UsageForegroundContext
import io.github.ahmed9461.tapsave.platform.instagram.InstagramApp
import java.util.concurrent.atomic.AtomicInteger

class OverlayService : Service() {
    private val main = Handler(Looper.getMainLooper())
    private lateinit var workerThread: HandlerThread
    private lateinit var worker: Handler
    private lateinit var foreground: UsageForegroundContext
    private lateinit var window: OverlayWindow
    private val generation = AtomicInteger()
    @Volatile private var polling = false

    private val poll = object : Runnable {
        override fun run() {
            if (!polling) return
            val current = generation.get()
            val allowed = canStart(this@OverlayService)
            val show = allowed && try {
                foreground.currentPackage() == InstagramApp.PACKAGE_NAME
            } catch (_: SecurityException) {
                false
            }
            main.post {
                if (polling && generation.get() == current) {
                    if (!allowed) stopSelf()
                    else if (show && screenUsable()) safelyShow() else window.hide()
                }
            }
            if (polling && generation.get() == current && allowed) worker.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) { updatePolling() }
    }

    override fun onCreate() {
        super.onCreate()
        foreground = UsageForegroundContext(this)
        window = OverlayWindow(this, onFailure = { stopSelf() })
        workerThread = HandlerThread("TapSave-context").apply { start() }
        worker = Handler(workerThread.looper)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
        else registerReceiver(screenReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP || !canStart(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        val notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.overlay_channel), NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(this, 0, Intent(this, OverlayService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.overlay_notification))
            .setContentText(getString(R.string.overlay_share_hint))
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, getString(R.string.stop_overlay), stop).build())
            .build()
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        startForeground(1, notification, type)
        updatePolling()
        return START_NOT_STICKY
    }

    private fun screenUsable() = getSystemService(PowerManager::class.java).isInteractive &&
        !getSystemService(KeyguardManager::class.java).isKeyguardLocked

    private fun updatePolling() {
        polling = false
        generation.incrementAndGet()
        worker.removeCallbacksAndMessages(null)
        window.hide()
        if (screenUsable()) {
            polling = true
            worker.post {
                foreground.reset()
                poll.run()
            }
        }
    }

    private fun safelyShow() {
        try {
            if (Settings.canDrawOverlays(this)) window.show() else stopSelf()
        } catch (_: WindowManager.BadTokenException) {
            stopSelf()
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Reattach on the next context result, with coordinates clamped to the new display.
        window.hide()
    }

    override fun onDestroy() {
        polling = false
        generation.incrementAndGet()
        worker.removeCallbacksAndMessages(null)
        main.removeCallbacksAndMessages(null)
        workerThread.quitSafely()
        unregisterReceiver(screenReceiver)
        window.hide()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL = "overlay-session"
        private const val ACTION_STOP = "io.github.ahmed9461.tapsave.STOP_OVERLAY"
        const val POLL_INTERVAL_MS = 1_500L

        fun canStart(context: Context): Boolean = Settings.canDrawOverlays(context) &&
            UsageForegroundContext.hasPermission(context) &&
            context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    }
}
