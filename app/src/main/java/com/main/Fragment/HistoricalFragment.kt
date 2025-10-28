package com.main.Fragment

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.ipad.R
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import com.main.Data.FakeHistoricalRepository
import com.main.models.HistoricalBar
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

class HistoricalFragment : Fragment(R.layout.fragment_historical) {

    companion object {
        private const val ARG_SYMBOL = "symbol"
        fun newInstance(symbol: String) = HistoricalFragment().apply {
            arguments = bundleOf(ARG_SYMBOL to symbol)
        }
    }

    private lateinit var viewModel: HistoricalViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val symbol = requireArguments().getString(ARG_SYMBOL) ?: "TSLA"

        val tvSymbol = view.findViewById<TextView>(R.id.tvSymbol)
        val tvError = view.findViewById<TextView>(R.id.tvError)
        val progress = view.findViewById<ProgressBar>(R.id.progress)
        val chart = view.findViewById<CandleStickChart>(R.id.candleChart)

        tvSymbol.text = symbol

        // === Chart Setup ===
        chart.apply {
            description.isEnabled = false
            setMaxVisibleValueCount(60)
            setPinchZoom(true)
            setDrawGridBackground(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = DateAxisFormatter()  // custom formatter below
            }

            axisLeft.apply {
                setDrawGridLines(true)
                setDrawAxisLine(true)
            }

            axisRight.isEnabled = false
            legend.isEnabled = false
        }

        val repo = FakeHistoricalRepository()

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoricalViewModel(repo) as T
            }
        })[HistoricalViewModel::class.java]

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.state.collectLatest { s ->
                progress.isVisible = s.loading
                tvError.isVisible = s.error != null
                tvError.text = s.error ?: ""

                if (s.data.isNotEmpty()) {
                    updateChart(chart, s.data)
                }
            }
        }

        viewModel.load(symbol, limit = 30)  // show more data for chart
    }

    private fun updateChart(chart: CandleStickChart, bars: List<HistoricalBar>) {
        val entries = bars.mapIndexed { index, bar ->
            CandleEntry(
                index.toFloat(),
                bar.high.toFloat(),
                bar.low.toFloat(),
                bar.open.toFloat(),
                bar.close.toFloat()
            )
        }

        val dataSet = CandleDataSet(entries, "Price").apply {
            setDrawValues(false)
            decreasingColor = Color.RED
            decreasingPaintStyle = android.graphics.Paint.Style.FILL
            increasingColor = Color.GREEN
            increasingPaintStyle = android.graphics.Paint.Style.FILL
            neutralColor = Color.GRAY
            shadowColor = Color.DKGRAY
            shadowWidth = 0.7f
            highLightColor = Color.WHITE
        }

        chart.data = CandleData(dataSet)
        chart.invalidate() // refresh
    }
}

// === Custom X-Axis: Show Date ===
class DateAxisFormatter : com.github.mikephil.charting.formatter.ValueFormatter() {
    private val sdf = SimpleDateFormat("MM-dd", Locale.US)
    private val calendar = Calendar.getInstance()

    override fun getFormattedValue(value: Float): String {
        // value is index → map to date from your data
        // For demo, we generate fake dates based on index
        calendar.timeInMillis = System.currentTimeMillis() - (30 - value.toInt()) * 24 * 60 * 60 * 1000
        return sdf.format(calendar.time)
    }
}