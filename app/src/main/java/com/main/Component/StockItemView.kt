package com.main.Component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.example.ipad.R
import com.main.Data.Stock
import java.util.Locale
import kotlin.math.abs

class StockItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val view = LayoutInflater.from(context).inflate(R.layout.stock_item_view, this, true)

    private val tvStockTicker = view.findViewById<android.widget.TextView>(R.id.tv_stock_ticker)
    private val tvStockName = view.findViewById<android.widget.TextView>(R.id.tv_stock_name)
    private val tvStockPrice = view.findViewById<android.widget.TextView>(R.id.tv_stock_price)
    private val tvStockChange = view.findViewById<android.widget.TextView>(R.id.tv_stock_change)
    private val tvStockChangePercent = view.findViewById<android.widget.TextView>(R.id.tv_stock_change_percent)

    fun setStock(stock: Stock) {
        tvStockTicker.text = stock.ticker
        tvStockPrice.text = context.getString(R.string.stock_price_format, stock.price)

        val changeText = String.format(Locale.US, "%.2f", abs(stock.change))
        val changePercentText = String.format(Locale.US, "%.2f%%", abs(stock.changePercent))

        if (stock.change >= 0) {
            tvStockChange.text = context.getString(R.string.stock_change_positive, changeText)
            tvStockChangePercent.text = context.getString(R.string.stock_change_percent_positive, changePercentText)
            tvStockChange.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            tvStockChangePercent.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
        } else {
            tvStockChange.text = context.getString(R.string.stock_change_negative, changeText)
            tvStockChangePercent.text = context.getString(R.string.stock_change_percent_negative, changePercentText)
            tvStockChange.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            tvStockChangePercent.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
        }
    }
}