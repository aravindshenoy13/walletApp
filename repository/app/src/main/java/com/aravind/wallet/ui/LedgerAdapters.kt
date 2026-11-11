package com.aravind.wallet.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aravind.wallet.R
import com.aravind.wallet.data.Account
import com.aravind.wallet.data.WalletRecord
import com.aravind.wallet.databinding.ItemDateBinding
import com.aravind.wallet.databinding.ItemRecordBinding
import com.aravind.wallet.domain.Ledger
import com.aravind.wallet.domain.Money
import com.aravind.wallet.domain.RecordType
import java.math.BigInteger
import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed interface RecordRow {
    data class Date(val date: LocalDate) : RecordRow
    data class Entry(val record: WalletRecord, val source: String, val destination: String?) : RecordRow
}

class RecordsAdapter(private val edit: (Long) -> Unit) :
    ListAdapter<RecordRow, RecyclerView.ViewHolder>(object : DiffUtil.ItemCallback<RecordRow>() {
        override fun areItemsTheSame(old: RecordRow, new: RecordRow): Boolean = when {
            old is RecordRow.Date && new is RecordRow.Date -> old.date == new.date
            old is RecordRow.Entry && new is RecordRow.Entry -> old.record.id == new.record.id
            else -> false
        }
        override fun areContentsTheSame(old: RecordRow, new: RecordRow) = old == new
    }) {
    init { stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY }
    private class DateHolder(val binding: ItemDateBinding) : RecyclerView.ViewHolder(binding.root)
    private class RecordHolder(val binding: ItemRecordBinding) : RecyclerView.ViewHolder(binding.root)

    fun show(ledger: Ledger) {
        val rows = buildList {
            ledger.records.groupBy { it.occurredAt.toLocalDate() }.forEach { (date, records) ->
                add(RecordRow.Date(date))
                records.forEach { record ->
                    add(RecordRow.Entry(record, ledger.accountsById.getValue(record.accountId).name,
                        record.destinationAccountId?.let { ledger.accountsById.getValue(it).name }))
                }
            }
        }
        submitList(rows)
    }

    override fun getItemViewType(position: Int) = if (getItem(position) is RecordRow.Date) 0 else 1
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) DateHolder(ItemDateBinding.inflate(inflater, parent, false))
        else RecordHolder(ItemRecordBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is RecordRow.Date -> (holder as DateHolder).binding.apply {
                dateLabel.text = row.date.display()
            }
            is RecordRow.Entry -> (holder as RecordHolder).binding.apply {
                val record = row.record
                val context = root.context
                description.text = record.description
                amount.text = context.getString(when (record.type) {
                    RecordType.EXPENSE -> R.string.expense_amount
                    RecordType.INCOME -> R.string.income_amount
                    RecordType.TRANSFER -> R.string.transfer_amount
                }, Money.format(record.amountMinor))
                amount.setTextColor(context.getColor(when (record.type) {
                    RecordType.INCOME -> R.color.income_amount
                    RecordType.EXPENSE -> R.color.expense_amount
                    RecordType.TRANSFER -> R.color.ink
                }))
                val time = record.occurredAt.format(DateTimeFormatter.ofPattern("HH:mm"))
                meta.text = if (record.type == RecordType.TRANSFER)
                    context.getString(R.string.transfer_meta, time, row.source, row.destination)
                else context.getString(R.string.record_meta, time, record.category?.label, row.source)
                root.setOnClickListener { edit(record.id) }
            }
        }
    }
}

data class AccountRow(val account: Account, val balance: BigInteger)

class AccountsAdapter(private val edit: (Long) -> Unit) :
    ListAdapter<AccountRow, AccountsAdapter.Holder>(object : DiffUtil.ItemCallback<AccountRow>() {
        override fun areItemsTheSame(old: AccountRow, new: AccountRow) = old.account.id == new.account.id
        override fun areContentsTheSame(old: AccountRow, new: AccountRow) = old == new
    }) {
    init { stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY }
    class Holder(val binding: ItemRecordBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = getItem(position)
        holder.binding.apply {
            description.text = row.account.name
            amount.text = Money.format(row.balance)
            amount.setTextColor(root.context.getColor(R.color.ink))
            meta.text = root.context.getString(R.string.opening_balance_value, Money.format(row.account.openingBalanceMinor))
            root.setOnClickListener { edit(row.account.id) }
        }
    }
}
