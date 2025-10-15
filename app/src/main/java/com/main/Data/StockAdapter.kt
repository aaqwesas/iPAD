package com.main.Data

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ipad.R
import com.main.Component.StockItemView

class StockAdapter(
    private val stocks: MutableList<Stock> = mutableListOf(),
    private val onItemClick: ((Stock) -> Unit)? = null
) : RecyclerView.Adapter<StockAdapter.StockViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun updateStocks(newStocks: List<Stock>) {
        stocks.clear()
        stocks.addAll(newStocks)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val stockItemView = StockItemView(parent.context)
        return StockViewHolder(stockItemView)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        holder.bind(stocks[position])
    }

    override fun getItemCount(): Int = stocks.size

    inner class StockViewHolder(
        private val stockItemView: StockItemView
    ) : RecyclerView.ViewHolder(stockItemView) {

        fun bind(stock: Stock) {
            stockItemView.setStock(stock)

            onItemClick?.let { listener ->
                stockItemView.setOnClickListener {
                    listener(stock)
                }
            }
        }
    }
}