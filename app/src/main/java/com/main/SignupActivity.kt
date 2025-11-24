package com.main

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ipad.databinding.ActivitySignupBinding

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar was removed from layout, so remove these lines:
        // setSupportActionBar(binding.toolbar)
        // supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.signupButton.setOnClickListener {
            handleSignup()
        }

        // Add click listener for login prompt
        binding.loginPrompt.setOnClickListener {
            finish() // Go back to login activity
        }
    }

    // Remove onSupportNavigateUp since we don't have toolbar anymore
    // override fun onSupportNavigateUp(): Boolean {
    //     onBackPressedDispatcher.onBackPressed()
    //     return true
    // }

    private fun handleSignup() {
        val email = binding.emailEditTextSignup.text.toString().trim()
        val password = binding.passwordEditTextSignup.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEditTextSignup.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading state
        binding.signupProgressOverlay.visibility = android.view.View.VISIBLE
        binding.signupButton.isEnabled = false

        // Simulate API call delay
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            // Save the new user account to a simulated database (SharedPreferences)
            val userAccounts = getSharedPreferences("UserAccounts", Context.MODE_PRIVATE)

            if (userAccounts.contains(email)) {
                Toast.makeText(this, "An account with this email already exists.", Toast.LENGTH_SHORT).show()
                binding.signupProgressOverlay.visibility = android.view.View.GONE
                binding.signupButton.isEnabled = true
                return@postDelayed
            }

            userAccounts.edit()
                .putString(email, password)
                .apply()

            // Hide loading state
            binding.signupProgressOverlay.visibility = android.view.View.GONE
            binding.signupButton.isEnabled = true

            Toast.makeText(this, "Sign-up successful! Please log in.", Toast.LENGTH_LONG).show()
            finish() // Close the SignupActivity and return to the LoginActivity
        }, 1500) // 1.5 second delay to show the loading state
    }
}