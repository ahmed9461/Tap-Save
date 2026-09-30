package io.github.ahmed9461.tapsave.download

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.core.content.edit

/** One explicit save's structural trace. Callers supply fixed stages/counts, never URLs or bodies. */
class DownloadDiagnostics(private val context: Context) {
    private val started = SystemClock.elapsedRealtime()
    private val lines = ArrayDeque<String>()
    init { add("BEGIN") }
    fun add(message: String) {
        val line = "${SystemClock.elapsedRealtime() - started}ms $message"
        lines.addLast(line.take(400))
        while (lines.size > 48) lines.removeFirst()
        Log.i("TapSaveDownload", line.take(400))
        context.getSharedPreferences("download-diagnostics", Context.MODE_PRIVATE).edit {
            putString("last", lines.joinToString("\n"))
        }
    }
    companion object {
        fun read(context: Context): String = context.getSharedPreferences("download-diagnostics", Context.MODE_PRIVATE).getString("last", "").orEmpty()
        fun clear(context: Context) { context.getSharedPreferences("download-diagnostics", Context.MODE_PRIVATE).edit { clear() } }
    }
}
