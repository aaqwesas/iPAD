package com.main.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ipad.R
import com.main.Data.Stock
import com.main.Data.StockAdapter
import com.main.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StockAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var txtPortfolioValue: TextView
    private lateinit var txtDailyPL: TextView
    private lateinit var portfolioBox: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        recyclerView = view.findViewById(R.id.recycler_view_stocks)
        progressBar = view.findViewById(R.id.progress_bar)
        txtPortfolioValue = view.findViewById(R.id.txt_portfolio_value)
        txtDailyPL = view.findViewById(R.id.txt_daily_pl)
        portfolioBox = view.findViewById(R.id.portfolio_box)

        setupPortfolioBox()
        loadStockData()

        return view
    }

    private fun setupPortfolioBox() {
        // Set initial portfolio data
        updatePortfolioData("$12,456.78", "+$245.67 (2.01%)")

        // Set click listener for portfolio box
        portfolioBox.setOnClickListener {
            showPortfolioDialog()
        }
    }

    private fun updatePortfolioData(totalValue: String, dailyPL: String) {
        txtPortfolioValue.text = totalValue
        txtDailyPL.text = dailyPL

        // Set color based on profit/loss (green for positive, red for negative)
        val color = if (dailyPL.startsWith("+")) {
            resources.getColor(android.R.color.holo_green_dark, null)
        } else {
            resources.getColor(android.R.color.holo_red_dark, null)
        }
        txtDailyPL.setTextColor(color)
    }

    private fun showPortfolioDialog() {
        val portfolioFragment = PortfolioFragment()
        portfolioFragment.show(parentFragmentManager, "PortfolioDialog")
    }

    private fun setupRecyclerView(tickerToNameMap: Map<String, String>) {
        adapter = StockAdapter { stock ->
            val companyName = tickerToNameMap[stock.ticker] ?: stock.ticker

            // Pass BOTH ticker and name!
            val detailFragment = CandleFragment.newInstance(stock.ticker, companyName)

            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.fragment_container, detailFragment)
                .addToBackStack("portfolio_detail")
                .commit()
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@HomeFragment.adapter
        }
    }

    private fun loadStockData() {
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val response = RetrofitClient.apiService.getStocks()
            if (response.isSuccessful) {

                val apiStocks = response.body() ?: emptyList()

                val stocks = apiStocks.map { apiStock ->
                    Stock(
                        ticker = apiStock.symbol,
                        price = apiStock.price,
                        change = apiStock.change,
                        changePercent = apiStock.change_percent,
                        volume = apiStock.volume
                    )
                }

                // Create mapping: ticker → name
                val tickerToNameMap = apiStocks.associate { it.symbol to it.name }

                withContext(Dispatchers.Main) {
                    setupRecyclerView(tickerToNameMap)
                    adapter.updateStocks(stocks)
                    progressBar.visibility = View.GONE
                }
            } else {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    // Handle error (show message, retry, etc.)
                }
            }
        }
    }
}