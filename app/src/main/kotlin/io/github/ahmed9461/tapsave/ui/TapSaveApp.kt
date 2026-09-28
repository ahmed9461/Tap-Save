package io.github.ahmed9461.tapsave.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import io.github.ahmed9461.tapsave.R
import io.github.ahmed9461.tapsave.download.*
import io.github.ahmed9461.tapsave.overlay.*
import io.github.ahmed9461.tapsave.platform.instagram.AcquisitionDiagnostics

enum class SetupPermission(val title: String, val explanation: String) {
    FLOATING("Floating button", "Allow Tap Save to display a small save button over Instagram."),
    CONTEXT("Instagram visibility", "Allow Usage Access so the button appears while you browse Instagram and hides when you leave."),
    NOTIFICATIONS("Save notifications", "Keep progress, cancellation and the off switch available in your notifications."),
    REEL_LINK("One-tap saving", "Enable Tap Save • Instagram only in Accessibility settings. After you tap, it selects Instagram’s Share and Copy link controls to find the current Reel."),
}

val TapMint = Color(0xFF35E6A7)
private val Ink = Color(0xFF0B1018)
@Composable fun TapSaveTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = TapMint, onPrimary = Ink, background = Ink,
        surface = Ink, surfaceContainer = Color(0xFF151C27), onSurface = Color(0xFFF5F7FA), onSurfaceVariant = Color(0xFF9099A8)), content = content)
}

