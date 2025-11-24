package com.main.Fragment

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import com.example.ipad.R
import com.example.ipad.databinding.FragmentAlertsBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.main.MainActivity
import com.main.StockRepository
import com.main.StockData
import com.main.api.RetrofitClient
import com.main.models.CreateAlertRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.text.substringBefore

// Move AlertType enum to top level or companion object
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

    // Track which alerts have been shown to avoid duplicates
    private val shownAlerts = mutableSetOf<String>()

    // Store the stock names from API
    private val availableStocks = mutableListOf<String>()

    // Enhanced sample data with proper types
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

    // Data class for alert conditions
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
        // Show loading state
        binding.stockSpinner.visibility = View.GONE
//        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getStocks()
                if (response.isSuccessful) {
                    val apiStocks = response.body() ?: emptyList()

                    // Create formatted list: "Company Name (TICKER)"
                    val stockDisplayList = apiStocks.map { stock ->
                        "${stock.symbol}"
                    }

                    // Store both display names and raw data for later use
                    availableStocks.clear()
                    availableStocks.addAll(stockDisplayList)

                    withContext(Dispatchers.Main) {
                        setupSpinner(stockDisplayList)
//                        binding.progressBar.visibility = View.GONE
                        binding.stockSpinner.visibility = View.VISIBLE
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        // Fallback to repository data if API fails
                        setupSpinner(StockRepository.stockNames)
//                        binding.progressBar.visibility = View.GONE
                        binding.stockSpinner.visibility = View.VISIBLE
                        Toast.makeText(requireContext(), "Using cached stock data", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Fallback to repository data on error
                    setupSpinner(StockRepository.stockNames)
//                    binding.progressBar.visibility = View.GONE
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

        // Set default selection if needed
        if (stockList.isNotEmpty()) {
            binding.stockSpinner.setSelection(0)
        }
    }

    private fun setupSaveButton() {
        binding.saveAlertButton.setOnClickListener {
            val selectedStock = binding.stockSpinner.selectedItem.toString()

//            // Extract ticker from selection (format: "Company Name (TICKER)")
//            val ticker = extractTickerFromSelection(selectedStock)
//            val companyName = extractCompanyNameFromSelection(selectedStock)

            val ticker = selectedStock

            Toast.makeText(requireContext(), "Alert saved for ($ticker)", Toast.LENGTH_SHORT).show()

            // Update any conditions with the selected stock
            updateConditionsWithSelectedStock(ticker)

            // Collect all enabled alerts
            val enabledAlerts = (priceRiseConditions + priceFallConditions + marketDataConditions)
                .filter { it.isEnabled }
                .mapNotNull { condition ->
                    mapToCreateAlertRequest(email, selectedStock, condition)
                }

            if (enabledAlerts.isEmpty()) {
                Toast.makeText(requireContext(), "No alerts enabled", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show loading
            binding.saveAlertButton.isEnabled = false
            binding.saveAlertButton.text = "Saving..."

            // Send all alerts in parallel (fire-and-forget with error handling)
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
        } ?: return null  // ← invalid number → skip alert

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
        // Update all conditions with the selected stock
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
        for (i in 1 until layout.childCount) {
            val child = layout.getChildAt(i)
            if (child is ViewGroup) {
                val conditionIndex = i - 1
                if (conditionIndex in conditions.indices) {
                    val condition = conditions[conditionIndex]

                    val valueView = child.findViewById<View>(R.id.tv_condition_value)
                    val switchView = child.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_condition)
                    val conditionNameView = child.findViewById<android.widget.TextView>(R.id.tv_condition_name)

                    // Set initial values
                    conditionNameView.text = condition.name
                    valueView.findViewById<android.widget.TextView>(R.id.tv_condition_value).text = condition.value
                    switchView.isChecked = condition.isEnabled

                    // Set click listener for value indicator
                    valueView.setOnClickListener {
                        showValueDialog(condition, valueView)
                    }

                    // Set switch change listener
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

    private fun updateConditionSwitch(condition: AlertCondition) {
        // Find and update the switch state in the UI
        val allConditions = priceRiseConditions + priceFallConditions + marketDataConditions
        allConditions.forEachIndexed { index, cond ->
            if (cond == condition) {
                // In a real app, you'd find the actual switch view and update it
                // For now, we'll just log it
                android.util.Log.d("AlertsFragment", "Updated switch for ${condition.name} to ${condition.isEnabled}")
            }
        }
    }

    private fun saveAlertCondition(condition: AlertCondition) {
        // In real app, save to SharedPreferences or database
        android.util.Log.d("AlertsFragment", "Saved: ${condition.name} = ${condition.value}, Enabled: ${condition.isEnabled}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun extractTickerFromSelection(selection: String): String {
        // Extract ticker from "Company Name (TICKER)" format
        return selection.substringAfterLast("(").removeSuffix(")").trim()
    }

    private fun extractCompanyNameFromSelection(selection: String): String {
        // Extract company name from "Company Name (TICKER)" format
        return selection.substringBeforeLast("(").trim()
    }
    // not using
    private fun showPriceAlertNotification(condition: AlertCondition, stockData: StockData) {
        // Create a unique key for this alert to avoid duplicates
        val alertKey = "${condition.stockSymbol}_${condition.name}_${condition.value}"

        // Check if we've already shown this alert
        if (shownAlerts.contains(alertKey)) {
            return
        }

        shownAlerts.add(alertKey)

        // Create notification
        createNotification(condition, stockData)

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_alert_dialog, null)

        val title = dialogView.findViewById<android.widget.TextView>(R.id.tv_alert_title)
        val message = dialogView.findViewById<android.widget.TextView>(R.id.tv_alert_message)
        val stockName = dialogView.findViewById<android.widget.TextView>(R.id.tv_stock_name)
        val priceChange = dialogView.findViewById<android.widget.TextView>(R.id.tv_price_change)

        // Customize dialog based on alert type
        when (condition.type) {
            AlertType.PRICE_ABOVE -> {
                title.text = "🎯 Price Target Reached!"
                message.text = "${condition.stockSymbol} has risen above ${condition.value}"
            }
            AlertType.PRICE_BELOW -> {
                title.text = "📉 Price Drop Alert!"
                message.text = "${condition.stockSymbol} has dropped to ${condition.value}"
            }
            AlertType.PERCENTAGE_RISE -> {
                title.text = "📈 Significant Rise!"
                message.text = "${condition.stockSymbol} has risen ${condition.value}"
            }
            AlertType.PERCENTAGE_FALL -> {
                title.text = "📉 Significant Drop!"
                message.text = "${condition.stockSymbol} has fallen ${condition.value}"
            }
            AlertType.VOLUME -> {
                title.text = "📊 Volume Alert!"
                message.text = "${condition.stockSymbol} volume ${condition.name.lowercase()} ${condition.value}"
            }
            else -> {
                title.text = "🔔 Market Alert"
                message.text = "${condition.name} condition met for ${condition.stockSymbol}"
            }
        }

        stockName.text = condition.stockSymbol
        priceChange.text = "${String.format("%.2f", stockData.changePercent)}%"
        priceChange.setTextColor(if (stockData.changePercent >= 0) {
            requireContext().getColor(android.R.color.holo_green_dark)
        } else {
            requireContext().getColor(android.R.color.holo_red_dark)
        })

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<View>(R.id.btn_close).setOnClickListener {
            shownAlerts.remove(alertKey)
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btn_dismiss).setOnClickListener {
            // Disable this alert temporarily
            condition.isEnabled = false
            saveAlertCondition(condition)
            shownAlerts.remove(alertKey)

            // Update the switch in UI
            updateConditionSwitch(condition)

            android.widget.Toast.makeText(requireContext(), "Alert disabled", android.widget.Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btn_view_details).setOnClickListener {
            android.widget.Toast.makeText(requireContext(), "View details for ${condition.stockSymbol}", android.widget.Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        // Auto-dismiss after 10 seconds if not interacted with
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (dialog.isShowing) {
                shownAlerts.remove(alertKey)
                dialog.dismiss()
            }
        }, 10000)

        dialog.show()
    }

    private fun createNotification(condition: AlertCondition, stockData: StockData) {
//        val channelId = "stock_alerts_channel"
//        val notificationId = System.currentTimeMillis().toInt() // Unique ID for each notification
//
//        // Create notification channel (required for Android 8.0+)
//        val channel = NotificationChannel(
//            channelId,
//            "Stock Alerts",
//            NotificationManager.IMPORTANCE_HIGH
//        ).apply {
//            description = "Notifications for stock price alerts"
//            enableVibration(true)
//            enableLights(true)
//            lightColor = android.graphics.Color.BLUE
//        }
//
//        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        notificationManager.createNotificationChannel(channel)
//
//        // Create intent for when notification is tapped
//        val intent = Intent(requireContext(), MainActivity::class.java).apply {
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        }
//
//        val pendingIntent = PendingIntent.getActivity(
//            requireContext(),
//            0,
//            intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        // Build notification
//        val notification = NotificationCompat.Builder(requireContext(), channelId)
//            .setSmallIcon(R.drawable.notification) // Add your notification icon
//            .setContentTitle(getNotificationTitle(condition))
//            .setContentText(getNotificationMessage(condition, stockData))
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setAutoCancel(true) // Dismiss notification when tapped
//            .setContentIntent(pendingIntent)
//            .setVibrate(longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400))
//            .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
//
//        // Show notification
//        with(NotificationManagerCompat.from(requireContext())) {
//            notify(notificationId, notification.build())
//        }
    }

    private fun getNotificationTitle(condition: AlertCondition): String {
        return when (condition.type) {
            AlertType.PRICE_ABOVE -> "🎯 Price Target Reached!"
            AlertType.PRICE_BELOW -> "📉 Price Drop Alert!"
            AlertType.PERCENTAGE_RISE -> "📈 Significant Rise!"
            AlertType.PERCENTAGE_FALL -> "📉 Significant Drop!"
            AlertType.VOLUME -> "📊 Volume Alert!"
            else -> "🔔 Market Alert"
        }
    }

    private fun getNotificationMessage(condition: AlertCondition, stockData: StockData): String {
        return when (condition.type) {
            AlertType.PRICE_ABOVE -> "${condition.stockSymbol} has risen above ${condition.value}"
            AlertType.PRICE_BELOW -> "${condition.stockSymbol} has dropped to ${condition.value}"
            AlertType.PERCENTAGE_RISE -> "${condition.stockSymbol} has risen ${condition.value}"
            AlertType.PERCENTAGE_FALL -> "${condition.stockSymbol} has fallen ${condition.value}"
            AlertType.VOLUME -> "${condition.stockSymbol} volume ${condition.name.lowercase()} ${condition.value}"
            else -> "${condition.name} condition met for ${condition.stockSymbol}"
        }
    }


}