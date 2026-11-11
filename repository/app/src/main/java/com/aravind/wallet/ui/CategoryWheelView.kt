package com.aravind.wallet.ui

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import androidx.core.view.ViewCompat
import com.aravind.wallet.R
import com.aravind.wallet.data.WalletCategory
import com.aravind.wallet.domain.Money
import java.math.BigInteger
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.NumberFormat
import kotlin.math.atan2
import kotlin.math.hypot

/** Drawing alone uses floating point; values and totals stay exact. */
class CategoryWheelView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var values = emptyList<Pair<WalletCategory, BigInteger>>()
    private var sweeps = emptyList<Float>()
    private var total = BigInteger.ZERO
    private var pressedSector: Int? = null
    init { isClickable = true; isFocusable = true; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES }

    fun show(values: List<Pair<WalletCategory, BigInteger>>) {
        val negative = values.filter { it.second.signum() < 0 }
        if (this.values == negative) return
        clearPress()
        this.values = negative
        total = negative.fold(BigInteger.ZERO) { sum, row -> sum + row.second.abs() }
        var angle = 0f
        sweeps = negative.mapIndexed { index, row ->
            val sweep = if (index == negative.lastIndex) 360f - angle else
                BigDecimal(row.second.abs()).divide(BigDecimal(total), MathContext.DECIMAL64).toFloat() * 360f
            angle += sweep
            sweep
        }
        contentDescription = context.getString(R.string.wheel_accessibility, negative.joinToString {
            "${it.first.name}: ${Money.format(it.second)}, ${percentage(it.second)}"
        })
        invalidate()
    }

    private fun percentage(value: BigInteger): String = NumberFormat.getPercentInstance().apply {
        maximumFractionDigits = 1
    }.format(BigDecimal(value.abs()).divide(BigDecimal(total), 12, RoundingMode.HALF_UP))

    private fun sectorAt(x: Float, y: Float): Int? {
        val dx = x - width / 2f; val dy = y - height / 2f
        val radius = minOf(width, height) * 0.42f
        val distance = hypot(dx, dy)
        if (distance < radius * 0.55f || distance > radius || values.isEmpty()) return null
        val angle = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 450.0) % 360.0).toFloat()
        var end = 0f
        return sweeps.indices.firstOrNull { end += sweeps[it]; angle < end }
    }

    private fun press(index: Int?) {
        if (pressedSector == index) return
        pressedSector = index
        ViewCompat.setStateDescription(this, index?.let {
            context.getString(R.string.wheel_percentage, values[it].first.name, percentage(values[it].second))
        })
        invalidate()
    }
    private fun clearPress() { press(null); parent?.requestDisallowInterceptTouchEvent(false) }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val sector = sectorAt(event.x, event.y) ?: return false
                parent?.requestDisallowInterceptTouchEvent(true)
                press(sector)
            }
            MotionEvent.ACTION_MOVE -> press(sectorAt(event.x, event.y))
            MotionEvent.ACTION_UP -> { clearPress(); performClick() }
            MotionEvent.ACTION_CANCEL -> clearPress()
            else -> return super.onTouchEvent(event)
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
    override fun onDetachedFromWindow() { clearPress(); super.onDetachedFromWindow() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = minOf(width, height) * 0.42f
        val cx = width / 2f; val cy = height / 2f
        val bounds = RectF(cx-radius, cy-radius, cx+radius, cy+radius)
        paint.style = Paint.Style.FILL
        if (total.signum() == 0) {
            paint.color = context.getColor(R.color.line); canvas.drawOval(bounds, paint)
        } else {
            var start = -90f
            values.forEachIndexed { index, (category, _) ->
                paint.style = Paint.Style.FILL
                paint.color = categoryDisplayColor(context, category.backgroundColorKey)
                canvas.drawArc(bounds, start, sweeps[index], true, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = resources.displayMetrics.density
                paint.color = context.getColor(R.color.ink)
                canvas.drawArc(bounds, start, sweeps[index], true, paint)
                start += sweeps[index]
            }
        }
        paint.style = Paint.Style.FILL
        paint.color = context.getColor(R.color.paper); canvas.drawCircle(cx, cy, radius*0.55f, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = resources.displayMetrics.density
        paint.color = context.getColor(R.color.ink); canvas.drawCircle(cx, cy, radius*0.55f, paint)
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        val label = pressedSector?.let { percentage(values[it].second) } ?: context.getString(R.string.wheel_negative_net)
        paint.textSize = 14f * resources.displayMetrics.scaledDensity
        val maxWidth = radius * 0.96f
        if (paint.measureText(label) > maxWidth) paint.textSize *= maxWidth / paint.measureText(label)
        canvas.drawText(label, cx, cy - (paint.ascent() + paint.descent()) / 2, paint)
    }
}
