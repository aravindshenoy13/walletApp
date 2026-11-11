package com.aravind.wallet.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aravind.wallet.R
import com.aravind.wallet.data.WalletCategory
import com.aravind.wallet.databinding.ActivityCategoryEditorBinding
import com.aravind.wallet.domain.CategoryCatalog
import com.aravind.wallet.domain.Ledger
import java.util.UUID
import kotlinx.coroutines.launch

class CategoryEditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCategoryEditorBinding
    private val model: WalletViewModel by viewModels()
    private var initialized = false
    private var draft: Bundle? = null
    private var persisted: WalletCategory? = null
    private var categoryId = ""
    private var icon = "tag"
    private var color = "Sky"
    private val iconButtons = mutableMapOf<String, Pair<View, ImageView>>()
    private val colorButtons = mutableMapOf<String, Button>()
    private val isNew get() = intent.getStringExtra("id") == null
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        draft = savedInstanceState?.getBundle("draft")
        categoryId = intent.getStringExtra("id") ?: savedInstanceState?.getString("categoryId") ?: UUID.randomUUID().toString()
        binding = ActivityCategoryEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)
        binding.title.setText(if (isNew) R.string.new_category else R.string.edit_category)
        binding.save.setText(if (isNew) R.string.save_category else R.string.save_changes)
        onBackPressedDispatcher.addCallback(this) { if (model.operation.value != Operation.Working) finish() }
        binding.back.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.name.doAfterTextChanged { updatePreview() }
        createChoices()
        binding.save.setOnClickListener {
            binding.error.isVisible = false
            if (binding.name.text.isNullOrBlank()) {
                binding.name.error = "Enter a category name."
                binding.name.requestFocus()
            } else model.saveCategory(WalletCategory(categoryId, binding.name.text.toString(), icon, color), isNew)
        }
        binding.archive.setOnClickListener {
            val saved = persisted ?: return@setOnClickListener
            AlertDialog.Builder(this).setTitle(getString(R.string.archive_title, saved.name))
                .setMessage(R.string.archive_message).setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.archive) { _, _ -> model.archiveCategory(saved.id) }.show()
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { model.ledger.collect { state ->
                    if (state is LedgerState.Ready && !initialized) initialize(state.ledger)
                    if (state == LedgerState.Failed) {
                        binding.loading.isVisible = false
                        binding.form.isVisible = true
                        showError(getString(R.string.load_failed))
                        updateEnabled()
                    }
                } }
                launch { model.operation.collect { operation ->
                    if (operation == Operation.Saved) finish()
                    if (operation is Operation.Failed) showError(operation.message ?: getString(R.string.save_failed))
                    updateEnabled()
                } }
            }
        }
    }

    private fun initialize(ledger: Ledger) {
        persisted = ledger.categoriesById[categoryId]
        if (!isNew && persisted == null) { finish(); return }
        icon = draft?.getString("icon") ?: persisted?.iconKey ?: "tag"
        color = draft?.getString("color") ?: persisted?.backgroundColorKey ?: "Sky"
        binding.name.setText(draft?.getString("name") ?: persisted?.name.orEmpty())
        val origin = getString(if (persisted?.isBuiltIn == true) R.string.built_in else R.string.custom)
        binding.origin.text = if (persisted?.isArchived == true) getString(R.string.archived_name, origin) else origin
        binding.origin.isVisible = !isNew
        binding.archive.isVisible = !isNew && persisted?.isArchived == false
        binding.archivedHelp.isVisible = persisted?.isArchived == true
        initialized = true
        binding.loading.isVisible = false
        binding.form.isVisible = true
        updatePreview()
        updateEnabled()
    }

    private fun createChoices() {
        CategoryCatalog.icons.forEach { choice ->
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                minimumHeight = dp(88)
                setPadding(dp(4), dp(8), dp(4), dp(8))
                layoutParams = gridParams(88)
                setOnClickListener { icon = choice.key; updatePreview() }
            }
            val badge = ImageView(this).apply { layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)); setPadding(dp(6),dp(6),dp(6),dp(6)); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
            cell.addView(badge)
            cell.addView(TextView(this).apply {
                text = choice.label; gravity = Gravity.CENTER; setTextColor(getColor(R.color.ink)); textSize = 13f
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            })
            binding.iconChoices.addView(cell)
            iconButtons[choice.key] = cell to badge
        }
        binding.colorChoices.columnCount = if (resources.configuration.screenWidthDp >= 600) 8 else 4
        CategoryCatalog.colors.forEach { choice ->
            val button = Button(this).apply {
                backgroundTintList = null
                layoutParams = gridParams(48)
                minWidth = 0; minimumWidth = 0; setPadding(0,0,0,0)
                setTextColor(Color.BLACK); textSize = 18f
                setOnClickListener { color = choice.key; updatePreview() }
            }
            binding.colorChoices.addView(button)
            colorButtons[choice.key] = button
        }
    }

    private fun gridParams(height: Int) = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply {
        width = 0; this.height = dp(height); setMargins(dp(3),dp(3),dp(3),dp(3))
    }

    private fun updatePreview() {
        if (!::binding.isInitialized) return
        iconButtons.forEach { (key, views) ->
            val selected = key == icon
            views.first.isSelected = selected
            views.first.contentDescription = selectedDescription(getString(R.string.icon_accessibility, CategoryCatalog.icons.first { it.key == key }.label), selected)
            views.first.background = GradientDrawable().apply { setColor(Color.WHITE); setStroke(dp(if (selected) 2 else 1), getColor(if (selected) R.color.ink else R.color.line)) }
            views.second.showCategory(key, color)
        }
        colorButtons.forEach { (key, button) ->
            val selected = key == color
            button.text = if (selected) "✓" else ""
            button.isSelected = selected
            button.contentDescription = selectedDescription(getString(R.string.color_accessibility, key), selected)
            button.background = GradientDrawable().apply {
                setColor(Color.parseColor(CategoryCatalog.colors.first { it.key == key }.hex))
                setStroke(dp(if (selected) 3 else 1), getColor(if (selected) R.color.ink else R.color.quiet))
            }
        }
        binding.previewBadge.showCategory(icon, color)
        binding.previewName.text = binding.name.text?.toString()?.ifBlank { getString(R.string.category_preview) }
        binding.selectionStatus.text = getString(R.string.appearance_status, CategoryCatalog.icons.first { it.key == icon }.label, color)
    }

    private fun selectedDescription(label: String, selected: Boolean) = if (selected) getString(R.string.selected_accessibility, label) else label
    private fun updateEnabled() {
        val busy = model.operation.value == Operation.Working || model.operation.value == Operation.Saved
        binding.form.enableTree(!busy)
        binding.back.isEnabled = !busy
        binding.save.isEnabled = initialized && !busy
        binding.archive.isEnabled = initialized && !busy
    }
    private fun showError(message: String) {
        binding.error.text = message
        binding.error.isVisible = true
        binding.error.post { binding.scroll.smoothScrollTo(0, binding.error.bottom) }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("categoryId", categoryId)
        outState.putBundle("draft", if (initialized) Bundle().apply {
            putString("name", binding.name.text.toString()); putString("icon", icon); putString("color", color)
        } else draft)
        super.onSaveInstanceState(outState)
    }
}
