package com.aravind.wallet.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.aravind.wallet.databinding.ActivityDateRangeBinding
import java.time.LocalDate

class DateRangeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDateRangeBinding
    private lateinit var start: LocalDate
    private lateinit var end: LocalDate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDateRangeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)
        start = LocalDate.parse(savedInstanceState?.getString("start") ?: intent.getStringExtra("start") ?: LocalDate.now().withDayOfMonth(1).toString())
        end = LocalDate.parse(savedInstanceState?.getString("end") ?: intent.getStringExtra("end") ?: LocalDate.now().toString())
        binding.back.setOnClickListener { finish() }
        binding.startDate.setOnClickListener { chooseDate(start) { start = it; render() } }
        binding.endDate.setOnClickListener { chooseDate(end) { end = it; render() } }
        binding.apply.setOnClickListener {
            if (end < start) binding.error.isVisible = true
            else {
                setResult(RESULT_OK, Intent().putExtra("start", start.toString()).putExtra("end", end.toString()))
                finish()
            }
        }
        render()
    }
    private fun render() {
        binding.startDate.text = start.display()
        binding.endDate.text = end.display()
        binding.error.isVisible = false
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("start", start.toString()); outState.putString("end", end.toString())
        super.onSaveInstanceState(outState)
    }
}
