package io.github.ahmed9461.tapsave.platform.instagram

/** Small semantic vocabulary; unknown/ambiguous controls fail instead of guessing a position. */
object InstagramControls {
    enum class Action { SHARE, COPY_LINK }
    fun matches(action: Action, text: String, resourceId: String = ""): Boolean {
        val label = text.trim().lowercase().removeSuffix(".").removeSuffix("…")
        if (action == Action.SHARE && label in setOf("send", "إرسال")) return resourceId.contains("share", true) || resourceId.contains("clips", true)
        return label in when (action) {
            Action.SHARE -> setOf("share", "share reel", "share video", "share post", "مشاركة", "مشاركة الريل", "مشاركة الفيديو", "مشاركة المنشور")
            Action.COPY_LINK -> setOf("copy link", "copy link to reel", "link", "نسخ الرابط", "نسخ رابط", "رابط")
        }
    }
}
