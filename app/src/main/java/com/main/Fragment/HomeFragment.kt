package com.main.Fragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.widget.TextView
import android.widget.Toast
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
    private lateinit var txtPortfolioValue: TextView
    private lateinit var txtDailyPL: TextView
    private lateinit var portfolioBox: View
    private lateinit var sharedPreferences: SharedPreferences
    private val symbolToNameMap = mutableMapOf<String, String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        recyclerView = view.findViewById(R.id.recycler_view_stocks)
        txtPortfolioValue = view.findViewById(R.id.txt_portfolio_value)
        txtDailyPL = view.findViewById(R.id.txt_daily_pl)
        portfolioBox = view.findViewById(R.id.portfolio_box)

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("TokenPrefs", Context.MODE_PRIVATE)
        print((sharedPreferences))
        setupPortfolioBox()
        loadStockData()
        loadPortfolioData() // Load actual portfolio data

        return view
    }

    private fun setupPortfolioBox() {
        // Set initial portfolio data - will be updated when real data loads
        updatePortfolioData("0.00%", "Today")

        // Set click listener for portfolio box
        portfolioBox.setOnClickListener {
            showPortfolioDialog()
        }
    }

    private fun loadPortfolioData() {
        val token = sharedPreferences.getString("user_email", null)
            ?:
            return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val portfolioResponse = RetrofitClient.apiService.getPortfolioPercentageChange(token)

                withContext(Dispatchers.Main) {
                    if (portfolioResponse.isSuccessful) {
                        val portfolioValue = portfolioResponse.body()
                        // Format as percentage (you might want to calculate actual percentage change)
                        updatePortfolioData("${String.format("%.2f", portfolioValue)}%", "Today")
                    } else {
                        // Keep default values if API fails
                        updatePortfolioData("0.00%", "Today")
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Failed to load portfolio data", e)
                // Keep default values on error
            }
        }
    }

    private fun updatePortfolioData(performance: String, timePeriod: String) {
        txtPortfolioValue.text = performance
        txtDailyPL.text = timePeriod

        // Set color based on performance (green for positive, red for negative)
        val color = if (performance.startsWith("-")) {
            resources.getColor(android.R.color.holo_red_dark, null)
        } else {
            resources.getColor(android.R.color.holo_green_dark, null)
        }
        txtPortfolioValue.setTextColor(color)
    }

    private fun showPortfolioDialog() {
        val token = sharedPreferences.getString("user_email", null)
        if (token != null){
            val portfolioFragment = PortfolioFragment.newInstance(token)
            portfolioFragment.show(parentFragmentManager, "PortfolioDialog")
        }else{
            Toast.makeText(context, "Please log in to view portfolio", Toast.LENGTH_SHORT).show()
        }

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

        CoroutineScope(Dispatchers.IO).launch {
            val response = RetrofitClient.apiService.getStocks()
            if (response.isSuccessful) {

                val apiStocks = response.body() ?: emptyList()

                apiStocks.forEach { apiStock ->
                    try {
                        val nameResponse = RetrofitClient.apiService.get_company_name(apiStock.symbol)
                        if (nameResponse.isSuccessful) {
                            val companyName = nameResponse.body()?.companyName ?: apiStock.symbol // fallback to ticker
                            symbolToNameMap[apiStock.symbol] = companyName
                        } else {
                            symbolToNameMap[apiStock.symbol] = apiStock.symbol // fallback
                        }
                    } catch (e: Exception) {
                        symbolToNameMap[apiStock.symbol] = apiStock.symbol // fallback on error
                    }
                }

                val stocks = apiStocks.map { apiStock ->
                    Stock(
                        ticker = apiStock.symbol,
                        stockName = symbolToNameMap[apiStock.symbol] ?: apiStock.symbol,
                        price = apiStock.price,
                        change = apiStock.change,
                        changePercent = apiStock.change_percent,
                        volume = apiStock.volume
                    )
                }

                // Create mapping: ticker → name
//                val tickerToNameMap = apiStocks.associate { it.symbol to it.name }

                val tickerToNameMap = symbolToNameMap

                withContext(Dispatchers.Main) {
                    setupRecyclerView(tickerToNameMap)
                    adapter.updateStocks(stocks)
                }
            }
        }
    }
}