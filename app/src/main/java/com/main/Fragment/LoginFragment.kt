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
import com.main.MainActivity
import androidx.core.content.edit

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
                //TODO: do a server request and get the actual token from database
                // Save token and navigate to home
                saveTokenAndNavigate(token)
            } else {
                Toast.makeText(context, "Please enter a token", Toast.LENGTH_SHORT).show()
            }
        }

        tvSignupLink.setOnClickListener {
            // Navigate to signup fragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SignupFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun saveTokenAndNavigate(token: String) {
        sharedPreferences.edit { putString("user_token", token) }
        Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()

        // Navigate to main app using the MainActivity method
        (requireActivity() as MainActivity).showMainApp()
    }
}