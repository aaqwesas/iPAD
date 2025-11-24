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
import com.main.models.OHLC_history
import com.main.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Paint
import android.widget.Button
import android.widget.ImageButton

class CandleFragment : Fragment() {

    companion object {
        fun newInstance(ticker: String, companyName: String? = null): CandleFragment {
            return CandleFragment().apply {
                arguments = Bundle().apply {
                    putString("ticker", ticker)
                    putString("company_name", companyName ?: ticker)
                }
            }
        }
    }


    private var weeklyHistoryCache: List<OHLC_history>? = null
    private var dailyHistoryCache: List<OHLC_history>? = null

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

    private lateinit var btn1Year: Button
    private lateinit var btn6Months: Button
    private lateinit var btn1Month: Button
    private lateinit var btn2Weeks: Button

    private var currentTimeframe = "1Y"  // default



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ticker = arguments?.getString("ticker").toString()  // ← Survives rotation!
        companyName = arguments?.getString("company_name") ?: ticker
    }

    private fun setupBackButton(backButton: ImageButton) {
        backButton.setOnClickListener {
            // Go back to previous fragment or close activity
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            } else {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_candlestick, container, false)

        candleStickChart = view.findViewById(R.id.candle_stick_chart)
        val btnBack = view.findViewById<ImageButton>(R.id.btn_back)
        setupCandleChart()
        setupBackButton(btnBack)
//        loadMockTeslaData()
//        loadStockHistoryChart()

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

        btn1Year = view.findViewById(R.id.last_year)
        btn6Months = view.findViewById(R.id.last_6_months)
        btn1Month = view.findViewById(R.id.last_month)
        btn2Weeks = view.findViewById(R.id.last_2_weeks)

// Set click listeners
        btn1Year.setOnClickListener { switchTimeframe("1Y") }
        btn6Months.setOnClickListener { switchTimeframe("6M") }
        btn1Month.setOnClickListener { switchTimeframe("1M") }
        btn2Weeks.setOnClickListener { switchTimeframe("2W") }

// Load default (1Y) on start
        switchTimeframe("1Y")

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


    private fun switchTimeframe(timeframe: String) {
        if (currentTimeframe == timeframe) return
        currentTimeframe = timeframe

        // Update button highlights
        val selectedColor = Color.parseColor("#6200EE")  // Purple
        val normalColor = Color.parseColor("#666666")
        val selectedBg = android.R.drawable.btn_default_small
        val normalBg = android.R.drawable.btn_default_small

        listOf(btn1Year, btn6Months, btn1Month, btn2Weeks).forEach { it.setBackgroundResource(normalBg) }
        listOf(btn1Year, btn6Months, btn1Month, btn2Weeks).forEach { it.setTextColor(normalColor) }

        when (timeframe) {
            "1Y" -> { btn1Year.setBackgroundColor(selectedColor); btn1Year.setTextColor(Color.WHITE) }
            "6M" -> { btn6Months.setBackgroundColor(selectedColor); btn6Months.setTextColor(Color.WHITE) }
            "1M" -> { btn1Month.setBackgroundColor(selectedColor); btn1Month.setTextColor(Color.WHITE) }
            "2W" -> { btn2Weeks.setBackgroundColor(selectedColor); btn2Weeks.setTextColor(Color.WHITE) }
        }

        loadChartData(timeframe)
    }

    private fun loadChartData(timeframe: String) {

        candleStickChart.data = null
        candleStickChart.invalidate()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isWeekly = timeframe == "1Y" || timeframe == "6M"
                val cache = if (isWeekly) weeklyHistoryCache else dailyHistoryCache

                // Load from cache if we have it
                if (cache != null) {
                    val filtered = filterHistoryByTimeframe(cache, timeframe)
                    withContext(Dispatchers.Main) { updateChartWithData(filtered) }
                    return@launch
                }

                // Otherwise: fetch once
                val response = if (isWeekly) {
                    RetrofitClient.apiService.getStockHistoryWeekly(ticker)
                } else {
                    RetrofitClient.apiService.getStockHistory(ticker)
                }

                if (response.isSuccessful && response.body() != null) {
                    val fullHistory = response.body()!!

                    // Cache full data
                    if (isWeekly) weeklyHistoryCache = fullHistory
                    else dailyHistoryCache = fullHistory

                    val filtered = filterHistoryByTimeframe(fullHistory, timeframe)

                    withContext(Dispatchers.Main) {
                        updateChartWithData(filtered)
                    }
                }
            } catch (e: Exception) {
                Log.e("CHART", "Failed to load $timeframe data", e)
            }
        }
    }

    private fun filterHistoryByTimeframe(
        history: List<OHLC_history>,
        timeframe: String
    ): List<OHLC_history> {
        if (history.isEmpty()) return emptyList()

        val now = System.currentTimeMillis()
        val cutoffTime = when (timeframe) {
            "1Y" -> now - 365L * 24 * 60 * 60 * 1000
            "6M" -> now - 180L * 24 * 60 * 60 * 1000
            "1M" -> now - 30L * 24 * 60 * 60 * 1000
            "2W" -> now - 14L * 24 * 60 * 60 * 1000
            else -> now
        }

        return history
            .filter { ohlc ->
                try {
                    // Parse ISO date string like "2025-11-17T00:00:00"
                    val date = java.time.LocalDate.parse(ohlc.date.substring(0, 10)) // "2025-11-17"
                    val timestampMs = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    timestampMs >= cutoffTime
                } catch (e: Exception) {
                    false
                }
            }
            .sortedBy { it.date }  // oldest first
    }

    private fun updateChartWithData(history: List<OHLC_history>) {
        if (history.isEmpty()) {
            candleStickChart.data = null
            candleStickChart.invalidate()
            return
        }

        val entries = history.mapIndexed { index, ohlc ->
            CandleEntry(
                index.toFloat(),
                ohlc.high_price.toFloat(),
                ohlc.low_price.toFloat(),
                ohlc.open_price.toFloat(),
                ohlc.close_price.toFloat()
            )
        }

        val dataSet = CandleDataSet(entries, ticker).apply {
            increasingColor = Color.rgb(0, 200, 83)
            increasingPaintStyle = Paint.Style.FILL
            decreasingColor = Color.rgb(255, 82, 82)
            decreasingPaintStyle = Paint.Style.FILL
            shadowColor = Color.DKGRAY
            shadowWidth = 1.5f
            setDrawValues(false)
            barSpace = 0.35f
        }

        candleStickChart.data = CandleData(dataSet)
        candleStickChart.invalidate()
        candleStickChart.fitScreen()
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

}