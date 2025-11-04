package com.main.Fragment

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.Fragment
import com.example.ipad.R
import com.example.ipad.databinding.FragmentTestAlertsBinding
import com.google.android.material.button.MaterialButton

class TestAlertsFragment : Fragment() {

    private var _binding: FragmentTestAlertsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTestAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTestButtons()
    }

    private fun setupTestButtons() {
        binding.btnTestPriceRise.setOnClickListener { showPriceRiseAlert() }
        binding.btnTestPriceFall.setOnClickListener { showPriceFallAlert() }
        binding.btnTestVolume.setOnClickListener { showVolumeAlert() }
    }

    private fun showPriceRiseAlert() {
        showAlertPopup(
            title = "Price Rise Alert",
            message = "AAPL has risen 5.2% in the last day, exceeding your alert threshold of 5%.",
            stock = "AAPL",
            change = "+5.2%",
            isPositive = true
        )
    }

    private fun showPriceFallAlert() {
        showAlertPopup(
            title = "Price Fall Alert",
            message = "TSLA has dropped 3.8% in the last hour, falling below your alert level of $150.",
            stock = "TSLA",
            change = "-3.8%",
            isPositive = false
        )
    }

    private fun showVolumeAlert() {
        showAlertPopup(
            title = "Volume Spike Alert",
            message = "GOOGL is experiencing unusually high volume - 2.5x the daily average. Potential news catalyst.",
            stock = "GOOGL",
            change = "+250%",
            isPositive = true
        )
    }

    private fun showRandomAlert() {
        val alertTypes = listOf(
            AlertData("Price Rise Alert", "MSFT has risen 8.9% today.", "MSFT", "+8.9%", true),
            AlertData("Price Fall Alert", "AMZN has dropped 2.1% this hour.", "AMZN", "-2.1%", false),
            AlertData("Volume Alert", "NVDA volume is 3x normal levels.", "NVDA", "+300%", true),
            AlertData("Turnover Alert", "High turnover detected in META.", "META", "15%", true)
        )

        val randomAlert = alertTypes.random()
        showAlertPopup(
            title = randomAlert.title,
            message = randomAlert.message,
            stock = randomAlert.stock,
            change = randomAlert.change,
            isPositive = randomAlert.isPositive
        )
    }

    private fun showAlertPopup(title: String, message: String, stock: String, change: String, isPositive: Boolean) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Inflate the custom dialog layout
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_alert_dialog, null)
        dialog.setContentView(dialogView)

        // Find views using the dialogView context
        val btnClose = dialogView.findViewById<View>(R.id.btn_close)
        val btnDismiss = dialogView.findViewById<MaterialButton>(R.id.btn_dismiss)
        val btnViewDetails = dialogView.findViewById<MaterialButton>(R.id.btn_view_details)
        val tvAlertTitle = dialogView.findViewById<android.widget.TextView>(R.id.tv_alert_title)
        val tvAlertMessage = dialogView.findViewById<android.widget.TextView>(R.id.tv_alert_message)
        val tvStockName = dialogView.findViewById<android.widget.TextView>(R.id.tv_stock_name)
        val tvPriceChange = dialogView.findViewById<android.widget.TextView>(R.id.tv_price_change)

        // Set up close button
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        // Set up dismiss button
        btnDismiss.setOnClickListener {
            dialog.dismiss()
        }

        // Set up view details button
        btnViewDetails.setOnClickListener {
            showToast("Viewing $stock details...")
            dialog.dismiss()
        }

        // Set alert content
        tvAlertTitle.text = title
        tvAlertMessage.text = message
        tvStockName.text = stock
        tvPriceChange.text = change

        // Set color based on positive/negative change
        val color = if (isPositive) requireContext().getColor(R.color.green)
        else requireContext().getColor(R.color.red)
        tvPriceChange.setTextColor(color)

        dialog.setCancelable(true)
        dialog.show()
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    data class AlertData(
        val title: String,
        val message: String,
        val stock: String,
        val change: String,
        val isPositive: Boolean
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}