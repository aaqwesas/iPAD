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
import com.main.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        loadStockData()

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

    private fun loadStockData() {
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val response = RetrofitClient.apiService.getStocks()
            if (response.isSuccessful) {
                val stocks = response.body()?.map { apiStock ->
                    Stock(
                        ticker = apiStock.symbol,
                        price = apiStock.price,
                        change = apiStock.change,
                        changePercent = apiStock.change_percent,
                        volume = apiStock.volume
                    )
                } ?: emptyList()

                withContext(Dispatchers.Main) {
                    adapter.updateStocks(stocks)
                    progressBar.visibility = View.GONE
                }
            } else {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    // Handle error (show message, retry, etc.)
                }
            }
        }
    }
}