@Composable
fun TapSaveApp(
    permissions: Set<SetupPermission>, sessionEnabled: Boolean, clearing: Boolean, message: String?,
    onPermission: (SetupPermission) -> Unit, onActive: (Boolean) -> Unit,
    onConnect: () -> Unit, onDisconnect: () -> Unit, onOpenVideo: (Uri) -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember { OverlayPreferences(context) }
    val setup = remember { context.getSharedPreferences("setup", android.content.Context.MODE_PRIVATE) }
    var page by rememberSaveable { mutableStateOf(if (setup.getBoolean("seen", false)) "home" else "setup") }
    var size by remember { mutableIntStateOf(preferences.sizeDp) }
    var opacity by remember { mutableFloatStateOf(preferences.opacity) }
    var floating by remember { mutableStateOf(preferences.enabled) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    fun diagnostics(): String {
        val state = SaveUiState.current ?: SaveJournal(context).read()
        return AcquisitionDiagnostics.read(context) + "\n\nLatest download: " + listOfNotNull(state?.phase?.name, state?.failure?.name, state?.diagnostic).joinToString(" · ")
    }
    var report by remember { mutableStateOf(diagnostics()) }
    val missing = SetupPermission.entries.filterNot { it in permissions }
    val pageScroll = key(page) { rememberScrollState() }
    val saved = SaveUiState.current ?: remember { SaveJournal(context).read()?.takeUnless { it.active } }
    fun home() { setup.edit { putBoolean("seen", true) }; page = "home" }
    BackHandler(page != "home") { home() }
    TapSaveTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding().padding(horizontal = 24.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (page != "home") IconButton(onClick = { home() }) { Icon(painterResource(R.drawable.ic_back), "Back") }
                    Column(Modifier.weight(1f)) {
                        Text(if (page == "settings") "Settings" else "Tap Save", style = MaterialTheme.typography.headlineMedium)
                        if (page == "home") Text("Tap. Save. Done.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (page == "home") IconButton(onClick = { page = "settings" }) { Icon(painterResource(R.drawable.ic_settings), "Settings") }
                }
                Column(Modifier.weight(1f).verticalScroll(pageScroll), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    when (page) {
                        "home" -> {
                            Panel {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    SaveMark()
                                    Column(Modifier.weight(1f)) {
                                        Text(if (OverlayService.running) "Tap Save active" else "Tap Save inactive", style = MaterialTheme.typography.titleLarge)
                                        Text(if (OverlayService.running) "Ready in Instagram" else "Ready when you are", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(checked = OverlayService.running, onCheckedChange = {
                                        if (it && missing.isNotEmpty()) page = "setup"
                                        else { floating = true; onActive(it) }
                                    }, modifier = Modifier.testTag("master-toggle"))
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(if (missing.isEmpty()) "Setup complete" else "${missing.size} setup steps remaining", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (missing.isNotEmpty()) TextButton(onClick = { page = "setup" }) { Text("Continue setup") }
                            }
                            if (OverlayService.lastAcquisitionError != null) Panel {
                                Text("Couldn’t read this Reel", style = MaterialTheme.typography.titleMedium)
                                Text("Try the floating button again, or use Instagram Share → Tap Save.")
                            }
                            if (saved != null) Panel {
                                Text("Latest save", style = MaterialTheme.typography.labelLarge, color = TapMint)
                                Text(saveMessage(context, saved), style = MaterialTheme.typography.titleMedium)
                                if (saved.active) {
                                    if (saved.total != null && saved.total > 0) LinearProgressIndicator(progress = { (saved.bytes.toFloat() / saved.total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                                    else LinearProgressIndicator(Modifier.fillMaxWidth())
                                }
                                if (saved.phase == SavePhase.SAVED && saved.uri != null) TextButton(onClick = { onOpenVideo(saved.uri) }) { Text("Open video") }
                                if (saved.failure == SaveFailure.Reason.AUTH_REQUIRED && !sessionEnabled) TextButton(onClick = onConnect) { Text("Connect Instagram") }
                            }
                            if (saved == null && OverlayService.lastAcquisitionError == null) {
                                Text("Keep browsing. Tap the floating button when a Reel is worth saving.", style = MaterialTheme.typography.bodyLarge)
                                Text("Share → Tap Save is always available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        "setup" -> {
                            SaveMark()
                            Text(if (missing.isEmpty()) "You’re ready" else "Make saving one tap", style = MaterialTheme.typography.headlineSmall)
                            Text("A few Android permissions keep Tap Save available while you browse.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LinearProgressIndicator(progress = { permissions.size / SetupPermission.entries.size.toFloat() }, modifier = Modifier.fillMaxWidth())
                            val next = missing.firstOrNull()
                            Panel {
                                Text(if (next == null) "Setup complete" else "Step ${SetupPermission.entries.size - missing.size + 1} of ${SetupPermission.entries.size}", color = TapMint, style = MaterialTheme.typography.labelLarge)
                                Text(next?.title ?: "Open Instagram and tap ↓", style = MaterialTheme.typography.titleLarge)
                                Text(next?.explanation ?: "Your save button is ready. You can change its size and opacity in Settings.")
                                Button(onClick = { if (next != null) onPermission(next) else { floating = true; onActive(true); home() } }, modifier = Modifier.fillMaxWidth()) { Text(if (next != null) "Open Android settings" else "Activate Tap Save") }
                            }
                        }
                        "settings" -> {
                            Panel {
                                Label("Floating button")
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Show floating button", Modifier.weight(1f))
                                    Switch(floating, { floating = it; preferences.enabled = it; if (!it) onActive(false) }, modifier = Modifier.testTag("floating-toggle"))
                                }
                                Text("Size · $size dp")
                                Slider(size.toFloat(), { size = it.toInt() }, onValueChangeFinished = { preferences.sizeDp = size }, valueRange = 48f..72f, steps = 2, modifier = Modifier.testTag("button-size"))
                                Text("Opacity · ${(opacity * 100).toInt()}%")
                                Slider(opacity, { opacity = it }, onValueChangeFinished = { preferences.opacity = opacity }, valueRange = .4f..1f, modifier = Modifier.testTag("button-opacity"))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    SaveMark(size, opacity)
                                    TextButton(onClick = { preferences.resetPosition() }) { Text("Reset position") }
                                }
                            }
                            Panel {
                                Label("Downloads")
                                Text("Best available video quality", style = MaterialTheme.typography.titleMedium)
                                Text("Highest available combined video and audio. Original file, no recompression.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                HorizontalDivider()
                                Text("Save location", style = MaterialTheme.typography.titleMedium)
                                Text("Movies / Tap Save", color = TapMint)
                                Text("Videos appear in your gallery. No storage permission is needed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Panel {
                                Label("Instagram")
                                Text(if (sessionEnabled) "Connected" else "Not connected", style = MaterialTheme.typography.titleMedium)
                                Text("Optional. Tap Save tries public videos first; connect if Instagram requires sign-in.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (!sessionEnabled) OutlinedButton(onClick = onConnect) { Text("Connect Instagram") }
                                TextButton(onClick = onDisconnect, enabled = !clearing) { Text(if (clearing) "Clearing…" else "Disconnect and clear session") }
                            }
                            Panel {
                                Label("Required permissions")
                                SetupPermission.entries.forEach { permission ->
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) { Text(permission.title); Text(if (permission in permissions) "Allowed" else "Not allowed", color = if (permission in permissions) TapMint else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                                        TextButton(onClick = { onPermission(permission) }) { Text("Manage") }
                                    }
                                }
                            }
                            TextButton(onClick = { advanced = !advanced; report = diagnostics() }) { Text(if (advanced) "Hide Advanced" else "Advanced") }
                            if (advanced) Panel {
                                Label("Acquisition diagnostics")
                                Text("Last tap only. Control IDs, supported actions, timing and clipboard freshness. Screen text and link contents are excluded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                SelectionContainer { Text(report, style = MaterialTheme.typography.bodySmall) }
                                Row {
                                    TextButton(onClick = { context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Tap Save diagnostics", report)) }) { Text("Copy report") }
                                    TextButton(onClick = { AcquisitionDiagnostics.clear(context); report = AcquisitionDiagnostics.read(context) }) { Text("Clear") }
                                }
                            }
                        }
                    }
                    message?.let { Text(it, color = TapMint) }
                    Spacer(Modifier.height(12.dp))
                }
                if (page == "setup") TextButton(onClick = { home() }, modifier = Modifier.fillMaxWidth()) { Text("Use Share for now") }
            }
        }
    }
}

@Composable private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable private fun Label(text: String) { Text(text, style = MaterialTheme.typography.labelLarge, color = TapMint) }
@Composable private fun SaveMark(size: Int = 56, opacity: Float = 1f) {
    Box(Modifier.size(size.dp).background(TapMint.copy(alpha = opacity), CircleShape), contentAlignment = Alignment.Center) {
        Icon(painterResource(R.drawable.ic_download), contentDescription = null, tint = Ink, modifier = Modifier.size(28.dp))
    }
}
