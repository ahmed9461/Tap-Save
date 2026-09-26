package io.github.ahmed9461.tapsave

import android.content.Intent
import android.media.MediaMetadataRetriever
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.github.ahmed9461.tapsave.download.*
import io.github.ahmed9461.tapsave.platform.ShareResult
import io.github.ahmed9461.tapsave.platform.instagram.InstagramShareParser
import io.github.ahmed9461.tapsave.share.ShareActivity
import org.junit.Assert.*
import org.junit.Test

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class LiveNetwork

/** Explicit workflow-dispatch gate, excluded from deterministic CI. Never downloads an arbitrary URL. */
@LiveNetwork
class LiveReelSaveTest {
    @Test fun publicReelShareSavesPlayableVideoAndAudio() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val raw = InstrumentationRegistry.getArguments().getString("liveReel")!!
        val target = (InstagramShareParser.parse(raw) as ShareResult.Target).target
        SaveService.testFactory = null
        instrumentation.runOnMainSync { SaveUiState.current = null }
        val intent = Intent(context, ShareActivity::class.java).setAction(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, target.canonicalUrl)
        ActivityScenario.launch<ShareActivity>(intent).use {
            waitForSave(120_000) { SaveUiState.current?.let { state -> !state.active } == true }
            val state = SaveUiState.current!!
            assertEquals("Public resolution failed: ${state.failure}", SavePhase.SAVED, state.phase)
            val uri = state.uri!!
            try {
                assertEquals(uri, reconcileMedia(context, target))
                val metadata = MediaMetadataRetriever()
                try {
                    metadata.setDataSource(context, uri)
                    assertEquals("yes", metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO))
                    assertEquals("yes", metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO))
                    assertTrue(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong() > 0)
                    assertNotNull(metadata.getFrameAtTime(0))
                } finally { metadata.release() }
                assertTrue(context.contentResolver.openAssetFileDescriptor(uri, "r")!!.use { it.length > 0 })
            } finally { context.contentResolver.delete(uri, null, null) }
        }
    }
}
