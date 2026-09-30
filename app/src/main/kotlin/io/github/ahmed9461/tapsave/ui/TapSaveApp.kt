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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import io.github.ahmed9461.tapsave.R
import io.github.ahmed9461.tapsave.download.*
import io.github.ahmed9461.tapsave.overlay.*
import io.github.ahmed9461.tapsave.platform.instagram.AcquisitionDiagnostics

enum class SetupPermission(val title: Int, val explanation: Int) {
    FLOATING(R.string.floating_button, R.string.setup_floating),
    CONTEXT(R.string.instagram_visibility, R.string.setup_context),
    NOTIFICATIONS(R.string.save_notifications, R.string.setup_notifications),
    REEL_LINK(R.string.one_tap_saving, R.string.setup_reel_link),
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
    onLanguage: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val preferences = remember { OverlayPreferences(context) }
    val setup = remember { context.getSharedPreferences("setup", android.content.Context.MODE_PRIVATE) }
    var page by rememberSaveable { mutableStateOf(if (setup.getBoolean("seen", false)) "home" else "setup") }
    var size by remember { mutableIntStateOf(preferences.sizeDp) }
    var opacity by remember { mutableFloatStateOf(preferences.opacity) }
    var floating by remember { mutableStateOf(preferences.enabled) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    fun diagnostics(): String {
        val state = SaveUiState.current ?: SaveJournal(context).read()
        return AcquisitionDiagnostics.read(context) + "\n\n" + resources.getString(R.string.latest_download_diagnostic, listOfNotNull(state?.phase?.name, state?.failure?.name, state?.diagnostic).joinToString(" · ")) + "\n" + DownloadDiagnostics.read(context)
    }
    var report by remember(resources) { mutableStateOf(diagnostics()) }
    val missing = SetupPermission.entries.filterNot { it in permissions }
    val pageScroll = key(page) { rememberScrollState() }
    val saved = SaveUiState.current ?: remember { SaveJournal(context).read()?.takeUnless { it.active } }
    fun home() { setup.edit { putBoolean("seen", true) }; page = "home" }
    BackHandler(page != "home") { home() }
    TapSaveTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding().padding(horizontal = 24.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (page != "home") IconButton(onClick = { home() }) { Icon(painterResource(R.drawable.ic_back), stringResource(R.string.back)) }
                    Column(Modifier.weight(1f)) {
                        Text(if (page == "settings") stringResource(R.string.settings) else stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
                        if (page == "home") Text(stringResource(R.string.tagline), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (page == "home") IconButton(onClick = { page = "settings" }) { Icon(painterResource(R.drawable.ic_settings), stringResource(R.string.settings)) }
                }
                Column(Modifier.weight(1f).verticalScroll(pageScroll), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    when (page) {
                        "home" -> {
                            Panel {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    SaveMark()
                                    Column(Modifier.weight(1f)) {
                                        Text(if (OverlayService.running) stringResource(R.string.active) else stringResource(R.string.inactive), style = MaterialTheme.typography.titleLarge)
                                        Text(if (OverlayService.running) stringResource(R.string.ready_instagram) else stringResource(R.string.ready_when_you_are), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(checked = OverlayService.running, onCheckedChange = {
                                        if (it && missing.isNotEmpty()) page = "setup"
                                        else { floating = true; onActive(it) }
                                    }, modifier = Modifier.testTag("master-toggle"))
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(if (missing.isEmpty()) stringResource(R.string.setup_complete) else pluralStringResource(R.plurals.setup_remaining, missing.size, missing.size), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (missing.isNotEmpty()) TextButton(onClick = { page = "setup" }) { Text(stringResource(R.string.continue_setup)) }
                            }
                            if (OverlayService.lastAcquisitionError != null) Panel {
                                Text(stringResource(R.string.acquisition_failed), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.acquisition_retry))
                            }
                            if (saved != null) Panel {
                                Text(stringResource(R.string.latest_save), style = MaterialTheme.typography.labelLarge, color = TapMint)
                                Text(saveMessage(context, saved), style = MaterialTheme.typography.titleMedium)
                                if (saved.active) {
                                    if (saved.total != null && saved.total > 0) LinearProgressIndicator(progress = { (saved.bytes.toFloat() / saved.total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                                    else LinearProgressIndicator(Modifier.fillMaxWidth())
                                }
                                if (saved.phase == SavePhase.SAVED && saved.uri != null) TextButton(onClick = { onOpenVideo(saved.uri) }) { Text(stringResource(R.string.open_video)) }
                                if (saved.failure == SaveFailure.Reason.AUTH_REQUIRED && !sessionEnabled) TextButton(onClick = onConnect) { Text(stringResource(R.string.connect_instagram)) }
                            }
                            if (saved == null && OverlayService.lastAcquisitionError == null) {
                                Text(stringResource(R.string.home_hint), style = MaterialTheme.typography.bodyLarge)
                                Text(stringResource(R.string.share_always), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        "setup" -> {
                            SaveMark()
                            Text(if (missing.isEmpty()) stringResource(R.string.you_are_ready) else stringResource(R.string.make_one_tap), style = MaterialTheme.typography.headlineSmall)
                            Text(stringResource(R.string.setup_intro), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LinearProgressIndicator(progress = { permissions.size / SetupPermission.entries.size.toFloat() }, modifier = Modifier.fillMaxWidth())
                            val next = missing.firstOrNull()
                            Panel {
                                Text(if (next == null) stringResource(R.string.setup_complete) else stringResource(R.string.setup_step, SetupPermission.entries.size - missing.size + 1, SetupPermission.entries.size), color = TapMint, style = MaterialTheme.typography.labelLarge)
                                Text(next?.let { stringResource(it.title) } ?: stringResource(R.string.open_instagram), style = MaterialTheme.typography.titleLarge)
                                Text(next?.let { stringResource(it.explanation) } ?: stringResource(R.string.setup_ready))
                                Button(onClick = { if (next != null) onPermission(next) else { floating = true; onActive(true); home() } }, modifier = Modifier.fillMaxWidth()) { Text(if (next != null) stringResource(R.string.open_android_settings) else stringResource(R.string.activate)) }
                            }
                        }
                        "settings" -> {
                            Panel {
                                Label(stringResource(R.string.language))
                                Text(stringResource(R.string.language_description))
                                if (onLanguage != null) TextButton(onClick = onLanguage) { Text(stringResource(R.string.choose_language)) }
                            }
                            Panel {
                                Label(stringResource(R.string.floating_button))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.show_floating_button), Modifier.weight(1f))
                                    Switch(floating, { floating = it; preferences.enabled = it; if (!it) onActive(false) }, modifier = Modifier.testTag("floating-toggle"))
                                }
                                Text(stringResource(R.string.size_value, size))
                                Slider(size.toFloat(), { size = it.toInt() }, onValueChangeFinished = { preferences.sizeDp = size }, valueRange = 48f..72f, steps = 2, modifier = Modifier.testTag("button-size"))
                                Text(stringResource(R.string.opacity_value, (opacity * 100).toInt()))
                                Slider(opacity, { opacity = it }, onValueChangeFinished = { preferences.opacity = opacity }, valueRange = .4f..1f, modifier = Modifier.testTag("button-opacity"))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    SaveMark(size, opacity)
                                    TextButton(onClick = { preferences.resetPosition() }) { Text(stringResource(R.string.reset_position)) }
                                }
                            }
                            Panel {
                                Label(stringResource(R.string.downloads))
                                Text(stringResource(R.string.best_quality), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.quality_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                HorizontalDivider()
                                Text(stringResource(R.string.save_location), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.save_folder), color = TapMint)
                                Text(stringResource(R.string.gallery_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Panel {
                                Label(stringResource(R.string.instagram_name))
                                Text(if (sessionEnabled) stringResource(R.string.connected) else stringResource(R.string.not_connected), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.session_optional), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (!sessionEnabled) OutlinedButton(onClick = onConnect) { Text(stringResource(R.string.connect_instagram)) }
                                TextButton(onClick = onDisconnect, enabled = !clearing) { Text(if (clearing) stringResource(R.string.clearing) else stringResource(R.string.disconnect)) }
                            }
                            Panel {
                                Label(stringResource(R.string.required_permissions))
                                SetupPermission.entries.forEach { permission ->
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) { Text(stringResource(permission.title)); Text(if (permission in permissions) stringResource(R.string.allowed) else stringResource(R.string.not_allowed), color = if (permission in permissions) TapMint else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                                        TextButton(onClick = { onPermission(permission) }) { Text(stringResource(R.string.manage)) }
                                    }
                                }
                            }
                            TextButton(onClick = { advanced = !advanced; report = diagnostics() }) { Text(if (advanced) stringResource(R.string.hide_advanced) else stringResource(R.string.advanced)) }
                            if (advanced) Panel {
                                Label(stringResource(R.string.diagnostics))
                                Text(stringResource(R.string.diagnostics_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                SelectionContainer { Text(report, style = MaterialTheme.typography.bodySmall) }
                                Row {
                                    TextButton(onClick = { context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(resources.getString(R.string.diagnostics), report)) }) { Text(stringResource(R.string.copy_report)) }
                                    TextButton(onClick = { AcquisitionDiagnostics.clear(context); DownloadDiagnostics.clear(context); report = diagnostics() }) { Text(stringResource(R.string.clear)) }
                                }
                            }
                        }
                    }
                    message?.let { Text(it, color = TapMint) }
                    Spacer(Modifier.height(12.dp))
                }
                if (page == "setup") TextButton(onClick = { home() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.use_share)) }
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
