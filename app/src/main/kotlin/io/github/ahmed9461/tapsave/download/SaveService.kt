package io.github.ahmed9461.tapsave.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.Manifest
import android.content.pm.PackageManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import io.github.ahmed9461.tapsave.R
import io.github.ahmed9461.tapsave.platform.pendingTarget
import io.github.ahmed9461.tapsave.platform.instagram.InstagramLinkNormalizer
import io.github.ahmed9461.tapsave.platform.instagram.InstagramShareParser
import io.github.ahmed9461.tapsave.share.ShareActivity
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors

class SaveService : Service() {
    private val main = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor { Thread(it, "TapSave-transfer") }
    private var cancellation: TransferCancellation? = null
    private var destroyed = false
    private var latestStartId = 0
    private var lastProgressTime = 0L
    private lateinit var notifications: NotificationManager
    private lateinit var journal: SaveJournal
    private val deadline = Runnable { cancelSave() }

    override fun onCreate() {
        super.onCreate()
        notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.save_channel), NotificationManager.IMPORTANCE_LOW))
        journal = SaveJournal(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        latestStartId = startId
        if (intent?.action == CANCEL) { cancelSave(); if (cancellation == null) stopSelf(); return START_NOT_STICKY }
        val target = InstagramShareParser.parse(intent?.getStringExtra(TARGET)).pendingTarget()
        if (target == null) { if (cancellation == null) stopSelf(); return START_NOT_STICKY }
        if (cancellation != null) return START_NOT_STICKY // One active job; UI shows busy for another target.
        val signal = TransferCancellation()
        cancellation = signal
        val initial = SaveState(target, SavePhase.RESOLVING)
        SaveUiState.current = initial
        try {
            startForeground(NOTIFICATION, notification(initial), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } catch (_: RuntimeException) {
            SaveUiState.current = initial.copy(phase = SavePhase.FAILED, failure = SaveFailure.Reason.INTERRUPTED)
            cancellation = null
            stopSelf()
            return START_NOT_STICKY
        }
        main.postDelayed(deadline, 10 * 60 * 1000L)
        executor.execute { synchronized(jobLock) {
            val trace = DownloadDiagnostics(this)
            var resolved = initial
            val finished = try {
                // No automatic resume: clean only the previous interrupted target before this explicit request.
                journal.read()?.takeIf { it.active }?.let { reconcileMedia(this, it.target) }
                val target = InstagramLinkNormalizer(HttpTransfer()) { io.github.ahmed9461.tapsave.session.InstagramSession.cookies(this) }.normalize(target, signal)
                resolved = initial.copy(target = target)
                journal.write(resolved)
                signal.check()
                val existing = reconcileMedia(this, target)
                if (existing != null) trace.add("ALREADY_SAVED")
                val uri = existing ?: (testFactory?.invoke(this) ?: SavePipeline(this, onResolved = { strategy -> resolved = resolved.copy(diagnostic = strategy) }, diagnostic = trace::add)).save(target, signal) { bytes, total ->
                    signal.check()
                    val now = SystemClock.elapsedRealtime()
                    if (bytes == 0L || now - lastProgressTime >= 200) {
                        lastProgressTime = now
                        main.post {
                            if (!destroyed && SaveUiState.current?.phase != SavePhase.CANCELLING) {
                                val progress = resolved.copy(phase = SavePhase.DOWNLOADING, bytes = bytes, total = total)
                                SaveUiState.current = progress
                                postNotification(progress)
                            }
                        }
                    }
                }
                // Once MediaStore published, cancellation must not turn a completed save into a failure.
                resolved.copy(phase = SavePhase.SAVED, uri = uri)
            } catch (_: CancellationException) {
                resolved.copy(phase = SavePhase.CANCELLED)
            } catch (failure: Exception) {
                val reason = when (failure) {
                    is SaveFailure -> failure.reason
                    is java.net.SocketTimeoutException, is java.net.UnknownHostException, is java.net.ConnectException -> SaveFailure.Reason.NETWORK
                    is IOException -> SaveFailure.Reason.STORAGE
                    else -> SaveFailure.Reason.UNSUPPORTED
                }
                // disconnect() may report IOException instead of InterruptedException.
                try { signal.check(); resolved.copy(phase = SavePhase.FAILED, failure = reason, diagnostic = if (failure is SaveFailure) listOfNotNull(failure.stage.takeIf { it.isNotEmpty() }, failure.status?.let { "HTTP $it" }).joinToString(" · ") else null) }
                catch (_: CancellationException) { resolved.copy(phase = SavePhase.CANCELLED) }
            }
            trace.add("END phase=${finished.phase} reason=${finished.failure ?: "none"} stage=${finished.diagnostic.orEmpty()}")
            try { journal.write(finished) } catch (_: IOException) { /* MediaStore remains authoritative for completed files. */ }
            main.post {
                if (!destroyed) {
                    main.removeCallbacks(deadline)
                    SaveUiState.current = finished
                    cancellation = null
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    postNotification(finished)
                    stopSelf(latestStartId)
                }
            }
        } }
        return START_NOT_STICKY
    }

    private fun cancelSave() {
        cancellation?.cancel()
        SaveUiState.current?.takeIf { it.active }?.let { SaveUiState.current = it.copy(phase = SavePhase.CANCELLING) }
    }

    override fun onTimeout(startId: Int, fgsType: Int) { cancelSave(); stopSelf() }

    private fun postNotification(state: SaveState) {
        if (notifications.areNotificationsEnabled() && (Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)) {
            notifications.notify(NOTIFICATION, notification(state))
        }
    }

    private fun notification(state: SaveState): Notification {
        val open = PendingIntent.getActivity(this, 2, Intent(this, ShareActivity::class.java).setAction(VIEW), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name)).setContentText(saveMessage(this, state))
            .setContentIntent(open).setOnlyAlertOnce(true).setOngoing(state.active).setAutoCancel(!state.active)
        if (state.active) {
            val stop = PendingIntent.getService(this, 2, Intent(this, SaveService::class.java).setAction(CANCEL), PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(Notification.Action.Builder(null, getString(R.string.cancel_save), stop).build())
            val percent = state.total?.let { ((state.bytes * 100) / it).coerceIn(0, 100).toInt() }
            builder.setProgress(100, percent ?: 0, percent == null || state.phase == SavePhase.RESOLVING)
        }
        return builder.build()
    }

    override fun onDestroy() {
        destroyed = true
        cancellation?.cancel()
        main.removeCallbacksAndMessages(null)
        executor.shutdown()
        if (cancellation != null) stopForeground(STOP_FOREGROUND_REMOVE)
        SaveUiState.current?.takeIf { it.active }?.let {
            SaveUiState.current = it.copy(phase = SavePhase.FAILED, failure = SaveFailure.Reason.INTERRUPTED)
        }
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val TARGET = "target"
        const val CANCEL = "io.github.ahmed9461.tapsave.CANCEL_SAVE"
        const val VIEW = "io.github.ahmed9461.tapsave.VIEW_SAVE"
        private const val CHANNEL = "media-saves"
        const val NOTIFICATION = 2
        @Volatile internal var testFactory: ((Context) -> SaveRunner)? = null
        private val jobLock = Any()
    }
}

