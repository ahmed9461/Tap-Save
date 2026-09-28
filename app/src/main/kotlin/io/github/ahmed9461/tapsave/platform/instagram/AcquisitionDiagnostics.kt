package io.github.ahmed9461.tapsave.platform.instagram

import io.github.ahmed9461.tapsave.R
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.content.edit
import java.util.Locale

/** Last user-requested acquisition only. Never records screen text, URLs, or clipboard contents. */
class AcquisitionDiagnostics(private val context: Context) {
    private val started = SystemClock.elapsedRealtime()
    private val lines = ArrayDeque<String>()
    init {
        val version = runCatching { context.packageManager.getPackageInfo(InstagramApp.PACKAGE_NAME, 0).versionName }.getOrNull() ?: "unknown"
        add("BEGIN sdk=${Build.VERSION.SDK_INT} device=${Build.MODEL} instagram=$version locale=${Locale.getDefault().toLanguageTag()}")
    }
    fun add(message: String) {
        val line = "${SystemClock.elapsedRealtime() - started}ms $message"
        lines.addLast(line.take(600))
        while (lines.size > 100) lines.removeFirst()
        Log.i("TapSaveAcquire", line.take(600))
    }
    fun flush() { context.getSharedPreferences("acquisition-diagnostics", Context.MODE_PRIVATE).edit { putString("last", lines.joinToString("\n").takeLast(32_000)) } }
    companion object {
        fun read(context: Context): String = context.getSharedPreferences("acquisition-diagnostics", Context.MODE_PRIVATE).getString("last", null) ?: context.getString(R.string.no_acquisition)
        fun clear(context: Context) { context.getSharedPreferences("acquisition-diagnostics", Context.MODE_PRIVATE).edit { clear() } }
    }
}
