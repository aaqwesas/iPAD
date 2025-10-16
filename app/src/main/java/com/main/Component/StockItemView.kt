package com.main.Component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.example.ipad.R
import com.main.Data.Stock

class StockItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val view = LayoutInflater.from(context).inflate(R.layout.stock_item_view, this, true)

    private val ivStockIcon = view.findViewById<android.widget.ImageView>(R.id.iv_stock_icon)
    private val tvStockTicker = view.findViewById<android.widget.TextView>(R.id.tv_stock_ticker)
    private val tvStockName = view.findViewById<android.widget.TextView>(R.id.tv_stock_name)
    private val tvStockPrice = view.findViewById<android.widget.TextView>(R.id.tv_stock_price)
    private val tvStockChange = view.findViewById<android.widget.TextView>(R.id.tv_stock_change)
    private val tvStockChangePercent = view.findViewById<android.widget.TextView>(R.id.tv_stock_change_percent)

    fun setStock(stock: Stock) {
        tvStockTicker.text = stock.ticker
        tvStockName.text = stock.name
        tvStockPrice.text = "$${String.format("%.2f", stock.price)}"

        val changeText = String.format("%.2f", Math.abs(stock.change))
        val changePercentText = String.format("%.2f%%", Math.abs(stock.changePercent))

        if (stock.change >= 0) {
            tvStockChange.text = "+$changeText"
            tvStockChangePercent.text = "(+$changePercentText)"
            tvStockChange.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            tvStockChangePercent.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
        } else {
            tvStockChange.text = "-$changeText"
            tvStockChangePercent.text = "(-$changePercentText)"
            tvStockChange.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            tvStockChangePercent.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
        }

        if (stock.iconResId != 0) {
            ivStockIcon.setImageResource(stock.iconResId)
        }
    }
}