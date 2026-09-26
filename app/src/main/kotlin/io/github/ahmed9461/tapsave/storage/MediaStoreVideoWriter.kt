package io.github.ahmed9461.tapsave.storage

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Looper
import android.provider.MediaStore
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CancellationException

/** Blocking storage primitive; after argument validation it consumes/closes input, even on failure. */
class MediaStoreVideoWriter(private val context: Context) {
    fun write(
        displayName: String,
        input: InputStream,
        expectedBytes: Long? = null,
        cancelled: () -> Boolean = { Thread.currentThread().isInterrupted },
        onProgress: (Long) -> Unit = {},
    ): Uri {
        check(Looper.myLooper() != Looper.getMainLooper()) { "Media writes must run off the UI thread" }
        require(Regex("TapSave_[A-Za-z0-9_-]{1,80}\\.mp4").matches(displayName)) { "Unsafe media filename" }
        require(expectedBytes == null || expectedBytes > 0) { "Invalid expected size" }
        val resolver = context.contentResolver
        var allocatedUri: Uri? = null
        return try {
            val uri = input.use { source ->
                checkCancelled(cancelled)
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, FOLDER)
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val pending = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("Could not allocate media storage")
                allocatedUri = pending
                var copied = 0L
                resolver.openOutputStream(pending, "w")?.use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        checkCancelled(cancelled)
                        val count = source.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        copied += count
                        if (expectedBytes != null && copied > expectedBytes) throw IOException("Unexpected media size")
                        onProgress(copied)
                    }
                } ?: throw IOException("Could not open media storage")
                if (copied == 0L || (expectedBytes != null && copied != expectedBytes)) throw IOException("Incomplete media transfer")
                pending
            }
            // Both streams have closed successfully. Metadata validation is not a full-frame decode guarantee.
            val metadata = MediaMetadataRetriever()
            try {
                metadata.setDataSource(context, uri)
                if (metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) != "yes") {
                    throw IOException("The response is not a supported video")
                }
            } finally {
                metadata.release()
            }
            checkCancelled(cancelled)
            val complete = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
            if (resolver.update(uri, complete, null, null) != 1) throw IOException("Could not publish video")
            uri
        } catch (failure: Throwable) {
            try {
                allocatedUri?.let { resolver.delete(it, null, null) }
            } catch (cleanup: Throwable) {
                failure.addSuppressed(cleanup)
            }
            throw failure
        }
    }

    private fun checkCancelled(cancelled: () -> Boolean) {
        if (cancelled()) throw CancellationException("Save cancelled")
    }

    companion object { const val FOLDER = "Movies/Tap Save/" }
}
