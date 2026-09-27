package io.github.ahmed9461.tapsave.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.Button
import androidx.core.content.edit
import io.github.ahmed9461.tapsave.R
import kotlin.math.roundToInt

/** One small, non-focusable native window. All calls are on the main thread. */
class OverlayWindow(baseContext: Context, private val onFailure: () -> Unit, private val onTap: () -> Unit = {}) {
    // A service is not a visual context. Bind resources and WindowManager to the overlay's display.
    private val context = if (Build.VERSION.SDK_INT >= 30) {
        val display = baseContext.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
        baseContext.createDisplayContext(display)
            .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
    } else baseContext
    private val manager = context.getSystemService(WindowManager::class.java)
    private val preferences = context.getSharedPreferences("overlay", Context.MODE_PRIVATE)
    private val size = (56 * context.resources.displayMetrics.density).roundToInt()
    private var button: Button? = null
    private var glyph = "↓"
    private var description = "Save current Reel"
    fun render(text: String, label: String) { glyph = text; description = label; button?.apply { this.text = glyph; contentDescription = description } }
    @SuppressLint("RtlHardcoded") // Drag coordinates and saved positions are physical screen coordinates.
    private val params = WindowManager.LayoutParams(
        size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.LEFT
        x = preferences.getInt("x", 0)
        y = preferences.getInt("y", size * 3)
    }

    val isAttached: Boolean get() = button != null

    @SuppressLint("ClickableViewAccessibility") // Touch-up delegates taps to performClick; Button keeps accessibility actions.
    fun show() {
        if (button != null) return
        clampPosition()
        val view = Button(context).apply {
            text = glyph
            contentDescription = description
            setOnClickListener {
                onTap()
            }
        }
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragging = false
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        view.setOnTouchListener { touched, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    startX = params.x; startY = params.y
                    dragging = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    dragging = dragging || dx * dx + dy * dy > slop * slop
                    if (dragging) {
                        params.x = startX + dx.roundToInt()
                        params.y = startY + dy.roundToInt()
                        reposition()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) touched.performClick() else persistPosition()
                }
                MotionEvent.ACTION_CANCEL -> if (dragging) persistPosition()
            }
            true
        }
        manager.addView(view, params)
        button = view
    }

    fun hide() {
        val view = button ?: return
        button = null
        try {
            manager.removeViewImmediate(view)
        } catch (_: IllegalArgumentException) {
            // The system may already have detached the window on permission revocation.
        }
    }

    fun reposition() {
        clampPosition()
        try {
            button?.let { manager.updateViewLayout(it, params) }
        } catch (_: IllegalArgumentException) {
            hide()
            onFailure()
        } catch (_: SecurityException) {
            hide()
            onFailure()
        }
    }

    private fun persistPosition() {
        preferences.edit {
            putInt("x", params.x)
            putInt("y", params.y)
        }
    }

    private fun clampPosition() {
        val width: Int
        val height: Int
        if (Build.VERSION.SDK_INT >= 30) {
            val bounds = manager.currentWindowMetrics.bounds
            width = bounds.width(); height = bounds.height()
        } else {
            width = context.resources.displayMetrics.widthPixels
            height = context.resources.displayMetrics.heightPixels
        }
        params.x = params.x.coerceIn(0, (width - size).coerceAtLeast(0))
        params.y = params.y.coerceIn(0, (height - size).coerceAtLeast(0))
    }
}
