package dev.sweitz.veniceglasses.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import dev.sweitz.veniceglasses.R

/** Mirrored bar waveform of the microphone level, shown while listening. */
class WaveformView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.accent)
        strokeCap = Paint.Cap.ROUND
    }
    private var levels: List<Float> = emptyList()

    fun setLevels(values: List<Float>) {
        levels = values
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bars = ChatViewModel.WAVE_BARS
        val step = width.toFloat() / bars
        paint.strokeWidth = step * 0.45f
        val mid = height / 2f
        val padded = List(bars - levels.size) { 0.06f } + levels.takeLast(bars)
        padded.forEachIndexed { i, level ->
            val h = (level * height * 0.95f).coerceAtLeast(paint.strokeWidth) / 2f
            val x = step * i + step / 2f
            canvas.drawLine(x, mid - h, x, mid + h, paint)
        }
    }
}
