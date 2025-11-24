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
        // Set sample portfolio data
        view?.findViewById<TextView>(R.id.txt_portfolio_value)?.text = "$12,456.78"
        view?.findViewById<TextView>(R.id.txt_daily_pl)?.text = "+$245.67 (2.01%)"

        // Setup RecyclerView with portfolio stocks
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recycler_view_stocks)
        // Setup your adapter here with portfolio data
    }
}