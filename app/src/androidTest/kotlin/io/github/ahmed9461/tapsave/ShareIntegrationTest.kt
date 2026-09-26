package io.github.ahmed9461.tapsave

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.github.ahmed9461.tapsave.platform.ShareResult
import io.github.ahmed9461.tapsave.share.ShareActivity
import io.github.ahmed9461.tapsave.share.readShareIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class ShareIntegrationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun share(text: String) = Intent(context, ShareActivity::class.java)
        .setAction(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)

    @Test fun manifestResolvesImplicitTextShare() {
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain").setPackage(context.packageName)
        assertNotNull(context.packageManager.resolveActivity(intent, 0))
    }

    @Test fun normalizesActualIntentAndSurvivesActivityRecreation() {
        ActivityScenario.launch<ShareActivity>(share("Reel: https://instagram.com/reel/AbC_12-/?igsh=remove-me")).use { scenario ->
            compose.onNodeWithText("https://www.instagram.com/reel/AbC_12-/").assertIsDisplayed()
            scenario.recreate()
            compose.onNodeWithText("https://www.instagram.com/reel/AbC_12-/").assertIsDisplayed()
            compose.onNodeWithText(context.getString(R.string.spike_status)).assertIsDisplayed()
        }
    }

    @Test fun invalidIntentShowsRecoveryMessage() {
        ActivityScenario.launch<ShareActivity>(share("https://instagram.com.evil.test/reel/test/")).use {
            compose.onNodeWithText(context.getString(R.string.invalid_share)).assertIsDisplayed()
        }
    }

    @Test fun rejectsWrongActionTypeAndMissingText() {
        assertEquals(ShareResult.Invalid, readShareIntent(Intent(Intent.ACTION_VIEW)))
        assertEquals(ShareResult.Invalid, readShareIntent(share("https://instagram.com/reel/one/").setType("image/jpeg")))
        assertEquals(ShareResult.Invalid, readShareIntent(Intent(Intent.ACTION_SEND).setType("text/plain")))
        assertEquals(ShareResult.Invalid, readShareIntent(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, 42)))
    }
}
