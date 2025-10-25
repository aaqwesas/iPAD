package com.main.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.ipad.R
import com.example.ipad.databinding.FragmentTradingBinding

class TradingFragment : Fragment() {

    private var _binding: FragmentTradingBinding? = null
    private val binding get() = _binding!!

    private var shares = 10
    private var limitPrice = 180.50
    private var ownedShares = 0.0
    private val stockSymbol = "AAPL"

    // Order type and duration options
    private val orderTypes = arrayOf("Market", "Limit", "Stop", "Stop Limit")
    private val durations = arrayOf("Day", "GT90", "GTC", "IOC")
    private var currentOrderTypeIndex = 1 // Start with Limit
    private var currentDurationIndex = 1 // Start with GT90

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTradingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        updateUI()

        // Set default selection
        binding.toggleActionButtons.check(R.id.btnBuy)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() {
        // Shares buttons
        binding.btnIncreaseShares.setOnClickListener {
            shares++
            updateUI()
        }

        binding.btnDecreaseShares.setOnClickListener {
            if (shares > 1) {
                shares--
                updateUI()
            }
        }

        // Price buttons
        binding.btnIncreasePrice.setOnClickListener {
            limitPrice += 0.01
            updateUI()
        }

        binding.btnDecreasePrice.setOnClickListener {
            if (limitPrice > 0.01) {
                limitPrice -= 0.01
                updateUI()
            }
        }

        // Order type navigation
        binding.btnOrderTypeLeft.setOnClickListener {
            currentOrderTypeIndex = (currentOrderTypeIndex - 1 + orderTypes.size) % orderTypes.size
            updateOrderTypeDisplay()
        }

        binding.btnOrderTypeRight.setOnClickListener {
            currentOrderTypeIndex = (currentOrderTypeIndex + 1) % orderTypes.size
            updateOrderTypeDisplay()
        }

        // Duration navigation
        binding.btnDurationLeft.setOnClickListener {
            currentDurationIndex = (currentDurationIndex - 1 + durations.size) % durations.size
            updateDurationDisplay()
        }

        binding.btnDurationRight.setOnClickListener {
            currentDurationIndex = (currentDurationIndex + 1) % durations.size
            updateDurationDisplay()
        }

        // Review order button
        binding.btnReviewOrder.setOnClickListener {
            reviewOrder()
        }
    }

    private fun updateUI() {
        binding.tvShares.text = shares.toString()
        binding.tvLimitPrice.text = "%.2f".format(limitPrice)

        val estimatedTotal = shares * limitPrice
        binding.tvEstimatedTotal.text = "$${"%.2f".format(estimatedTotal)}"

        binding.tvOwnership.text = "You own %.2f %s".format(ownedShares, stockSymbol)
    }

    private fun updateOrderTypeDisplay() {
        binding.tvOrderType.text = orderTypes[currentOrderTypeIndex]
    }

    private fun updateDurationDisplay() {
        binding.tvDuration.text = durations[currentDurationIndex]
    }

    private fun reviewOrder() {
        val selectedActionId = binding.toggleActionButtons.checkedButtonId
        val action = when (selectedActionId) {
            R.id.btnSell -> "Sell"
            R.id.btnShort -> "Short"
            R.id.btnCover -> "Cover"
            else -> "Buy" // default to Buy
        }

        // Create order summary and proceed to review
        val orderSummary = String.format(
            "%s %d shares of %s at $%.2f %s",
            action, shares, stockSymbol, limitPrice, orderTypes[currentOrderTypeIndex]
        )

        // Show confirmation dialog or navigate to review screen
        // You can implement this based on your app flow
    }
}