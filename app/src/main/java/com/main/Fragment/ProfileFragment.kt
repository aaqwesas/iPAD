package com.main.Fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.ipad.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.switchmaterial.SwitchMaterial
import com.main.MainActivity

class ProfileFragment : Fragment() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var notificationsSwitch: SwitchMaterial

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        notificationsSwitch.isChecked = isGranted
        if (!isGranted) {
            Toast.makeText(requireContext(), "Notification permission was denied.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences("TokenPrefs", Context.MODE_PRIVATE)
        notificationsSwitch = view.findViewById(R.id.switch_notifications)

        setupUserInfo(view)
        setupNotificationSwitch()

        view.findViewById<View>(R.id.logout_button).setOnClickListener {
            handleLogout()
        }
    }

    override fun onResume() {
        super.onResume()
        updateNotificationSwitchState()
    }

    private fun setupUserInfo(view: View) {
        val userName = view.findViewById<TextView>(R.id.tv_user_name)
        val userEmail = view.findViewById<TextView>(R.id.tv_user_email)

        val googleAccount = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (googleAccount != null) {
            userName.text = googleAccount.displayName ?: "User"
            userEmail.text = googleAccount.email ?: "user@example.com"
        } else {
            val savedEmail = sharedPreferences.getString("user_email", null)
            if (savedEmail != null) {
                userName.text = savedEmail.substringBefore('@')
                userEmail.text = savedEmail
            } else {
                userName.text = "User"
                userEmail.text = "user@example.com"
            }
        }
    }

    private fun setupNotificationSwitch() {
        updateNotificationSwitchState()

        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                Toast.makeText(requireContext(), "Please disable notifications from app settings", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
        }
    }

    private fun updateNotificationSwitchState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            notificationsSwitch.isChecked = isGranted
        }
    }

    private fun handleLogout() {
        (requireActivity() as MainActivity).showLoginScreen()
    }
}
