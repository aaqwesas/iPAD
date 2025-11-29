package com.main.Fragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.ipad.R
import com.example.ipad.databinding.FragmentAlertsBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.main.StockRepository
import com.main.api.RetrofitClient
import com.main.models.CreateAlertRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.text.substringBefore

enum class AlertType {
    PRICE_ABOVE,
    PRICE_BELOW,
    PERCENTAGE_RISE,
    PERCENTAGE_FALL,
    VOLUME
}

class AlertsFragment : Fragment() {

    private var username: String = ""
    private var email: String = ""

    private lateinit var sharedPreferences: SharedPreferences

    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!

    private val availableStocks = mutableListOf<String>()

    private val priceRiseConditions = listOf(
        AlertCondition("Price rises above", "100.00", true, AlertType.PRICE_ABOVE),
        AlertCondition("1D rise exceeds", "5%", false, AlertType.PERCENTAGE_RISE)
    )

    private val priceFallConditions = listOf(
        AlertCondition("Price drops to", "50.00", true, AlertType.PRICE_BELOW),
        AlertCondition("1D fall exceeds", "3%", false, AlertType.PERCENTAGE_FALL)
    )

    private val marketDataConditions = listOf(
        AlertCondition("Volume exceeds", "1M", true, AlertType.VOLUME)
    )

