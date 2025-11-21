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

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.signupButton.setOnClickListener {
            handleSignup()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

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

        // Save the new user account to a simulated database (SharedPreferences)
        val userAccounts = getSharedPreferences("UserAccounts", Context.MODE_PRIVATE)
        
        if (userAccounts.contains(email)) {
            Toast.makeText(this, "An account with this email already exists.", Toast.LENGTH_SHORT).show()
            return
        }
        
        userAccounts.edit()
            .putString(email, password)
            .apply()

        Toast.makeText(this, "Sign-up successful! Please log in.", Toast.LENGTH_LONG).show()
        finish() // Close the SignupActivity and return to the LoginActivity
    }
}
