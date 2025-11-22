package com.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.ipad.R
import com.example.ipad.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenClient
import com.google.android.libraries.identity.googleid.GoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleIdTokenClient: GoogleIdTokenClient

    private companion object {
        private const val TAG = "LoginActivity"
    }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        } else {
            Log.w(TAG, "Google Sign-In was cancelled by the user. Result Code: ${result.resultCode}")
            Toast.makeText(this, "Sign-in was cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleIdTokenClient = GoogleIdTokenClient(this)

        binding.loginButton.setOnClickListener {
            handleTraditionalLogin()
        }

        binding.googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        binding.signUpPrompt.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
    }

    private fun signInWithGoogle() {
        // Use the modern Google Identity approach
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .build()

        val signInIntent = googleIdTokenClient.getSignInIntent(googleIdOption)
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Log.d(TAG, "Google Sign-In was successful.")
            val token = account.idToken ?: "google_dummy_token"
            val email = account.email
            if (email != null) {
                saveCredentials(token, email)
            }
            updateUI(account)
        } catch (e: ApiException) {
            val errorText = "Sign in failed. Google Error Code: ${e.statusCode}"
            Log.e(TAG, errorText, e)
            Toast.makeText(this, errorText, Toast.LENGTH_LONG).show()
            updateUI(null)
        } catch (e: Exception) {
            Log.e(TAG, "An unexpected error occurred during Google Sign-In", e)
            Toast.makeText(this, "An unexpected error occurred.", Toast.LENGTH_LONG).show()
            updateUI(null)
        }
    }

    private fun handleTraditionalLogin() {
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
        
        updateUI(Any())
    }
    
    private fun saveCredentials(token: String, email: String) {
        val sharedPreferences = getSharedPreferences("TokenPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString("user_token", token)
            .putString("user_email", email)
            .apply()
    }

    private fun updateUI(account: Any?) {
        if (account != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // Sign in failed
        }
    }
}
