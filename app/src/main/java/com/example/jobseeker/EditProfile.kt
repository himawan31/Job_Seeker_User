package com.example.jobseeker

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.jobseeker.databinding.ActivityEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class EditProfile : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    private var loadingProgressBar: ProgressBar? = null
    private lateinit var dimView: View
    private var currentProfileImageUrl: String = ""
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        dimView = findViewById(R.id.dimView)

        binding.imageBack.setOnClickListener {
            onBackPressed()
        }

        // Tampilkan data profil saat ini
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid

            firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val fullname = document.getString("fullname")
                        val email = if (isGoogleSignIn()) {
                            // Gunakan email dari Google
                            currentUser.email
                        } else {
                            document.getString("email") // Gunakan email dari Firestore
                        }
                        val phoneNumber = document.getString("phone_number")
                        val address = document.getString("address")
                        val job = document.getString("job")

                        // Tambahkan ini untuk menyimpan URL gambar profil saat ini
                        currentProfileImageUrl = document.getString("profile_image").toString()

                        // Isi formulir dengan data saat ini
                        binding.formFullName.setText(fullname)
                        binding.formEmail.setText(email)
                        binding.formPhoneNumber.setText(phoneNumber)
                        binding.formAddress.setText(address)
                        binding.formJob.setText(job)
                    }
                }
        }

        binding.btnSave.setOnClickListener {
            val updatedFullname = binding.formFullName.text.toString()
            val updatedEmail = if (isGoogleSignIn()) {
                auth.currentUser?.email // Gunakan email dari Google
            } else {
                binding.formEmail.text.toString() // Gunakan email dari formulir jika bukan Google SignIn
            }
            val updatedPhoneNumber = binding.formPhoneNumber.text.toString()
            val updatedAddress = binding.formAddress.text.toString()
            val updatedJob = binding.formJob.text.toString()

            // Simpan perubahan ke Firestore
            val user = auth.currentUser
            if (user != null) {
                val uid = user.uid

                if (user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }) {
                    // Pengguna login menggunakan Google, buat data baru di Firestore
                    val userMap = hashMapOf(
                        "fullname" to updatedFullname,
                        "email" to user.email, // Gunakan email dari Google
                        "phone_number" to updatedPhoneNumber,
                        "address" to updatedAddress,
                        "job" to updatedJob
                    )

                    firestore.collection("users")
                        .document(uid)
                        .set(userMap as Map<String, Any>, SetOptions.merge())
                        .addOnSuccessListener {
                            loadingProgressBar?.visibility = View.VISIBLE
                            dimView.visibility = View.VISIBLE
                            Toast.makeText(this, "Data berhasil disimpan.", Toast.LENGTH_SHORT).show()
                            onBackPressed()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            loadingProgressBar?.visibility = View.INVISIBLE
                            dimView.visibility = View.INVISIBLE
                            Toast.makeText(this, "Data gagal disimpan, silakan coba lagi.", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    val userMap = hashMapOf(
                        "fullname" to updatedFullname,
                        "email" to updatedEmail,
                        "phone_number" to updatedPhoneNumber,
                        "address" to updatedAddress,
                        "job" to updatedJob
                    )

                    firestore.collection("users")
                        .document(uid)
                        .update(userMap as Map<String, Any>)
                        .addOnSuccessListener {
                            loadingProgressBar?.visibility = View.VISIBLE
                            dimView.visibility = View.VISIBLE
                            Toast.makeText(this, "Data berhasil disimpan.", Toast.LENGTH_SHORT).show()
                            onBackPressed()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            loadingProgressBar?.visibility = View.INVISIBLE
                            dimView.visibility = View.INVISIBLE
                            Toast.makeText(this, "Data gagal disimpan, silakan coba lagi.", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }

    }

    private fun isGoogleSignIn(): Boolean {
        val currentUser = auth.currentUser
        return currentUser != null && currentUser.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
    }
}