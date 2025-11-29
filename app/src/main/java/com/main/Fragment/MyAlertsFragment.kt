package com.main.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ipad.R
import com.example.ipad.databinding.FragmentMyAlertsBinding

class MyAlertsFragment : Fragment() {

    private var _binding: FragmentMyAlertsBinding? = null
    private val binding get() = _binding!!

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

        val mockAlerts = listOf(
            Alert("AAPL", "Price Rises Above", "$150.00"),
            Alert("GOOGL", "Price Drops To", "$2700.00"),
            Alert("TSLA", "1D Rise Exceeds", "5%")
        )

        binding.alertsRecyclerView.adapter = AlertsAdapter(mockAlerts)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
