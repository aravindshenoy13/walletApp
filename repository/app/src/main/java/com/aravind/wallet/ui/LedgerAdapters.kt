package com.aravind.wallet.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aravind.wallet.R
import com.aravind.wallet.data.Account
import com.aravind.wallet.data.WalletRecord
import com.aravind.wallet.data.WalletCategory
import com.aravind.wallet.databinding.ItemDateBinding
import com.aravind.wallet.databinding.ItemRecordBinding
import com.aravind.wallet.databinding.ItemAccountBinding
import com.aravind.wallet.domain.Ledger
import com.aravind.wallet.domain.Money
import com.aravind.wallet.domain.RecordType
import java.math.BigInteger
import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed interface RecordRow {
    data class Date(val date: LocalDate) : RecordRow
    data class Entry(val record: WalletRecord, val source: String, val destination: String?, val category: WalletCategory?, val accountScope: Long? = null) : RecordRow
    data class Opening(val account: Account) : RecordRow
}

class RecordsAdapter(private val edit: (Long) -> Unit) :
    ListAdapter<RecordRow, RecyclerView.ViewHolder>(object : DiffUtil.ItemCallback<RecordRow>() {
        override fun areItemsTheSame(old: RecordRow, new: RecordRow): Boolean = when {
            old is RecordRow.Date && new is RecordRow.Date -> old.date == new.date
            old is RecordRow.Entry && new is RecordRow.Entry -> old.record.id == new.record.id
            old is RecordRow.Opening && new is RecordRow.Opening -> old.account.id == new.account.id
            else -> false
        }
        override fun areContentsTheSame(old: RecordRow, new: RecordRow) = old == new
    }) {
    init { stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY }
    private class DateHolder(val binding: ItemDateBinding) : RecyclerView.ViewHolder(binding.root)
    private class RecordHolder(val binding: ItemRecordBinding) : RecyclerView.ViewHolder(binding.root)
    private class OpeningHolder(val binding: ItemAccountBinding) : RecyclerView.ViewHolder(binding.root)

    fun show(ledger: Ledger, records: List<WalletRecord> = ledger.records, accountId: Long? = null, opening: Boolean = false) {
        val rows = buildList {
            records.sortedWith(compareByDescending<WalletRecord> { it.occurredAt }.thenByDescending { it.id }).groupBy { it.occurredAt.toLocalDate() }.forEach { (date, records) ->
                add(RecordRow.Date(date))
                records.forEach { record ->
                    add(RecordRow.Entry(record, ledger.accountsById.getValue(record.accountId).name,
                        record.destinationAccountId?.let { ledger.accountsById.getValue(it).name },
                        record.categoryId?.let { ledger.categoriesById.getValue(it) }, accountId))
                }
            }
            if (opening) ledger.accountsById[accountId]?.let { add(RecordRow.Opening(it)) }
        }
        submitList(rows)
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) { is RecordRow.Date -> 0; is RecordRow.Entry -> 1; is RecordRow.Opening -> 2 }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 2) OpeningHolder(ItemAccountBinding.inflate(inflater, parent, false))
        else if (viewType == 0) DateHolder(ItemDateBinding.inflate(inflater, parent, false))
        else RecordHolder(ItemRecordBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is RecordRow.Opening -> (holder as OpeningHolder).binding.apply {
                description.setText(R.string.opening_balance_label)
                amount.text = Money.format(row.account.openingBalanceMinor)
                meta.isVisible = false; editAccount.isVisible = false
                root.isClickable = false; root.isFocusable = false
            }
            is RecordRow.Date -> (holder as DateHolder).binding.apply {
                dateLabel.text = row.date.display()
            }
            is RecordRow.Entry -> (holder as RecordHolder).binding.apply {
                val record = row.record
                val context = root.context
                description.text = row.category?.name ?: context.getString(R.string.transfer)
                if (record.type == RecordType.TRANSFER && row.accountScope != null) {
                    description.text = if (record.accountId == row.accountScope) context.getString(R.string.transfer_to, row.destination)
                        else context.getString(R.string.transfer_from, row.source)
                }
                badge.isVisible = true
                if (record.type == RecordType.TRANSFER) {
                    badge.setImageResource(R.drawable.ic_transfer)
                    badge.imageTintList = null
                    badge.background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(context.getColor(R.color.transfer_background))
                        cornerRadius = 4 * context.resources.displayMetrics.density
                    }
                } else row.category?.let { badge.showCategory(it.iconKey, it.backgroundColorKey) }
                amount.text = context.getString(when (record.type) {
                    RecordType.EXPENSE -> R.string.expense_amount
                    RecordType.INCOME -> R.string.income_amount
                    RecordType.TRANSFER -> if (row.accountScope == null) R.string.transfer_amount
                        else if (record.accountId == row.accountScope) R.string.expense_amount else R.string.income_amount
                }, Money.format(record.amountMinor))
                amount.setTextColor(context.getColor(when (record.type) {
                    RecordType.INCOME -> R.color.income_amount
                    RecordType.EXPENSE -> R.color.expense_amount
                    RecordType.TRANSFER -> R.color.transfer_amount
                }))
                val time = record.occurredAt.format(DateTimeFormatter.ofPattern("HH:mm"))
                meta.text = listOf(time, if (record.type == RecordType.TRANSFER)
                    context.getString(R.string.transfer_accounts, row.source, row.destination) else row.source,
                    record.description).filter { it.isNotBlank() }.joinToString(" · ")
                root.setOnClickListener { edit(record.id) }
            }
        }
    }
}

data class AccountRow(val account: Account, val balance: BigInteger)

class AccountsAdapter(private val history: (Long) -> Unit, private val edit: (Long) -> Unit) :
    ListAdapter<AccountRow, AccountsAdapter.Holder>(object : DiffUtil.ItemCallback<AccountRow>() {
        override fun areItemsTheSame(old: AccountRow, new: AccountRow) = old.account.id == new.account.id
        override fun areContentsTheSame(old: AccountRow, new: AccountRow) = old == new
    }) {
    init { stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY }
    class Holder(val binding: ItemAccountBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = getItem(position)
        holder.binding.apply {
            description.text = row.account.name
            amount.showNet(row.balance)
            meta.text = root.context.getString(R.string.opening_balance_value, Money.format(row.account.openingBalanceMinor))
            root.setOnClickListener { history(row.account.id) }
            editAccount.contentDescription = root.context.getString(R.string.edit_account_named, row.account.name)
            editAccount.setOnClickListener { edit(row.account.id) }
        }
    }
}
