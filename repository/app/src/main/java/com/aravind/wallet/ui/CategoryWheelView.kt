package com.aravind.wallet.ui

import android.content.Context
import android.graphics.*
import android.view.View
import com.aravind.wallet.R
import com.aravind.wallet.data.WalletCategory
import com.aravind.wallet.domain.CategoryCatalog
import com.aravind.wallet.domain.Money
import java.math.BigInteger
import java.math.BigDecimal
import java.math.MathContext

/** Drawing alone uses floating point; values and totals stay exact. */
class CategoryWheelView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var values = emptyList<Pair<WalletCategory, BigInteger>>()
    fun show(values: List<Pair<WalletCategory, BigInteger>>) {
        this.values = values.filter { it.second.signum() < 0 }
        contentDescription = "Negative net by category. " + this.values.joinToString { "${it.first.name}: ${Money.format(it.second)}" }
        invalidate()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val total = values.fold(BigInteger.ZERO) { sum, row -> sum + row.second.abs() }
        val radius = minOf(width, height) * 0.42f
        val cx = width / 2f; val cy = height / 2f
        val bounds = RectF(cx-radius, cy-radius, cx+radius, cy+radius)
        paint.style = Paint.Style.FILL
        if (total.signum() == 0) {
            paint.color = context.getColor(R.color.line); canvas.drawOval(bounds, paint)
        } else {
            var start = -90f
            values.forEachIndexed { index, (category, value) ->
                val sweep = if (index == values.lastIndex) 270f-start else BigDecimal(value.abs()).divide(BigDecimal(total), MathContext.DECIMAL64).toFloat()*360f
                paint.color = Color.parseColor(CategoryCatalog.colors.first { it.key == category.backgroundColorKey }.hex)
                canvas.drawArc(bounds, start, sweep, true, paint); start += sweep
            }
        }
        paint.color = context.getColor(R.color.paper); canvas.drawCircle(cx,cy,radius*0.55f,paint)
        paint.color = context.getColor(R.color.ink); paint.textAlign = Paint.Align.CENTER; paint.textSize = 14f*resources.displayMetrics.scaledDensity
        canvas.drawText("Negative net",cx,cy,paint)
    }
}
