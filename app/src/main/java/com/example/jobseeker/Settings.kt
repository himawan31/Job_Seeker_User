package com.example.jobseeker

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.jobseeker.databinding.ActivitySettingsBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class Settings : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth

        binding.imageBack.setOnClickListener {
            onBackPressed()
        }

        binding.logout.setOnClickListener {
            if (auth.currentUser!=null){
                auth.signOut()
                val intent = Intent(this, Login::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            finish()
        }

        binding.deleteAccount.setOnClickListener {
            val message: String = "Apakah Anda yakin ingin menghapus akun?"
            showCustomDialogBox(message)
        }
    }
    private fun showCustomDialogBox(message: String){
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.alert_delete_account)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvMessage : TextView = dialog.findViewById(R.id.confirmationMessage)
        val btnYes : Button = dialog.findViewById(R.id.confirmButton)
        val btnNo : Button = dialog.findViewById(R.id.cancelButton)

        btnYes.setOnClickListener {
            val user = Firebase.auth.currentUser
            val userId = user?.uid

            val firestore = FirebaseFirestore.getInstance()
            val storage = FirebaseStorage.getInstance()
            val userDocRef = userId?.let { it1 -> firestore.collection("users").document(it1) }

            // Hapus data dari Firestore
            userDocRef?.delete()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Hapus data dari Firebase Storage (foto profil)
                    val storageRef = storage.reference.child("profile_images").child("$userId.jpg")
                    storageRef.delete().addOnCompleteListener { storageTask ->
                        if (storageTask.isSuccessful) {
                            // Hapus akun pengguna
                            user?.delete()?.addOnCompleteListener { deleteTask ->
                                if (deleteTask.isSuccessful) {
                                    Toast.makeText(this, "Akun Anda berhasil dihapus. Sampai jumpa!", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, Login::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(this, "Gagal menghapus akun. Silakan coba lagi.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(this, "Gagal menghapus foto profil. Silakan coba lagi.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Gagal menghapus akun. Silakan coba lagi.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnNo.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}