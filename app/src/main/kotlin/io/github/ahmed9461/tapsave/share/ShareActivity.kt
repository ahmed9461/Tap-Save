package io.github.ahmed9461.tapsave.share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.github.ahmed9461.tapsave.R
import io.github.ahmed9461.tapsave.platform.ShareResult
import io.github.ahmed9461.tapsave.ui.SpikeScreen

class ShareActivity : ComponentActivity() {
    private var result by mutableStateOf<ShareResult>(ShareResult.Invalid)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        result = readShareIntent(intent)
        enableEdgeToEdge()
        setContent {
            SpikeScreen {
                when (val current = result) {
                    is ShareResult.Target -> {
                        Text(stringResource(R.string.link_received))
                        Text(current.target.canonicalUrl)
                        Text(stringResource(R.string.spike_status))
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
    }
}
