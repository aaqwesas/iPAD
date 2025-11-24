package com.main.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.example.ipad.R

class PortfolioFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // You can set up your portfolio data here
        setupPortfolioData()
    }

    override fun onStart() {
        super.onStart()
        // Set dialog size
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupPortfolioData() {
        // Set default portfolio data - will be handled later from database

        view?.findViewById<TextView>(R.id.txt_portfolio_value)?.text = "$100,000.00"
        view?.findViewById<TextView>(R.id.txt_daily_pl)?.text = "+$0.00 (0.00%)"

        // Setup RecyclerView with portfolio stocks
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recycler_view_stocks)
        // Setup your adapter here with portfolio data
        // Will be implemented later with database data
    }
}