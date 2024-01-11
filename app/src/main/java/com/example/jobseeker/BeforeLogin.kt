package com.example.jobseeker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.jobseeker.databinding.ActivityBeforeLoginBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class BeforeLogin : AppCompatActivity() {
    private lateinit var binding: ActivityBeforeLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBeforeLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pindah.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

        val auth = Firebase.auth
        if (auth.currentUser!=null){
            val intent = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent)
            finish()

        }
    }
}