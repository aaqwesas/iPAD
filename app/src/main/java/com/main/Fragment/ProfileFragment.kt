package com.main.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.ipad.R

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Add click listeners for profile options
        view.findViewById<View>(R.id.ll_account_settings).setOnClickListener {
            // Handle account settings click
        }

        view.findViewById<View>(R.id.ll_watchlist).setOnClickListener {
            // Handle watchlist click
        }

        view.findViewById<View>(R.id.ll_notifications).setOnClickListener {
            // Handle notifications click
        }

        view.findViewById<View>(R.id.ll_about).setOnClickListener {
            // Handle about click
        }

        view.findViewById<View>(R.id.ll_logout).setOnClickListener {
            // Handle logout click
        }
    }
}