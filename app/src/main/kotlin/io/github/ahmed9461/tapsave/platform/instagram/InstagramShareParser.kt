package io.github.ahmed9461.tapsave.platform.instagram

import io.github.ahmed9461.tapsave.platform.ShareResult
import io.github.ahmed9461.tapsave.platform.SharedTarget
import io.github.ahmed9461.tapsave.platform.SharedTargetParser
import java.net.URI

object InstagramShareParser : SharedTargetParser {
    const val MAX_TEXT_LENGTH = 16_384
    private val urls = Regex("https?://[^\\s<>\"\\p{Cntrl}]+", RegexOption.IGNORE_CASE)
    private val reelPath = Regex("/reels?/([A-Za-z0-9_-]{1,64})/?")
    private val sharePath = Regex("/share/reel/([A-Za-z0-9_-]{1,64})/?")

    override fun parse(text: String?): ShareResult {
        if (text.isNullOrBlank() || text.length > MAX_TEXT_LENGTH) return ShareResult.Invalid
        val candidates = urls.findAll(text).map {
            parseUrl(it.value.trimEnd('.', ',', ';', '!', ')', ']', '}', '\u060c'))
        }.toList()
        if (candidates.isEmpty() || candidates.any { it == ShareResult.Invalid }) return ShareResult.Invalid
        return candidates.distinct().singleOrNull() ?: ShareResult.Ambiguous
    }

    private fun parseUrl(value: String): ShareResult {
        val uri = try {
            URI(value)
        } catch (_: java.net.URISyntaxException) {
            return ShareResult.Invalid
        }
        if (!uri.scheme.equals("https", ignoreCase = true) || uri.rawUserInfo != null || uri.port != -1) {
            return ShareResult.Invalid
        }
        if (uri.host?.lowercase() !in setOf("instagram.com", "www.instagram.com")) return ShareResult.Invalid
        val shortcode = reelPath.matchEntire(uri.rawPath ?: "")?.groupValues?.get(1)
        if (shortcode != null) {
            return ShareResult.Target(SharedTarget("instagram:reel:$shortcode", "https://www.instagram.com/reel/$shortcode/"))
        }
        val token = sharePath.matchEntire(uri.rawPath ?: "")?.groupValues?.get(1)
        return if (token != null) ShareResult.RedirectLink("https://www.instagram.com/share/reel/$token/")
        else ShareResult.Invalid
    }
}
