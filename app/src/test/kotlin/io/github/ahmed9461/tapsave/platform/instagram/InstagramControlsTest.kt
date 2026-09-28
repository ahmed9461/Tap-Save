package io.github.ahmed9461.tapsave.platform.instagram
import org.junit.Assert.*
import org.junit.Test
class InstagramControlsTest {
    @Test fun arabicAndEnglishControlsMatchWithoutCoordinates() {
        for (text in listOf("مشاركة", "مشاركة الريل", "Share", "Share reel")) assertTrue(text, InstagramControls.matches(InstagramControls.Action.SHARE, text))
        for (text in listOf("نسخ الرابط", "نسخ رابط", "Copy link", "Copy Link")) assertTrue(text, InstagramControls.matches(InstagramControls.Action.COPY_LINK, text))
    }
    @Test fun sendRequiresShareSpecificResourceAndUnrelatedControlsNeverMatch() {
        assertFalse(InstagramControls.matches(InstagramControls.Action.SHARE, "إرسال"))
        assertTrue(InstagramControls.matches(InstagramControls.Action.SHARE, "إرسال", "com.instagram.android:id/clips_share_button"))
        for (text in listOf("Send message", "إرسال رسالة", "Like", "Follow", "مشاركة في قصتك")) assertFalse(text, InstagramControls.matches(InstagramControls.Action.SHARE, text))
        assertFalse(InstagramControls.matches(InstagramControls.Action.COPY_LINK, "Copy caption"))
    }
    @Test fun semanticResourceIdsDoNotDependOnLocaleAndGenericLinksAreRejected() {
        assertTrue(InstagramControls.idMatches(InstagramControls.Action.COPY_LINK, "com.instagram.android:id/copy_link_button"))
        assertFalse(InstagramControls.idMatches(InstagramControls.Action.COPY_LINK, "com.instagram.android:id/link_preview"))
        assertTrue(InstagramControls.matches(InstagramControls.Action.COPY_LINK, "\u200fنسخ  الرابط\u200f"))
        assertFalse(InstagramControls.matches(InstagramControls.Action.COPY_LINK, "Link"))
    }
}
