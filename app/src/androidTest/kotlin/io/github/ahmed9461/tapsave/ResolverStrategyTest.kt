package io.github.ahmed9461.tapsave

import androidx.test.platform.app.InstrumentationRegistry
import io.github.ahmed9461.tapsave.download.*
import io.github.ahmed9461.tapsave.platform.*
import io.github.ahmed9461.tapsave.platform.instagram.*
import io.github.ahmed9461.tapsave.session.*
import org.junit.Assert.*
import org.junit.Test
import java.net.URI

class ResolverStrategyTest {
    private val target = SharedTarget("instagram:reel:Strategy", "https://www.instagram.com/reel/Strategy/")
    @Test fun publicSuccessNeverReadsSession() {
        HttpFixture { HttpFixture.Reply("text/html", publicEmbed("Strategy").toByteArray()) }.use {
            val result = InstagramPublicResolver(it.http) { throw AssertionError("Session touched before public failure") }.resolve(target, TransferCancellation())
            assertEquals("public-page", result.strategy)
            assertFalse(it.headers.single().containsKey("cookie"))
        }
    }
    @Test fun publicFailuresThenSessionUseBoundedOrderedChain() {
        var requests = 0
        HttpFixture { HttpFixture.Reply("text/html", (if (++requests == 4) publicEmbed("Strategy") else "<html>shell</html>").toByteArray()) }.use {
            val result = InstagramPublicResolver(it.http) { "sessionid=synthetic_fixture" }.resolve(target, TransferCancellation())
            assertEquals("session-page", result.strategy)
            assertEquals(listOf("/reel/Strategy/", "/reel/Strategy/embed/", "/p/Strategy", "/reel/Strategy/"), it.paths)
            assertFalse(it.headers[0].containsKey("cookie")); assertFalse(it.headers[1].containsKey("cookie"))
            assertFalse(it.headers[2].containsKey("cookie"))
            assertEquals("sessionid=synthetic_fixture", it.headers[3]["cookie"])
        }
    }
    @Test fun rateLimitsAndRestrictionsNeverEscalateToSession() {
        for (reply in listOf(HttpFixture.Reply("text/html", byteArrayOf(), status = 429), HttpFixture.Reply("text/html", publicEmbed("Strategy", restricted = true).toByteArray()))) {
            HttpFixture { reply }.use {
                val error = assertThrows(SaveFailure::class.java) { InstagramPublicResolver(it.http) { throw AssertionError("Must stop") }.resolve(target, TransferCancellation()) }
                assertTrue(error.reason in setOf(SaveFailure.Reason.RATE_LIMITED, SaveFailure.Reason.RESTRICTED))
                assertEquals(1, it.paths.size)
            }
        }
    }
    @Test fun sessionCookiesNeverReachCdnOrRedirectedDifferentOrigin() {
        HttpFixture { path -> if (path == "/reel/Strategy/") HttpFixture.Reply("text/html", byteArrayOf(), status = 302, location = "https://instagram.com/reel/Next/") else HttpFixture.Reply("text/html", byteArrayOf()) }.use {
            it.http.get(target.canonicalUrl, NetworkPolicy::page, TransferCancellation(), "sessionid=synthetic_fixture").close()
            it.http.get("https://video.cdninstagram.com/media.mp4", NetworkPolicy::media, TransferCancellation(), "sessionid=synthetic_fixture", media = true).close()
            assertEquals("sessionid=synthetic_fixture", it.headers[0]["cookie"])
            assertFalse(it.headers[1].containsKey("cookie")); assertFalse(it.headers[2].containsKey("cookie"))
        }
        assertFalse(InstagramSession.allowsCookie(URI("https://www.instagram.com.evil.test/")))
        assertFalse(InstagramLoginActivity.navigationAllowed("https://example.com/"))
        assertFalse(InstagramLoginActivity.resourceAllowed("file:///data/private"))
        assertTrue(InstagramLoginActivity.resourceAllowed("https://static.cdninstagram.com/login.js"))
    }
    @Test fun choosesHighestResolutionAcrossAllMatchingMetadataAndPreservesAudioFlag() {
        val html = """<script>{"items":[{"code":"Strategy","has_audio":true,"video_versions":[{"width":1080,"height":1920,"url":"https://video.cdninstagram.com/high.mp4"},{"width":320,"height":480,"url":"https://video.cdninstagram.com/low.mp4"}]},{"code":"Strategy","video_url":"https://video.cdninstagram.com/unknown.mp4"}]}</script>"""
        val media = PublicReelMetadata.parse(html, "Strategy")!!
        assertTrue(media.url.endsWith("high.mp4")); assertEquals(true, media.expectsAudio)
        assertEquals(1080L * 1920, media.pixels)
    }
    @Test fun shortShareRedirectBecomesCanonicalIdentity() {
        HttpFixture { path -> if (path.startsWith("/share/")) HttpFixture.Reply("text/html", byteArrayOf(), status = 302, location = target.canonicalUrl + "?tracking=discard") else HttpFixture.Reply("text/html", byteArrayOf()) }.use {
            val input = InstagramShareParser.parse("https://www.instagram.com/share/reel/token/").pendingTarget()!!
            assertEquals(target, InstagramLinkNormalizer(it.http).normalize(input, TransferCancellation()))
        }
    }
    @Test fun expiredMediaGetsExactlyOneFreshResolution() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var resolutions = 0
        var transfers = 0
        val bytes = instrumentation.context.assets.open("fixture.mp4").use { it.readBytes() }
        HttpFixture { if (++transfers == 1) HttpFixture.Reply("video/mp4", byteArrayOf(), status = 403) else HttpFixture.Reply("video/mp4", bytes) }.use {
            val pipeline = SavePipeline(instrumentation.targetContext, it.http, ReelResolver { _, _ -> resolutions++; ResolvedVideo("https://video.cdninstagram.com/media.mp4", true) })
            val uri = pipeline.save(target, TransferCancellation()) { _, _ -> }
            try { assertEquals(2, resolutions); assertEquals(2, transfers) } finally { instrumentation.targetContext.contentResolver.delete(uri, null, null) }
        }
        resolutions = 0
        HttpFixture { HttpFixture.Reply("video/mp4", byteArrayOf(), status = 410) }.use {
            val error = assertThrows(SaveFailure::class.java) { SavePipeline(instrumentation.targetContext, it.http, ReelResolver { _, _ -> resolutions++; ResolvedVideo("https://video.cdninstagram.com/media.mp4") }).save(target, TransferCancellation()) { _, _ -> } }
            assertEquals(SaveFailure.Reason.EXPIRED_URL, error.reason); assertEquals(2, resolutions)
        }
    }
    @Test fun modernPublicPermalinkFallbackUsesExplicitHtmlAcceptAndMatchingCode() {
        val html = """<script type="application/json" data-sjs>{"require":[{"__bbox":{"result":{"data":{"xig_polaris_media":{"if_not_gated_logged_out":{"code":"Strategy","has_audio":true,"original_width":720,"original_height":1280,"video_versions":[{"type":101,"url":"https://video.cdninstagram.com/native.mp4"}]}}}}}}]}</script>"""
        HttpFixture { path -> HttpFixture.Reply("text/html", (if (path == "/p/Strategy") html else "<html>shell</html>").toByteArray()) }.use {
            val result = InstagramPublicResolver(it.http) { throw AssertionError("Public permalink must precede session") }.resolve(target, TransferCancellation())
            assertEquals("public-post", result.strategy); assertTrue(result.url.endsWith("native.mp4")); assertEquals(true, result.expectsAudio)
            assertTrue(it.headers.all { headers -> headers["accept"]!!.startsWith("text/html,application/xhtml+xml") })
        }
    }
    @Test fun isolatedSessionBrokerStartsClearsAndStaysOptional() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        InstagramSession.enable(context, false)
        assertNull(InstagramSession.cookies(context))
        assertTrue(InstagramSession.clear(context))
        InstagramSession.enable(context, true)
        try { assertNull(InstagramSession.cookies(context)) } finally { assertTrue(InstagramSession.clear(context)) }
        assertFalse(InstagramSession.enabled(context))
    }
}
