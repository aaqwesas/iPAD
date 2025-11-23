package com.main.Fragment

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.ipad.R
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import com.main.Data.Stock
import com.main.models.OHLC
import com.main.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.Paint

class PortfolioFragment : Fragment() {

    companion object {
        fun newInstance(ticker: String, companyName: String? = null): PortfolioFragment {
            return PortfolioFragment().apply {
                arguments = Bundle().apply {
                    putString("ticker", ticker)
                    putString("company_name", companyName ?: ticker)
                }
            }
        }
    }


    // Arguments
    private var ticker: String = "AAPL"
    private var companyName: String = "Apple Inc."

    // All your TextViews
    private lateinit var tvCompanyName: TextView
    private lateinit var tvTicker: TextView
    private lateinit var tvChangePercent: TextView
    private lateinit var tvPrevClose: TextView
    private lateinit var tvDayHigh: TextView
    private lateinit var tvDayLow: TextView
    private lateinit var tvVolume: TextView


    private lateinit var candleStickChart: CandleStickChart



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ticker = arguments?.getString("ticker").toString()  // ← Survives rotation!
        companyName = arguments?.getString("company_name") ?: ticker
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_portfolio, container, false)

        candleStickChart = view.findViewById(R.id.candle_stick_chart)
        setupCandleChart()
