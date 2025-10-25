package com.main.Fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.ipad.R
import com.main.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit

class SignupFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var btnGenerateToken: Button
    private lateinit var llTokenSection: LinearLayout
    private lateinit var tvGeneratedToken: TextView
    private lateinit var btnCopyToken: Button
    private lateinit var tvLoginLink: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_signup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences("TokenPrefs", Context.MODE_PRIVATE)
        btnGenerateToken = view.findViewById(R.id.btn_generate_token)
        llTokenSection = view.findViewById(R.id.ll_token_section)
        tvGeneratedToken = view.findViewById(R.id.tv_generated_token)
        btnCopyToken = view.findViewById(R.id.btn_copy_token)
        tvLoginLink = view.findViewById(R.id.tv_login_link)

        btnGenerateToken.setOnClickListener {
            generateTokenFromServer()
        }

        btnCopyToken.setOnClickListener {
            val token = tvGeneratedToken.text.toString()
            if (token != "Token will appear here") {
                copyTokenToClipboard(token)
            }
        }

        tvLoginLink.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()
        }
    }

    private fun generateTokenFromServer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.generateToken()
                if (response.isSuccessful) {
                    val token = response.body()?.token
                    if (!token.isNullOrEmpty()) {
                        withContext<Unit>(Dispatchers.Main) {
                            tvGeneratedToken.text = token
                            llTokenSection.visibility = View.VISIBLE

                            // Save the token
                            sharedPreferences.edit { putString("user_token", token) }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to generate token", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun copyTokenToClipboard(token: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Token", token)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Token copied to clipboard!", Toast.LENGTH_SHORT).show()
    }
}