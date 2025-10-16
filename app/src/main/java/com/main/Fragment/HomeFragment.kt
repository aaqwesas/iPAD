package com.main.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ipad.R
import com.main.Data.Stock
import com.main.Data.StockAdapter

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StockAdapter
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        recyclerView = view.findViewById(R.id.recycler_view_stocks)
        progressBar = view.findViewById(R.id.progress_bar)

        setupRecyclerView()
        loadSampleData()

        return view
    }

    private fun setupRecyclerView() {
        adapter = StockAdapter { stock ->
            // Handle stock item click
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@HomeFragment.adapter
        }
    }

    private fun loadSampleData() {
        progressBar.visibility = View.VISIBLE

        Thread {

            requireActivity().runOnUiThread {
                val sampleStocks = listOf(
                    Stock(
                        ticker = "AAPL",
                        name = "Apple Inc.",
                        price = 150.25,
                        change = 2.50,
                        changePercent = 1.69,
                        iconResId = R.drawable.ic_default_stock
                    ),
                    Stock(
                        ticker = "GOOGL",
                        name = "Alphabet Inc.",
                        price = 2750.50,
                        change = -15.20,
                        changePercent = -0.55,
                        iconResId = R.drawable.ic_default_stock
                    ),
                    Stock(
                        ticker = "MSFT",
                        name = "Microsoft Corp.",
                        price = 330.75,
                        change = 5.25,
                        changePercent = 1.61,
                        iconResId = R.drawable.ic_default_stock
                    ),
                    Stock(
                        ticker = "TSLA",
                        name = "Tesla Inc.",
                        price = 245.30,
                        change = 12.40,
                        changePercent = 5.32,
                        iconResId = R.drawable.ic_default_stock
                    ),
                    Stock(
                        ticker = "AMZN",
                        name = "Amazon.com Inc.",
                        price = 125.50,
                        change = -0.75,
                        changePercent = -0.60,
                        iconResId = R.drawable.ic_default_stock
                    ),
                    Stock(
                        ticker = "META",
                        name = "Meta Platforms Inc.",
                        price = 485.20,
                        change = 8.75,
                        changePercent = 1.84,
                        iconResId = R.drawable.ic_default_stock
                    ),
                    Stock(
                        ticker = "NFLX",
                        name = "Netflix Inc.",
                        price = 675.80,
                        change = -12.30,
                        changePercent = -1.79,
                        iconResId = R.drawable.ic_default_stock
                    )
                )

                adapter.updateStocks(sampleStocks)
                progressBar.visibility = View.GONE
            }
        }.start()
    }
}