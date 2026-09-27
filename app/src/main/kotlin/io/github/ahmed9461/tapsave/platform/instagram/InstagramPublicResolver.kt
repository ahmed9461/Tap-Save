package io.github.ahmed9461.tapsave.platform.instagram

import android.text.Html
import io.github.ahmed9461.tapsave.download.HttpTransfer
import io.github.ahmed9461.tapsave.download.NetworkPolicy
import io.github.ahmed9461.tapsave.download.SaveFailure
import io.github.ahmed9461.tapsave.download.TransferCancellation
import io.github.ahmed9461.tapsave.platform.SharedTarget
import java.net.URI
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

data class ResolvedVideo(val url: String, val expectsAudio: Boolean? = null, val pixels: Long = 0, val strategy: String = "")
fun interface ReelResolver { fun resolve(target: SharedTarget, cancellation: TransferCancellation): ResolvedVideo }

/** Public documents first, then the same documents with the explicitly enabled local session. */
class InstagramPublicResolver(
    private val http: HttpTransfer,
    private val sessionCookie: () -> String? = { null },
) : ReelResolver {
    override fun resolve(target: SharedTarget, cancellation: TransferCancellation): ResolvedVideo {
        val code = target.canonicalUrl.trimEnd('/').substringAfterLast('/')
        var last = SaveFailure(SaveFailure.Reason.METADATA_UNAVAILABLE)
        var authFailure: SaveFailure? = null
        fun attempt(cookie: String?, session: Boolean): ResolvedVideo? {
            for ((suffix, name) in listOf("" to "page", "embed/" to "embed")) {
                cancellation.check()
                val stage = (if (session) "session-" else "public-") + name
                try {
                    val (_, page) = http.page(target.canonicalUrl + suffix, cancellation, cookie)
                    PublicReelMetadata.parse(page, code)?.let { return it.copy(strategy = stage) }
                    last = SaveFailure(SaveFailure.Reason.METADATA_UNAVAILABLE, stage)
                } catch (failure: SaveFailure) {
                    val reason = if (failure.reason == SaveFailure.Reason.UNSUPPORTED) SaveFailure.Reason.EXTRACTOR_INCOMPATIBLE else failure.reason
                    last = SaveFailure(reason, stage, failure.status)
                    if (reason == SaveFailure.Reason.AUTH_REQUIRED) authFailure = last
                    if (reason in setOf(SaveFailure.Reason.RATE_LIMITED, SaveFailure.Reason.RESTRICTED, SaveFailure.Reason.NETWORK, SaveFailure.Reason.TOO_LARGE)) throw last
                }
            }
            return null
        }
        attempt(null, false)?.let { return it }
        cancellation.check()
        sessionCookie()?.let { cookie -> attempt(cookie, true)?.let { return it } }
        throw authFailure ?: last
    }
}

