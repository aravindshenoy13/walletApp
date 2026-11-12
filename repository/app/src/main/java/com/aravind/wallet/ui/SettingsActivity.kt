package com.aravind.wallet.ui

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.lifecycle.repeatOnLifecycle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.aravind.wallet.R
import kotlinx.coroutines.*

class SettingsActivity : AppCompatActivity() {
    private val model: BackupViewModel by viewModels()
    private lateinit var actions: LinearLayout
    private var dialog: AlertDialog? = null
    private var dialogState: BackupState? = null
    private val save = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { model.selected(it, false) }
    private val restore = registerForActivityResult(ActivityResultContracts.OpenDocument()) { model.selected(it, true) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actions = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)) }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; setContentView(root); applyInsets(root)
        val header = LinearLayout(this).apply { gravity = android.view.Gravity.CENTER_VERTICAL; minimumHeight = dp(56); setPadding(dp(8), 0, dp(8), 0) }
        header.addView(Button(this, null, 0, R.style.PlainButton).apply {
            compoundDrawablePadding = dp(4)
            text = "Back"; setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_back, 0, 0, 0)
            setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        })
        header.addView(TextView(this, null, 0, R.style.ScreenTitle).apply {
            text = "Settings"; setPadding(dp(8), dp(8), 0, dp(8))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(header)
        root.addView(android.view.View(this).apply { setBackgroundColor(getColor(R.color.ink)) }, LinearLayout.LayoutParams(-1, dp(1)))
        root.addView(ScrollView(this).apply { addView(actions) }, LinearLayout.LayoutParams(-1, 0, 1f))
        val prefs = getSharedPreferences("appearance", MODE_PRIVATE)
        actions.addView(CheckBox(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = 0 }
            setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_dark_mode, 0, 0, 0)
            compoundDrawablePadding = dp(8)
            text = "Dark mode"; isChecked = prefs.getBoolean("dark", false)
            setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean("dark", checked).apply(); AppCompatDelegate.setDefaultNightMode(if (checked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO) }
        })
        button("Export CSV") { startActivity(Intent(this, ExportActivity::class.java)) }
        button("Back up data") { if (model.choose(false)) {
            try { save.launch("wallet-backup-${java.time.LocalDate.now()}.json") }
            catch (_: RuntimeException) { model.pickerFailed() }
        } }
        button("Restore backup") { if (model.choose(true)) {
            try { restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
            catch (_: RuntimeException) { model.pickerFailed() }
        } }
        onBackPressedDispatcher.addCallback(this) { if (model.state.value != BackupState.Working) finish() }
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                model.state.collect { state ->
                    actions.enableTree(state == BackupState.Idle)
                    if (dialogState != state) {
                        dialog?.dismiss()
                        dialog = null
                        dialogState = state
                        dialog = when (state) {
                            is BackupState.Confirm -> AlertDialog.Builder(this@SettingsActivity)
                                .setTitle("Restore backup?")
                                .setMessage("${state.ledger.accounts.size} accounts, ${state.ledger.categories.size} categories, ${state.ledger.records.size} transactions. Restore requires an empty wallet with untouched built-in categories.")
                                .setNegativeButton("Cancel") { _, _ -> model.dismiss() }
                                .setPositiveButton("Restore") { _, _ -> model.confirm() }
                                .setOnCancelListener { model.dismiss() }.show()
                            is BackupState.Result -> AlertDialog.Builder(this@SettingsActivity)
                                .setMessage(state.message).setPositiveButton("OK") { _, _ -> model.dismiss() }
                                .setOnCancelListener { model.dismiss() }.show()
                            else -> null
                        }
                    }
                }
            }
        }
    }
    private fun button(label: String, clicked: () -> Unit) { actions.addView(Button(this, null, 0, R.style.OutlineButton).apply { text = label; isAllCaps = false; gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
        val icon = when (label) { "Export CSV" -> com.aravind.wallet.R.drawable.ic_export; "Back up data" -> com.aravind.wallet.R.drawable.ic_save; "Restore backup" -> com.aravind.wallet.R.drawable.ic_restore; else -> com.aravind.wallet.R.drawable.ic_back }
        setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
        compoundDrawablePadding = dp(8); setTextColor(getColor(R.color.ink))
        setOnClickListener { clicked() } }) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun onDestroy() {
        dialog?.dismiss()
        super.onDestroy()
    }
}