fun saveMessage(context: Context, state: SaveState): String = context.getString(when (state.phase) {
    SavePhase.RESOLVING -> R.string.resolving
    SavePhase.DOWNLOADING -> R.string.downloading
    SavePhase.CANCELLING -> R.string.cancelling
    SavePhase.CANCELLED -> R.string.cancelled
    SavePhase.SAVED -> R.string.saved
    SavePhase.FAILED -> when (state.failure) {
        SaveFailure.Reason.AUTH_REQUIRED -> R.string.save_auth_required
        SaveFailure.Reason.RATE_LIMITED -> R.string.save_rate_limited
        SaveFailure.Reason.METADATA_UNAVAILABLE -> R.string.save_metadata_unavailable
        SaveFailure.Reason.EXTRACTOR_INCOMPATIBLE -> R.string.save_extractor_incompatible
        SaveFailure.Reason.EXPIRED_URL -> R.string.save_expired_url
        SaveFailure.Reason.UNAVAILABLE -> R.string.save_unavailable
        SaveFailure.Reason.RESTRICTED -> R.string.save_restricted
        SaveFailure.Reason.NETWORK -> R.string.save_network
        SaveFailure.Reason.STORAGE -> R.string.save_storage
        SaveFailure.Reason.TOO_LARGE -> R.string.save_too_large
        SaveFailure.Reason.INTERRUPTED -> R.string.save_interrupted
        else -> R.string.save_unsupported
    }
})