object PublicReelMetadata {
    private val scripts = Regex("<script\\b[^>]*>(.*?)</script\\s*>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val tags = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val attributes = Regex("([\\w:]+)\\s*=\\s*([\"'])(.*?)\\2", RegexOption.DOT_MATCHES_ALL)
    private val embeddedContext = Regex("\"contextJSON\"\\s*:\\s*(?=\")")

    fun parse(html: String, shortcode: String): ResolvedVideo? {
        if (html.contains("Post isn't available", true) || html.contains("The link to this photo or video may be broken", true)) {
            throw SaveFailure(SaveFailure.Reason.UNAVAILABLE)
        }
        if (Regex("\"challenge_required\"\\s*:\\s*true").containsMatchIn(html)) throw SaveFailure(SaveFailure.Reason.RESTRICTED)
        if (Regex("\"login_required\"\\s*:\\s*true").containsMatchIn(html)) throw SaveFailure(SaveFailure.Reason.AUTH_REQUIRED)
        var candidate: ResolvedVideo? = null
        fun select(value: ResolvedVideo) { if (candidate == null || value.pixels > candidate!!.pixels) candidate = value }
        var visited = 0
        var embeddedFields = 0
        fun walk(value: Any?, depth: Int) {
            if (depth > 60 || ++visited > 60_000) throw SaveFailure(SaveFailure.Reason.UNSUPPORTED)
            when (value) {
                is JSONObject -> {
                    val matches = value.optString("shortcode") == shortcode || value.optString("code") == shortcode
                    if (matches) {
                        if (value.optBoolean("copyright_blocked") || value.optBoolean("is_private") || value.optJSONObject("owner")?.optBoolean("is_private") == true || value.optJSONObject("user")?.optBoolean("is_private") == true) {
                            throw SaveFailure(SaveFailure.Reason.RESTRICTED)
                        }
                        val audio = if (value.has("has_audio")) value.optBoolean("has_audio") else null
                        val versions = value.optJSONArray("video_versions")
                        var best: Pair<Long, String>? = null
                        if (versions != null) for (i in 0 until versions.length()) {
                            val video = versions.optJSONObject(i) ?: continue
                            val url = video.optString("url")
                            val area = video.optLong("width", 0) * video.optLong("height", 0)
                            if (validMedia(url) && (best == null || area > best.first)) best = area to url
                        }
                        val url = best?.second ?: value.optString("video_url")
                        val dimensions = value.optJSONObject("dimensions")
                        val pixels = best?.first ?: ((dimensions?.optLong("width", 0) ?: value.optLong("original_width", 0)) * (dimensions?.optLong("height", 0) ?: value.optLong("original_height", 0)))
                        if (validMedia(url)) select(ResolvedVideo(url, audio, pixels))
                    }
                    value.keys().forEach { key ->
                        val child = value.opt(key)
                        if (key == "contextJSON" && child is String) {
                            try { walk(JSONTokener(child).nextValue(), depth + 1) } catch (_: JSONException) { /* unsupported document */ }
                        } else if (child is JSONObject || child is JSONArray) walk(child, depth + 1)
                    }
                }
                is JSONArray -> for (i in 0 until value.length()) walk(value.opt(i), depth + 1)
            }
        }
        scripts.findAll(html).forEach { script ->
            val data = script.groupValues[1].trim()
            if (data.startsWith('{') || data.startsWith('[')) {
                try { walk(JSONTokener(data).nextValue(), 0) } catch (_: JSONException) { /* scripts are not all JSON */ }
            } else {
                // Public embeds wrap ServerJS data in requireLazy(...). Decode only the JSON
                // string literal, never evaluate JavaScript or import its runtime/configuration.
                embeddedContext.findAll(data).forEach { field ->
                    if (++embeddedFields > 16) throw SaveFailure(SaveFailure.Reason.UNSUPPORTED)
                    try {
                        val encoded = JSONTokener(data.substring(field.range.last + 1)).nextValue()
                        if (encoded is String) walk(JSONTokener(encoded).nextValue(), 0)
                    } catch (_: JSONException) { /* unsupported embedded data */ }
                }
            }
        }
        if (candidate != null) return candidate
        val meta = mutableMapOf<String, String>()
        tags.findAll(html).forEach { tag ->
            val attrs = attributes.findAll(tag.value).associate { it.groupValues[1].lowercase() to it.groupValues[3] }
            val name = attrs["property"] ?: attrs["name"]
            if (name != null) meta[name] = Html.fromHtml(attrs["content"].orEmpty(), Html.FROM_HTML_MODE_LEGACY).toString()
        }
        val canonical = meta["og:url"]?.let { InstagramShareParser.parse(it) }
        if (canonical is io.github.ahmed9461.tapsave.platform.ShareResult.Target && canonical.target.canonicalUrl == "https://www.instagram.com/reel/$shortcode/") {
            val url = meta["og:video:secure_url"] ?: meta["og:video"]
            if (url != null && validMedia(url)) return ResolvedVideo(url)
        }
        return null
    }

    private fun validMedia(value: String) = try { NetworkPolicy.media(URI(value)) } catch (_: Exception) { false }
}
