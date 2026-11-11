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
    private var mode = FlowMode.BOTH
    fun show(start: LocalDate, end: LocalDate, data: PeriodAnalysis, mode: FlowMode) {
        this.start = start; this.end = end; this.daily = data.daily; this.mode = mode
        contentDescription = context.getString(R.string.graph_summary, start.display(), end.display(), Money.format(data.totals.income), Money.format(data.totals.expense))
        invalidate()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val left = 10f * density; val right = width - 12f * density
        val top = 38f * density; val bottom = height - 30f * density
        if (right <= left || bottom <= top) return
        val highest = daily.values.flatMap { when (mode) {
            FlowMode.BOTH -> listOf(it.income, it.expense)
            FlowMode.INCOME -> listOf(it.income)
            FlowMode.EXPENSE -> listOf(it.expense)
        } }.maxOrNull() ?: BigInteger.ZERO
        val max = highest.coerceAtLeast(BigInteger.ONE)
        paint.style = Paint.Style.FILL; paint.color = context.getColor(R.color.quiet)
        paint.textSize = 11f * resources.displayMetrics.scaledDensity
        canvas.drawText(Money.format(highest), left, top - 15f * density, paint)
        canvas.drawText("0", left, bottom + 13f * density, paint)
        paint.color = context.getColor(R.color.line); paint.strokeWidth = density
        canvas.drawLine(left, top, left, bottom, paint); canvas.drawLine(left, bottom, right, bottom, paint)
        val days = ChronoUnit.DAYS.between(start, end)
        val points = sortedSetOf(start, end)
        daily.keys.forEach { date ->
            points.add(date)
            if (date > start) points.add(date.minusDays(1))
            if (date < end) points.add(date.plusDays(1))
        }
        fun x(date: LocalDate) = if (days == 0L) (left + right) / 2 else left + ((right-left) * ChronoUnit.DAYS.between(start, date).toDouble() / days).toFloat()
        fun series(income: Boolean, color: Int) {
            val path = Path()
            points.forEachIndexed { index, date ->
                val total = daily[date]
                val value = (if (income) total?.income else total?.expense) ?: BigInteger.ZERO
                val ratio = BigDecimal(value).divide(BigDecimal(max), MathContext.DECIMAL64).toFloat()
                val y = bottom - (bottom - top) * ratio
                if (index == 0) path.moveTo(x(date), y) else path.lineTo(x(date), y)
                paint.color = color; paint.style = Paint.Style.FILL
                if (days == 0L || value.signum() != 0) canvas.drawCircle(x(date), y, 3f*density, paint)
            }
            paint.color = color; paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f*density
            canvas.drawPath(path, paint)
        }
        if (mode != FlowMode.EXPENSE) series(true, context.getColor(R.color.income_amount))
        if (mode != FlowMode.INCOME) series(false, context.getColor(R.color.expense_amount))
        paint.style = Paint.Style.FILL; paint.color = context.getColor(R.color.quiet)
        val format = DateTimeFormatter.ofPattern("d MMM")
        paint.textAlign = Paint.Align.LEFT; canvas.drawText(start.format(format), left, height-4*density, paint)
        if (days > 0) { paint.textAlign = Paint.Align.RIGHT; canvas.drawText(end.format(format), right, height-4*density, paint) }
        paint.textAlign = Paint.Align.LEFT
    }
}
