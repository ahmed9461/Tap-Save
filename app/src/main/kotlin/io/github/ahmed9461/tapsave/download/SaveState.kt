package io.github.ahmed9461.tapsave.download

import android.content.Context
import android.net.Uri
import android.annotation.SuppressLint
import androidx.core.net.toUri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.ahmed9461.tapsave.platform.pendingTarget
import io.github.ahmed9461.tapsave.platform.SharedTarget
import io.github.ahmed9461.tapsave.platform.instagram.InstagramShareParser
import java.io.IOException

enum class SavePhase { RESOLVING, DOWNLOADING, CANCELLING, SAVED, FAILED, CANCELLED }
data class SaveState(
    val target: SharedTarget,
    val phase: SavePhase,
    val bytes: Long = 0,
    val total: Long? = null,
    val uri: Uri? = null,
    val failure: SaveFailure.Reason? = null,
    val diagnostic: String? = null,
    val requestedUrl: String = target.canonicalUrl,
) { val active get() = phase in setOf(SavePhase.RESOLVING, SavePhase.DOWNLOADING, SavePhase.CANCELLING) }

object SaveUiState {
    private var state by mutableStateOf<SaveState?>(null)
    private val listeners = mutableSetOf<(SaveState?) -> Unit>()
    var current: SaveState?
        get() = state
        set(value) { state = value; listeners.toList().forEach { it(value) } }
    fun observe(listener: (SaveState?) -> Unit) { listeners += listener; listener(current) }
    fun remove(listener: (SaveState?) -> Unit) { listeners -= listener }
}

/** One latest job checkpoint, not a browsing history. Never persists CDN URLs or response bodies. */
class SaveJournal(context: Context) {
    private val preferences = context.getSharedPreferences("latest-save", Context.MODE_PRIVATE)
    fun read(): SaveState? {
        val target = InstagramShareParser.parse(preferences.getString("target", null)).pendingTarget() ?: return null
        val phase = runCatching { SavePhase.valueOf(preferences.getString("phase", "FAILED")!!) }.getOrDefault(SavePhase.FAILED)
        val failure = preferences.getString("failure", null)?.let { runCatching { SaveFailure.Reason.valueOf(it) }.getOrNull() }
        return SaveState(target, phase, uri = preferences.getString("uri", null)?.toUri(), failure = failure, diagnostic = preferences.getString("diagnostic", null), requestedUrl = preferences.getString("requested", null) ?: target.canonicalUrl)
    }
    @SuppressLint("UseKtx") // Must check commit's boolean result before allocating shared media.
    fun write(state: SaveState) {
        if (!preferences.edit().clear().putString("target", state.target.canonicalUrl)
                .putString("phase", state.phase.name).putString("uri", state.uri?.toString())
                .putString("requested", state.requestedUrl).putString("failure", state.failure?.name).putString("diagnostic", state.diagnostic).commit()) throw IOException("Could not checkpoint save")
    }
}
