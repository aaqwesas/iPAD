package com.main

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.ipad.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.main.Fragment.HomeFragment
import com.main.Fragment.SettingsFragment
import com.main.Fragment.WatchlistFragment
import com.main.Fragment.ProfileFragment
import com.main.Fragment.TradingFragment

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottom_navigation)


        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
            bottomNavigationView.selectedItemId = R.id.nav_home
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment? = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_favorite -> WatchlistFragment()
                R.id.nav_settings -> SettingsFragment()
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
}