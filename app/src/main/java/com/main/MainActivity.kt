package com.main

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.ipad.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.main.Fragment.HomeFragment
import com.main.Fragment.LoginFragment
import com.main.Fragment.PortfolioFragment
import com.main.Fragment.ProfileFragment
import com.main.Fragment.TradingFragment

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("TokenPrefs", MODE_PRIVATE)
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        // Check if user has a saved token
        val savedToken = sharedPreferences.getString("user_token", "")

        if (savedToken.isNullOrEmpty()) {
            // No token found, show login screen
            loadFragment(LoginFragment())
            // Hide bottom navigation when showing login
            bottomNavigationView.visibility = android.view.View.GONE
        } else {
            // Token exists, show main app with bottom navigation
            bottomNavigationView.visibility = android.view.View.VISIBLE

            if (savedInstanceState == null) {
                loadFragment(HomeFragment())
                bottomNavigationView.selectedItemId = R.id.nav_home
            }

            setupBottomNavigation()
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment? = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_portfolio -> PortfolioFragment()
                R.id.nav_profile -> ProfileFragment()
                R.id.navigation_trading -> TradingFragment()
                else -> null
            }

            selectedFragment?.let {
                loadFragment(it)
                true
            } ?: false
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right, // enter
                R.anim.slide_out_left, // exit
                R.anim.slide_in_left, // popEnter
                R.anim.slide_out_left // popExit
            )
            .replace(R.id.fragment_container, fragment)
            .commitNow()
    }

    // Method to switch to main app after login
    fun showMainApp() {
        bottomNavigationView.visibility = android.view.View.VISIBLE
        loadFragment(HomeFragment())
        bottomNavigationView.selectedItemId = R.id.nav_home
        setupBottomNavigation()
    }

    // Method to show login screen after logout
    fun showLoginScreen() {
        bottomNavigationView.visibility = android.view.View.GONE
        loadFragment(LoginFragment())
    }
}