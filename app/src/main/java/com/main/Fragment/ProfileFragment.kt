package com.main.Fragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.ipad.R
import androidx.core.content.edit
import com.main.MainActivity

class ProfileFragment : Fragment() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences("TokenPrefs", Context.MODE_PRIVATE)

        view.findViewById<View>(R.id.portfolio).setOnClickListener {

        }

        view.findViewById<View>(R.id.password).setOnClickListener {

        }

        view.findViewById<View>(R.id.ll_notifications).setOnClickListener {

        }


        view.findViewById<View>(R.id.ll_logout).setOnClickListener {
            handleLogout()
        }
    }
    private fun handleLogout() {
        // Clear the saved token
        sharedPreferences.edit { remove("user_token") }
        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()

        // Navigate back to login screen
        (requireActivity() as MainActivity).showLoginScreen()
    }
}