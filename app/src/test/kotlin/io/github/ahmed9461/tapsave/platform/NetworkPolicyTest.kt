package io.github.ahmed9461.tapsave.platform

import io.github.ahmed9461.tapsave.download.NetworkPolicy
import java.net.URI
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkPolicyTest {
    @Test fun publicDocumentsOnly() {
        assertTrue(NetworkPolicy.page(URI("https://www.instagram.com/p/Ab_1-")))
        assertTrue(NetworkPolicy.page(URI("https://www.instagram.com/reel/Ab_1-/embed/")))
        listOf("https://www.instagram.com/accounts/login/", "https://www.instagram.com/api/v1/media/1/info/", "https://instagram.com.evil.test/reel/a/", "http://instagram.com/reel/a/", "https://user@instagram.com/reel/a/", "https://instagram.com:443/reel/a/", "https://127.0.0.1/reel/a/")
            .forEach { assertFalse(it, NetworkPolicy.page(URI(it))) }
    }
    @Test fun exactCdnHostBoundaries() {
        assertTrue(NetworkPolicy.media(URI("https://scontent.cdninstagram.com/video.mp4?sig=test")))
        assertTrue(NetworkPolicy.media(URI("https://video.xx.fbcdn.net/video.mp4")))
        listOf("https://cdninstagram.com.evil.test/a.mp4", "https://evilcdninstagram.com/a.mp4", "http://video.fbcdn.net/a.mp4", "https://video.fbcdn.net:443/a.mp4", "https://127.0.0.1/a.mp4", "file:///a.mp4")
            .forEach { assertFalse(it, NetworkPolicy.media(URI(it))) }
    }
}
