package com.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.ipad.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.main.Fragment.AlertsFragment
import com.main.Fragment.HomeFragment
import com.main.Fragment.CandleFragment
import com.main.Fragment.ProfileFragment

import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var googleSignInClient: GoogleSignInClient

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        } else {
            // Permission denied
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        requestNotificationPermission()

        bottomNavigationView = findViewById(R.id.bottom_navigation)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val sharedPreferences = getSharedPreferences("TokenPrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("user_token", null)

        if (token == null) {
            showLoginScreen(false)
        } else {
            if (savedInstanceState == null) {
                loadFragment(HomeFragment())
                bottomNavigationView.selectedItemId = R.id.nav_home
            }
            setupBottomNavigation()
        }

        // In MainActivity.kt — keep ONLY this for testing
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM_TOKEN", "Your FCM token: $token")
                Toast.makeText(this, "FCM ready (check Logcat)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_portfolio -> CandleFragment()
                R.id.nav_profile -> ProfileFragment()
                R.id.nav_alert -> AlertsFragment()
                else -> HomeFragment() // Default to HomeFragment
            }
            loadFragment(selectedFragment)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right, // enter
                R.anim.slide_out_left, // exit
                R.anim.slide_in_left, // popEnter
                R.anim.slide_out_right // popExit
            )
            .replace(R.id.fragment_container, fragment)
            .commitNow()
    }

    fun showLoginScreen(performSignOut: Boolean = true) {
        // Always clear the local shared preferences
        val sharedPreferences = getSharedPreferences("TokenPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .remove("user_token")
            .remove("user_email")
            .apply()

        if (performSignOut) {
            // Use revokeAccess() for a complete sign out to clear getLastSignedInAccount
            googleSignInClient.revokeAccess().addOnCompleteListener(this) {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        } else {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
