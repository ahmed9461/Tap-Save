package io.github.ahmed9461.tapsave.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import io.github.ahmed9461.tapsave.R

/** Native vector control; frames are scheduled only while visible and indeterminate. */
class FloatingSaveView(context: Context) : View(context) {
    enum class State { IDLE, BUSY, PROGRESS, SUCCESS, ERROR }
    var state = State.IDLE
        private set
    private var progress = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val ring = RectF()
    private val icon = context.getDrawable(R.drawable.ic_download)!!
    private val mint = Color.rgb(53, 230, 167)
    private val ink = Color.rgb(11, 16, 24)
    init {
        isClickable = true; isFocusable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        elevation = 6 * resources.displayMetrics.density
        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(mint) }
        clipToOutline = true
    }
    fun render(state: State, progress: Float, label: String) {
        this.state = state; this.progress = progress.coerceIn(0f, 1f)
        contentDescription = label
        invalidate()
    }
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = "android.widget.Button"
    }
    override fun performClick(): Boolean { super.performClick(); return true }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = width.coerceAtMost(height).toFloat()
        val unit = d / 56
        paint.color = ink; paint.strokeWidth = 2.4f * unit; paint.style = Paint.Style.STROKE
        if (state == State.BUSY || state == State.PROGRESS) {
            paint.color = Color.argb(45, 11, 16, 24)
            ring.set(4 * unit, 4 * unit, d - 4 * unit, d - 4 * unit)
            canvas.drawOval(ring, paint)
            paint.color = ink
            val start = if (state == State.BUSY) (SystemClock.uptimeMillis() % 1_200) * .3f else -90f
            canvas.drawArc(ring, start, if (state == State.BUSY) 85f else progress * 360, false, paint)
        }
        when (state) {
            State.SUCCESS -> {
                canvas.drawLine(17 * unit, 28 * unit, 24 * unit, 35 * unit, paint)
                canvas.drawLine(24 * unit, 35 * unit, 39 * unit, 20 * unit, paint)
            }
            State.ERROR -> {
                canvas.drawLine(28 * unit, 17 * unit, 28 * unit, 30 * unit, paint)
                canvas.drawCircle(28 * unit, 38 * unit, 1 * unit, paint)
            }
            else -> { icon.setBounds((16 * unit).toInt(), (16 * unit).toInt(), (40 * unit).toInt(), (40 * unit).toInt()); icon.draw(canvas) }
        }
        if (state == State.BUSY && isAttachedToWindow && windowVisibility == VISIBLE) postInvalidateDelayed(32)
    }
}
