package com.example.jobseeker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.jobseeker.databinding.ActivityApplyForJobBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private lateinit var binding: ActivityApplyForJobBinding
private val db = FirebaseFirestore.getInstance()
private var loadingProgressBar: ProgressBar? = null
private lateinit var dimView: View
private const val REQUEST_PDF_FILE = 1
private var isApplyButtonClicked = false
private var hasAppliedBefore: Boolean = false
private var applyJobListener: ListenerRegistration? = null

class ApplyForJobActivity : AppCompatActivity() {

    private var selectedFileUri: Uri? = null
    private val storage = FirebaseStorage.getInstance()
    private val storageReference = storage.reference
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApplyForJobBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bottomSheetFragment = BottomSheetFragment()

        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        dimView = findViewById(R.id.dimView)

        binding.imageBack.setOnClickListener {
            onBackPressed()
        }

        binding.detailLamarSebelumnya.setOnClickListener {
            // Membuka BottomSheetFragment dan mengirimkan informasi pekerjaan
            val bottomSheetFragment = BottomSheetFragment()

            // Pastikan jobName sudah di-set sebelumnya
            val jobName = binding.jobName.text.toString() // Gantilah dengan informasi pekerjaan yang sesuai

            val bundle = Bundle()
            bundle.putString("jobName", jobName)
            bottomSheetFragment.arguments = bundle

            bottomSheetFragment.show(supportFragmentManager, "BottomSheetDialog")
        }

