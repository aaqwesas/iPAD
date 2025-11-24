package com.main.Fragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ipad.R
import com.main.Data.UserHoldingResponse
import com.main.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PortfolioFragment : DialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var txtPortfolioValue: TextView
    private lateinit var txtDailyPL: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var adapter: PortfolioAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_portfolio, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("TokenPrefs", Context.MODE_PRIVATE)

        // Find views
        recyclerView = view.findViewById(R.id.recycler_view_portfolio)
        txtPortfolioValue = view.findViewById(R.id.txt_portfolio_value)
        txtDailyPL = view.findViewById(R.id.txt_daily_pl)

        // Setup RecyclerView
        setupRecyclerView()

        // Load portfolio data
        loadPortfolioData()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupRecyclerView() {
        adapter = PortfolioAdapter(emptyList())
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@PortfolioFragment.adapter
        }
    }

    private fun loadPortfolioData() {
        val token = sharedPreferences.getString("user_email", null)
        if (token == null) {
            // User not logged in, show empty state
            Log.d("PortfolioFragment", "Token is null - user not logged in")
            updatePortfolioData("0.00%", "Today")
            adapter.updateHoldings(emptyList())
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get portfolio value
                val portfolioResponse = RetrofitClient.apiService.getPortfolioValue(token)

                // Get user holdings
                val holdingsResponse = RetrofitClient.apiService.getUserHoldings(token)

                withContext(Dispatchers.Main) {
                    if (portfolioResponse.isSuccessful) {
                        val portfolioValue = portfolioResponse.body()?.value ?: 0.0f
                        updatePortfolioData("${String.format("%.2f", portfolioValue)}%", "Today")
                    } else {
                        updatePortfolioData("0.00%", "Today")
                    }

                    if (holdingsResponse.isSuccessful) {
                        val holdings = holdingsResponse.body() ?: emptyList()
                        adapter.updateHoldings(holdings)

                        if (holdings.isEmpty()) {
                            // Show empty state message if no holdings
                            // You could add a TextView for this in your layout
                        }
                    } else {
                        adapter.updateHoldings(emptyList())
                    }
                }
            } catch (e: Exception) {
                Log.e("PortfolioFragment", "Failed to load portfolio data", e)
                withContext(Dispatchers.Main) {
                    updatePortfolioData("0.00%", "Today")
                    adapter.updateHoldings(emptyList())
                }
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

    // Portfolio Adapter
    inner class PortfolioAdapter(private var holdings: List<UserHoldingResponse>) :
        RecyclerView.Adapter<PortfolioAdapter.PortfolioViewHolder>() {

        inner class PortfolioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ticker: TextView = itemView.findViewById(R.id.stock_ticker)
            val quantity: TextView = itemView.findViewById(R.id.stock_quantity)
            // Add more views if needed (price, change, etc.)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PortfolioViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_portfolio_stock, parent, false)
            return PortfolioViewHolder(view)
        }

        override fun onBindViewHolder(holder: PortfolioViewHolder, position: Int) {
            val holding = holdings[position]
            holder.ticker.text = holding.stock_ticker
            holder.quantity.text = "Quantity: ${holding.quantity}"

            // You can add more data here like current price, percentage change, etc.
        }

        override fun getItemCount(): Int = holdings.size

        fun updateHoldings(newHoldings: List<UserHoldingResponse>) {
            holdings = newHoldings
            notifyDataSetChanged()
        }
    }
}