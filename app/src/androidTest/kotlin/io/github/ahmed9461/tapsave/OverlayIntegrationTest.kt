package io.github.ahmed9461.tapsave

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.github.ahmed9461.tapsave.overlay.OverlayService
import io.github.ahmed9461.tapsave.overlay.OverlayWindow
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val notifications = context.getSystemService(NotificationManager::class.java)

    private fun appOp(op: String, mode: String) {
        ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("appops set ${context.packageName} $op $mode"),
        ).use { it.readBytes() }
    }

    private fun grantSessionPermissions() {
        appOp("SYSTEM_ALERT_WINDOW", "allow")
        appOp("GET_USAGE_STATS", "allow")
        if (Build.VERSION.SDK_INT >= 33) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @After fun cleanup() {
        context.stopService(Intent(context, OverlayService::class.java))
        waitUntil { notifications.activeNotifications.none { it.id == 1 } && Thread.getAllStackTraces().keys.none { it.name == "TapSave-context" && it.isAlive } }
        appOp("SYSTEM_ALERT_WINDOW", "default")
        appOp("GET_USAGE_STATS", "default")
    }

    @Test fun missingPermissionsLeaveOverlayDisabled() {
        appOp("SYSTEM_ALERT_WINDOW", "deny")
        appOp("GET_USAGE_STATS", "deny")
        assertFalse(OverlayService.canStart(context))
    }

    @Test fun nativeWindowFromNonActivityContextAttachDetachIsIdempotent() {
        grantSessionPermissions()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val window = OverlayWindow(activity.applicationContext, onFailure = { throw AssertionError("Window update failed") })
                try {
                    window.show()
                    window.show()
                    assertTrue(window.isAttached)
                    window.reposition()
                } finally {
                    window.hide()
                    window.hide()
                }
                assertFalse(window.isAttached)
            }
        }
    }

    @Test fun foregroundSessionStopsAfterUsagePermissionRevocation() {
        grantSessionPermissions()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { it.startForegroundService(Intent(it, OverlayService::class.java)) }
            waitUntil { notifications.activeNotifications.any { it.id == 1 && it.notification.actions?.isNotEmpty() == true } }
            appOp("GET_USAGE_STATS", "deny")
            waitUntil { notifications.activeNotifications.none { it.id == 1 } }
        }
    }

    @Test fun foregroundSessionStopsExplicitly() {
        grantSessionPermissions()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { it.startForegroundService(Intent(it, OverlayService::class.java)) }
            waitUntil { notifications.activeNotifications.any { it.id == 1 && it.notification.actions?.isNotEmpty() == true } }
            context.stopService(Intent(context, OverlayService::class.java))
            waitUntil { notifications.activeNotifications.none { it.id == 1 } }
        }
    }

    @Test fun permissionRevokedBeforeServiceStartupDoesNotCrashTheProcess() {
        grantSessionPermissions()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {
                it.startForegroundService(Intent(it, OverlayService::class.java))
                // Keep the app main thread occupied until the permission is revoked,
                // so service startup observes the changed prerequisite.
                appOp("GET_USAGE_STATS", "deny")
            }
            SystemClock.sleep(10_000) // Observe the Android foreground-start deadline, not just an early empty notification list.
            waitUntil { notifications.activeNotifications.none { it.id == 1 } && Thread.getAllStackTraces().keys.none { it.name == "TapSave-context" && it.isAlive } }
        }
    }

    @Test fun notificationStopActionAlsoTerminatesWorkerThread() {
        grantSessionPermissions()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { it.startForegroundService(Intent(it, OverlayService::class.java)) }
            waitUntil { notifications.activeNotifications.any { it.id == 1 && it.notification.actions?.isNotEmpty() == true } }
            val notification = notifications.activeNotifications.single { it.id == 1 }.notification
            notification.actions.single().actionIntent.send()
            waitUntil { notifications.activeNotifications.none { it.id == 1 } }
            waitUntil { Thread.getAllStackTraces().keys.none { it.name == "TapSave-context" && it.isAlive } }
        }
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 10_000
        while (!condition() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(100)
        assertTrue("Session did not reach the expected state", condition())
    }
}
