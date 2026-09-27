package io.github.ahmed9461.tapsave.platform.instagram

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import io.github.ahmed9461.tapsave.platform.CurrentReelAcquirer
import java.util.UUID

/** No tree reads while idle. Only this request's Instagram Share -> Copy link actions are allowed. */
class InstagramAccessibilityService : AccessibilityService(), CurrentReelAcquirer {
    private val main = Handler(Looper.getMainLooper())
    private var waitingForCopy = false
    private var handingOff = false
    private val inspect = Runnable { inspectCopy() }
    private val timeout = Runnable { finish(Result.failure(IllegalStateException("ACQUIRE_TIMEOUT"))) }

    override fun onServiceConnected() { connected = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (pending == null || handingOff || !waitingForCopy || event?.packageName?.toString() != InstagramApp.PACKAGE_NAME) return
        main.removeCallbacks(inspect)
        main.postDelayed(inspect, 150)
    }

    override fun acquire(completed: (Result<String>) -> Unit) {
        if (pending != null) { completed(Result.failure(IllegalStateException("ACQUIRE_BUSY"))); return }
        if (getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
            completed(Result.failure(IllegalStateException("SCREEN_LOCKED"))); return
        }
        pending = Request(UUID.randomUUID().toString(), completed = completed)
        main.postDelayed(timeout, 8_000)
        val share = findUnique(InstagramControls.Action.SHARE)
        if (share == null) { finish(Result.failure(IllegalStateException("SHARE_CONTROL_NOT_UNIQUE"))); return }
        waitingForCopy = true
        if (!share.performAction(AccessibilityNodeInfo.ACTION_CLICK)) finish(Result.failure(IllegalStateException("SHARE_ACTION_FAILED")))
        else main.postDelayed(inspect, 200)
    }

    private fun inspectCopy() {
        if (pending == null || handingOff) return
        if (getSystemService(KeyguardManager::class.java).isKeyguardLocked) { finish(Result.failure(IllegalStateException("SCREEN_LOCKED"))); return }
        val root = rootInActiveWindow
        if (root?.packageName?.toString() != InstagramApp.PACKAGE_NAME) {
            finish(Result.failure(IllegalStateException("INSTAGRAM_LEFT"))); return
        }
        val copy = findUnique(InstagramControls.Action.COPY_LINK) ?: return // Wait for the sheet's bounded event window.
        pending?.copiedAfter = System.currentTimeMillis()
        if (!copy.performAction(AccessibilityNodeInfo.ACTION_CLICK)) { finish(Result.failure(IllegalStateException("COPY_ACTION_FAILED"))); return }
        handingOff = true
        main.removeCallbacks(inspect)
        main.postDelayed({
            val request = pending ?: return@postDelayed
            if (rootInActiveWindow?.packageName?.toString() != InstagramApp.PACKAGE_NAME) {
                finish(Result.failure(IllegalStateException("INSTAGRAM_LEFT"))); return@postDelayed
            }
            // Only close a sheet we opened if its Copy link control is still present.
            if (findUnique(InstagramControls.Action.COPY_LINK) != null) performGlobalAction(GLOBAL_ACTION_BACK)
            try {
                startActivity(Intent(this, ReelLinkCaptureActivity::class.java).putExtra("request", request.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: RuntimeException) { finish(Result.failure(IllegalStateException("LINK_HANDOFF_FAILED"))) }
        }, 300)
    }

    private fun findUnique(action: InstagramControls.Action): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        if (root.packageName?.toString() != InstagramApp.PACKAGE_NAME) return null
        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        queue.add(root to 0)
        val matches = mutableListOf<AccessibilityNodeInfo>()
        var visited = 0
        while (queue.isNotEmpty()) {
            val (node, depth) = queue.removeFirst()
            if (++visited > 400 || depth > 40) return null
            if (node.packageName?.toString() != InstagramApp.PACKAGE_NAME || !node.isVisibleToUser) continue
            if (InstagramControls.matches(action, node.text?.toString().orEmpty(), node.viewIdResourceName.orEmpty()) || InstagramControls.matches(action, node.contentDescription?.toString().orEmpty(), node.viewIdResourceName.orEmpty())) {
                var click: AccessibilityNodeInfo? = node
                repeat(4) {
                    val candidate = click
                    if (candidate != null && candidate.packageName?.toString() == InstagramApp.PACKAGE_NAME && candidate.isVisibleToUser && candidate.isEnabled) {
                        if (candidate.isClickable) { if (matches.none { it == candidate }) matches += candidate; click = null }
                        else click = candidate.parent
                    } else click = null
                }
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it to depth + 1) }
        }
        return matches.singleOrNull()
    }

    fun finish(result: Result<String>) {
        val callback = pending?.completed
        pending = null
        waitingForCopy = false
        handingOff = false
        main.removeCallbacksAndMessages(null)
        callback?.invoke(result)
    }
    override fun onInterrupt() { finish(Result.failure(IllegalStateException("ACQUISITION_INTERRUPTED"))) }
    override fun onDestroy() { onInterrupt(); if (connected === this) connected = null; super.onDestroy() }

    data class Request(val id: String, var copiedAfter: Long = 0, val completed: (Result<String>) -> Unit)
    companion object {
        var connected: InstagramAccessibilityService? = null
            private set
        var pending: Request? = null
            private set
    }
}
