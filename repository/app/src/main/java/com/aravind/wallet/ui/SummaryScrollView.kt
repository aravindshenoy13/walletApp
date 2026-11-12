package com.aravind.wallet.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView

/** Only scrolls when scaled text or long exact totals would otherwise hide record actions. */
class SummaryScrollView(context: Context, attrs: AttributeSet) : ScrollView(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val mode = MeasureSpec.getMode(heightMeasureSpec)
        val bounded = if (mode == MeasureSpec.UNSPECIFIED) heightMeasureSpec else
            MeasureSpec.makeMeasureSpec((MeasureSpec.getSize(heightMeasureSpec) * 0.65f).toInt(), MeasureSpec.AT_MOST)
        super.onMeasure(widthMeasureSpec, bounded)
    }
}
