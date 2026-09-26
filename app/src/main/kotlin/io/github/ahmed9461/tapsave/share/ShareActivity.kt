package io.github.ahmed9461.tapsave.share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.github.ahmed9461.tapsave.R
import io.github.ahmed9461.tapsave.platform.ShareResult
import io.github.ahmed9461.tapsave.ui.SpikeScreen
import io.github.ahmed9461.tapsave.download.SaveFailure
import io.github.ahmed9461.tapsave.download.SaveJournal
import io.github.ahmed9461.tapsave.download.SavePhase
import io.github.ahmed9461.tapsave.download.SaveService
import io.github.ahmed9461.tapsave.download.SaveState
import io.github.ahmed9461.tapsave.download.SaveUiState
import io.github.ahmed9461.tapsave.download.saveMessage
import android.content.ActivityNotFoundException

class ShareActivity : ComponentActivity() {
    private var result by mutableStateOf<ShareResult>(ShareResult.Invalid)
    private var startOnResume = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        result = readShareIntent(intent)
        if (SaveUiState.current == null) {
            SaveUiState.current = SaveJournal(this).read()?.let {
                if (it.active) it.copy(phase = SavePhase.FAILED, failure = SaveFailure.Reason.INTERRUPTED) else it
            }
        }
        if (intent.action == SaveService.VIEW) result = SaveUiState.current?.target?.let(ShareResult::Target) ?: ShareResult.Invalid
        startOnResume = savedInstanceState == null && intent.action == Intent.ACTION_SEND && result is ShareResult.Target
        enableEdgeToEdge()
        setContent {
            SpikeScreen {
                when (val current = result) {
                    is ShareResult.Target -> {
                        Text(stringResource(R.string.link_received))
                        Text(current.target.canonicalUrl)
                        val status = SaveUiState.current
                        if (status?.target?.key == current.target.key) {
                            Text(saveMessage(this@ShareActivity, status))
                            if (status.active) {
                                val total = status.total
                                if (total != null) LinearProgressIndicator(progress = { (status.bytes.toFloat() / total).coerceIn(0f, 1f) })
                                else LinearProgressIndicator()
                                if (status.bytes > 0) Text(stringResource(R.string.downloaded_bytes, status.bytes / 1024))
                                Button(onClick = { startService(Intent(this@ShareActivity, SaveService::class.java).setAction(SaveService.CANCEL)) }) {
                                    Text(stringResource(R.string.cancel_save))
                                }
                            } else if (status.phase == SavePhase.SAVED) {
                                Button(onClick = {
                                    try { startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(status.uri, "video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                                    catch (_: ActivityNotFoundException) { /* Saved file remains in Movies/Tap Save. */ }
                                }) { Text(stringResource(R.string.open_video)) }
                            } else Button(onClick = { startSave() }) { Text(stringResource(R.string.retry_save)) }
                        } else if (status?.active == true) Text(stringResource(R.string.save_busy))
                        else Button(onClick = { startSave() }) { Text(stringResource(R.string.save_reel)) }
                    }
                    is ShareResult.RedirectLink -> {
                        Text(stringResource(R.string.short_link_received))
                        Text(current.canonicalUrl)
                    }
                    ShareResult.Ambiguous -> Text(stringResource(R.string.share_one_reel))
                    ShareResult.Invalid -> Text(stringResource(R.string.invalid_share))
                }
                Button(onClick = { finish() }) { Text(stringResource(R.string.done)) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        result = readShareIntent(intent)
        if (intent.action == SaveService.VIEW) result = SaveUiState.current?.target?.let(ShareResult::Target) ?: ShareResult.Invalid
        startOnResume = intent.action == Intent.ACTION_SEND && result is ShareResult.Target
    }

    override fun onPostResume() {
        super.onPostResume()
        if (startOnResume) { startOnResume = false; startSave() }
    }

    private fun startSave() {
        val target = (result as? ShareResult.Target)?.target ?: return
        if (SaveUiState.current?.active == true) return
        try { startForegroundService(Intent(this, SaveService::class.java).putExtra(SaveService.TARGET, target.canonicalUrl)) }
        catch (_: RuntimeException) { SaveUiState.current = SaveState(target, SavePhase.FAILED, failure = SaveFailure.Reason.INTERRUPTED) }
    }
}
