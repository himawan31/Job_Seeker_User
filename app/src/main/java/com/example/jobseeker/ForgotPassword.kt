package com.example.jobseeker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import com.example.jobseeker.databinding.ActivityForgotPasswordBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class ForgotPassword : BaseActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var auth: FirebaseAuth
    private var loadingProgressBar: ProgressBar? = null
    private lateinit var dimView: View
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth

        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        dimView = findViewById(R.id.dimView)

        binding.backToLogin.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

        binding.resetPassword.setOnClickListener {
            resetPassword()
        }
    }

    private fun resetPassword() {
        val email = binding.edEmail.text.toString()
        if (validateForm(email)) {
            loadingProgressBar?.visibility = View.VISIBLE
            dimView.visibility = View.VISIBLE
            auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    loadingProgressBar?.visibility = View.INVISIBLE
                    dimView.visibility = View.INVISIBLE
                    binding.edEmail.visibility = View.GONE
                    binding.resetPassword.visibility = View.VISIBLE

                    updatePasswordStatus(false)

                    redirectToCheckYourEmail(email)

                } else {
                    loadingProgressBar?.visibility = View.INVISIBLE
                    dimView.visibility = View.INVISIBLE
                    showToast(this, "Reset kata sandi gagal, silakan coba lagi")
                }
            }
        }
    }

    private fun updatePasswordStatus(isUpdated: Boolean) {
        val editor = getSharedPreferences("PasswordUpdateStatus", Context.MODE_PRIVATE).edit()
        editor.putBoolean("isPasswordUpdated", isUpdated)
        editor.apply()
    }

    private fun validateForm(email: String): Boolean {
        var isValid = true

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.pkEmail?.visibility = View.VISIBLE
            isValid = false
        } else {
            binding.pkEmail?.visibility = View.INVISIBLE
        }

        return isValid
    }

    private fun redirectToCheckYourEmail(email: String) {
        val intent = Intent(this, CheckYourEmail::class.java)
        intent.putExtra("email", email)
        startActivity(intent)
    }
}