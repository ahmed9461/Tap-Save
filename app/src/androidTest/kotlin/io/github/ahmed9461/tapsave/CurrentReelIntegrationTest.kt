package io.github.ahmed9461.tapsave

import android.Manifest
import android.os.Build
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import io.github.ahmed9461.tapsave.overlay.OverlayService
import android.app.UiAutomation
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import io.github.ahmed9461.tapsave.download.*
import io.github.ahmed9461.tapsave.platform.instagram.InstagramAccessibilityService
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** A disposable native fixture validates Android semantics, not Instagram's production node tree. */
class CurrentReelIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private lateinit var automation: UiAutomation
    private var server: HttpFixture? = null
    private fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use { it.readBytes().toString(Charsets.UTF_8) }
    @Before fun enableAdapterOnEmulator() {
        automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        assertEquals("1", shell("getprop ro.kernel.qemu").trim())
        shell("settings put secure enabled_accessibility_services ${context.packageName}/.platform.instagram.InstagramAccessibilityService")
        shell("settings put secure accessibility_enabled 1")
        waitForSave { InstagramAccessibilityService.connected != null }
        instrumentation.runOnMainSync { SaveUiState.current = null }
    }
    @After fun cleanup() {
        instrumentation.runOnMainSync { InstagramAccessibilityService.connected?.onInterrupt() }
        context.stopService(Intent(context, OverlayService::class.java))
        context.stopService(Intent(context, SaveService::class.java))
        waitForSave { Thread.getAllStackTraces().keys.none { it.name == "TapSave-transfer" && it.isAlive } }
        SaveUiState.current?.uri?.let { context.contentResolver.delete(it, null, null) }
        SaveService.testFactory = null
        server?.close()
        shell("settings delete secure enabled_accessibility_services")
        shell("settings put secure accessibility_enabled 0")
        shell("appops set ${context.packageName} SYSTEM_ALERT_WINDOW default")
        shell("appops set ${context.packageName} GET_USAGE_STATS default")
    }
    private fun launch(extra: String = "") {
        shell("am force-stop com.instagram.android")
        shell("am start -W -n com.instagram.android/.FixtureActivity $extra")
        SystemClock.sleep(500)
    }
    private fun acquire(): Result<String> {
        val done = CountDownLatch(1)
        var result: Result<String>? = null
        instrumentation.runOnMainSync {
            InstagramAccessibilityService.connected!!.acquire {
                result = it
                done.countDown()
            }
        }
        assertTrue("Acquisition timed out", done.await(10, TimeUnit.SECONDS))
        return result!!
    }
    @Test fun floatingButtonAcquiresArabicReelAndStartsRealDownloadAndMediaStoreSave() {
        val bytes = instrumentation.context.assets.open("fixture.mp4").use { it.readBytes() }
        val fixture = HttpFixture { path -> if (path == "/media.mp4") HttpFixture.Reply("video/mp4", bytes) else HttpFixture.Reply("text/html", publicEmbed("AdapterFixture").toByteArray()) }
        server = fixture; SaveService.testFactory = { SavePipeline(it, fixture.http) }
        shell("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
        shell("appops set ${context.packageName} GET_USAGE_STATS allow")
        if (Build.VERSION.SDK_INT >= 33) automation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        automation.serviceInfo = automation.serviceInfo.apply { flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { it.startForegroundService(Intent(it, OverlayService::class.java)) }
            launch("--ez arabic true")
            var button: AccessibilityNodeInfo? = null
            waitForSave {
                button = automation.windows.mapNotNull { it.root }.flatMap { it.findAccessibilityNodeInfosByText("↓") }
                    .singleOrNull { it.packageName?.toString() == context.packageName }
                button != null
            }
            assertTrue(button!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            waitForSave(30_000) { SaveUiState.current?.phase == SavePhase.SAVED }
        }
        assertEquals("https://www.instagram.com/reel/AdapterFixture/", SaveUiState.current!!.target.canonicalUrl)
        assertArrayEquals(bytes, context.contentResolver.openInputStream(SaveUiState.current!!.uri!!)!!.use { it.readBytes() })
        assertNull(InstagramAccessibilityService.pending)
    }
    @Test fun englishSemanticAcquisitionAcceptsOnlyFreshLink() {
        launch("--ez arabic false")
        assertEquals("https://www.instagram.com/reel/AdapterFixture/", acquire().getOrThrow())
        launch("--ez stale true")
        assertEquals("FRESH_REEL_LINK_MISSING", acquire().exceptionOrNull()?.message)
    }
    @Test fun ambiguityFailsWithoutGuessingAndOtherAppsAreRejected() {
        launch("--ez ambiguous true")
        assertEquals("SHARE_CONTROL_NOT_UNIQUE", acquire().exceptionOrNull()?.message)
        shell("am start -W -n ${context.packageName}/.MainActivity")
        SystemClock.sleep(300)
        assertEquals("SHARE_CONTROL_NOT_UNIQUE", acquire().exceptionOrNull()?.message)
    }
}
