package com.aravind.wallet.ui

import android.graphics.Typeface
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.aravind.wallet.R
import com.aravind.wallet.domain.DailyTotals
import com.aravind.wallet.domain.Money
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** A summary grid of native text views, with exact values available to accessibility. */
class MonthlyCalendar(private val container: LinearLayout) {
    private val context get() = container.context
    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()

    fun show(month: YearMonth, daily: Map<LocalDate, DailyTotals>) {
        container.removeAllViews()
        fun row() = LinearLayout(context).also {
            it.orientation = LinearLayout.HORIZONTAL
            container.addView(it, LinearLayout.LayoutParams(-1, -2))
        }
        val headings = row()
        // Monday-first grid, including leading and trailing blank cells.
        DayOfWeek.values().forEach { day ->
            headings.addView(TextView(context).apply {
                text = day.getDisplayName(if (context.resources.configuration.fontScale > 1.3f) TextStyle.NARROW else TextStyle.SHORT, Locale.getDefault())
                contentDescription = day.getDisplayName(TextStyle.FULL, Locale.getDefault())
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTextColor(context.getColor(R.color.quiet))
                setPadding(0, dp(6), 0, dp(6))
            }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        val offset = month.atDay(1).dayOfWeek.value - 1
        val weeks = (offset + month.lengthOfMonth() + 6) / 7
        repeat(weeks) { week ->
            val weekRow = row()
            repeat(7) { column ->
                val number = week * 7 + column - offset + 1
                val cell = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    setPadding(dp(1), dp(3), dp(1), dp(3))
                    // Each shared edge belongs to just one cell.
                    background = object : Drawable() {
                        private val paint = Paint().apply { color = context.getColor(R.color.line) }
                        override fun draw(canvas: Canvas) {
                            val b = bounds
                            val stroke = dp(1).coerceAtLeast(1).toFloat()
                            canvas.drawRect(b.right - stroke, b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat(), paint)
                            canvas.drawRect(b.left.toFloat(), b.bottom - stroke, b.right.toFloat(), b.bottom.toFloat(), paint)
                            if (column == 0) canvas.drawRect(b.left.toFloat(), b.top.toFloat(), b.left + stroke, b.bottom.toFloat(), paint)
                            if (week == 0) canvas.drawRect(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.top + stroke, paint)
                        }
                        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
                        override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
                        @Deprecated("Deprecated in Android")
                        override fun getOpacity() = PixelFormat.TRANSLUCENT
                    }
                }
                weekRow.addView(cell, LinearLayout.LayoutParams(0, -1, 1f))
                if (number !in 1..month.lengthOfMonth()) {
                    cell.minimumHeight = dp(46)
                    cell.importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                } else {
                    val date = month.atDay(number)
                    val totals = daily[date]
                    val net = totals?.net ?: java.math.BigInteger.ZERO
                    cell.contentDescription = if (totals == null) context.getString(R.string.calendar_empty_day, date.display())
                        else context.getString(R.string.calendar_day_summary, date.display(), Money.format(net))
                    cell.importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_YES
                    cell.addView(TextView(context).apply {
                        text = number.toString()
                        gravity = Gravity.CENTER
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        setTextColor(context.getColor(R.color.ink))
                        if (date == LocalDate.now()) setTypeface(null, Typeface.BOLD)
                        importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }, LinearLayout.LayoutParams(-1, -2))
                    cell.addView(TextView(context).apply {
                        text = if (totals == null) "—" else Money.compactNet(net)
                        gravity = Gravity.CENTER
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                        setTextColor(context.getColor(when (net.signum()) {
                            1 -> R.color.income_amount
                            -1 -> R.color.expense_amount
                            else -> R.color.quiet
                        }))
                        importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }, LinearLayout.LayoutParams(-1, -2))
                }
            }
        }
    }
}
