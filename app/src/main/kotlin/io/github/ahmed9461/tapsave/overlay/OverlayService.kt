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
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.ahmed9461.tapsave.download.*
import io.github.ahmed9461.tapsave.platform.instagram.InstagramAccessibilityService
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
    private var acquiring = false
    private lateinit var preferences: OverlayPreferences
    private val appearanceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (!preferences.enabled) stopSelf()
        else window.refreshAppearance(key == "position-reset")
    }
    private val resetStatus = Runnable { if (!acquiring && SaveUiState.current?.active != true) window.render("↓", "Save current Reel") }
    private val saveObserver: (SaveState?) -> Unit = { state ->
        main.removeCallbacks(resetStatus)
        if (state != null) {
            val glyph = when (state.phase) {
                SavePhase.RESOLVING -> "…"
                SavePhase.DOWNLOADING -> state.total?.let { "${(state.bytes * 100 / it).coerceIn(0, 100)}%" } ?: "…"
                SavePhase.CANCELLING -> "…"
                SavePhase.SAVED -> "✓"
                SavePhase.FAILED -> "!"
                SavePhase.CANCELLED -> "↓"
            }
            window.render(glyph, saveMessage(this, state) + if (state.active) ". Tap to cancel" else ". Tap to save current Reel")
            if (!state.active) main.postDelayed(resetStatus, 3_000)
        }
    }
    private fun tapSave() {
        if (SaveUiState.current?.active == true) { startService(Intent(this, SaveService::class.java).setAction(SaveService.CANCEL)); return }
        if (acquiring) { InstagramAccessibilityService.connected?.onInterrupt(); return }
        val adapter = InstagramAccessibilityService.connected
        if (adapter == null) {
            window.render("!", getString(R.string.accessibility_enable))
            lastAcquisitionError = getString(R.string.accessibility_enable); return
        }
        acquiring = true
        lastAcquisitionError = null
        main.removeCallbacks(resetStatus)
        window.render("…", getString(R.string.reading_reel))
        adapter.acquire { result ->
            acquiring = false
            result.onSuccess { url ->
                try { startForegroundService(Intent(this, SaveService::class.java).putExtra(SaveService.TARGET, url)) }
                catch (_: RuntimeException) { acquisitionFailed("SAVE_START_FAILED") }
            }.onFailure { acquisitionFailed(it.message.orEmpty()) }
        }
    }
    private fun acquisitionFailed(code: String) {
        window.render("!", "$code. " + getString(R.string.overlay_share_hint))
        lastAcquisitionError = code
    }
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
        window = OverlayWindow(this, onFailure = { stopSelf() }, onTap = { tapSave() })
        preferences = OverlayPreferences(this)
        preferences.storage.registerOnSharedPreferenceChangeListener(appearanceListener)
        SaveUiState.observe(saveObserver)
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
        if (intent?.action == ACTION_STOP) {
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
            .setContentText("Ready while you browse Instagram")
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, getString(R.string.stop_overlay), stop).build())
            .build()
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        startForeground(1, notification, type)
        // A permission can change after the caller checked it. Fulfil the foreground
        // startup contract before stopping; otherwise Android can later kill this process.
        if (canStart(this) && preferences.enabled) { running = true; updatePolling() } else stopSelf()
        return START_NOT_STICKY
    }

    private fun screenUsable() = getSystemService(PowerManager::class.java).isInteractive &&
        !getSystemService(KeyguardManager::class.java).isKeyguardLocked

    private fun updatePolling() {
        polling = false
        generation.incrementAndGet()
        worker.removeCallbacksAndMessages(null)
        window.hide()
        if (!screenUsable() && acquiring) InstagramAccessibilityService.connected?.onInterrupt()
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
        running = false
        preferences.storage.unregisterOnSharedPreferenceChangeListener(appearanceListener)
        SaveUiState.remove(saveObserver)
        if (acquiring) InstagramAccessibilityService.connected?.onInterrupt()
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
        var running by mutableStateOf(false)
            private set
        var lastAcquisitionError by mutableStateOf<String?>(null)
            private set
        private const val CHANNEL = "overlay-session"
        private const val ACTION_STOP = "io.github.ahmed9461.tapsave.STOP_OVERLAY"
        const val POLL_INTERVAL_MS = 1_500L

        fun canStart(context: Context): Boolean = Settings.canDrawOverlays(context) &&
            UsageForegroundContext.hasPermission(context) &&
            context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    }
}
