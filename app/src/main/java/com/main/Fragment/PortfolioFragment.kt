package com.main.Fragment

import android.content.Context
import android.content.SharedPreferences
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
import com.main.models.PortfolioValueResponse // If you still use this elsewhere
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
    // Remove the SharedPreferences declaration as we'll get the token from arguments
    // private lateinit var sharedPreferences: SharedPreferences
    private lateinit var adapter: PortfolioAdapter

    // Define a SimpleDateFormat for parsing the timestamp string
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
        // Adjust timezone if your server sends UTC and you want to display in local
        // timeZone = TimeZone.getTimeZone("UTC")
    }

    companion object {
        // Define a key for the argument
        const val ARG_TOKEN = "token"

        // Helper function to create a new instance with the token
        fun newInstance(token: String?): PortfolioFragment {
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

        // Retrieve the token from arguments
        val token = arguments?.getString(ARG_TOKEN)
        if (token == null) {
            Log.e("PortfolioFragment", "Token is null in arguments!")
            // Handle error: maybe show a message and dismiss the dialog
            dismiss() // Close the dialog if no token is provided
            return
        }

        // Find views
        recyclerView = view.findViewById(R.id.recycler_view_portfolio)
        txtPortfolioValue = view.findViewById(R.id.txt_portfolio_value)
        txtDailyPL = view.findViewById(R.id.txt_daily_pl)
        txtPerformanceChange = view.findViewById(R.id.txt_performance_change)
        portfolioChart = view.findViewById(R.id.portfolio_chart)
        val closeButton = view.findViewById<ImageButton>(R.id.btn_close)

        // Setup close button
        closeButton.setOnClickListener {
            handleClose()
        }

        // Setup RecyclerView
        setupRecyclerView()

        // Setup chart
        setupChart()

        // Load portfolio data using the token from arguments
        loadPortfolioData(token)
    }

    private fun handleClose() {
        dismiss()
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
    private fun setupChart() {
        portfolioChart.apply {
            // Basic look & feel
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)                 // Disable pinch in small dialogs (prevents weird zoom)
            setDrawGridBackground(false)
            legend.isEnabled = false

            // THE SAFEST OFFSETS POSSIBLE
            // These values work perfectly from 150dp → 400dp chart height
            setViewPortOffsets(45f, 10f, 20f, 40f)   // left, top, right, bottom (in dp)

            // Extra safety: guarantee minimum visible area
            setExtraOffsets(8f, 8f, 8f, 8f)         // tiny breathing room

            // X Axis – clean and reliable
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawLabels(true)
                textColor = Color.GRAY
                textSize = 10f
                granularity = 1f
                isGranularityEnabled = true
                setAvoidFirstLastClipping(true)     // Prevents labels from being cut off
                setCenterAxisLabels(false)
            }

            // Y Axis – beautiful auto-scaling with guaranteed padding
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#1AFFFFFF")  // Very light on dark, works on light too
                textColor = Color.GRAY
                textSize = 10f

                // This combo is magic: perfect fit, never clipped
                setLabelCount(5, true)      // Force 5 horizontal lines
                spaceTop = 18f              // ~18% extra space at top
                spaceBottom = 18f           // ~18% extra space at bottom

                // NEVER set axisMinimum/axisMaximum manually unless you have a very specific reason
                // Let MPAndroidChart calculate it — it’s smarter than us
                resetAxisMinimum()
                resetAxisMaximum()
            }

            // Right axis – always disable
            axisRight.isEnabled = false

            // Final safety net: make sure old data doesn’t interfere
            clear()
            data = null
        }
    }

    // Update the function to accept the token as a parameter
    private fun loadPortfolioData(token: String) {

        // The token is now passed as a parameter, no need to get it from SharedPreferences
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get portfolio value (assuming you have this endpoint)
                val portfolioResponse = RetrofitClient.apiService.getPortfolioValue(token)



                // Get user holdings
                val holdingsResponse = RetrofitClient.apiService.getUserHoldings(token)

                // Get portfolio history for chart - Use the correct endpoint that returns objects
                val historyResponse = RetrofitClient.apiService.getPortfolioHistory(token, 100)



                withContext(Dispatchers.Main) {
                    if (portfolioResponse.isSuccessful) {
                        val portfolioValue = portfolioResponse.body()?.value ?: 0f
                        updatePortfolioData("$${String.format("%.2f", portfolioValue)}", "0.00%", "Today")
                    } else {
                        updatePortfolioData("$0.00", "0.00%", "Today")
                    }

                    if (holdingsResponse.isSuccessful) {
                        val holdings = holdingsResponse.body() ?: emptyList()
                        adapter.updateHoldings(holdings)
                        // Update holdings count
                        view?.findViewById<TextView>(R.id.txt_holdings_count)?.text = "${holdings.size} stocks"
                    } else {
                        adapter.updateHoldings(emptyList())
                        view?.findViewById<TextView>(R.id.txt_holdings_count)?.text = "0 stocks"
                    }

                    if (historyResponse.isSuccessful) {
                        val historyData = historyResponse.body() ?: emptyList()
                        if (historyData.isNotEmpty()) {
                            updateChartWithData(historyData)
                        } else {
                            showEmptyChart()
                        }
                    } else {
                        showEmptyChart()
                    }
                }
            } catch (e: Exception) {
                Log.e("PortfolioFragment", "Failed to load portfolio data", e)
                withContext(Dispatchers.Main) {
                    updatePortfolioData("$0.00", "0.00%", "Today")
                    adapter.updateHoldings(emptyList())
                    view?.findViewById<TextView>(R.id.txt_holdings_count)?.text = "0 stocks"
                    showEmptyChart()
                }
            }
        }
    }

    private fun showEmptyChart() {
        portfolioChart.clear()
        portfolioChart.setNoDataText("No data available")
        portfolioChart.setNoDataTextColor(Color.GRAY)
        portfolioChart.invalidate()
    }

    private fun updatePortfolioData(totalValue: String, performance: String, timePeriod: String) {
        txtPortfolioValue.text = totalValue
        txtPerformanceChange.text = performance
        txtDailyPL.text = timePeriod

        // Set color based on performance
        val color = if (performance.startsWith("-")) {
            ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
        } else {
            ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
        }
        txtPerformanceChange.setTextColor(color)

        // Update background based on performance
        val backgroundRes = if (performance.startsWith("-")) {
            R.drawable.bg_negative_pill
        } else {
            R.drawable.bg_positive_pill
        }
        txtPerformanceChange.setBackgroundResource(backgroundRes)
    }

    private fun updateChartWithData(historyData: List<PortfolioHistoryResponse>) {
        if (historyData.isEmpty()) {
            showEmptyChart()
            return
        }

        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()

        historyData.forEachIndexed { i, it ->
            entries.add(Entry(i.toFloat(), it.value))
            try {
                val date = timestampFormat.parse(it.timestamp)
                labels.add(SimpleDateFormat("MMM dd", Locale.getDefault()).format(date!!))
            } catch (e: Exception) {
                labels.add("")
            }
        }

        val dataSet = LineDataSet(entries, "").apply {
            color = ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)
            setCircleColor(color)
            lineWidth = 2.5f
            circleRadius = 3f
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(requireContext(), android.R.color.holo_blue_bright)
            fillAlpha = 60
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        portfolioChart.apply {
            clear()
            data = LineData(dataSet)

            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.labelCount = minOf(6, labels.size)

            notifyDataSetChanged()
            invalidate()
        }

        // Update performance
        if (historyData.size >= 2) {
            val first = historyData.first().value
            val last = historyData.last().value
            val change = (last - first) / first * 100
            val perf = if (change >= 0) "+%.2f%%".format(change) else "%.2f%%".format(change)
            updatePortfolioData("$${"%.2f".format(last)}", perf, "${historyData.size} days")
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