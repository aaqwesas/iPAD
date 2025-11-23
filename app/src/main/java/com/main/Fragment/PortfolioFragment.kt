package com.main.Fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.ipad.R
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry

class PortfolioFragment : Fragment() {

    private lateinit var candleStickChart: CandleStickChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_portfolio, container, false)

        candleStickChart = view.findViewById(R.id.candle_stick_chart)
        setupCandleChart()
        loadMockTeslaData()

        return view
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

    private fun loadMockTeslaData() {
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