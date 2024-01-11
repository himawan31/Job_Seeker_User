package com.example.jobseeker

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.jobseeker.databinding.ActivityCheckYourEmailBinding
import com.google.firebase.auth.FirebaseAuth

class CheckYourEmail : AppCompatActivity() {
    private lateinit var binding: ActivityCheckYourEmailBinding
    private lateinit var sharedPreferences: SharedPreferences
    private var isPasswordUpdated = false
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckYourEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("PasswordUpdateStatus", Context.MODE_PRIVATE)
        auth = FirebaseAuth.getInstance()

        val tampungEmail = intent.getStringExtra("email")
        binding.txEmail.text = tampungEmail

        // Check if password is already updated successfully
        val isPasswordUpdated = sharedPreferences.getBoolean("isPasswordUpdated", false)

        if (isPasswordUpdated) {
            // If password is updated, redirect to Successfully activity
            redirectToSuccessfully()
        } else {
            // If password is not updated, proceed with the usual logic
            binding.openEmail.setOnClickListener {
                // Assume password is successfully updated when user clicks the button
                updatePasswordStatus(true)

                val gmailPackageName = "com.google.android.gm"

                // Mengecek apakah Gmail terinstal di perangkat
                val isGmailInstalled = isPackageInstalled(gmailPackageName)

                if (isGmailInstalled) {
                    // Membuat intent untuk membuka Gmail
                    val intent = packageManager.getLaunchIntentForPackage(gmailPackageName)

                    if (intent != null) {
                        startActivity(intent)
                    } else {
                        // Handle kesalahan jika intent tidak dapat dibuat
                        Toast.makeText(this, "Gagal membuka Gmail.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Jika Gmail tidak terinstal, berikan pesan kepada pengguna
                    Toast.makeText(
                        this,
                        "Aplikasi Gmail tidak terinstal di perangkat Anda.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.btnBack.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        // Check if password is updated each time the activity is resumed
        isPasswordUpdated = sharedPreferences.getBoolean("isPasswordUpdated", false)

        if (isPasswordUpdated) {
            // If password is updated, redirect to Successfully activity
            redirectToSuccessfully()
        }
    }

    private fun updatePasswordStatus(isUpdated: Boolean) {
        // Save the password update status
        val editor = sharedPreferences.edit()
        editor.putBoolean("isPasswordUpdated", isUpdated)
        editor.apply()
    }

    private fun redirectToSuccessfully() {
        val intent = Intent(this, Successfully::class.java)
        startActivity(intent)
        finish() // Optional: finish the current activity to prevent going back to it
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }
    }
}