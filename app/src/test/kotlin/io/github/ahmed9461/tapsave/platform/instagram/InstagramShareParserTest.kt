package io.github.ahmed9461.tapsave.platform.instagram

import io.github.ahmed9461.tapsave.platform.ShareResult
import io.github.ahmed9461.tapsave.platform.SharedTarget
import org.junit.Assert.assertEquals
import org.junit.Test

class InstagramShareParserTest {
    private val target = ShareResult.Target(SharedTarget("instagram:reel:AbC_12-", "https://www.instagram.com/reel/AbC_12-/"))

    @Test fun normalizesCaptionTrackingAndPunctuation() {
        assertEquals(target, InstagramShareParser.parse("شاهد هذا: (https://www.instagram.com/reel/AbC_12-/?igsh=secret#fragment)."))
    }

    @Test fun acceptsHostCaseAndReelsAliasWithoutChangingShortcode() {
        assertEquals(target, InstagramShareParser.parse("HTTPS://INSTAGRAM.COM/reels/AbC_12-"))
    }

    @Test fun collapsesRepeatedSameTarget() {
        assertEquals(target, InstagramShareParser.parse("https://instagram.com/reel/AbC_12-/ https://www.instagram.com/reels/AbC_12-/?igsh=x"))
    }

    @Test fun rejectsAmbiguousTargets() {
        assertEquals(ShareResult.Ambiguous, InstagramShareParser.parse("https://instagram.com/reel/one/ https://instagram.com/reel/two/"))
    }

    @Test fun shareTokenIsNeverAssumedToBeShortcode() {
        assertEquals(ShareResult.RedirectLink("https://www.instagram.com/share/reel/token/"), InstagramShareParser.parse("https://www.instagram.com/share/reel/token/?igsh=x"))
    }

    @Test fun rejectsUntrustedAuthoritiesAndSchemes() {
        listOf(
            "https://instagram.com.evil.test/reel/AbC_12-/",
            "https://evil.test/?next=https://instagram.com/reel/AbC_12-/",
            "https://instagram.com@evil.test/reel/AbC_12-/",
            "https://evil.test@instagram.com/reel/AbC_12-/",
            "https://instagram.com:443/reel/AbC_12-/",
            "https://instagram.com\\@evil.test/reel/AbC_12-/",
            "http://instagram.com/reel/AbC_12-/",
            "file:///reel/AbC_12-/",
        ).forEach { assertEquals(it, ShareResult.Invalid, InstagramShareParser.parse(it)) }
    }

    @Test fun rejectsUnsupportedOrEncodedPaths() {
        listOf("/", "/p/AbC_12-/", "/stories/user/1/", "/reel/", "/reel/abc/extra", "/reel/a%2Fb/", "/reel/a%00b/", "/reel/../", "/reel/a+b/", "/reel/عربي/")
            .forEach { assertEquals(it, ShareResult.Invalid, InstagramShareParser.parse("https://instagram.com$it")) }
    }

    @Test fun rejectsEmptyAndOversizedInput() {
        listOf(null, "", "caption only", "x".repeat(InstagramShareParser.MAX_TEXT_LENGTH + 1))
            .forEach { assertEquals(ShareResult.Invalid, InstagramShareParser.parse(it)) }
    }

    @Test fun rejectsAdditionalUnsupportedLinkInsteadOfGuessing() {
        assertEquals(ShareResult.Invalid, InstagramShareParser.parse("https://instagram.com/reel/AbC_12-/ https://evil.test/"))
    }
}
