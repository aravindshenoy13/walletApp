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

fun AppCompatActivity.applyInsets(root: View) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
        view.setPadding(bars.left, bars.top, bars.right, max(bars.bottom, keyboard.bottom))
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

fun View.enableTree(enabled: Boolean) {
    isEnabled = enabled
    if (this is ViewGroup) for (index in 0 until childCount) getChildAt(index).enableTree(enabled)
}
