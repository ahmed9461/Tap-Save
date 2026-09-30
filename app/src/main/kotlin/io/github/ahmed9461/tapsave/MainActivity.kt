package io.github.ahmed9461.tapsave

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import io.github.ahmed9461.tapsave.download.*
import io.github.ahmed9461.tapsave.overlay.*
import io.github.ahmed9461.tapsave.platform.UsageForegroundContext
import io.github.ahmed9461.tapsave.session.*
import io.github.ahmed9461.tapsave.ui.*

class MainActivity : ComponentActivity() {
    private var permissions by mutableStateOf(emptySet<SetupPermission>())
    private var sessionEnabled by mutableStateOf(false)
    private var message by mutableStateOf<String?>(null)
    private var clearing by mutableStateOf(false)
    private val login = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) InstagramSession.enable(this, true)
        refresh()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        refresh()
        setContent {
            TapSaveApp(permissions, sessionEnabled, clearing, message,
                onPermission = ::openPermission,
                onLanguage = if (android.os.Build.VERSION.SDK_INT >= 33) ({
                    try { startActivity(Intent(Settings.ACTION_APP_LOCALE_SETTINGS, "package:$packageName".toUri())) }
                    catch (_: ActivityNotFoundException) { message = getString(R.string.settings_unavailable) }
                }) else null,
                onActive = { active ->
                    message = null
                    if (active) {
                        OverlayPreferences(this).enabled = true
                        try { startForegroundService(Intent(this, OverlayService::class.java)) }
                        catch (_: RuntimeException) { message = getString(R.string.overlay_start_failed) }
                    } else stopService(Intent(this, OverlayService::class.java))
                },
                onConnect = { login.launch(Intent(this, InstagramLoginActivity::class.java)) },
                onDisconnect = {
                    InstagramSession.enable(this, false); sessionEnabled = false; clearing = true
                    if (SaveUiState.current?.active == true) startService(Intent(this, SaveService::class.java).setAction(SaveService.CANCEL))
                    Thread({
                        val cleared = InstagramSession.clear(this)
                        runOnUiThread { clearing = false; message = getString(if (cleared) R.string.session_cleared else R.string.session_clear_failed) }
                    }, "TapSave-session-clear").start()
                },
                onOpenVideo = { uri ->
                    try { startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                    catch (_: ActivityNotFoundException) { message = getString(R.string.open_gallery) }
                },
            )
        }
    }
    override fun onResume() { super.onResume(); refresh() }
    private fun refresh() {
        permissions = buildSet {
            if (Settings.canDrawOverlays(this@MainActivity)) add(SetupPermission.FLOATING)
            if (UsageForegroundContext.hasPermission(this@MainActivity)) add(SetupPermission.CONTEXT)
            if (getSystemService(NotificationManager::class.java).areNotificationsEnabled()) add(SetupPermission.NOTIFICATIONS)
            if (getSystemService(AccessibilityManager::class.java).getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                    .any { it.resolveInfo.serviceInfo.packageName == packageName }) add(SetupPermission.REEL_LINK)
        }
        sessionEnabled = InstagramSession.enabled(this)
    }
    private fun openPermission(permission: SetupPermission) {
        val intent = when (permission) {
            SetupPermission.FLOATING -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())
            SetupPermission.CONTEXT -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, "package:$packageName".toUri())
            SetupPermission.NOTIFICATIONS -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            SetupPermission.REEL_LINK -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        }
        try { startActivity(intent) } catch (_: ActivityNotFoundException) { message = getString(R.string.settings_unavailable) }
    }
}
