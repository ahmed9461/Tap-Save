package io.github.ahmed9461.tapsave

import android.content.ContentValues
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import io.github.ahmed9461.tapsave.download.*
import io.github.ahmed9461.tapsave.platform.SharedTarget
import io.github.ahmed9461.tapsave.platform.instagram.PublicReelMetadata
import io.github.ahmed9461.tapsave.storage.MediaStoreVideoWriter
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CancellationException

class SavePipelineIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val target = SharedTarget("instagram:reel:FixtureSave", "https://www.instagram.com/reel/FixtureSave/")
    private val bytes get() = instrumentation.context.assets.open("fixture.mp4").use { it.readBytes() }
    private val allocated = mutableListOf<Uri>()
    @After fun cleanup() { allocated.forEach { context.contentResolver.delete(it, null, null) } }

    private fun fixture(media: HttpFixture.Reply) = HttpFixture { path -> when {
        path.endsWith("/embed/") -> HttpFixture.Reply("text/html", publicEmbed("FixtureSave").toByteArray())
        path == "/media.mp4" -> media
        else -> HttpFixture.Reply("text/html", "<html>Public page shell</html>".toByteArray())
    } }

    @Test fun resolvesDownloadsAndPublishesExactBytesWithProgress() {
        val source = bytes
        fixture(HttpFixture.Reply("video/mp4", source)).use { server ->
            var received = 0L
            val uri = SavePipeline(context, server.http).save(target, TransferCancellation()) { count, total ->
                assertTrue(count >= received); received = count; assertEquals(source.size.toLong(), total)
            }.also { allocated += it }
            assertEquals(source.size.toLong(), received)
            assertArrayEquals(source, context.contentResolver.openInputStream(uri)!!.use { it.readBytes() })
            assertEquals(uri, reconcileMedia(context, target))
            assertEquals(listOf("/reel/FixtureSave/", "/reel/FixtureSave/embed/", "/media.mp4"), server.paths)
        }
    }

    @Test fun cancellationDuringHttpCopyDeletesPartialFile() {
        fixture(HttpFixture.Reply("video/mp4", bytes, delayMs = 150)).use { server ->
            val signal = TransferCancellation()
            assertThrows(CancellationException::class.java) {
                SavePipeline(context, server.http).save(target, signal) { count, _ -> if (count > 0) signal.cancel() }
            }
            assertNoMediaRows()
        }
    }

    @Test fun truncatedHttpAndNonVideoResponsesAreNotPublished() {
        val source = bytes
        listOf(HttpFixture.Reply("video/mp4", source.copyOf(500), source.size), HttpFixture.Reply("text/html", source)).forEach { response ->
            fixture(response).use { server ->
                assertThrows(Exception::class.java) { SavePipeline(context, server.http).save(target, TransferCancellation()) { _, _ -> } }
                assertNoMediaRows()
            }
        }
    }

    @Test fun restrictedMetadataNeverRequestsMediaOrAnotherEndpoint() {
        HttpFixture { HttpFixture.Reply("text/html", publicEmbed("FixtureSave", restricted = true).toByteArray()) }.use { server ->
            assertEquals(SaveFailure.Reason.RESTRICTED, assertThrows(SaveFailure::class.java) {
                SavePipeline(context, server.http).save(target, TransferCancellation()) { _, _ -> }
            }.reason)
            assertEquals(listOf("/reel/FixtureSave/"), server.paths)
        }
    }

    @Test fun metadataCannotSelectAnotherReelOrUntrustedCdn() {
        assertNull(PublicReelMetadata.parse(publicEmbed("OtherReel"), "FixtureSave"))
        assertNull(PublicReelMetadata.parse(publicEmbed("FixtureSave").replace("video.cdninstagram.com", "cdninstagram.com.evil.test"), "FixtureSave"))
    }

    @Test fun redirectToLoginStopsWithoutFollowingIt() {
        HttpFixture { HttpFixture.Reply("text/html", byteArrayOf(), status = 302, location = "https://www.instagram.com/accounts/login/") }.use { server ->
            assertEquals(SaveFailure.Reason.RESTRICTED, assertThrows(SaveFailure::class.java) {
                SavePipeline(context, server.http).save(target, TransferCancellation()) { _, _ -> }
            }.reason)
            assertEquals(1, server.paths.size)
        }
    }

    @Test fun interruptedPendingRowIsCleanedWithoutDeletingPublishedMedia() {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, mediaName(target))
            put(MediaStore.Video.Media.RELATIVE_PATH, MediaStoreVideoWriter.FOLDER)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val pending = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)!!.also { allocated += it }
        assertNull(reconcileMedia(context, target))
        // A deleted item URI can throw SecurityException on modern MediaStore; inspect the collection.
        assertNoMediaRows()
        allocated.remove(pending)
        val complete = MediaStoreVideoWriter(context).write(mediaName(target), bytes.inputStream()).also { allocated += it }
        assertEquals(complete, reconcileMedia(context, target))
        assertNotNull(context.contentResolver.openInputStream(complete)?.use { it.read() })
    }

    @Suppress("DEPRECATION") // API 29 includes pending rows via the URI flag.
    private fun assertNoMediaRows() {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val uri = if (Build.VERSION.SDK_INT >= 30) collection else MediaStore.setIncludePending(collection)
        val query = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.Video.Media.DISPLAY_NAME} = ? AND ${MediaStore.Video.Media.RELATIVE_PATH} = ?")
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf(mediaName(target), MediaStoreVideoWriter.FOLDER))
            if (Build.VERSION.SDK_INT >= 30) putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE)
        }
        context.contentResolver.query(uri, arrayOf(MediaStore.Video.Media._ID), query, null)!!.use {
            assertEquals("Neither pending nor published rows may remain", 0, it.count)
        }
    }
}
