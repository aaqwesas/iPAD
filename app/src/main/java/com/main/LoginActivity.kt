package com.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.OpenForTesting
import androidx.appcompat.app.AppCompatActivity
import com.example.ipad.databinding.ActivityLoginBinding

import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import com.main.api.RetrofitClient
import com.main.models.RegisterRequest
import com.main.models.FCMUpdateRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

@OpenForTesting
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private companion object {
        private const val TAG = "LoginActivity"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener {
            handleTraditionalLogin()
        }



        binding.signUpPrompt.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
    }

    private suspend fun registerAndSyncFcm(email: String) {
        try {
            // Step 1: Register / login user (idempotent)
            val registerResponse = RetrofitClient.apiService.registerUser(
                RegisterRequest(email = email)
            )

            if (!registerResponse.isSuccessful) {
                Log.w("Auth", "Register failed: ${registerResponse.code()}")
                // Don't return — still try to send FCM token (user might already exist)
            } else {
                Log.d("Auth", "Register: ${registerResponse.body()?.message}")
            }

            // Step 2: Get FCM token and send it
            val fcmToken = Firebase.messaging.token.await()  // This is suspend + safe!

            val fcmResponse = RetrofitClient.apiService.setFcmToken(
                FCMUpdateRequest(email = email, fcm_token = fcmToken)
            )

            if (fcmResponse.isSuccessful) {
                withContext(Dispatchers.Main) {
                    Log.d("FCM", "Token synced successfully for $email")
                    // Optional: Toast.makeText(this@SignInActivity, "Ready!", Toast.SHORT).show()
                }
            } else {
                Log.w("FCM", "Failed to sync FCM token: ${fcmResponse.code()}")
            }

        } catch (e: Exception) {
            Log.e("Auth/FCM", "Failed during register or FCM sync", e)
            // Don't crash — user can still use app, will retry next login
        }
    }
    fun handleTraditionalLogin() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email and password", Toast.LENGTH_SHORT).show()
            return
        }

        // Check credentials against the simulated database
        val userAccounts = getSharedPreferences("UserAccounts", Context.MODE_PRIVATE)
        val savedPassword = userAccounts.getString(email, null)

        if (savedPassword == null) {
            Toast.makeText(this, "No account found with this email. Please sign up.", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (savedPassword != password) {
            Toast.makeText(this, "Invalid email or password.", Toast.LENGTH_SHORT).show()
            return
        }

        // Credentials are correct, proceed with login
        val dummyToken = "token_for_$email"
        saveCredentials(dummyToken, email)

        // Register + Sync FCM using coroutines
        CoroutineScope(Dispatchers.IO).launch {
            registerAndSyncFcm(email)
        }

        updateUI(Any())
    }
    
    fun saveCredentials(token: String, email: String) {
        val sharedPreferences = getSharedPreferences("TokenPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString("user_token", token)
            .putString("user_email", email)
            .apply()
    }

    fun updateUI(account: Any?) {
        if (account != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // Sign in failed
        }
    }
}
