package io.github.ahmed9461.tapsave

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.github.ahmed9461.tapsave.download.*
import io.github.ahmed9461.tapsave.share.ShareActivity
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

fun waitForSave(timeout: Long = 15_000, condition: () -> Boolean) {
    val end = SystemClock.elapsedRealtime() + timeout
    while (!condition() && SystemClock.elapsedRealtime() < end) SystemClock.sleep(50)
    assertTrue("Save did not reach expected state: ${SaveUiState.current?.phase}", condition())
}

class SaveServiceIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private var server: HttpFixture? = null
    @Before fun permissions() {
        if (Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        instrumentation.runOnMainSync { SaveUiState.current = null }
    }
    @After fun cleanup() {
        context.stopService(Intent(context, SaveService::class.java))
        waitForSave { Thread.getAllStackTraces().keys.none { it.name == "TapSave-transfer" && it.isAlive } }
        SaveUiState.current?.uri?.let { context.contentResolver.delete(it, null, null) }
        SaveService.testFactory = null
        server?.close()
    }
    private fun share(code: String) = Intent(context, ShareActivity::class.java).setAction(Intent.ACTION_SEND).setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, "https://www.instagram.com/reel/$code/")
    private fun serve(code: String, delay: Long) {
        val bytes = instrumentation.context.assets.open("fixture.mp4").use { it.readBytes() }
        val fixture = HttpFixture { path ->
            if (path == "/media.mp4") HttpFixture.Reply("video/mp4", bytes, delayMs = delay)
            else HttpFixture.Reply("text/html", publicEmbed(code).toByteArray())
        }
        server = fixture
        SaveService.testFactory = { SavePipeline(it, fixture.http) }
    }

    @Test fun shareSavesAcrossRecreationAndRepeatDoesNotDownloadAgain() {
        serve("ServiceRecreate", 200)
        ActivityScenario.launch<ShareActivity>(share("ServiceRecreate")).use { scenario ->
            waitForSave { SaveUiState.current?.phase == SavePhase.DOWNLOADING }
            scenario.recreate()
        }
        // Closing the share screen must leave the user-started foreground transfer running.
        waitForSave { SaveUiState.current?.phase == SavePhase.SAVED }
        val first = SaveUiState.current!!.uri
        val firstState = SaveUiState.current
        waitForSave { Thread.getAllStackTraces().keys.none { it.name == "TapSave-transfer" && it.isAlive } }
        ActivityScenario.launch<ShareActivity>(share("ServiceRecreate")).use {
            waitForSave { SaveUiState.current !== firstState && SaveUiState.current?.phase == SavePhase.SAVED }
            assertEquals(first, SaveUiState.current!!.uri)
            assertEquals(1, server!!.paths.count { it == "/media.mp4" })
        }
    }

    @Test fun notificationCancelInterruptsBlockedReadAndTerminatesWorker() {
        serve("ServiceCancel", 10_000)
        ActivityScenario.launch<ShareActivity>(share("ServiceCancel")).use {
            waitForSave { SaveUiState.current?.bytes?.let { bytes -> bytes > 0 } == true }
            val action = notifications.activeNotifications.single { it.id == SaveService.NOTIFICATION }.notification.actions.single()
            val start = SystemClock.elapsedRealtime()
            action.actionIntent.send()
            waitForSave(5_000) { SaveUiState.current?.phase == SavePhase.CANCELLED }
            assertTrue(SystemClock.elapsedRealtime() - start < 5_000)
            waitForSave { Thread.getAllStackTraces().keys.none { it.name == "TapSave-transfer" && it.isAlive } }
            assertEquals(SavePhase.CANCELLED, SaveJournal(context).read()!!.phase)
        }
    }

    @Test fun aSecondShareCannotQueueOrReplaceAnActiveSave() {
        serve("FirstActive", 500)
        ActivityScenario.launch<ShareActivity>(share("FirstActive")).use {
            waitForSave { SaveUiState.current?.phase == SavePhase.DOWNLOADING }
            ActivityScenario.launch<ShareActivity>(share("SecondActive")).use {
                assertEquals("instagram:reel:FirstActive", SaveUiState.current!!.target.key)
                waitForSave { SaveUiState.current?.phase == SavePhase.SAVED }
                assertFalse(server!!.paths.any { it.contains("SecondActive") })
            }
        }
    }
}
