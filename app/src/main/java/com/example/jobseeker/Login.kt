package com.example.jobseeker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.jobseeker.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class Login : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private var loadingProgressBar: ProgressBar? = null
    private lateinit var dimView: View
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth

        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        dimView = findViewById(R.id.dimView)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)


        binding.forgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPassword::class.java)
            startActivity(intent)
        }

        binding.signUp.setOnClickListener {
            val intent = Intent(this, Registrasi::class.java)
            startActivity(intent)
        }

        binding.btnLogin?.setOnClickListener {
            signInUser()
        }

        binding.sigInGoogle.setOnClickListener {
            signInWithGoogle()
        }
    }

    private var loginAttempts = 0

    private fun signInUser(){
        val email = binding.edEmail?.text.toString()
        val password = binding.edPassword.editText?.text.toString()

        clearErrors()

        if (validateForm(email, password)){
            loadingProgressBar?.visibility = View.VISIBLE
            dimView.visibility = View.VISIBLE
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful){
                        loginAttempts = 0
                        startActivity(Intent(this, MainActivity::class.java))
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        finish()
                    }else{
                        loginAttempts++

                        if (loginAttempts >= 6) {
                            Toast.makeText(this, "Percobaan login gagal terlalu banyak. Akun diblokir.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Login gagal. Silakan coba lagi.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    loadingProgressBar?.visibility = View.INVISIBLE
                    dimView.visibility = View.INVISIBLE
                }
        }
    }

    private fun signInWithGoogle(){
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    {result ->
        if (result.resultCode == Activity.RESULT_OK){
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleResult(task)
        }
    }

    private fun handleResult(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful){
            val account: GoogleSignInAccount = task.result
            if (account!=null){
                updateUI(account)
            }
        }else{
            Toast.makeText(this, "Login gagal, silakan coba lagi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(account: GoogleSignInAccount) {
        loadingProgressBar?.visibility = View.VISIBLE
        dimView.visibility = View.VISIBLE
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful){
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }else{
                Toast.makeText(this, "Login Gagal!", Toast.LENGTH_SHORT).show()
            }
            loadingProgressBar?.visibility = View.INVISIBLE
            dimView.visibility = View.INVISIBLE
        }
    }

    private fun validateForm(email: String, password: String): Boolean {
        var isValid = true

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
        binding.pkEmail?.visibility = View.INVISIBLE
        binding.pkPassword?.visibility = View.INVISIBLE
    }
}