package io.github.ahmed9461.tapsave

import android.content.pm.ApplicationInfo
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import io.github.ahmed9461.tapsave.storage.MediaStoreVideoWriter
import org.junit.Assert.*
import org.junit.Test

/** Invoked twice around adb install -r on a disposable emulator, never on an owner device. */
@ManualGate
class ReleaseUpgradeTest {
    @Test fun retainedSettingsAndOwnedMediaAcrossSignedUpdate() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val prefs = context.getSharedPreferences("release-upgrade-proof", 0)
        val overlay = context.getSharedPreferences("overlay", 0)
        val setup = context.getSharedPreferences("setup", 0)
        val bytes = instrumentation.context.assets.open("fixture.mp4").use { it.readBytes() }
        val phase = InstrumentationRegistry.getArguments().getString("upgradePhase")
        assertEquals(0, context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)
        val version = context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
        assertEquals(InstrumentationRegistry.getArguments().getString("expectedVersion")!!.toLong(), version)
        if (phase == "seed") {
            val uri = MediaStoreVideoWriter(context).write("TapSave_upgrade_test.mp4", bytes.inputStream(), bytes.size.toLong())
            assertTrue(prefs.edit().putString("uri", uri.toString()).putString("sentinel", "retained").commit())
            assertTrue(overlay.edit().putInt("size", 64).putInt("x", 73).putFloat("opacity", .6f).commit())
            assertTrue(setup.edit().putBoolean("seen", true).commit())
        } else {
            assertEquals("verify", phase)
            assertEquals("retained", prefs.getString("sentinel", null))
            assertEquals(64, overlay.getInt("size", 0))
            assertEquals(73, overlay.getInt("x", 0))
            assertEquals(.6f, overlay.getFloat("opacity", 0f), .001f)
            assertTrue(setup.getBoolean("seen", false))
            val uri = Uri.parse(requireNotNull(prefs.getString("uri", null)))
            assertArrayEquals(bytes, context.contentResolver.openInputStream(uri)!!.use { it.readBytes() })
            assertEquals(1, context.contentResolver.delete(uri, null, null))
            assertTrue(prefs.edit().clear().commit())
        }
    }
}
