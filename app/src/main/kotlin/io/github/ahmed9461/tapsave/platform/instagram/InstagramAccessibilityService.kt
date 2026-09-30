package io.github.ahmed9461.tapsave.platform.instagram

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import io.github.ahmed9461.tapsave.platform.CurrentReelAcquirer
import java.util.UUID

/** Only an explicit tap starts tree reads. Events and bounded retries drive acquisition. */
class InstagramAccessibilityService : AccessibilityService(), CurrentReelAcquirer {
    private enum class Stage { SHARE, COPY, CLEANUP, HANDOFF }
    private val main = Handler(Looper.getMainLooper())
    private var stage = Stage.SHARE
    private var trace: AcquisitionDiagnostics? = null
    private var attempts = 0
    private var nextAttempt = 0L
    private var openedSheet = false
    private var observedCopy = false
    private var cleanupBackSent = false
    private var cleanupResult: Result<String>? = null
    private var cleanupDeadline = 0L
    private val inspect = Runnable { inspectStage() }
    private val timeout = Runnable { fail(if (stage == Stage.COPY && attempts > 0) "COPY_ACTION_FAILED" else "ACQUIRE_TIMEOUT") }

    override fun onServiceConnected() { connected = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (pending == null || stage == Stage.HANDOFF || event?.packageName?.toString() != InstagramApp.PACKAGE_NAME) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        trace?.add("EVENT stage=$stage type=${event.eventType} window=${event.windowId}")
        schedule(50)
    }
    override fun acquire(completed: (Result<String>) -> Unit) {
        if (pending != null) { completed(Result.failure(IllegalStateException("ACQUIRE_BUSY"))); return }
        trace = AcquisitionDiagnostics(this)
        pending = Request(UUID.randomUUID().toString(), completed = completed)
        stage = Stage.SHARE; attempts = 0; nextAttempt = 0; openedSheet = false; observedCopy = false; cleanupBackSent = false
        main.postDelayed(timeout, 12_000)
        inspectStage()
    }
    private fun schedule(delay: Long) {
        if (!main.hasCallbacks(inspect)) main.postDelayed(inspect, delay)
    }
    private fun inspectStage() {
        if (pending == null || stage == Stage.HANDOFF) return
        if (getSystemService(KeyguardManager::class.java).isKeyguardLocked) { complete(Result.failure(IllegalStateException("SCREEN_LOCKED"))); return }
        if (stage == Stage.CLEANUP) { inspectCleanup(); return }
        val root = rootInActiveWindow
        if (root == null) {
            trace?.add("WAIT stage=$stage window_missing=true")
            schedule(100); return
        }
        if (root.packageName?.toString() != InstagramApp.PACKAGE_NAME) {
            fail("INSTAGRAM_LEFT"); return
        }
        val action = if (stage == Stage.SHARE) InstagramControls.Action.SHARE else InstagramControls.Action.COPY_LINK
        val scan = InstagramNodes.scan(root, action, trace)
        if (stage == Stage.COPY && scan.matches.isNotEmpty()) observedCopy = true
        val match = scan.unique()
        if (match == null) {
            if (stage == Stage.SHARE) { fail("SHARE_CONTROL_NOT_UNIQUE"); return }
            // Read again within this request even if Instagram omits a later content event.
            schedule(250); return
        }
        val now = SystemClock.uptimeMillis()
        if (now < nextAttempt) { schedule(nextAttempt - now); return }
        if (stage == Stage.COPY) pending?.copiedAfter = System.currentTimeMillis()
        val accepted = InstagramNodes.click(match, action, trace)
        attempts++
        trace?.add("ATTEMPT stage=$stage number=$attempts accepted=$accepted"); trace?.flush()
        if (!accepted) {
            if (attempts >= 4) { fail(if (stage == Stage.COPY) "COPY_ACTION_FAILED" else "SHARE_ACTION_FAILED"); return }
            nextAttempt = now + attempts * 200L
            schedule(attempts * 200L); return
        }
        if (stage == Stage.SHARE) {
            openedSheet = true; stage = Stage.COPY; attempts = 0; nextAttempt = 0
            schedule(50)
        } else startHandoff()
    }
    private fun fail(code: String) {
        trace?.add("FAIL stage=$stage code=$code")
        if (openedSheet) beginCleanup(Result.failure(IllegalStateException(code)))
        else complete(Result.failure(IllegalStateException(code)))
    }
    private fun beginCleanup(result: Result<String>) {
        main.removeCallbacks(inspect); main.removeCallbacks(timeout)
        cleanupResult = result; cleanupDeadline = SystemClock.uptimeMillis() + 2_500
        stage = Stage.CLEANUP
        inspectCleanup()
    }
    private fun inspectCleanup() {
        val root = rootInActiveWindow
        // The focused reader reports before its Activity finishes. Let it return to
        // Instagram; never send Back to Tap Save (or to a different foreground app).
        if (root?.packageName?.toString() == packageName && SystemClock.uptimeMillis() < cleanupDeadline) {
            schedule(100); return
        }
        if (root == null && SystemClock.uptimeMillis() < cleanupDeadline) {
            trace?.add("CLEANUP waiting_for_window=true")
            schedule(100); return
        }
        if (root?.packageName?.toString() != InstagramApp.PACKAGE_NAME || getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
            trace?.add("CLEANUP skipped=foreground_changed")
            if (cleanupResult?.isFailure != true) cleanupResult = Result.failure(IllegalStateException(if (root == null) "WINDOW_NOT_READY" else "INSTAGRAM_LEFT"))
            finishCleanup(); return
        }
        val copyVisible = InstagramNodes.scan(root, InstagramControls.Action.COPY_LINK).matches.isNotEmpty()
        val reelReady = InstagramNodes.scan(root, InstagramControls.Action.SHARE).unique() != null && !copyVisible
        if (reelReady) { trace?.add("CLEANUP reel_ready=true"); finishCleanup(); return }
        // Never Back in a different app or from the already restored Reel.
        if (!cleanupBackSent && openedSheet && (observedCopy || copyVisible)) {
            cleanupBackSent = true
            trace?.add("CLEANUP back_accepted=${performGlobalAction(GLOBAL_ACTION_BACK)}")
        }
        if (SystemClock.uptimeMillis() >= cleanupDeadline) {
            trace?.add("CLEANUP unconfirmed copy_visible=$copyVisible")
            if (cleanupResult?.isFailure != true) cleanupResult = Result.failure(IllegalStateException("SHARE_SHEET_NOT_CLOSED"))
            finishCleanup()
        } else schedule(100)
    }
    private fun finishCleanup() {
        main.removeCallbacks(inspect)
        complete(requireNotNull(cleanupResult))
    }
    private fun startHandoff() {
        main.removeCallbacks(inspect); main.removeCallbacks(timeout)
        // Copy may complete asynchronously. Keep its sheet alive until the focused
        // reader confirms a fresh link; closing it immediately can cancel the copy.
        stage = Stage.HANDOFF
        trace?.add("HANDOFF launching_focused_activity"); trace?.flush()
        main.postDelayed(timeout, 3_000)
        try {
            startActivity(Intent(this, ReelLinkCaptureActivity::class.java).putExtra("request", pending?.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: RuntimeException) { captured(Result.failure(IllegalStateException("LINK_HANDOFF_FAILED"))) }
    }
    fun diagnostic(message: String) { trace?.add(message) }
    fun captureActive(requestId: String?) = pending?.id == requestId && stage == Stage.HANDOFF
    fun captured(result: Result<String>) {
        if (pending == null || stage != Stage.HANDOFF) return
        trace?.add("HANDOFF captured=${result.isSuccess}")
        beginCleanup(result)
    }
    private fun complete(result: Result<String>) {
        trace?.add("END result=${if (result.isSuccess) "acquired" else result.exceptionOrNull()?.message}"); trace?.flush()
        val callback = pending?.completed
        pending = null
        main.removeCallbacksAndMessages(null)
        callback?.invoke(result)
    }
    override fun onInterrupt() { if (pending != null) fail("ACQUISITION_INTERRUPTED") }
    override fun onDestroy() {
        if (pending != null) complete(Result.failure(IllegalStateException("ADAPTER_STOPPED")))
        if (connected === this) connected = null
        super.onDestroy()
    }
    data class Request(val id: String, var copiedAfter: Long = 0, val completed: (Result<String>) -> Unit)
    companion object {
        var connected: InstagramAccessibilityService? = null
            private set
        var pending: Request? = null
            private set
    }
}
