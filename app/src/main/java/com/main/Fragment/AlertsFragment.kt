package com.main.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.ipad.R
import com.example.ipad.databinding.FragmentAlertsBinding

class AlertsFragment : Fragment() {

    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!

    // Sample data for alert conditions
    private val priceRiseConditions = listOf(
        AlertCondition("Price rises above", "100.00", true),
        AlertCondition("1D rise exceeds", "5%", false),
        AlertCondition("Change in 3 min is up", "2%", true),
        AlertCondition("Change in 5 min is up", "3%", false)
    )

    private val priceFallConditions = listOf(
        AlertCondition("Price drops to", "50.00", true),
        AlertCondition("1D fall exceeds", "3%", false),
        AlertCondition("Change in 3 min is down", "1.5%", true),
        AlertCondition("Change in 5 min is down", "2.5%", false)
    )

    private val marketDataConditions = listOf(
        AlertCondition("Volume exceeds", "1M", true),
        AlertCondition("Turnover Above", "500K", false),
        AlertCondition("Turnover ratio exceeds", "10%", true)
    )

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
        setupAlertConditions()
    }

    private fun setupAlertConditions() {
        setupConditionClicks(binding.priceRiseLayout, priceRiseConditions)
        setupConditionClicks(binding.priceFallLayout, priceFallConditions)
        setupConditionClicks(binding.marketDataLayout, marketDataConditions)
    }

    private fun setupConditionClicks(layout: ViewGroup, conditions: List<AlertCondition>) {
        // Start from index 1 to skip the header TextView
        for (i in 1 until layout.childCount) {
            val child = layout.getChildAt(i)
            if (child is ViewGroup) {
                val conditionIndex = i - 1 // Adjust for header
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

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Set ${condition.name}")
            .setMessage("Enter new value for ${condition.name}")
            .setView(editText)
            .setPositiveButton("Save") { dialog, _ ->
                val newValue = editText.text.toString()
                condition.value = newValue
                valueView.findViewById<android.widget.TextView>(R.id.tv_condition_value).text = newValue
                saveAlertCondition(condition)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveAlertCondition(condition: AlertCondition) {
        android.util.Log.d("AlertsFragment", "Saved: ${condition.name} = ${condition.value}, Enabled: ${condition.isEnabled}")
    }

    // Data class to represent alert conditions
    data class AlertCondition(
        var name: String,
        var value: String,
        var isEnabled: Boolean
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}