        binding.uploadFile.setOnClickListener {
            // Membuka aplikasi file atau galeri
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "application/pdf" // Filter hanya file PDF, sesuaikan dengan jenis file yang diizinkan

            try {
                startActivityForResult(intent, REQUEST_PDF_FILE)
            } catch (e: Exception) {
                // Tangani kesalahan jika aplikasi file tidak ditemukan
                Toast.makeText(this, "Aplikasi file tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        }

        val jobName = intent.getStringExtra("jobName")
        val location = intent.getStringExtra("location")
        val imageJobs = intent.getStringExtra("jobImage")
        val companyName = intent.getStringExtra("companyName")
        val workingTime = intent.getStringExtra("workingTime")
        val employees = intent.getStringExtra("employees")
        val salary = intent.getStringExtra("salary")

        binding.jobName.text = jobName
        binding.location.text = location

        // Load gambar menggunakan Picasso
        if (imageJobs != null && imageJobs.isNotEmpty()) {
            Picasso.get()
                .load(imageJobs)
                .error(R.drawable.image_home)
                .into(binding.imagePekerjaan)
        } else {
            binding.imagePekerjaan.setImageResource(R.drawable.image_home)
        }

        binding.namaPerusahaan.text = companyName
        binding.waktuKerja.text = workingTime
        binding.jumlahKaryawan.text = employees
        binding.gajiPekerjaan.text = salary

        binding.Apply.setOnClickListener {
            val description = binding.describe.text.toString()

            if (description.isNotEmpty() && selectedFileUri != null) {
                // Mendapatkan ID pengguna yang sedang masuk
                val userId = auth.currentUser?.uid

                if (userId != null) {
                    // Membaca informasi pengguna dari Firestore
                    db.collection("users").document(userId)
                        .get()
                        .addOnSuccessListener { documentSnapshot ->
                            // Memeriksa apakah dokumen pengguna ditemukan
                            if (documentSnapshot.exists()) {
                                // Mendapatkan informasi pengguna
                                val userData = documentSnapshot.data
                                val userName = userData?.get("fullname") as? String
                                val userEmail = userData?.get("email") as? String
                                val userPhoneNumber = userData?.get("phone_number") as? String
                                val userAddress = userData?.get("address") as? String

                                // Membuat objek data untuk diunggah ke Firebase Firestore
                                val applicationData = hashMapOf(
                                    "userId" to userId,
                                    "userName" to userName,
                                    "userEmail" to userEmail,
                                    "userPhoneNumber" to userPhoneNumber,
                                    "userAddress" to userAddress,
                                    "jobName" to jobName,
                                    "location" to location,
                                    "companyName" to companyName,
                                    "workingTime" to workingTime,
                                    "employees" to employees,
                                    "salary" to salary,
                                    "description" to description,
                                    "status" to "Menunggu konfirmasi", // Status default
                                    "date" to getCurrentDate() // Tanggal saat ini
                                )

                                // Mendapatkan referensi storage untuk file yang akan diunggah
                                val randomFileName = generateRandomFileName(10)
                                val formattedDate = getCurrentDateForFileName()
                                val fileRef = storageReference.child("apply_job_files/$formattedDate$randomFileName.pdf")

                                loadingProgressBar?.visibility = View.VISIBLE
                                dimView.visibility = View.VISIBLE

                                // Mengunggah file ke Firebase Storage
                                fileRef.putFile(selectedFileUri!!)
                                    .addOnSuccessListener { _ ->
                                        // Mendapatkan URL download file yang diunggah
                                        fileRef.downloadUrl.addOnSuccessListener { uri ->
                                            // Menambahkan URL download ke data yang akan diunggah ke Firestore
                                            applicationData["fileUrl"] = uri.toString()

                                            // Mengunggah data ke koleksi "apply_job" di Firebase Firestore
                                            db.collection("apply_job")
                                                .add(applicationData)
                                                .addOnSuccessListener { documentReference ->
                                                    // Menampilkan pesan sukses jika data berhasil diunggah
                                                    Toast.makeText(
                                                        this,
                                                        "Pengajuan berhasil",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                    // Melakukan navigasi ke halaman ApplyJobSuccessfully
                                                    val intent = Intent(this, ApplyJobSuccessfully::class.java)
                                                    intent.putExtra("jobName", jobName)
                                                    startActivity(intent)

                                                    // Menutup Activity saat ini
                                                    finish()
                                                }
                                                .addOnFailureListener { e ->
                                                    loadingProgressBar?.visibility = View.INVISIBLE
                                                    dimView.visibility = View.INVISIBLE
                                                    // Menampilkan pesan error jika terjadi kesalahan di Firestore
                                                    Toast.makeText(
                                                        this,
                                                        "Terjadi kesalahan dalam menyimpan data ke Firestore: $e",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        // Menampilkan pesan error jika terjadi kesalahan dalam mengunggah file
                                        Toast.makeText(
                                            this,
                                            "Terjadi kesalahan dalam mengunggah file: $e",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            } else {
                                // Dokumen pengguna tidak ditemukan
                                Toast.makeText(
                                    this,
                                    "Informasi pengguna tidak ditemukan",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            // Menampilkan pesan error jika terjadi kesalahan
                            Toast.makeText(
                                this,
                                "Terjadi kesalahan dalam membaca informasi pengguna: $e",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    // Menampilkan pesan jika pengguna tidak masuk (belum terautentikasi)
                    Toast.makeText(
                        this,
                        "Silakan masuk terlebih dahulu",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                // Menampilkan pesan jika keterangan atau file kosong
                if (description.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Keterangan tidak boleh kosong",
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (selectedFileUri == null) {
                    Toast.makeText(
                        this,
                        "File tidak boleh kosong",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Mendapatkan ID pengguna yang sedang masuk
        val userId = auth.currentUser?.uid

        // Mendapatkan informasi pekerjaan yang sedang dibuka

        if (userId != null) {
            // Mengecek apakah pengguna telah melamar pekerjaan sebelumnya
            checkPreviousApplication(userId, jobName)
        }
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy | HH:mm:ss", Locale.getDefault())
        val currentDate = Date()
        return dateFormat.format(currentDate)
    }

    private fun checkPreviousApplication(userId: String, jobName: String?) {
        if (jobName != null) {
            // Hapus listener sebelumnya jika ada
            applyJobListener?.remove()

            // Tambahkan listener pada koleksi "apply_job" untuk mendengarkan perubahan data
            applyJobListener = db.collection("apply_job")
                .whereEqualTo("userId", userId)
                .whereEqualTo("jobName", jobName)
                .addSnapshotListener { documents, error ->
                    if (error != null) {
                        // Tangani kesalahan jika terjadi
                        Toast.makeText(
                            this,
                            "Terjadi kesalahan dalam memeriksa pengajuan sebelumnya: $error",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@addSnapshotListener
                    }

                    // Jika ada dokumen yang terkait, tampilkan UI pengajuan sebelumnya
                    if (documents != null && !documents.isEmpty) {
                        hasAppliedBefore = true
                        showPreviousApplicationUI(documents.documents[0].data)
                        disableApplicationUI()
                    } else {
                        // Jika tidak ada dokumen yang terkait, sembunyikan CardView
                        hasAppliedBefore = false
                        enableApplicationUI()
                        hidePreviousApplicationCardView()
                    }
                }
        }
    }

    private fun hidePreviousApplicationCardView() {
        binding.cardLamarSebelumnya.visibility = View.GONE
        binding.textLamarSebelumnya.visibility = View.GONE
    }

    private fun disableApplicationUI() {
        binding.Apply.isEnabled = false
        binding.Apply.setBackgroundResource(R.drawable.bg_rounded_btn_false)
    }

    private fun enableApplicationUI() {
        binding.Apply.isEnabled = true
        binding.Apply.setBackgroundResource(R.drawable.bg_rounded_btn)
        submitApplication()
    }

    private fun submitApplication() {
        val description = binding.describe.text.toString()

        // Check if the Apply button is clicked before showing the message
        if (isApplyButtonClicked) {
            if (description.isNotEmpty() && selectedFileUri != null) {
                // (your job application code)
            } else {
                // Show the message only if the Apply button is clicked and either description or file is empty
                if (description.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Keterangan tidak boleh kosong",
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (selectedFileUri == null) {
                    Toast.makeText(
                        this,
                        "File tidak boleh kosong",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showPreviousApplicationUI(applicationData: Map<String, Any>?) {
        // Mengubah visibility elemen UI untuk menunjukkan bahwa pengguna telah melamar pekerjaan sebelumnya
        binding.textLamarSebelumnya.visibility = View.VISIBLE
        binding.cardLamarSebelumnya.visibility = View.VISIBLE

        // Menampilkan data keterangan pengajuan sebelumnya di TextView keterangan
        val description = applicationData?.get("description") as? String
        binding.keterangan.text = limitDescriptionLength(description)

        // Menampilkan nama file pengajuan sebelumnya di TextView namaFile
        val fileUrl = applicationData?.get("fileUrl") as? String
        val fileName = getFileName1(Uri.parse(fileUrl))
        binding.namaFile.text = limitFileNameLength(fileName)

        // Menampilkan tanggal dan status di TextView tanggal dan status
        val date = applicationData?.get("date") as? String
        val status = applicationData?.get("status") as? String
        binding.tanggal.text = date
        binding.status.text = status
        when (status) {
            "Diterima" -> {
                binding.status.setTextColor(ContextCompat.getColor(this, R.color.colorAccepted)) // Ganti dengan warna yang sesuai
            }
            "Ditolak" -> {
                binding.status.setTextColor(ContextCompat.getColor(this, R.color.colorRejected)) // Ganti dengan warna yang sesuai
            }
            else -> {
                binding.status.setTextColor(ContextCompat.getColor(this, R.color.defaultTextColor)) // Ganti dengan warna default jika diperlukan
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_PDF_FILE && resultCode == RESULT_OK) {
            selectedFileUri = data?.data

            if (selectedFileUri != null) {
                val fileName = getFileName(selectedFileUri!!)

                // Set visibility image_upload menjadi GONE dan image_upload2 menjadi VISIBLE
                binding.imageUpload.visibility = View.GONE
                binding.imageUpload2.visibility = View.VISIBLE

                // Set visibility text_upload menjadi GONE dan text_upload2 menjadi VISIBLE
                binding.textUpload.visibility = View.GONE
                binding.textUpload2.visibility = View.VISIBLE

                // Tampilkan nama file yang di-upload pada text_upload2
                binding.textUpload2.text = limitFileNameLength(fileName)
            } else {
                Toast.makeText(this, "Gagal memilih file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun limitDescriptionLength(description: String?): String {
        // Batasi panjang keterangan, misalnya maksimal 25 karakter
        val maxLength = 25

        if (description != null && description.length > maxLength) {
            // Jika lebih panjang dari maxLength, potong dan tambahkan elipsis
            return description.substring(0, maxLength - 3) + "..."
        }

        return description ?: ""
    }

    private fun limitFileNameLength(fileName: String?): String {
        // Batasi panjang nama file, misalnya maksimal 20 karakter
        val maxLength = 20

        if (fileName != null && fileName.length > maxLength) {
            // Jika lebih panjang dari maxLength, potong dan tambahkan elipsis
            return fileName.substring(0, maxLength - 3) + "..."
        }

        return fileName ?: ""
    }

    private fun getFileNameFromPath(path: String?): String? {
        return path?.substringAfterLast("/")
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    result = it.getString(displayNameIndex)
                }
            }
        }

        return result
    }

    private fun getFileName1(uri: Uri): String? {
        val fileNameFromPath = getFileNameFromPath(uri.lastPathSegment)
        return fileNameFromPath ?: uri.lastPathSegment
    }

    private fun getCurrentDateForFileName(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val currentDate = Date()
        return dateFormat.format(currentDate)
    }

    private fun generateRandomFileName(length: Int): String {
        val allowedChars = "0123456789"
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}