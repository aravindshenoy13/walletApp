package com.aravind.wallet.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.aravind.wallet.R
import com.aravind.wallet.domain.*
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter

/** Sparse day points plus adjacent zero days avoid allocating every day in very long ranges. */
class DailyFlowView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var start = LocalDate.now()
    private var end = start
    private var daily = emptyMap<LocalDate, DailyTotals>()
    private var inspectedDate = start
    private var totals = DailyTotals(BigInteger.ZERO, BigInteger.ZERO)
    private var mode = BreakdownMode.NET
    fun show(start: LocalDate, end: LocalDate, data: PeriodAnalysis, mode: BreakdownMode) {
        this.start = start; this.end = end; this.daily = data.daily; this.mode = mode
        totals = data.totals
        if (inspectedDate < start || inspectedDate > end) inspectedDate = start
        updateAccessibility()
        invalidate()
    }
    private fun updateAccessibility() {
        val label = context.getString(when (mode) { BreakdownMode.NET -> R.string.net; BreakdownMode.INCOME -> R.string.income; BreakdownMode.EXPENSE -> R.string.expenses })
        val value = daily[inspectedDate]?.value(mode) ?: BigInteger.ZERO
        contentDescription = context.getString(R.string.graph_summary, start.display(), end.display(), Money.format(totals.income), Money.format(totals.expense)) + " " +
            context.getString(R.string.graph_day_value, inspectedDate.display(), Money.format(value), label, Money.format(totals.value(mode)))
    }
    override fun onInitializeAccessibilityNodeInfo(info: android.view.accessibility.AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.isScrollable = start < end
        if (inspectedDate < end) info.addAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(
            android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, context.getString(R.string.next_day)))
        if (inspectedDate > start) info.addAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(
            android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, context.getString(R.string.previous_day)))
    }
    override fun performAccessibilityAction(action: Int, arguments: android.os.Bundle?): Boolean {
        inspectedDate = when {
            action == android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD && inspectedDate < end -> inspectedDate.plusDays(1)
            action == android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD && inspectedDate > start -> inspectedDate.minusDays(1)
            else -> return super.performAccessibilityAction(action, arguments)
        }
        updateAccessibility()
        sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED)
        return true
    }
    private fun drawAxisLabel(canvas: Canvas, label: String, x: Float, y: Float, availableWidth: Float) {
        val size = paint.textSize
        val measured = paint.measureText(label)
        if (measured > availableWidth) paint.textSize *= availableWidth / measured
        canvas.drawText(label, x, y, paint)
        paint.textSize = size
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val left = 10f * density; val right = width - 12f * density
        paint.textSize = 11f * resources.displayMetrics.scaledDensity
        val top = paint.fontSpacing + 12f * density
        val bottom = height - 2 * paint.fontSpacing - 8f * density
        if (right <= left || bottom <= top) return
        val values = daily.mapValues { it.value.value(mode) }
        val highest = (values.values.maxOrNull() ?: BigInteger.ZERO).coerceAtLeast(BigInteger.ZERO)
        val lowest = (values.values.minOrNull() ?: BigInteger.ZERO).coerceAtMost(BigInteger.ZERO)
        val span = (highest - lowest).coerceAtLeast(BigInteger.ONE)
        fun y(value: BigInteger): Float {
            val ratio = BigDecimal(highest - value).divide(BigDecimal(span), MathContext.DECIMAL64).toFloat()
            // An empty or entirely zero series sits on the bottom baseline.
            return if (highest == lowest) bottom else top + (bottom - top) * ratio
        }
        val zero = y(BigInteger.ZERO)
        paint.style = Paint.Style.FILL; paint.color = context.getColor(R.color.quiet)
        paint.textSize = 11f * resources.displayMetrics.scaledDensity
        drawAxisLabel(canvas, Money.compactNet(highest), left, top - 6f * density, right - left)
        if (lowest.signum() < 0) drawAxisLabel(canvas, Money.compactNet(lowest), left, bottom + paint.fontSpacing, right - left)
        else canvas.drawText("0", left, bottom + paint.fontSpacing, paint)
        if (zero - top > 20f * density && bottom - zero > 20f * density) canvas.drawText("0", left, zero + 13f * density, paint)
        paint.color = context.getColor(R.color.line); paint.strokeWidth = density
        canvas.drawLine(left, top, left, bottom, paint); canvas.drawLine(left, zero, right, zero, paint)
        val days = ChronoUnit.DAYS.between(start, end)
        val points = sortedSetOf(start, end)
        daily.keys.forEach { date ->
            points.add(date)
            if (date > start) points.add(date.minusDays(1))
            if (date < end) points.add(date.plusDays(1))
        }
        fun x(date: LocalDate) = if (days == 0L) (left + right) / 2 else left + ((right-left) * ChronoUnit.DAYS.between(start, date).toDouble() / days).toFloat()
        val path = Path()
        points.forEachIndexed { index, date ->
            val value = values[date] ?: BigInteger.ZERO
            if (index == 0) path.moveTo(x(date), y(value)) else path.lineTo(x(date), y(value))
        }
        fun drawLine(color: Int) {
            paint.color = color; paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f * density
            canvas.drawPath(path, paint)
        }
        if (mode == BreakdownMode.NET && highest == lowest) drawLine(context.getColor(R.color.quiet))
        else if (mode == BreakdownMode.NET) {
            // Clip the same path at zero so crossing segments change color exactly at the baseline.
            canvas.save(); canvas.clipRect(0f, 0f, width.toFloat(), zero)
            drawLine(context.getColor(R.color.income_amount)); canvas.restore()
            canvas.save(); canvas.clipRect(0f, zero, width.toFloat(), height.toFloat())
            drawLine(context.getColor(R.color.expense_amount)); canvas.restore()
        } else drawLine(context.getColor(if (mode == BreakdownMode.INCOME) R.color.income_amount else R.color.expense_amount))
        paint.style = Paint.Style.FILL
        points.forEach { date ->
            val value = values[date] ?: BigInteger.ZERO
            if (days == 0L || value.signum() != 0) {
                paint.color = context.getColor(when {
                    mode == BreakdownMode.EXPENSE || value.signum() < 0 -> R.color.expense_amount
                    value.signum() == 0 -> R.color.quiet
                    else -> R.color.income_amount
                })
                canvas.drawCircle(x(date), y(value), 3f * density, paint)
            }
        }
        paint.color = context.getColor(R.color.quiet)
        val format = DateTimeFormatter.ofPattern("d MMM")
        paint.textAlign = Paint.Align.LEFT; canvas.drawText(start.format(format), left, height-4*density, paint)
        if (days > 0) { paint.textAlign = Paint.Align.RIGHT; canvas.drawText(end.format(format), right, height-4*density, paint) }
        paint.textAlign = Paint.Align.LEFT
    }
}
