package com.aravind.wallet.ui

import android.app.DatePickerDialog
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.aravind.wallet.R
import com.aravind.wallet.domain.Money
import java.math.BigInteger
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

fun AppCompatActivity.applyInsets(root: View, onKeyboardVisibilityChanged: ((Boolean) -> Unit)? = null) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
        view.setPadding(bars.left, bars.top, bars.right, max(bars.bottom, keyboard.bottom))
        val keyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
        onKeyboardVisibilityChanged?.invoke(keyboardVisible)
        insets
    }
    ViewCompat.requestApplyInsets(root)
}

fun AppCompatActivity.chooseDate(date: LocalDate, selected: (LocalDate) -> Unit) {
    DatePickerDialog(this, { _, year, month, day -> selected(LocalDate.of(year, month + 1, day)) },
        date.year, date.monthValue - 1, date.dayOfMonth).show()
}

fun LocalDate.display(): String = format(DateTimeFormatter.ofPattern("dd MMM uuuu", Locale.getDefault()))

fun TextView.showNet(value: BigInteger) {
    text = Money.format(value)
    setTextColor(context.getColor(when (value.signum()) {
        1 -> R.color.income_amount
        -1 -> R.color.expense_amount
        else -> R.color.ink
    }))
}

fun TextView.showBalance(value: BigInteger) {
    text = Money.format(value)
    setTextColor(context.getColor(if (value.signum() < 0) R.color.expense_amount else R.color.ink))
}

fun TextView.showAccountIcon(key: String) {
    setCompoundDrawablesRelativeWithIntrinsicBounds(when (key) {
        "cash" -> R.drawable.ic_cash; "card" -> R.drawable.ic_card; else -> R.drawable.ic_bank
    }, 0, 0, 0)
    androidx.core.widget.TextViewCompat.setCompoundDrawableTintList(this,
        android.content.res.ColorStateList.valueOf(context.getColor(R.color.icon_stroke)))
    compoundDrawablePadding = (6 * resources.displayMetrics.density).toInt()
}

fun View.enableTree(enabled: Boolean) {
    isEnabled = enabled
    if (this is ViewGroup) for (index in 0 until childCount) getChildAt(index).enableTree(enabled)
}
