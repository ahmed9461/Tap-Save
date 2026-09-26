package io.github.ahmed9461.tapsave.download

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import io.github.ahmed9461.tapsave.platform.SharedTarget
import io.github.ahmed9461.tapsave.platform.instagram.InstagramPublicResolver
import io.github.ahmed9461.tapsave.platform.instagram.ReelResolver
import io.github.ahmed9461.tapsave.storage.MediaStoreVideoWriter

fun interface SaveRunner {
    fun save(target: SharedTarget, cancellation: TransferCancellation, progress: (Long, Long?) -> Unit): Uri
}

class SavePipeline(
    private val context: Context,
    private val http: HttpTransfer = HttpTransfer(),
    private val resolver: ReelResolver = InstagramPublicResolver(http),
) : SaveRunner {
    override fun save(target: SharedTarget, cancellation: TransferCancellation, progress: (Long, Long?) -> Unit): Uri {
        val video = resolver.resolve(target, cancellation)
        cancellation.check()
        return http.get(video.url, NetworkPolicy::media, cancellation).use { response ->
            if (response.type !in setOf("video/mp4", "application/octet-stream")) throw SaveFailure(SaveFailure.Reason.UNSUPPORTED)
            if ((response.length ?: 0) > MediaStoreVideoWriter.MAX_BYTES) throw SaveFailure(SaveFailure.Reason.TOO_LARGE)
            progress(0, response.length)
            try {
                MediaStoreVideoWriter(context).write(
                    mediaName(target), response.input, response.length,
                    cancelled = { cancellation.check(); false },
                    onProgress = { progress(it, response.length) },
                    expectsAudio = video.expectsAudio,
                )
            } catch (failure: Exception) {
                cancellation.check()
                throw failure
            }
        }
    }
}

fun mediaName(target: SharedTarget): String = "TapSave_${target.key.substringAfterLast(':')}.mp4"

/** Only app-owned rows for this exact target. Covers death between insert and a URI checkpoint. */
@Suppress("DEPRECATION")
fun reconcileMedia(context: Context, target: SharedTarget): Uri? {
    val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    val uri = if (Build.VERSION.SDK_INT >= 30) collection else MediaStore.setIncludePending(collection)
    val args = Bundle().apply {
        putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.Video.Media.DISPLAY_NAME} = ? AND ${MediaStore.Video.Media.RELATIVE_PATH} = ?")
        putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf(mediaName(target), MediaStoreVideoWriter.FOLDER))
        if (Build.VERSION.SDK_INT >= 30) putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE)
    }
    val pending = mutableListOf<Uri>()
    var saved: Uri? = null
    context.contentResolver.query(uri, arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.IS_PENDING, MediaStore.Video.Media.OWNER_PACKAGE_NAME), args, null)?.use { cursor ->
        while (cursor.moveToNext()) {
            if (cursor.getString(2) != context.packageName) continue
            val row = ContentUris.withAppendedId(collection, cursor.getLong(0))
            if (cursor.getInt(1) == 1) pending += row else saved = row
        }
    }
    pending.forEach { context.contentResolver.delete(it, null, null) }
    return saved
}
