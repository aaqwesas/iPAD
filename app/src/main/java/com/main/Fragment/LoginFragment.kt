package com.main.Fragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.ipad.R
import com.google.android.material.textfield.TextInputEditText
import com.main.api.RetrofitClient
import com.main.models.TokenVerifyRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit
import com.main.MainActivity

class LoginFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var etToken: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var tvSignupLink: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences("TokenPrefs", Context.MODE_PRIVATE)
        etToken = view.findViewById(R.id.et_token)
        btnLogin = view.findViewById(R.id.btn_login)
        tvSignupLink = view.findViewById(R.id.tv_signup_link)

        btnLogin.setOnClickListener {
            val token = etToken.text.toString().trim()
            if (token.isNotEmpty()) {
                verifyToken(token)
            } else {
                Toast.makeText(context, "Please enter a token", Toast.LENGTH_SHORT).show()
            }
        }

        tvSignupLink.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SignupFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun verifyToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.verifyToken(TokenVerifyRequest(token))
                if (response.isSuccessful && response.body()?.valid == true) {
                    // Token is valid, save and navigate
                    withContext(Dispatchers.Main) {
                        saveTokenAndNavigate(token)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Invalid token", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveTokenAndNavigate(token: String) {
        sharedPreferences.edit { putString("user_token", token) }
        Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
        (requireActivity() as MainActivity).showMainApp()
    }
}