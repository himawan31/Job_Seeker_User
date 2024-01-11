package com.example.jobseeker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.jobseeker.databinding.ActivitySuccessfullyBinding

private lateinit var binding: ActivitySuccessfullyBinding
class Successfully : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuccessfullyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backToLogin?.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
    }
}