package com.main

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.ipad.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.main.Fragment.HomeFragment
import com.main.Fragment.LoginFragment
import com.main.Fragment.PortfolioFragment
import com.main.Fragment.ProfileFragment
import com.main.Fragment.AlertsFragment
import com.main.Fragment.TradingFragment
import com.main.Fragment.TestAlertsFragment

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var sharedPreferences: SharedPreferences
    private fun requestNotificationPermission() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                when {
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        // Permission already granted - proceed
                    }
                    else -> {
                        // Request permission
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
            else -> {
                // No permission needed for older Android versions
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted - proceed with notification setup
            // You can now show notifications
        } else {

        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        requestNotificationPermission()

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
                R.id.nav_alert -> AlertsFragment()
//                R.id.navigation_trading -> TradingFragment()
//                R.id.nav_test_alerts -> TestAlertsFragment()
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