package io.github.ahmed9461.tapsave

import android.content.ActivityNotFoundException
import android.content.Intent
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
    private var canStart by mutableStateOf(false)
    private var message by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpikeScreen {
                Text(stringResource(R.string.spike_status))
                Text(stringResource(R.string.overlay_experiment))
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
