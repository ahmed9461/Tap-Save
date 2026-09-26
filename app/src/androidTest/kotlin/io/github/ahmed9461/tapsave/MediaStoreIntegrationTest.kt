package io.github.ahmed9461.tapsave

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import io.github.ahmed9461.tapsave.storage.MediaStoreVideoWriter
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.concurrent.CancellationException
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class MediaStoreIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val resolver = context.contentResolver
    private val writer = MediaStoreVideoWriter(context)
    private val allocated = mutableListOf<Uri>()
    private val fixture get() = instrumentation.context.assets.open("fixture.mp4").use { it.readBytes() }
    private fun name() = "TapSave_test_${System.nanoTime()}.mp4"

    @After fun cleanup() { allocated.forEach { resolver.delete(it, null, null) } }

    @Test fun publishesExactVideoAndAudioOnlyAfterCopy() {
        val bytes = fixture
        val filename = name()
        var lastProgress = 0L
        val uri = writer.write(filename, bytes.inputStream(), bytes.size.toLong(), onProgress = { copied ->
            assertTrue(copied >= lastProgress)
            lastProgress = copied
            assertEquals(1, pendingValue(filename))
        })
        allocated += uri
        assertEquals(bytes.size.toLong(), lastProgress)
        assertEquals(0, pendingValue(filename))
        assertArrayEquals(bytes, resolver.openInputStream(uri)!!.use { it.readBytes() })
        val metadata = MediaMetadataRetriever()
        try {
            metadata.setDataSource(context, uri)
            assertEquals("yes", metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO))
            assertEquals("yes", metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO))
        } finally { metadata.release() }
    }

    @Test fun cancellationDeletesPendingRowAndAllowsRetry() {
        val filename = name()
        val bytes = fixture
        var cancel = false
        assertThrows(CancellationException::class.java) {
            writer.write(filename, bytes.inputStream(), bytes.size.toLong(), cancelled = { cancel }, onProgress = { cancel = true })
        }
        assertEquals(null, pendingValue(filename))
        allocated += writer.write(filename, bytes.inputStream(), bytes.size.toLong())
        assertEquals(0, pendingValue(filename))
    }

    @Test fun interruptedInputAndTruncatedContentAreNeverPublished() {
        val filename = name()
        val bytes = fixture
        val broken = object : ByteArrayInputStream(bytes) {
            var reads = 0
            var closed = false
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (reads++ > 0) throw IOException("Simulated connection loss")
                return super.read(buffer, offset, minOf(length, 512))
            }
            override fun close() { closed = true; super.close() }
        }
        assertThrows(IOException::class.java) { writer.write(filename, broken, bytes.size.toLong()) }
        assertTrue(broken.closed)
        assertEquals(null, pendingValue(filename))
        assertThrows(IOException::class.java) { writer.write(filename, bytes.copyOf(512).inputStream(), bytes.size.toLong()) }
        assertEquals(null, pendingValue(filename))
    }

    @Test fun nonVideoResponseIsRemoved() {
        val filename = name()
        assertThrows(Exception::class.java) { writer.write(filename, "<html>Not a video</html>".byteInputStream()) }
        assertEquals(null, pendingValue(filename))
    }

    @Test fun inputCloseFailureCannotPublishAFile() {
        val filename = name()
        val bytes = fixture
        val input = object : ByteArrayInputStream(bytes) {
            override fun close() { throw IOException("Simulated source close failure") }
        }
        assertThrows(IOException::class.java) { writer.write(filename, input, bytes.size.toLong()) }
        assertEquals(null, pendingValue(filename))
    }

    @Test fun storageNameCollisionDoesNotOverwriteExistingVideo() {
        val filename = name()
        val bytes = fixture
        val first = writer.write(filename, bytes.inputStream(), bytes.size.toLong()).also { allocated += it }
        val second = writer.write(filename, bytes.inputStream(), bytes.size.toLong()).also { allocated += it }
        assertNotEquals(first, second)
        assertArrayEquals(bytes, resolver.openInputStream(first)!!.use { it.readBytes() })
        assertArrayEquals(bytes, resolver.openInputStream(second)!!.use { it.readBytes() })
    }

    private fun pendingValue(filename: String): Int? = resolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Video.Media.IS_PENDING, MediaStore.Video.Media.RELATIVE_PATH),
        "${MediaStore.Video.Media.DISPLAY_NAME} = ?",
        arrayOf(filename), null,
    )!!.use { cursor ->
        if (!cursor.moveToFirst()) null else {
            assertEquals(MediaStoreVideoWriter.FOLDER, cursor.getString(1))
            val pending = cursor.getInt(0)
            assertFalse(cursor.moveToNext())
            pending
        }
    }
}
