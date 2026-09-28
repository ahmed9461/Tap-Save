package io.github.ahmed9461.tapsave.platform.instagram

import android.app.Activity
import android.content.ClipboardManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import io.github.ahmed9461.tapsave.platform.ShareResult

/** A visible focused handoff, because Android does not grant background clipboard access. */
class ReelLinkCaptureActivity : Activity() {
    private val main = Handler(Looper.getMainLooper())
    private var attempts = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { text = getString(io.github.ahmed9461.tapsave.R.string.reading_reel); setPadding(32, 32, 32, 32) })
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        InstagramAccessibilityService.connected?.diagnostic("CLIPBOARD focus=$hasFocus")
        main.removeCallbacksAndMessages(null)
        if (hasFocus) readFreshLink()
    }
    private fun readFreshLink() {
        val request = InstagramAccessibilityService.pending
        if (request == null || request.id != intent.getStringExtra("request") || !hasWindowFocus()) { finish(); return }
        val clipboard = getSystemService(ClipboardManager::class.java)
        val clip = try { clipboard.primaryClip } catch (_: SecurityException) {
            InstagramAccessibilityService.connected?.diagnostic("CLIPBOARD denied=true")
            InstagramAccessibilityService.connected?.finish(Result.failure(IllegalStateException("CLIPBOARD_ACCESS_DENIED")))
            finish(); return
        }
        val timestamp = clip?.description?.timestamp ?: 0
        if (attempts == 0) InstagramAccessibilityService.connected?.diagnostic("CLIPBOARD focused=${hasWindowFocus()} present=${clip != null} fresh=${timestamp >= request.copiedAfter} items=${clip?.itemCount ?: 0}")
        if (timestamp >= request.copiedAfter && request.copiedAfter > 0 && clip?.itemCount == 1) {
            val item = clip.getItemAt(0)
            val raw = item.text?.toString() ?: item.uri?.toString()
            val result = InstagramShareParser.parse(raw)
            val url = when (result) {
                is ShareResult.Target -> result.target.canonicalUrl
                is ShareResult.RedirectLink -> result.canonicalUrl
                else -> null
            }
            if (url != null) { InstagramAccessibilityService.connected?.diagnostic("CLIPBOARD fresh_reel=true"); InstagramAccessibilityService.connected?.finish(Result.success(url)); finish(); return }
        }
        if (++attempts < 20) main.postDelayed({ readFreshLink() }, 100)
        else { InstagramAccessibilityService.connected?.finish(Result.failure(IllegalStateException("FRESH_REEL_LINK_MISSING"))); finish() }
    }
    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        if (InstagramAccessibilityService.pending?.id == intent.getStringExtra("request")) {
            InstagramAccessibilityService.connected?.finish(Result.failure(IllegalStateException("LINK_HANDOFF_CLOSED")))
        }
        super.onDestroy()
    }
}
