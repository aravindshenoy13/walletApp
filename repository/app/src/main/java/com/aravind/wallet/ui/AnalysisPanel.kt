package com.aravind.wallet.ui

import android.os.Bundle
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

class AnalysisPanel(private val activity: AppCompatActivity, private val binding: PanelAnalysisBinding, saved: Bundle?, range: (LocalDate, LocalDate) -> Unit) {
    var start: LocalDate = saved?.getString("analysisStart")?.let(LocalDate::parse) ?: LocalDate.now().withDayOfMonth(1)
        private set
    var end: LocalDate = saved?.getString("analysisEnd")?.let(LocalDate::parse) ?: LocalDate.now()
        private set
    private var custom = saved?.getBoolean("analysisCustom") ?: false
    private var accountId = saved?.getLong("analysisAccount")?.takeIf { it != 0L }
    private var flow = saved?.getString("analysisFlow")?.let(FlowMode::valueOf) ?: FlowMode.BOTH
    private var breakdown = saved?.getString("analysisBreakdown")?.let(BreakdownMode::valueOf) ?: BreakdownMode.NET
    private var ledger: Ledger? = null
    private var accounts = emptyList<Pair<Long?, String>>()
    init {
        binding.flowMode.check(when(flow) { FlowMode.BOTH -> R.id.flow_both; FlowMode.INCOME -> R.id.flow_income; FlowMode.EXPENSE -> R.id.flow_expense })
        binding.breakdownMode.check(when(breakdown) { BreakdownMode.NET -> R.id.breakdown_net; BreakdownMode.INCOME -> R.id.breakdown_income; BreakdownMode.EXPENSE -> R.id.breakdown_expense })
        binding.customRange.setOnClickListener { range(start, end) }
        binding.flowMode.setOnCheckedChangeListener { _, id ->
            flow = when(id) { R.id.flow_income -> FlowMode.INCOME; R.id.flow_expense -> FlowMode.EXPENSE; else -> FlowMode.BOTH }; render()
        }
        binding.breakdownMode.setOnCheckedChangeListener { _, id ->
            breakdown = when(id) { R.id.breakdown_income -> BreakdownMode.INCOME; R.id.breakdown_expense -> BreakdownMode.EXPENSE; else -> BreakdownMode.NET }; render()
        }
        binding.analysisAccount.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = accounts.getOrNull(position)?.first
                if (selected != accountId) { accountId = selected; render() }
            }
        }
    }
    fun open() {
        start = LocalDate.now().withDayOfMonth(1); end = LocalDate.now(); custom = false
        accountId = null
        binding.analysisAccount.setSelection(0)
        binding.flowMode.check(R.id.flow_both); binding.breakdownMode.check(R.id.breakdown_net)
        flow = FlowMode.BOTH; breakdown = BreakdownMode.NET
        render()
    }
    fun range(start: LocalDate, end: LocalDate) {
        if (end < start) return
        this.start = start; this.end = end; custom = true; render()
    }
    fun show(ledger: Ledger) {
        this.ledger = ledger
        val choices = listOf(null to activity.getString(R.string.all_accounts)) + ledger.accounts.map { it.id to it.name }
        if (choices != accounts) {
            accounts = choices
            if (choices.none { it.first == accountId }) accountId = null
            binding.analysisAccount.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, choices.map { it.second })
            binding.analysisAccount.setSelection(choices.indexOfFirst { it.first == accountId }.coerceAtLeast(0))
        }
        render()
    }
    private fun render() {
        val current = ledger ?: return
        val data = current.analyze(start, end, accountId)
        binding.periodLabel.setText(if (custom) R.string.custom_range else R.string.this_month)
        binding.periodDates.text = activity.getString(R.string.date_range, start.display(), end.display())
        binding.periodIncome.text = Money.format(data.totals.income)
        binding.periodExpense.text = Money.format(data.totals.expense)
        binding.periodNet.showNet(data.totals.net)
        binding.flowGraph.show(start, end, data, flow)
        binding.breakdownList.removeAllViews()
        val values = data.categories.mapNotNull { (id, totals) ->
            current.categoriesById[id]?.let { it to totals.value(breakdown) }
        }.filter { it.second.signum() != 0 }.sortedWith(compareByDescending<Pair<com.aravind.wallet.data.WalletCategory, java.math.BigInteger>> { it.second.abs() }.thenBy { it.first.name })
        binding.noCategoryValues.isVisible = values.isEmpty()
        values.forEach { (category, value) ->
            val row = ItemRecordBinding.inflate(activity.layoutInflater, binding.breakdownList, false)
            row.root.isClickable = false; row.root.isFocusable = false
            row.description.text = category.name
            row.badge.showCategory(category.iconKey, category.backgroundColorKey)
            row.amount.text = Money.format(value)
            row.amount.setTextColor(activity.getColor(when {
                breakdown == BreakdownMode.EXPENSE || value.signum() < 0 -> R.color.expense_amount
                else -> R.color.income_amount
            }))
            row.meta.isVisible = false
            binding.breakdownList.addView(row.root)
        }
    }
    fun save(out: Bundle) {
        out.putString("analysisStart", start.toString()); out.putString("analysisEnd", end.toString())
        out.putBoolean("analysisCustom", custom); out.putLong("analysisAccount", accountId ?: 0L)
        out.putString("analysisFlow", flow.name); out.putString("analysisBreakdown", breakdown.name)
    }
}
