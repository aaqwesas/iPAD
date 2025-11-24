package com.main.Fragment

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
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
import java.text.DecimalFormat

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
    private var currentPrice: Double = 0.0

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
    private lateinit var btnTrade: Button

    private var currentTimeframe = "1Y"
    private var isBuyMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ticker = arguments?.getString("ticker").toString()
        companyName = arguments?.getString("company_name") ?: ticker
    }

    private fun setupBackButton(backButton: ImageButton) {
        backButton.setOnClickListener {
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
        btnTrade = view.findViewById(R.id.btn_trade)

        setupCandleChart()
        setupBackButton(btnBack)
        setupTradeButton()

        return view
    }

    private fun setupTradeButton() {
        btnTrade.setOnClickListener {
            showTradeDialog()
        }
    }

    private fun showTradeDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_trade, null)

        val tvStockInfo = dialogView.findViewById<TextView>(R.id.tv_stock_info)
        val btnBuy = dialogView.findViewById<Button>(R.id.btn_buy)
        val btnSell = dialogView.findViewById<Button>(R.id.btn_sell)
        val etQuantity = dialogView.findViewById<EditText>(R.id.et_quantity)
        val tvTotalCost = dialogView.findViewById<TextView>(R.id.tv_total_cost)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm)

        // Remove portfolio balance view since we don't need it
        val tvPortfolioBalance = dialogView.findViewById<TextView>(R.id.tv_portfolio_balance)
        tvPortfolioBalance.visibility = View.GONE

        // Update stock info with current price
        tvStockInfo.text = "$ticker - $${String.format("%.2f", currentPrice)}"

        // Update total cost when quantity changes
        etQuantity.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateTotalCost(etQuantity, tvTotalCost)
            }
        })

        // Buy/Sell mode toggle
        btnBuy.setOnClickListener {
            isBuyMode = true
            updateTradeMode(btnBuy, btnSell, btnConfirm)
            updateTotalCost(etQuantity, tvTotalCost)
        }

        btnSell.setOnClickListener {
            isBuyMode = false
            updateTradeMode(btnBuy, btnSell, btnConfirm)
            updateTotalCost(etQuantity, tvTotalCost)
        }

        // Initial mode
        updateTradeMode(btnBuy, btnSell, btnConfirm)

        // Confirm order
        btnConfirm.setOnClickListener {
            executeTrade(etQuantity.text.toString())
        }

        // Initial total cost calculation
        updateTotalCost(etQuantity, tvTotalCost)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.show()
    }

    private fun updateTradeMode(btnBuy: Button, btnSell: Button, btnConfirm: Button) {
        if (isBuyMode) {
            btnBuy.setBackgroundColor(Color.parseColor("#4CAF50"))
            btnBuy.setTextColor(Color.WHITE)
            btnSell.setBackgroundColor(Color.parseColor("#CCCCCC"))
            btnSell.setTextColor(Color.BLACK)
            btnConfirm.text = "BUY NOW"
            btnConfirm.setBackgroundColor(Color.parseColor("#4CAF50"))
        } else {
            btnSell.setBackgroundColor(Color.parseColor("#F44336"))
            btnSell.setTextColor(Color.WHITE)
            btnBuy.setBackgroundColor(Color.parseColor("#CCCCCC"))
            btnBuy.setTextColor(Color.BLACK)
            btnConfirm.text = "SELL NOW"
            btnConfirm.setBackgroundColor(Color.parseColor("#F44336"))
        }
    }

    private fun updateTotalCost(etQuantity: EditText, tvTotalCost: TextView) {
        try {
            val quantity = etQuantity.text.toString().toIntOrNull() ?: 0
            val total = quantity * currentPrice
            val action = if (isBuyMode) "Cost" else "Proceeds"
            tvTotalCost.text = "Total $action: $${String.format("%.2f", total)}"
        } catch (e: Exception) {
            tvTotalCost.text = "Total: $0.00"
        }
    }

    private fun executeTrade(quantityStr: String) {
        val quantity = quantityStr.toIntOrNull() ?: 0
        if (quantity <= 0) {
            Toast.makeText(requireContext(), "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
            return
        }

        val total = quantity * currentPrice

        if (isBuyMode) {
            // No money check - user can buy any amount
            Toast.makeText(requireContext(), "Bought $quantity shares of $ticker", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(requireContext(), "Sold $quantity shares of $ticker", Toast.LENGTH_LONG).show()
        }

        // Dismiss dialog
        requireActivity().currentFocus?.let { view ->
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }

        // In a real app, you'd want to properly dismiss the dialog and update portfolio
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
        loadChartData("1Y")

        tvCompanyName.text = companyName
        tvTicker.text = ticker

        // Start loading ONLY when the view is ready
        CoroutineScope(Dispatchers.IO).launch {
            val response = RetrofitClient.apiService.getStock(ticker)

            if (response.isSuccessful && response.body() != null) {
                val stock = response.body()!!

                Log.d("STOCK", "Success $ticker → $stock")

                withContext(Dispatchers.Main) {
                    // Update current price for trading
                    currentPrice = stock.price

                    tvChangePercent.text = stock.change_percent.toString()
                    tvPrevClose.text = stock.close_price.toString()
                    tvDayHigh.text = stock.high_price.toString()
                    tvDayLow.text = stock.low_price.toString()
                    tvVolume.text = stock.volume.toString()
                }
            } else {
                Log.e("STOCK", "Failed to load $ticker – ${response.code()}")
            }
        }
    }

    // Rest of your existing methods remain the same
    private fun switchTimeframe(timeframe: String) {
        if (currentTimeframe == timeframe) return
        currentTimeframe = timeframe

        // Update button highlights
        val selectedColor = Color.parseColor("#6200EE")
        val normalColor = Color.parseColor("#666666")

        listOf(btn1Year, btn6Months, btn1Month, btn2Weeks).forEach {
            it.setBackgroundColor(Color.TRANSPARENT)
            it.setTextColor(normalColor)
        }

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

                if (cache != null) {
                    val filtered = filterHistoryByTimeframe(cache, timeframe)
                    withContext(Dispatchers.Main) { updateChartWithData(filtered) }
                    return@launch
                }

                val response = if (isWeekly) {
                    RetrofitClient.apiService.getStockHistoryWeekly(ticker)
                } else {
                    RetrofitClient.apiService.getStockHistory(ticker)
                }

                if (response.isSuccessful && response.body() != null) {
                    val fullHistory = response.body()!!

                    if (isWeekly) weeklyHistoryCache = fullHistory
                    else dailyHistoryCache = fullHistory

                    val filtered = filterHistoryByTimeframe(fullHistory, timeframe)
                    withContext(Dispatchers.Main) { updateChartWithData(filtered) }
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
                    val date = java.time.LocalDate.parse(ohlc.date.substring(0, 10))
                    val timestampMs = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    timestampMs >= cutoffTime
                } catch (e: Exception) {
                    false
                }
            }
            .sortedBy { it.date }
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
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)

            xAxis.apply {
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.GRAY
            }

            axisLeft.apply {
                setDrawGridLines(true)
                setDrawAxisLine(true)
                textColor = Color.GRAY
            }

            axisRight.isEnabled = false
            legend.isEnabled = false
            setExtraOffsets(16f, 16f, 16f, 16f)
        }
    }
}