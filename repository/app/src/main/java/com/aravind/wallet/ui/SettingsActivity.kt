package com.aravind.wallet.ui

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.aravind.wallet.WalletApplication
import com.aravind.wallet.R
import com.aravind.wallet.data.WalletBackup
import kotlinx.coroutines.*

class SettingsActivity : AppCompatActivity() {
    private val repository get() = (application as WalletApplication).repository
    private lateinit var actions: LinearLayout
    private var busy = false
    private val save = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) work {
            try {
                withContext(Dispatchers.IO) {
                    val text = WalletBackup.encode(repository.snapshot())
                    requireNotNull(contentResolver.openOutputStream(uri, "w")).bufferedWriter(Charsets.UTF_8).use { it.write(text) }
                }
                message("Full backup saved.")
            } catch (e: Exception) {
                withContext(NonCancellable + Dispatchers.IO) { runCatching { android.provider.DocumentsContract.deleteDocument(contentResolver, uri) } }
                throw e
            }
        }
    }
    private val restore = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) work {
            val backup = withContext(Dispatchers.IO) {
                val bytes = requireNotNull(contentResolver.openInputStream(uri)).use { stream -> val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192); var total = 0; while (true) { val n = stream.read(buffer); if (n < 0) break; total += n; require(total <= 20 * 1024 * 1024) { "Backup exceeds the 20 MB limit." }; out.write(buffer, 0, n) }; out.toByteArray() }
                require(bytes.size <= 20 * 1024 * 1024) { "Backup exceeds the 20 MB limit." }
                WalletBackup.decode(bytes.toString(Charsets.UTF_8))
            }
            AlertDialog.Builder(this).setTitle("Restore backup?")
                .setMessage("${backup.accounts.size} accounts, ${backup.categories.size} categories, ${backup.records.size} transactions. Restore requires an empty wallet.")
                .setNegativeButton("Cancel", null).setPositiveButton("Restore") { _, _ -> work { withContext(Dispatchers.IO) { repository.restore(backup) }; message("Backup restored.") } }.show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actions = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(16)) }
        val scroll = ScrollView(this).apply { addView(actions) }; setContentView(scroll); applyInsets(scroll)
        val header = LinearLayout(this).apply { gravity = android.view.Gravity.CENTER_VERTICAL }
        header.addView(Button(this, null, 0, R.style.PlainButton).apply {
            text = "Back"; setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_back, 0, 0, 0)
            setOnClickListener { finish() }
        })
        header.addView(TextView(this).apply { text = "Settings"; textSize = 20f; setPadding(dp(8), 0, 0, 0) })
        actions.addView(header)
        actions.addView(android.view.View(this).apply { setBackgroundColor(getColor(R.color.line)) }, LinearLayout.LayoutParams(-1, dp(1)))
        val prefs = getSharedPreferences("appearance", MODE_PRIVATE)
        actions.addView(CheckBox(this).apply {
            text = "Dark mode"; isChecked = prefs.getBoolean("dark", false)
            setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean("dark", checked).apply(); AppCompatDelegate.setDefaultNightMode(if (checked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO) }
        })
        button("Export CSV") { startActivity(Intent(this, ExportActivity::class.java)) }
        button("Back up data") { save.launch("wallet-backup-${java.time.LocalDate.now()}.json") }
        button("Restore backup") { restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
    }
    private fun button(label: String, clicked: () -> Unit) { actions.addView(Button(this, null, 0, R.style.OutlineButton).apply { text = label; isAllCaps = false; gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
        val icon = when (label) { "Export CSV" -> com.aravind.wallet.R.drawable.ic_expense; "Back up data" -> com.aravind.wallet.R.drawable.ic_save; "Restore backup" -> com.aravind.wallet.R.drawable.ic_back; else -> com.aravind.wallet.R.drawable.ic_back }
        setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
        compoundDrawablePadding = dp(8); setTextColor(getColor(R.color.ink))
        setOnClickListener { clicked() } }) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun message(text: String) { AlertDialog.Builder(this).setMessage(text).setPositiveButton("OK", null).show() }
    private fun work(block: suspend () -> Unit) {
        if (busy) return
        busy = true; actions.enableTree(false)
        lifecycleScope.launch {
            try { block() } catch (e: CancellationException) { throw e } catch (e: Exception) { message(e.message ?: "Could not complete this operation.") }
            finally { busy = false; actions.enableTree(true) }
        }
    }
}
