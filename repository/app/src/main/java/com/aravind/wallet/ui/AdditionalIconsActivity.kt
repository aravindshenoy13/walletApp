package com.aravind.wallet.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.aravind.wallet.domain.CategoryCatalog

class AdditionalIconsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var selected = savedInstanceState?.getString("icon") ?: intent.getStringExtra("icon").orEmpty()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        setContentView(root); applyInsets(root)
        root.addView(Button(this).apply { text = "Back · Additional Icons"; setOnClickListener { finish() } })
        val grid = GridLayout(this).apply { columnCount = 3 }
        root.addView(ScrollView(this).apply { addView(grid) }, LinearLayout.LayoutParams(-1, 0, 1f))
        val checks = mutableListOf<CheckBox>()
        CategoryCatalog.icons.drop(6).forEach { choice ->
            val cell = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; layoutParams = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply { width = 0 } }
            cell.addView(ImageView(this).apply { showCategory(choice.key, intent.getStringExtra("color") ?: "Sky") }, LinearLayout.LayoutParams(64, 64))
            val check = CheckBox(this).apply { text = choice.label; isChecked = selected == choice.key; tag = choice.key }
            checks.add(check); cell.addView(check); grid.addView(cell)
            check.setOnClickListener { selected = choice.key; intent.putExtra("icon", selected); checks.forEach { it.isChecked = it.tag == selected } }
        }
        root.addView(Button(this).apply { text = "Use selected icon"; setOnClickListener { if (CategoryCatalog.icons.any { it.key == selected }) { setResult(RESULT_OK, Intent().putExtra("icon", selected)); finish() } } })
    }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("icon", intent.getStringExtra("icon")); super.onSaveInstanceState(outState) }
}
