package com.example.jobseeker

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {
    private lateinit var editProfile: Button
    private lateinit var titikTiga: TextView
    private lateinit var displayNameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var jobTextView: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var apkVersion: TextView
    private lateinit var resume: Button
    private lateinit var portoFolio: Button
    private lateinit var certificate: Button
    private val storage = FirebaseStorage.getInstance()
    private val storageReference: StorageReference = storage.reference
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val PDF_VIEW_REQUEST = 123 // atau nomor unik lainnya
    private var isPdfViewerOpen = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // inisialisasi
        editProfile = view.findViewById(R.id.editProfile)
        titikTiga = view.findViewById(R.id.settings)
        displayNameTextView = view.findViewById(R.id.userName)
        emailTextView = view.findViewById(R.id.userEmail)
        jobTextView = view.findViewById(R.id.userJob)
        profileImageView = view.findViewById(R.id.userProfileImage)
        apkVersion = view.findViewById(R.id.apkVersion)
        resume = view.findViewById(R.id.resume)
        portoFolio = view.findViewById(R.id.portoFolio)
        certificate = view.findViewById(R.id.sertifikasi)

        profileImageView.setOnClickListener {
            selectImage()
        }

        editProfile.setOnClickListener {
            val intent = Intent(activity, EditProfile::class.java)
            startActivity(intent)
        }

        titikTiga.setOnClickListener {
            val intent = Intent(activity, Settings::class.java)
            startActivity(intent)
        }

        resume.setOnClickListener {
            if (!isPdfViewerOpen) {
                showPDFViewer("file_resume")
                isPdfViewerOpen = true
            }
        }

        portoFolio.setOnClickListener {
            if (!isPdfViewerOpen) {
                showPDFViewer("file_portofolio")
                isPdfViewerOpen = true
            }
        }

        certificate.setOnClickListener {
            if (!isPdfViewerOpen) {
                showPDFViewer("file_certificate")
                isPdfViewerOpen = true
            }
        }

        val versionName = getVersionName()
        apkVersion.text = "Versi: $versionName"

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val email = currentUser.email

            val userDocRef = firestore.collection("users").document(uid)

            userDocRef.addSnapshotListener { document, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    val displayName = document.getString("fullname")
                    val job = document.getString("job")
                    val profileImageURL = document.getString("profile_image")

                    if (profileImageURL != null) {
                        Picasso.get()
                            .load(profileImageURL)
                            .into(profileImageView)
                    }
                    displayNameTextView.text = displayName
                    jobTextView.text = job
                    emailTextView.text = email
                }
            }
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        isPdfViewerOpen = false
    }

    // Metode untuk memilih gambar dari penyimpanan perangkat
    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, 1)
    }

    // Metode untuk menangani hasil pemilihan gambar
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val imageUri = data.data

            // Upload gambar ke Firebase Storage
            if (imageUri != null) {
                uploadImage(imageUri)
            }
        }
        if (requestCode == PDF_VIEW_REQUEST && resultCode == Activity.RESULT_CANCELED) {
            // Pengguna kembali dari PdfViewerActivity
            isPdfViewerOpen = false
        }
    }

    // Metode untuk mengunggah gambar ke Firebase Storage
    private fun uploadImage(imageUri: Uri) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid

            val profileImageRef = storageReference.child("profile_images/$uid.jpg")

            profileImageRef.putFile(imageUri)
                .addOnSuccessListener { taskSnapshot ->
                    profileImageRef.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        firestore.collection("users")
                            .document(uid)
                            .update("profile_image", imageUrl)
                            .addOnSuccessListener {
                            }
                            .addOnFailureListener {
                            }
                    }
                }
                .addOnFailureListener { exception ->
                }
        }
    }

    private fun getVersionName(): String {
        val packageManager = activity?.packageManager
        var versionName = "Unknown"
        try {
            if (packageManager != null) {
                val pInfo = packageManager.getPackageInfo(activity?.packageName!!, 0)
                versionName = pInfo.versionName
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return versionName
    }

    private fun showPDFViewer(fieldName: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid

            val userDocRef = firestore.collection("users").document(uid)

            userDocRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val fileUrl = document.getString(fieldName)

                        // Tambahkan log untuk melihat nilai fileUrl
                        Log.d("PDFViewer", "File URL: $fileUrl")

                        if (!fileUrl.isNullOrBlank()) {
                            // Membuka aplikasi eksternal untuk menampilkan file PDF
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.setDataAndType(Uri.parse(fileUrl), "application/pdf")
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            startActivity(intent)
                        } else {
                            Toast.makeText(context, "File tidak tersedia", Toast.LENGTH_SHORT).show()
                            isPdfViewerOpen = false // Atur kembali ke false jika file tidak tersedia
                        }
                    } else {
                        Log.d("PDFViewer", "Document tidak ada atau kosong")
                        Toast.makeText(context, "Gagal memuat file", Toast.LENGTH_SHORT).show()
                        isPdfViewerOpen = false // Atur kembali ke false jika terjadi kesalahan
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("PDFViewer", "Error getting document: $e")
                    Toast.makeText(context, "Gagal memuat file", Toast.LENGTH_SHORT).show()
                    isPdfViewerOpen = false // Atur kembali ke false jika terjadi kesalahan
                }
        }
    }
}