//        loadMockTeslaData()
        loadStockHistoryChart()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // find all views to update
        tvCompanyName = view.findViewById(R.id.stock_name)
        tvTicker = view.findViewById(R.id.ticket)
        tvChangePercent = view.findViewById(R.id.percentage_change)
        tvPrevClose = view.findViewById(R.id.previous_close_value)
        tvDayHigh = view.findViewById(R.id.day_high)
        tvDayLow = view.findViewById(R.id.day_low)
        tvVolume = view.findViewById(R.id.volume)

        tvCompanyName.text = companyName
        tvTicker.text = ticker

        // Start loading ONLY when the view is ready
        CoroutineScope(Dispatchers.IO).launch {
            val response = RetrofitClient.apiService.getStock(ticker)

            if (response.isSuccessful && response.body() != null) {
                val stock = response.body()!!

                Log.d("STOCK", "Success $ticker → $stock")

                withContext(Dispatchers.Main) {
                    // 100% safe – view exists

                    tvChangePercent.text = stock.change_percent.toString()
                    tvPrevClose.text = stock.close_price.toString()
                    tvDayHigh.text = stock.high_price.toString()
                    tvDayLow.text = stock.low_price.toString()
                    tvVolume.text = stock.volume.toString()

//                    updateStockLatest(stock)
                }
            } else {
                Log.e("STOCK", "Failed to load $ticker – ${response.code()}")
            }
        }
    }

    private fun setupCandleChart() {
        candleStickChart.apply {
            description.isEnabled = false
            setBackgroundColor(Color.WHITE)
            setDrawGridBackground(false)

            // Touch & scaling
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)

            // X Axis
            xAxis.apply {
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.GRAY
            }

            // Left Y Axis
            axisLeft.apply {
                setDrawGridLines(true)
                setDrawAxisLine(true)
                textColor = Color.GRAY
            }

            // Right Y Axis (disable)
            axisRight.isEnabled = false

            // Legend
            legend.isEnabled = false

            // Extra offsets so candles don't touch edges
            setExtraOffsets(16f, 16f, 16f, 16f)
        }
    }

    private fun loadStockHistoryChart() {
        // Show loading state (optional)
        candleStickChart.data = null
        candleStickChart.invalidate()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getStockHistory(ticker)
                if (response.isSuccessful) {
                    val historyList = response.body() ?: emptyList()

                    if (historyList.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            candleStickChart.data = null
                            candleStickChart.invalidate()
                        }
                        return@launch
                    }

                    // Sort by timestamp just in case
                    val sortedHistory = historyList.sortedBy { it.timestamp }

                    val entries = ArrayList<CandleEntry>()

                    sortedHistory.forEachIndexed { index, ohlc ->
                        entries.add(
                            CandleEntry(
                                index.toFloat(),                    // x = index
                                ohlc.high_price.toFloat(),          // high
                                ohlc.low_price.toFloat(),           // low
                                ohlc.open_price.toFloat(),          // open
                                ohlc.close_price.toFloat()          // close
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        val dataSet = CandleDataSet(entries, ticker).apply {
                            increasingColor = Color.rgb(0, 200, 83)   // Green
                            increasingPaintStyle = android.graphics.Paint.Style.FILL
                            decreasingColor = Color.rgb(255, 82, 82)  // Red
                            decreasingPaintStyle = android.graphics.Paint.Style.FILL

                            shadowColor = Color.DKGRAY
                            shadowWidth = 1.5f
                            setDrawValues(false)
                            barSpace = 0.35f
                            axisDependency = com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT
                        }

                        candleStickChart.data = CandleData(dataSet)
                        candleStickChart.invalidate()

                        // Optional: Auto zoom to fit
                        candleStickChart.fitScreen()
                        candleStickChart.moveViewToX(entries.size.toFloat())
                    }
                } else {
                    Log.e("CHART", "Failed to load history for $ticker: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("CHART", "Error loading chart data", e)
            }
        }
    }
    private fun loadMockTeslaData() {

        CoroutineScope(Dispatchers.IO).launch {
            val response = RetrofitClient.apiService.getStockHistory(ticker)

            if (response.isSuccessful && response.body() != null) {
                val stock = response.body()!!

                Log.d("STOCK", "Success $ticker → $stock")

                withContext(Dispatchers.Main) {
                    // 100% safe – view exists

                    val apiStock_history = response.body() ?: emptyList()
                    // converts to entries
                }
            } else {
                Log.e("STOCK", "Failed to load $ticker – ${response.code()}")
            }
        }


        // Realistic 1-month TSLA daily candles (Open, High, Low, Close)
        val entries = ArrayList<CandleEntry>()

        // Index = day, values = OHLC in USD
        entries.add(CandleEntry(0f, 418.32f, 402.15f, 410.50f, 415.20f))  // Day 1
        entries.add(CandleEntry(1f, 425.10f, 408.70f, 413.20f, 422.80f))
        entries.add(CandleEntry(2f, 435.50f, 420.10f, 423.00f, 432.60f))
        entries.add(CandleEntry(3f, 440.00f, 425.30f, 438.00f, 428.90f))  // Red day
        entries.add(CandleEntry(4f, 438.20f, 420.50f, 427.00f, 435.10f))
        entries.add(CandleEntry(5f, 448.90f, 430.20f, 433.00f, 446.50f))
        entries.add(CandleEntry(6f, 455.00f, 440.10f, 447.80f, 452.30f))
        entries.add(CandleEntry(7f, 460.50f, 448.00f, 451.00f, 458.70f))
        entries.add(CandleEntry(8f, 465.20f, 450.30f, 459.00f, 453.10f))  // Red
        entries.add(CandleEntry(9f, 462.80f, 448.90f, 452.00f, 460.50f))
        entries.add(CandleEntry(10f, 475.00f, 458.20f, 460.00f, 472.30f))
        entries.add(CandleEntry(11f, 480.50f, 465.10f, 473.00f, 478.90f))
        entries.add(CandleEntry(12f, 485.00f, 470.50f, 479.00f, 475.20f)) // Red
        entries.add(CandleEntry(13f, 482.10f, 468.00f, 474.00f, 480.80f))
        entries.add(CandleEntry(14f, 490.00f, 475.50f, 479.50f, 488.40f))

        val dataSet = CandleDataSet(entries, "TSLA").apply {
            // Bullish (green) / Bearish (red) colors
            increasingColor = Color.rgb(0, 200, 83)   // Green
            increasingPaintStyle = android.graphics.Paint.Style.FILL
            decreasingColor = Color.rgb(255, 82, 82)  // Red
            decreasingPaintStyle = android.graphics.Paint.Style.FILL

            shadowColor = Color.DKGRAY
            shadowWidth = 1f

            // Remove value labels on top of candles
            setDrawValues(false)

            // Optional: make candles thicker
            barSpace = 0.3f
        }

        candleStickChart.data = CandleData(dataSet)
        candleStickChart.invalidate() // Refresh
    }
}