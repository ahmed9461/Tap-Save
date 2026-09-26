package io.github.ahmed9461.tapsave.share

import android.content.Intent
import io.github.ahmed9461.tapsave.platform.ShareResult
import io.github.ahmed9461.tapsave.platform.instagram.InstagramShareParser

fun readShareIntent(intent: Intent): ShareResult {
    if (intent.action != Intent.ACTION_SEND || intent.type != "text/plain") return ShareResult.Invalid
    return try {
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
        if (text == null || text.length > InstagramShareParser.MAX_TEXT_LENGTH) ShareResult.Invalid
        else InstagramShareParser.parse(text.toString())
    } catch (_: RuntimeException) {
        // Exported entry point: malformed parcelables must not crash the receiver.
        ShareResult.Invalid
    }
}
