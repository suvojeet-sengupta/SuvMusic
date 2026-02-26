package com.suvojeet.suvmusic.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.suvojeet.suvmusic.ui.components.SeekbarStyle
import kotlin.math.sin
import kotlin.random.Random

/**
 * Custom View that renders different seekbar styles for the Dynamic Island.
 */
class DynamicSeekbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    var style: SeekbarStyle = SeekbarStyle.WAVEFORM
        set(value) {
            field = value
            invalidate()
        }

    var onSeekListener: ((Float) -> Unit)? = null

    // Colors
    private val activeColor = Color.WHITE
    private val inactiveColor = Color.parseColor("#55FFFFFF")
    
    // Paints
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    // Data for waveform
    private val amplitudes = List(50) { Random.nextFloat() * 0.6f + 0.4f }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        when (style) {
            SeekbarStyle.WAVEFORM -> drawWaveform(canvas)
            SeekbarStyle.WAVE_LINE -> drawWaveLine(canvas)
            SeekbarStyle.CLASSIC -> drawClassic(canvas)
            SeekbarStyle.DOTS -> drawDots(canvas)
            SeekbarStyle.GRADIENT_BAR -> drawGradient(canvas)
            SeekbarStyle.MATERIAL -> drawClassic(canvas) // Reuse classic style for now
        }
    }

    private fun drawWaveform(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()
        val centerY = height / 2f
        val barWidth = width / amplitudes.size
        val maxBarHeight = height * 0.8f
        val progressX = progress * width

        paint.style = Paint.Style.FILL
        
        amplitudes.forEachIndexed { index, amp ->
            val x = index * barWidth + barWidth / 2
            val barHeight = amp * maxBarHeight
            val top = centerY - barHeight / 2
            val bottom = centerY + barHeight / 2
            
            paint.color = if (x < progressX) activeColor else inactiveColor
            
            canvas.drawRoundRect(
                x - barWidth * 0.3f, top,
                x + barWidth * 0.3f, bottom,
                barWidth * 0.3f, barWidth * 0.3f,
                paint
            )
        }
    }

    private fun drawWaveLine(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()
        val centerY = height / 2f
        val amplitude = height * 0.3f
        val progressX = progress * width

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.strokeCap = Paint.Cap.ROUND

        // Inactive line
        path.reset()
        path.moveTo(0f, centerY)
        var x = 0f
        while (x <= width) {
            val y = centerY + sin(x * 0.1f) * amplitude
            path.lineTo(x, y)
            x += 5f
        }
        paint.color = inactiveColor
        canvas.drawPath(path, paint)

        // Active line
        path.reset()
        path.moveTo(0f, centerY)
        x = 0f
        while (x <= progressX) {
            val y = centerY + sin(x * 0.1f) * amplitude
            path.lineTo(x, y)
            x += 5f
        }
        paint.color = activeColor
        canvas.drawPath(path, paint)
    }

    private fun drawClassic(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()
        val centerY = height / 2f
        val trackHeight = 6f // thinner track

        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.ROUND

        // Background track
        paint.color = inactiveColor
        canvas.drawRoundRect(0f, centerY - trackHeight/2, width, centerY + trackHeight/2, trackHeight/2, trackHeight/2, paint)

        // Progress track
        paint.color = activeColor
        canvas.drawRoundRect(0f, centerY - trackHeight/2, progress * width, centerY + trackHeight/2, trackHeight/2, trackHeight/2, paint)

        // Thumb
        canvas.drawCircle(progress * width, centerY, 12f, paint)
    }

    private fun drawDots(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()
        val centerY = height / 2f
        val dotCount = 20
        val spacing = width / dotCount
        val progressX = progress * width
        val radius = 6f

        paint.style = Paint.Style.FILL
        
        for (i in 0 until dotCount) {
            val cx = i * spacing + spacing / 2
            paint.color = if (cx < progressX) activeColor else inactiveColor
            canvas.drawCircle(cx, centerY, radius, paint)
        }
    }

    private fun drawGradient(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()
        val centerY = height / 2f
        val trackHeight = 10f

        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.ROUND

        // Background
        paint.color = inactiveColor
        canvas.drawRoundRect(0f, centerY - trackHeight/2, width, centerY + trackHeight/2, trackHeight/2, trackHeight/2, paint)

        // Gradient Progress
        val gradient = LinearGradient(
            0f, 0f, width, 0f,
            intArrayOf(Color.parseColor("#7B2CBF"), Color.parseColor("#D43A9C"), Color.parseColor("#008498")), // Purple, Magenta, Cyan
            null,
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        
        // We need to clip or draw rect for progress
        canvas.drawRoundRect(0f, centerY - trackHeight/2, progress * width, centerY + trackHeight/2, trackHeight/2, trackHeight/2, paint)
        
        paint.shader = null // Reset shader
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                progress = event.x / width.toFloat()
                onSeekListener?.invoke(progress)
                return true
            }
            MotionEvent.ACTION_UP -> {
                progress = event.x / width.toFloat()
                onSeekListener?.invoke(progress)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