    data class AlertCondition(
        var name: String,
        var value: String,
        var isEnabled: Boolean,
        var type: AlertType = AlertType.PRICE_ABOVE,
        var stockSymbol: String = ""
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as AlertCondition
            return name == other.name && value == other.value && stockSymbol == other.stockSymbol
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + value.hashCode()
            result = 31 * result + stockSymbol.hashCode()
            return result
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPreferences = requireContext().getSharedPreferences("TokenPrefs", Context.MODE_PRIVATE)
        setupUserInfoInMemory()
        loadStockDataAndSetupSpinner()
        setupAlertConditions()
        setupSaveButton()

        binding.viewAlertsButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MyAlertsFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupUserInfoInMemory(){

        val googleAccount = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (googleAccount != null) {
            username = googleAccount.displayName ?: "User"
            email = googleAccount.email ?: "user@example.com"
        } else {
            val savedEmail = sharedPreferences.getString("user_email", null)
            if (savedEmail != null) {
                username = savedEmail.substringBefore('@')
                email = savedEmail
            } else {
                username = "User"
                email = "user@example.com"
            }
        }
    }

    private fun loadStockDataAndSetupSpinner() {
        binding.stockSpinner.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getStocks()
                if (response.isSuccessful) {
                    val apiStocks = response.body() ?: emptyList()

                    val stockDisplayList = listOf("Customized portfolio") + apiStocks.map { stock ->
                        "${stock.symbol}"
                    }

                    availableStocks.clear()
                    availableStocks.addAll(stockDisplayList)

                    withContext(Dispatchers.Main) {
                        setupSpinner(stockDisplayList)
                        binding.stockSpinner.visibility = View.VISIBLE
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        setupSpinner(StockRepository.stockNames)
                        binding.stockSpinner.visibility = View.VISIBLE
                        Toast.makeText(requireContext(), "Using cached stock data", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setupSpinner(StockRepository.stockNames)
                    binding.stockSpinner.visibility = View.VISIBLE
                    Toast.makeText(requireContext(), "Using cached stock data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupSpinner(stockList: List<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, stockList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.stockSpinner.adapter = adapter

        if (stockList.isNotEmpty()) {
            binding.stockSpinner.setSelection(0)
        }
    }

    private fun setupSaveButton() {
        binding.saveAlertButton.setOnClickListener {
            val selectedStock = binding.stockSpinner.selectedItem.toString()

            val ticker = selectedStock

            Toast.makeText(requireContext(), "Alert saved for ($ticker)", Toast.LENGTH_SHORT).show()

            updateConditionsWithSelectedStock(ticker)

            val enabledAlerts = (priceRiseConditions + priceFallConditions + marketDataConditions)
                .filter { it.isEnabled }
                .mapNotNull { condition ->
                    mapToCreateAlertRequest(email, selectedStock, condition)
                }

            if (enabledAlerts.isEmpty()) {
                Toast.makeText(requireContext(), "No alerts enabled", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.saveAlertButton.isEnabled = false
            binding.saveAlertButton.text = "Saving..."

            CoroutineScope(Dispatchers.IO).launch {
                var successCount = 0
                var errorCount = 0

                enabledAlerts.forEach { request ->
                    try {
                        val response = RetrofitClient.apiService.createAlert(request)
                        if (response.isSuccessful) {
                            successCount++
                        } else {
                            errorCount++
                            Log.w("Alert", "Failed: ${request.condition} ${request.target} for ${request.symbol}")
                        }
                    } catch (e: Exception) {
                        errorCount++
                        Log.e("Alert", "Network error", e)
                    }
                }

                withContext(Dispatchers.Main) {
                    binding.saveAlertButton.isEnabled = true
                    binding.saveAlertButton.text = "Save Alerts"

                    if (errorCount == 0) {
                        Toast.makeText(
                            requireContext(),
                            "$successCount alert(s) saved successfully!",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "$successCount saved, $errorCount failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun mapToCreateAlertType(
        type: AlertType,
        value: String,
        isPrice: Boolean
    ): Pair<String, Double>? {
        val cleanedValue = when (type) {
            AlertType.PERCENTAGE_RISE,
            AlertType.PERCENTAGE_FALL -> value.replace("%", "").replace(",", "")
            AlertType.VOLUME -> value
            else -> value.replace(",", "")
        }

        val targetValue = when (type) {
            AlertType.VOLUME -> parseVolume(cleanedValue)
            else -> cleanedValue.toDoubleOrNull()
        } ?: return null

        val conditionStr = when (type) {
            AlertType.PRICE_ABOVE -> "above"
            AlertType.PRICE_BELOW -> "below"
            AlertType.PERCENTAGE_RISE -> "rises_above"
            AlertType.PERCENTAGE_FALL -> "drops_below"
            AlertType.VOLUME -> "volume_above"
        }

        return conditionStr to targetValue
    }

    private fun parseVolume(value: String): Double {
        val clean = value.uppercase().replace(",", "")
        return when {
            clean.endsWith("K") -> clean.dropLast(1).toDoubleOrNull()?.times(1_000) ?: 0.0
            clean.endsWith("M") -> clean.dropLast(1).toDoubleOrNull()?.times(1_000_000) ?: 0.0
            clean.endsWith("B") -> clean.dropLast(1).toDoubleOrNull()?.times(1_000_000_000) ?: 0.0
            else -> clean.toDoubleOrNull() ?: 0.0
        }
    }

    private fun mapToCreateAlertRequest(
        email: String,
        symbol: String,
        condition: AlertCondition
    ): CreateAlertRequest? {
        val mapped = mapToCreateAlertType(condition.type, condition.value, condition.type != AlertType.VOLUME)
            ?: return null

        val (conditionStr, targetValue) = mapped

        return CreateAlertRequest(
            email = email,
            symbol = symbol,
            target = targetValue,
            condition = conditionStr
        )
    }


    private fun updateConditionsWithSelectedStock(ticker: String) {
        val allConditions = priceRiseConditions + priceFallConditions + marketDataConditions
        allConditions.forEach { condition ->
            condition.stockSymbol = ticker
        }
    }

    private fun setupAlertConditions() {
        setupConditionClicks(binding.priceRiseLayout, priceRiseConditions)
        setupConditionClicks(binding.priceFallLayout, priceFallConditions)
        setupConditionClicks(binding.marketDataLayout, marketDataConditions)
    }

    private fun setupConditionClicks(layout: ViewGroup, conditions: List<AlertCondition>) {
        // Start at 1 to skip the title TextView at index 0
        for (i in 1 until layout.childCount) {
            val child = layout.getChildAt(i)
            if (child is ViewGroup) {
                val conditionIndex = i - 1 // The index in the `conditions` list
                if (conditionIndex in conditions.indices) {
                    val condition = conditions[conditionIndex]

                    val valueView = child.findViewById<View>(R.id.tv_condition_value)
                    val switchView = child.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_condition)
                    val conditionNameView = child.findViewById<android.widget.TextView>(R.id.tv_condition_name)

                    conditionNameView.text = condition.name
                    valueView.findViewById<android.widget.TextView>(R.id.tv_condition_value).text = condition.value
                    switchView.isChecked = condition.isEnabled

                    valueView.setOnClickListener {
                        showValueDialog(condition, valueView)
                    }

                    switchView.setOnCheckedChangeListener { _, isChecked ->
                        condition.isEnabled = isChecked
                        saveAlertCondition(condition)
                    }
                }
            }
        }
    }

    private fun showValueDialog(condition: AlertCondition, valueView: View) {
        val editText = android.widget.EditText(requireContext())
        editText.setText(condition.value)
        editText.inputType = when (condition.type) {
            AlertType.PRICE_ABOVE, AlertType.PRICE_BELOW -> android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            else -> android.text.InputType.TYPE_CLASS_TEXT
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Set ${condition.name}")
            .setMessage("Enter new value for ${condition.name}")
            .setView(editText)
            .setPositiveButton("Save") { dialog, _ ->
                val newValue = editText.text.toString()
                if (isValidValue(newValue, condition.type)) {
                    condition.value = newValue
                    valueView.findViewById<android.widget.TextView>(R.id.tv_condition_value).text = newValue
                    saveAlertCondition(condition)
                    Toast.makeText(requireContext(), "Value updated.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Invalid value format", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun isValidValue(value: String, type: AlertType): Boolean {
        return when (type) {
            AlertType.PRICE_ABOVE, AlertType.PRICE_BELOW -> {
                value.toDoubleOrNull() != null && value.toDouble() > 0
            }
            AlertType.PERCENTAGE_RISE, AlertType.PERCENTAGE_FALL -> {
                value.endsWith("%") && value.dropLast(1).toDoubleOrNull() != null
            }
            else -> value.isNotBlank()
        }
    }

    private fun saveAlertCondition(condition: AlertCondition) {
        android.util.Log.d("AlertsFragment", "Saved: ${condition.name} = ${condition.value}, Enabled: ${condition.isEnabled}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
