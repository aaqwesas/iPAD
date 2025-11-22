package com.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.ipad.R
import com.example.ipad.databinding.ActivitySignupBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenClient

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleIdTokenClient: GoogleIdTokenClient

    private companion object {
        private const val TAG = "SignupActivity"
    }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleGoogleSignInResult(task)
        } else {
            Log.w(TAG, "Google Sign-Up was cancelled by the user. Result Code: ${result.resultCode}")
            Toast.makeText(this, "Sign-up was cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

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

        // Initialize Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleIdTokenClient = GoogleIdTokenClient(this)

        // Set up Google Sign-Up button if it exists in the layout
        if (::binding.isInitialized && ::googleSignInClient.isInitialized) {
            binding.root.findViewById<View?>(R.id.google_sign_up_button)?.setOnClickListener {
                signInWithGoogle()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun signInWithGoogle() {
        // Use the modern Google Identity approach for sign-up
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .build()

        val signInIntent = googleIdTokenClient.getSignInIntent(googleIdOption)
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Log.d(TAG, "Google Sign-Up was successful.")
            val email = account.email
            
            if (email != null) {
                // Check if user already exists
                val userAccounts = getSharedPreferences("UserAccounts", Context.MODE_PRIVATE)
                if (userAccounts.contains(email)) {
                    Toast.makeText(this, "An account with this email already exists.", Toast.LENGTH_SHORT).show()
                    return
                }
                
                // Create a new account with Google credentials
                // For simplicity, we'll store a dummy password
                userAccounts.edit()
                    .putString(email, "google_account") // Placeholder password for Google accounts
                    .apply()
                
                Toast.makeText(this, "Sign-up successful with Google!", Toast.LENGTH_LONG).show()
                
                // Return to login activity
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        } catch (e: ApiException) {
            val errorText = "Sign up failed. Google Error Code: ${e.statusCode}"
            Log.e(TAG, errorText, e)
            Toast.makeText(this, errorText, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "An unexpected error occurred during Google Sign-Up", e)
            Toast.makeText(this, "An unexpected error occurred.", Toast.LENGTH_LONG).show()
        }
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
