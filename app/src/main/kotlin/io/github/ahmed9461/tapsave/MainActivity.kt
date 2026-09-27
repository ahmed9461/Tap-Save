package io.github.ahmed9461.tapsave

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.ComponentName
import androidx.activity.result.contract.ActivityResultContracts
import io.github.ahmed9461.tapsave.platform.instagram.InstagramAccessibilityService
import io.github.ahmed9461.tapsave.session.*
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import io.github.ahmed9461.tapsave.overlay.OverlayService
import io.github.ahmed9461.tapsave.ui.SpikeScreen

class MainActivity : ComponentActivity() {
    private var sessionEnabled by mutableStateOf(false)
    private val login = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) InstagramSession.enable(this, true)
        sessionEnabled = InstagramSession.enabled(this)
    }
    private var canStart by mutableStateOf(false)
    private var message by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpikeScreen {
                Text(stringResource(R.string.spike_status))
                Text(stringResource(R.string.overlay_experiment))
                Button(onClick = {
                    try { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                    catch (_: ActivityNotFoundException) { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                }) { Text("Enable Instagram adapter (optional)") }
                Text(if (sessionEnabled) "Instagram session enabled • used only after public resolution fails" else "Public resolution first • Instagram session disconnected")
                Button(onClick = { login.launch(Intent(this@MainActivity, InstagramLoginActivity::class.java)) }) { Text("Connect Instagram (optional)") }
                Button(onClick = {
                    InstagramSession.enable(this@MainActivity, false)
                    sessionEnabled = false
                    if (io.github.ahmed9461.tapsave.download.SaveUiState.current?.active == true) startService(Intent(this@MainActivity, io.github.ahmed9461.tapsave.download.SaveService::class.java).setAction(io.github.ahmed9461.tapsave.download.SaveService.CANCEL))
                    Thread({
                        val cleared = InstagramSession.clear(this@MainActivity)
                        runOnUiThread { message = if (cleared) R.string.session_cleared else R.string.session_clear_failed }
                    }, "TapSave-session-clear").start()
                }) { Text("Disconnect / Clear Instagram session") }
                Button(onClick = { openSettings(Settings.ACTION_MANAGE_OVERLAY_PERMISSION) }) {
                    Text(stringResource(R.string.overlay_permission))
                }
                Button(onClick = { openSettings(Settings.ACTION_USAGE_ACCESS_SETTINGS) }) {
                    Text(stringResource(R.string.usage_permission))
                }
                Button(onClick = { openNotificationSettings() }) { Text(stringResource(R.string.notification_permission)) }
                Button(enabled = canStart, onClick = {
                    try {
                        startForegroundService(Intent(this@MainActivity, OverlayService::class.java))
                        message = R.string.overlay_started
                    } catch (_: IllegalStateException) {
                        message = R.string.overlay_start_failed
                    } catch (_: SecurityException) {
                        message = R.string.overlay_start_failed
                    }
                }) { Text(stringResource(R.string.start_overlay)) }
                Button(onClick = {
                    stopService(Intent(this@MainActivity, OverlayService::class.java))
                    message = R.string.overlay_stopped
                }) { Text(stringResource(R.string.stop_overlay)) }
                message?.let { Text(stringResource(it)) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        canStart = OverlayService.canStart(this)
        sessionEnabled = InstagramSession.enabled(this)
    }

    private fun openSettings(action: String) {
        try {
            startActivity(Intent(action, "package:$packageName".toUri()))
        } catch (_: ActivityNotFoundException) {
            message = R.string.settings_unavailable
        }
    }

    private fun openNotificationSettings() {
        try {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        } catch (_: ActivityNotFoundException) {
            message = R.string.settings_unavailable
        }
    }
}
