package com.aravind.wallet.ui

import android.os.Bundle
import android.content.Intent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.aravind.wallet.R
import com.aravind.wallet.databinding.PanelAnalysisBinding
import com.aravind.wallet.databinding.ItemRecordBinding
import com.aravind.wallet.domain.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class AnalysisPanel(private val activity: AppCompatActivity, private val binding: PanelAnalysisBinding, saved: Bundle?, range: (LocalDate, LocalDate) -> Unit) {
    var start: LocalDate = saved?.getString("analysisStart")?.let(LocalDate::parse) ?: LocalDate.now().withDayOfMonth(1)
        private set
    var end: LocalDate = saved?.getString("analysisEnd")?.let(LocalDate::parse) ?: LocalDate.now()
        private set
    private var calendarMonth = saved?.getString("calendarMonth")?.let(YearMonth::parse) ?: YearMonth.now()
    private var calendarCustom = saved?.takeIf { it.containsKey("calendarCustom") }?.getBoolean("calendarCustom") ?: (calendarMonth != YearMonth.now())
    private val calendar = MonthlyCalendar(binding.calendarGrid)
    private var custom = saved?.getBoolean("analysisCustom") ?: false
    private var breakdown = saved?.getString("analysisBreakdown")?.let(BreakdownMode::valueOf) ?: BreakdownMode.NET
    private val wheel = CategoryWheelView(activity)
    private var viewIndex = saved?.getInt("analysisView") ?: 0
    private var ledger: Ledger? = null
    init {
        binding.wheelPanel.addView(wheel, android.widget.LinearLayout.LayoutParams(-1, -2))
        binding.analysisView.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, activity.resources.getStringArray(R.array.analysis_views)) .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.analysisView.setSelection(viewIndex)
        binding.analysisView.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { viewIndex = position; render() }
        }

        binding.breakdownMode.check(when(breakdown) { BreakdownMode.NET -> R.id.breakdown_net; BreakdownMode.INCOME -> R.id.breakdown_income; BreakdownMode.EXPENSE -> R.id.breakdown_expense })
        binding.previousMonth.setOnClickListener { calendarCustom = true; calendarMonth = calendarMonth.minusMonths(1); render() }
        binding.nextMonth.setOnClickListener { calendarCustom = true; calendarMonth = calendarMonth.plusMonths(1); render() }
        binding.customRange.setOnClickListener { range(start, end) }
        binding.breakdownMode.setOnCheckedChangeListener { _, id ->
            breakdown = when(id) { R.id.breakdown_income -> BreakdownMode.INCOME; R.id.breakdown_expense -> BreakdownMode.EXPENSE; else -> BreakdownMode.NET }; render()
        }
    }
    fun open() {
        start = LocalDate.now().withDayOfMonth(1); end = LocalDate.now(); custom = false
        calendarCustom = false
        calendarMonth = YearMonth.now()
        breakdown = BreakdownMode.NET
        binding.breakdownMode.check(R.id.breakdown_net)
        render()
    }
    fun range(start: LocalDate, end: LocalDate) {
        if (end < start) return
        this.start = start; this.end = end; custom = true; render()
    }
    fun refreshDefaults(today: LocalDate = LocalDate.now()) {
        if (!custom) { start = today.withDayOfMonth(1); end = today }
        if (!calendarCustom) calendarMonth = YearMonth.from(today)
        render()
    }
    fun show(ledger: Ledger) {
        this.ledger = ledger
        render()
    }
    private fun render() {
        binding.calendarPanel.isVisible = viewIndex == 0
        binding.detailPanel.isVisible = viewIndex != 0
        binding.flowPanel.isVisible = viewIndex == 2
        binding.wheelPanel.isVisible = viewIndex == 1
        binding.breakdownMode.isVisible = viewIndex == 2
        binding.categoryHeading.isVisible = viewIndex == 1
        binding.breakdownList.isVisible = viewIndex == 1
        binding.modeLabel.isVisible = viewIndex == 2
        val activeMode = if (viewIndex == 1) BreakdownMode.NET else breakdown
        val current = ledger ?: return
        val data = current.analyze(start, end)
        binding.periodLabel.setText(if (custom) R.string.custom_range else R.string.this_month)
        binding.periodDates.text = activity.getString(R.string.date_range, start.display(), end.display())
        val monthly = current.analyze(calendarMonth.atDay(1), calendarMonth.atEndOfMonth())
        binding.calendarMonth.text = calendarMonth.format(DateTimeFormatter.ofPattern("MMMM uuuu", Locale.getDefault()))
        binding.totalsPeriod.text = activity.getString(R.string.date_range, calendarMonth.atDay(1).display(), calendarMonth.atEndOfMonth().display())
        binding.periodIncome.text = Money.format(monthly.totals.income)
        binding.periodExpense.text = Money.format(monthly.totals.expense)
        binding.periodNet.showNet(monthly.totals.net)
        calendar.show(calendarMonth, monthly.daily)
        binding.rangeIncome.text = Money.format(data.totals.income)
        binding.rangeExpense.text = Money.format(data.totals.expense)
        binding.rangeNet.showNet(data.totals.net)
        binding.flowGraph.show(start, end, data, breakdown)
        binding.breakdownList.removeAllViews()
        val values = data.categories.mapNotNull { (id, totals) ->
            (current.categoriesById[id] ?: if (id.isEmpty()) com.aravind.wallet.data.WalletCategory("", "Uncategorised", "tag", "Sky") else null)?.let { it to totals.value(activeMode) }
        }.filter { if (viewIndex == 1) it.second.signum() < 0 else it.second.signum() != 0 }.sortedWith(compareByDescending<Pair<com.aravind.wallet.data.WalletCategory, java.math.BigInteger>> { it.second.abs() }.thenBy { it.first.name })
        wheel.show(values)
        binding.noCategoryValues.isVisible = viewIndex == 1 && values.isEmpty()
        if (viewIndex == 1) values.forEach { (category, value) ->
            val row = ItemRecordBinding.inflate(activity.layoutInflater, binding.breakdownList, false)
            row.root.minimumHeight = 0
            row.root.setOnClickListener {
                activity.startActivity(Intent(activity, CategoryHistoryActivity::class.java)
                    .putExtra("category", category.id).putExtra("start", start.toString()).putExtra("end", end.toString())
                    .putExtra("mode", activeMode.name))
            }
            row.description.text = category.name
            row.badge.showCategory(category.iconKey, category.backgroundColorKey)
            row.amount.text = Money.format(value)
            row.amount.setTextColor(activity.getColor(when {
                activeMode == BreakdownMode.EXPENSE || value.signum() < 0 -> R.color.expense_amount
                else -> R.color.income_amount
            }))
            row.meta.isVisible = false
            binding.breakdownList.addView(row.root)
        }
    }
    fun save(out: Bundle) {
        out.putBoolean("calendarCustom", calendarCustom)
        out.putInt("analysisView", viewIndex)
        out.putString("calendarMonth", calendarMonth.toString())
        out.putString("analysisStart", start.toString()); out.putString("analysisEnd", end.toString())
        out.putBoolean("analysisCustom", custom)
        out.putString("analysisBreakdown", breakdown.name)
    }
}
