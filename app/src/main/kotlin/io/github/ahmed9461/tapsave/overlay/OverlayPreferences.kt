package io.github.ahmed9461.tapsave.overlay

import android.content.Context
import androidx.core.content.edit

class OverlayPreferences(context: Context) {
    val storage = context.getSharedPreferences("overlay", Context.MODE_PRIVATE)
    var enabled: Boolean
        get() = storage.getBoolean("enabled", true)
        set(value) { storage.edit { putBoolean("enabled", value) } }
    var sizeDp: Int
        get() = storage.getInt("size", 56).coerceIn(48, 72)
        set(value) { storage.edit { putInt("size", value.coerceIn(48, 72)) } }
    var opacity: Float
        get() = storage.getFloat("opacity", .95f).coerceIn(.4f, 1f)
        set(value) { storage.edit { putFloat("opacity", value.coerceIn(.4f, 1f)) } }
    fun resetPosition() { storage.edit { remove("x"); remove("y"); putLong("position-reset", System.nanoTime()) } }
}
