package io.github.ahmed9461.tapsave.platform.instagram

/** Small semantic vocabulary; unknown/ambiguous controls fail instead of guessing a position. */
object InstagramControls {
    enum class Action { SHARE, COPY_LINK }
    // Semantic ID tokens, not claims about a particular Instagram build's IDs.
    // Unknown IDs remain available in the redacted on-device trace for validation.
    fun idMatches(action: Action, resourceId: String): Boolean {
        val id = resourceId.substringAfter(":id/", "").lowercase()
        return when (action) {
            Action.COPY_LINK -> id.contains("copy") && (id.contains("link") || id.contains("permalink"))
            Action.SHARE -> id in setOf("reel_share_button", "clips_share_button", "row_feed_button_share", "share_button")
        }
    }
    fun matches(action: Action, text: String, resourceId: String = ""): Boolean {
        val label = text.replace(Regex("[\u200e\u200f\u202a-\u202e\u2066-\u2069]"), "").trim().lowercase().replace(Regex("\\s+"), " ").removeSuffix(".").removeSuffix("…")
        if (action == Action.SHARE && label in setOf("send", "إرسال")) return resourceId.contains("share", true) || resourceId.contains("clips", true)
        return label in when (action) {
            Action.SHARE -> setOf("share", "share reel", "share video", "share post", "مشاركة", "مشاركة الريل", "مشاركة الفيديو", "مشاركة المنشور")
            Action.COPY_LINK -> setOf("copy link", "copy link to reel", "نسخ الرابط", "نسخ رابط")
        }
    }
}
