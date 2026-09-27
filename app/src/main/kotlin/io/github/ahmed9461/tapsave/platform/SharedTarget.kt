package io.github.ahmed9461.tapsave.platform

data class SharedTarget(val key: String, val canonicalUrl: String)

sealed interface ShareResult {
    data class Target(val target: SharedTarget) : ShareResult
    // A share token is not a Reel shortcode. Resolution needs a separate, bounded network step.
    data class RedirectLink(val canonicalUrl: String) : ShareResult
    data object Invalid : ShareResult
    data object Ambiguous : ShareResult
}

fun interface SharedTargetParser {
    fun parse(text: String?): ShareResult
}

/** A share token is a pending input, never used as a media identity. */
fun ShareResult.pendingTarget(): SharedTarget? = when (this) {
    is ShareResult.Target -> target
    is ShareResult.RedirectLink -> SharedTarget("instagram:share:${canonicalUrl.trimEnd('/').substringAfterLast('/')}", canonicalUrl)
    else -> null
}
