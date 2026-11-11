package com.aravind.wallet.ui

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault())
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
                    gravity = Gravity.CENTER
                    setPadding(dp(1), dp(3), dp(1), dp(3))
                    background = GradientDrawable().apply {
                        setColor(context.getColor(R.color.paper))
                        setStroke(dp(1).coerceAtLeast(1), context.getColor(R.color.line))
                    }
                }
                weekRow.addView(cell, LinearLayout.LayoutParams(0, -2, 1f))
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
                    }, LinearLayout.LayoutParams(-1, dp(18)))
                    cell.addView(TextView(context).apply {
                        text = if (totals == null) "—" else Money.compactNet(net)
                        gravity = Gravity.CENTER
                        maxLines = 1
                        setAutoSizeTextTypeUniformWithConfiguration(7, 11, 1, TypedValue.COMPLEX_UNIT_SP)
                        setTextColor(context.getColor(when (net.signum()) {
                            1 -> R.color.income_amount
                            -1 -> R.color.expense_amount
                            else -> R.color.quiet
                        }))
                        importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }, LinearLayout.LayoutParams(-1, dp(22)))
                }
            }
        }
    }
}
