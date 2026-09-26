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

data class ResolvedVideo(val url: String, val expectsAudio: Boolean? = null)
fun interface ReelResolver { fun resolve(target: SharedTarget, cancellation: TransferCancellation): ResolvedVideo }

/** Only metadata actually served on public Reel documents; no logged-in API or browser-cookie access. */
class InstagramPublicResolver(private val http: HttpTransfer) : ReelResolver {
    override fun resolve(target: SharedTarget, cancellation: TransferCancellation): ResolvedVideo {
        val code = target.canonicalUrl.trimEnd('/').substringAfterLast('/')
        val (_, page) = http.page(target.canonicalUrl, cancellation)
        PublicReelMetadata.parse(page, code)?.let { return it }
        cancellation.check()
        val (_, embed) = http.page("${target.canonicalUrl}embed/", cancellation)
        return PublicReelMetadata.parse(embed, code) ?: throw SaveFailure(SaveFailure.Reason.UNSUPPORTED)
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
        if (html.contains("\"login_required\":true") || html.contains("\"challenge_required\":true")) {
            throw SaveFailure(SaveFailure.Reason.RESTRICTED)
        }
        var candidate: ResolvedVideo? = null
        var visited = 0
        fun walk(value: Any?, depth: Int) {
            if (depth > 60 || ++visited > 60_000) throw SaveFailure(SaveFailure.Reason.UNSUPPORTED)
            when (value) {
                is JSONObject -> {
                    val matches = value.optString("shortcode") == shortcode || value.optString("code") == shortcode
                    if (matches) {
                        if (value.optBoolean("copyright_blocked") || value.optBoolean("is_private") || value.optJSONObject("owner")?.optBoolean("is_private") == true) {
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
                        if (validMedia(url)) candidate = ResolvedVideo(url, audio)
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
