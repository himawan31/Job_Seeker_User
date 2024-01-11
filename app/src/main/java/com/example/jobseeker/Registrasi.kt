package com.example.jobseeker

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.jobseeker.databinding.ActivityRegistrasiBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class Registrasi : AppCompatActivity() {
    private lateinit var binding: ActivityRegistrasiBinding
    private var loadingProgressBar: ProgressBar? = null
    private lateinit var dimView: View
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrasiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth

        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        dimView = findViewById(R.id.dimView)

        binding.signIn.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

        binding.btnRegister.setOnClickListener {
            registrasiUser()
        }
    }

    private fun registrasiUser() {
        val fullname = binding.edFullName.text.toString()
        val email = binding.edEmail.text.toString()
        val password = binding.edPassword.editText?.text.toString()

        clearErrors()

        if (validateForm(fullname, email, password)) {
            loadingProgressBar?.visibility = View.VISIBLE
            dimView.visibility = View.VISIBLE
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            val uid = user.uid
                            val userMap = HashMap<String, Any>()
                            userMap["fullname"] = fullname

                            firestore.collection("users")
                                .document(uid)
                                .set(userMap)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Registrasi Berhasil!", Toast.LENGTH_SHORT).show()
                                    loadingProgressBar?.visibility = View.INVISIBLE
                                    dimView.visibility = View.INVISIBLE
                                    startActivity(Intent(this, GoToHomepage::class.java))
                                    finish()
                                }
                        }
                    } else {
                        Toast.makeText(this, "Registrasi Gagal! ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        loadingProgressBar?.visibility = View.INVISIBLE
                        dimView.visibility = View.INVISIBLE
                    }
                }
        }
    }

    private fun validateForm(fullname: String, email: String, password: String): Boolean {
        var isValid = true

        if (TextUtils.isEmpty(fullname)) {
            binding.pkFullName?.visibility = View.VISIBLE
            isValid = false
        } else {
            binding.pkFullName?.visibility = View.INVISIBLE
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.pkEmail?.visibility = View.VISIBLE
            isValid = false
        } else {
            binding.pkEmail?.visibility = View.INVISIBLE
        }

        if (TextUtils.isEmpty(password) || password.length < 8) {
            binding.pkPassword?.visibility = View.VISIBLE
            isValid = false
        } else {
            binding.pkPassword?.visibility = View.INVISIBLE
        }

        return isValid
    }

    private fun clearErrors() {
        binding.pkFullName?.visibility = View.INVISIBLE
        binding.pkEmail?.visibility = View.INVISIBLE
        binding.pkPassword?.visibility = View.INVISIBLE
    }
}