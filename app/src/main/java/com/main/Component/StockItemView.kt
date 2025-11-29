package com.main.Component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
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

    init {

    }

    private val tvStockTicker: TextView
    private val tvStockName: TextView
    private val tvStockPrice: TextView
    private val tvStockChange: TextView
    private val tvStockChangePercent: TextView

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.stock_item_view, this, true)
        tvStockTicker = view.findViewById(R.id.tv_stock_ticker)
        tvStockName = view.findViewById(R.id.tv_stock_name)
        tvStockPrice = view.findViewById(R.id.tv_stock_price)
        tvStockChange = view.findViewById(R.id.tv_stock_change)
        tvStockChangePercent = view.findViewById(R.id.tv_stock_change_percent)
    }
    fun setStock(stock: Stock) {
        tvStockTicker.text = stock.ticker
        tvStockName.text = stock.stockName
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