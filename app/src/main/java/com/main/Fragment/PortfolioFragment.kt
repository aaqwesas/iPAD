package com.main.Fragment

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ipad.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.main.Data.UserHoldingResponse
import com.main.api.RetrofitClient
import com.main.models.PortfolioHistoryResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class PortfolioFragment : DialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var txtPortfolioValue: TextView
    private lateinit var txtDailyPL: TextView
    private lateinit var txtPerformanceChange: TextView
    private lateinit var portfolioChart: LineChart
    private lateinit var adapter: PortfolioAdapter

    companion object {
        const val ARG_TOKEN = "token"

        fun newInstance(token: String): PortfolioFragment {
            val fragment = PortfolioFragment()
            val args = Bundle()
            args.putString(ARG_TOKEN, token)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_portfolio, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val token = arguments?.getString(ARG_TOKEN)
        if (token == null) {
            Log.e("PortfolioFragment", "Token is null!")
            dismiss()
            return
        }

        initViews(view)
        setupRecyclerView()
        setupChart()
        loadPortfolioData(token)
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recycler_view_portfolio)
        txtPortfolioValue = view.findViewById(R.id.txt_portfolio_value)
        txtDailyPL = view.findViewById(R.id.txt_daily_pl)
        txtPerformanceChange = view.findViewById(R.id.txt_performance_change)
        portfolioChart = view.findViewById(R.id.portfolio_chart)

        val closeButton = view.findViewById<ImageButton>(R.id.btn_close)
        closeButton.setOnClickListener { dismiss() }
    }

    private fun setupRecyclerView() {
        adapter = PortfolioAdapter(emptyList())
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@PortfolioFragment.adapter
        }
    }
    private fun setupChart() {
        portfolioChart.apply {
            // Clear any previous data
            clear()

            // Basic configuration
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            setNoDataText("Loading portfolio history...")
            setNoDataTextColor(Color.GRAY)

            // X Axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = Color.GRAY
                textSize = 10f
            }

            // Y Axis
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E0E0E0")
                textColor = Color.GRAY
                textSize = 10f
            }

            // Disable right axis
            axisRight.isEnabled = false
            legend.isEnabled = false
        }
    }

    private fun loadPortfolioData(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("PortfolioFragment", "Loading portfolio data...")

                val portfolioResponse = RetrofitClient.apiService.getPortfolioValue(token)
                val holdingsResponse = RetrofitClient.apiService.getUserHoldings(token)
                val historyResponse = RetrofitClient.apiService.getPortfolioHistory(token, 30)

                withContext(Dispatchers.Main) {
                    // Handle portfolio value
                    if (portfolioResponse.isSuccessful) {
                        val portfolioValue = portfolioResponse.body()?.value ?: 0f
                        txtPortfolioValue.text = "$${String.format("%.2f", portfolioValue)}"
                    } else {
                        txtPortfolioValue.text = "$0.00"
                    }

                    // Handle holdings
                    if (holdingsResponse.isSuccessful) {
                        val holdings = holdingsResponse.body() ?: emptyList()
                        adapter.updateHoldings(holdings)
                        view?.findViewById<TextView>(R.id.txt_holdings_count)?.text = "${holdings.size} stocks"
                        Log.d("PortfolioFragment", "Loaded ${holdings.size} holdings")
                    } else {
                        adapter.updateHoldings(emptyList())
                        view?.findViewById<TextView>(R.id.txt_holdings_count)?.text = "0 stocks"
                    }

                    // Handle history data - CRITICAL PART
                    if (historyResponse.isSuccessful) {
                        val historyData = historyResponse.body()
                        Log.d("PortfolioFragment", "History API success: ${historyData?.size ?: 0} items")

                        if (!historyData.isNullOrEmpty()) {
                            updateChartWithData(historyData)
                        } else {
                            Log.d("PortfolioFragment", "History data is empty")
                            showEmptyChart("No portfolio history available")
                        }
                    } else {
                        Log.e("PortfolioFragment", "History API error: ${historyResponse.code()}")
                        showEmptyChart("Failed to load history data")
                    }
                }
            } catch (e: Exception) {
                Log.e("PortfolioFragment", "Network error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showEmptyChart("Network error")
                    adapter.updateHoldings(emptyList())
                    txtPortfolioValue.text = "$0.00"
                }
            }
        }
    }

    private fun showEmptyChart(message: String) {
        portfolioChart.clear()
        portfolioChart.setNoDataText(message)
        portfolioChart.invalidate()
    }

    private fun updateChartWithData(historyData: List<PortfolioHistoryResponse>) {
        Log.d("PortfolioFragment", "updateChartWithData called with ${historyData.size} items")

        if (historyData.isEmpty()) {
            showEmptyChart("No data available")
            return
        }

        // Create entries - use data in the order received
        val entries = ArrayList<Entry>()
        val xAxisLabels = ArrayList<String>()

        historyData.forEachIndexed { index, history ->
            entries.add(Entry(index.toFloat(), history.value))

            // Simple labels - use day numbers to avoid timestamp parsing issues
            xAxisLabels.add(if (index == 0) "Start" else "D${index + 1}")

            Log.d("PortfolioFragment", "Entry $index: value=${history.value}")
        }

        // Create dataset with basic styling
        val dataSet = LineDataSet(entries, "Portfolio Value").apply {
            color = Color.parseColor("#4CAF50") // Green line
            lineWidth = 3f
            setDrawCircles(true)
            circleRadius = 4f
            setCircleColor(Color.parseColor("#4CAF50"))
            setDrawValues(false) // Don't show values on points
            setDrawFilled(true) // Fill area under line
            fillColor = Color.parseColor("#804CAF50") // Semi-transparent green
            fillAlpha = 100
            mode = LineDataSet.Mode.LINEAR // Use linear instead of cubic for reliability
        }

        // Create line data
        val lineData = LineData(dataSet)
        lineData.setValueTextSize(11f)

        // Apply to chart
        portfolioChart.apply {
            // Clear previous data
            clear()

            // Set new data
            data = lineData

            // Configure X axis with labels
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(xAxisLabels)
                labelCount = minOf(6, xAxisLabels.size)
                granularity = 1f
                setCenterAxisLabels(false)
            }

            // Configure Y axis to auto-scale with padding
            axisLeft.apply {
                resetAxisMinimum()
                resetAxisMaximum()
                // Add some padding to prevent clipping
                setSpaceTop(15f)
                setSpaceBottom(15f)
            }

            fitScreen()
            setVisibleXRangeMaximum(historyData.size.toFloat())

            // Refresh chart
            notifyDataSetChanged()
            invalidate()

            // Animate
            animateX(1000)
            animateY(1000)
        }

        Log.d("PortfolioFragment", "Chart updated with ${entries.size} entries")

        // Update performance text
        updatePerformanceText(historyData)
    }

    private fun updatePerformanceText(historyData: List<PortfolioHistoryResponse>) {
        if (historyData.size >= 2) {
            val firstValue = historyData.first().value
            val lastValue = historyData.last().value
            val change = ((lastValue - firstValue) / firstValue) * 100
            val changeText = if (change >= 0) "+${String.format("%.2f", change)}%"
            else "${String.format("%.2f", change)}%"

            txtPerformanceChange.text = changeText
            txtDailyPL.text = "${historyData.size} days"

            // Set color based on performance
            val color = if (change >= 0) android.R.color.holo_green_dark
            else android.R.color.holo_red_dark
            txtPerformanceChange.setTextColor(ContextCompat.getColor(requireContext(), color))

            val bgRes = if (change >= 0) R.drawable.bg_positive_pill
            else R.drawable.bg_negative_pill
            txtPerformanceChange.setBackgroundResource(bgRes)

            // Update portfolio value to latest
            txtPortfolioValue.text = "$${String.format("%.2f", lastValue)}"
        } else if (historyData.size == 1) {
            val currentValue = historyData.first().value
            txtPortfolioValue.text = "$${String.format("%.2f", currentValue)}"
            txtPerformanceChange.text = "0.00%"
            txtDailyPL.text = "Today"
        }
    }

    // Portfolio Adapter
    inner class PortfolioAdapter(private var holdings: List<UserHoldingResponse>) :
        RecyclerView.Adapter<PortfolioAdapter.PortfolioViewHolder>() {

        inner class PortfolioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ticker: TextView = itemView.findViewById(R.id.stock_ticker)
            val quantity: TextView = itemView.findViewById(R.id.stock_quantity)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PortfolioViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_portfolio_stock, parent, false)
            return PortfolioViewHolder(view)
        }

        override fun onBindViewHolder(holder: PortfolioViewHolder, position: Int) {
            val holding = holdings[position]
            holder.ticker.text = holding.stock_ticker
            holder.quantity.text = "Quantity: ${String.format("%.2f", holding.quantity)}"
        }

        override fun getItemCount(): Int = holdings.size

        fun updateHoldings(newHoldings: List<UserHoldingResponse>) {
            holdings = newHoldings
            notifyDataSetChanged()
        }
    }
}