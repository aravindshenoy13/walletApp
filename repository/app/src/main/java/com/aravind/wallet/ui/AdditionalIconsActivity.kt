package com.aravind.wallet.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import com.aravind.wallet.R
import com.aravind.wallet.domain.CategoryCatalog

class AdditionalIconsActivity : AppCompatActivity() {
    private var selected = ""
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selected = savedInstanceState?.getString("icon") ?: intent.getStringExtra("icon").orEmpty()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        setContentView(root); applyInsets(root)
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; minimumHeight = dp(56); setPadding(dp(8), 0, dp(8), 0) }
        header.addView(Button(this, null, 0, R.style.PlainButton).apply {
            setText(R.string.back); setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_back, 0, 0, 0)
            setOnClickListener { finish() }
        })
        header.addView(TextView(this).apply {
            setText(R.string.additional_icons); textSize = 20f; setTextColor(getColor(R.color.ink))
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(header)
        root.addView(View(this).apply { setBackgroundColor(getColor(R.color.ink)) }, LinearLayout.LayoutParams(-1, dp(1)))
        val grid = GridLayout(this).apply { columnCount = 3; setPadding(dp(12), dp(12), dp(12), dp(12)) }
        root.addView(ScrollView(this).apply { addView(grid) }, LinearLayout.LayoutParams(-1, 0, 1f))
        val cells = mutableMapOf<String, View>()
        fun update() { cells.forEach { (key, cell) ->
            cell.isSelected = key == selected
            ViewCompat.setStateDescription(cell, if (key == selected) "Selected" else "Not selected")
        } }
        CategoryCatalog.icons.drop(6).forEach { choice ->
            val cell = FrameLayout(this).apply {
                layoutParams = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply {
                    width = 0; height = dp(72); setMargins(dp(3), dp(3), dp(3), dp(3))
                }
                setBackgroundResource(R.drawable.icon_selection)
                isClickable = true; isFocusable = true
                contentDescription = getString(R.string.icon_accessibility, choice.label)
                setOnClickListener { selected = choice.key; update() }
            }
            cell.addView(ImageView(this).apply {
                showCategory(choice.key, intent.getStringExtra("color") ?: "Sky")
                setPadding(dp(8), dp(8), dp(8), dp(8))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, FrameLayout.LayoutParams(dp(48), dp(48), Gravity.CENTER))
            cells[choice.key] = cell; grid.addView(cell)
        }
        update()
        root.addView(Button(this, null, 0, R.style.OutlineButton).apply {
            setText(R.string.use_selected_icon)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(16), dp(12), dp(16), dp(16)) }
            setOnClickListener { if (CategoryCatalog.icons.any { it.key == selected }) {
                setResult(RESULT_OK, Intent().putExtra("icon", selected)); finish()
            } }
        })
    }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("icon", selected); super.onSaveInstanceState(outState) }
}
