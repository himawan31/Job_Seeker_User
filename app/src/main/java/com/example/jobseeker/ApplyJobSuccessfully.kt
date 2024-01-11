package com.example.jobseeker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.jobseeker.databinding.ActivityApplyJobSuccessfullyBinding

class ApplyJobSuccessfully : AppCompatActivity() {
    private lateinit var binding: ActivityApplyJobSuccessfullyBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApplyJobSuccessfullyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Menerima data jobName dari aktivitas sebelumnya
        val jobName = intent.getStringExtra("jobName")

        // Menampilkan data jobName di TextView
        binding.successMessage.text = "Anda telah berhasil melamar ke posisi $jobName, cek halaman lamar pekerjaan untuk melihat status lamar pekerjaan"

        binding.goToJobs.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}