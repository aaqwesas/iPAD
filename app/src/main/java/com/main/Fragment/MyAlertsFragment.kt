package com.main.Fragment

import android.content.Context
import android.content.SharedPreferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ipad.databinding.FragmentMyAlertsBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.main.api.RetrofitClient
import com.main.models.AlertResponse
import android.util.Log

class MyAlertsFragment : Fragment() {

    private var _binding: FragmentMyAlertsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.alertsRecyclerView.layoutManager = LinearLayoutManager(context)

        sharedPreferences = requireContext().getSharedPreferences("TokenPrefs", Context.MODE_PRIVATE)
//        val mockAlerts = listOf(
//            Alert("AAPL", "Price Rises Above", "$150.00"),
//            Alert("GOOGL", "Price Drops To", "$2700.00"),
//            Alert("TSLA", "1D Rise Exceeds", "5%")
//        )
//
//        binding.alertsRecyclerView.adapter = AlertsAdapter(mockAlerts)
        viewLifecycleOwner.lifecycleScope.launch {
            // Optional: repeat on lifecycle start (for pull-to-refresh later)
            // repeatOnLifecycle(Lifecycle.State.STARTED) { }

            try {

                val email = sharedPreferences.getString("user_email", null)
                val token: String = email ?: "Guest"
                val response = RetrofitClient.apiService.getAlertHistory(token)

                if (response.isSuccessful) {
                    val alertResponses = response.body() ?: emptyList()

                    val uiAlerts = alertResponses.map { it.toUiAlert() }

                    binding.alertsRecyclerView.adapter = AlertsAdapter(uiAlerts)

                    // Optional: hide shimmer, show content
                    // binding.shimmerLayout.stopShimmer()
                    // binding.shimmerLayout.gone()
                    // binding.alertsRecyclerView.visible()

                } else {
                    Log.w("Alert","Server error: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("Alert","Network failed: ${e.localizedMessage}")
            }
        }
    }


    fun AlertResponse.toUiAlert(): Alert {
        val conditionText = when (condition.lowercase()) {
            "above" -> "Price Rises Above"
            "below" -> "Price Drops To"
            "dayup" -> "1D Rise Exceeds"
            "daydown" -> "1D Drop Exceeds"
            else -> condition.replaceFirstChar { it.uppercase() }
        }

        val targetText = when (condition.lowercase()) {
            "dayup", "daydown" -> "${target}%"
            else -> "$${target.format(2)}"  // or use NumberFormat for better formatting
            // else -> "$target"
        }

        val triggeredInfo = if (is_triggered) "Triggered at $trigger_time" else ""

        return Alert(symbol, conditionText, targetText, triggeredInfo)
    }

    // Helper extension for formatting
    fun Double.format(digits: Int) = "%.${digits}f".format(this)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
