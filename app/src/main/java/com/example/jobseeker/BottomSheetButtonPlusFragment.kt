package com.example.jobseeker

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import com.example.jobseeker.databinding.BottomsheetButtonPlusFragmentBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BottomSheetButtonPlusFragment : BottomSheetDialogFragment() {

    private var _binding: BottomsheetButtonPlusFragmentBinding? = null
    private val binding get() = _binding!!

    private val PICK_FILE_REQUEST = 1
    private var selectedFileUri: Uri? = null
    private var currentFieldName: String? = null
    private var loadingProgressBar1: ProgressBar? = null
    private var loadingProgressBar2: ProgressBar? = null
    private var loadingProgressBar3: ProgressBar? = null


    override fun getTheme(): Int = R.style.Theme_AppBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetButtonPlusFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun convertFieldNameToFile(fieldName: String): String {
        return when (fieldName) {
            "file_resume" -> "Resume"
            "file_portofolio" -> "Portofolio"
            "file_certificate" -> "Sertifikat"
            else -> fieldName // Jika tidak ada konversi yang sesuai, kembalikan nilai awal
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingProgressBar1 = view.findViewById(R.id.loadingProgressBar1)
        loadingProgressBar2 = view.findViewById(R.id.loadingProgressBar2)
        loadingProgressBar3 = view.findViewById(R.id.loadingProgressBar3)


        binding.layoutResume.setOnClickListener {
            checkFileStatus("file_resume")
        }

        binding.layoutPortofolio.setOnClickListener {
            checkFileStatus("file_portofolio")
        }

        binding.layoutSertifikasi.setOnClickListener {
            checkFileStatus("file_certificate")
        }
    }

    private fun checkFileStatus(fieldName: String) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user != null) {
            val userRef = FirebaseFirestore.getInstance().collection("users").document(user.uid)

            userRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Cek apakah field sudah ada
                        if (document.contains(fieldName)) {
                            // Jika ada, tampilkan dialog edit dan hapus
                            showEditDeleteDialog(fieldName)
                        } else {
                            // Jika tidak ada, buka file explorer
                            openFileExplorer(fieldName)
                        }
                    } else {
                        Toast.makeText(context, "Gagal mendapatkan informasi pengguna", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Gagal memeriksa status $fieldName: $e", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Handle the case where the user is not authenticated
            Toast.makeText(context, "User not authenticated.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditDeleteDialog(fieldName: String) {
        val dialog = AlertDialog.Builder(requireContext())
        dialog.setTitle("File ${convertFieldNameToFile(fieldName)} sudah diupload")
        dialog.setMessage("Apa yang ingin Anda lakukan?")
        dialog.setPositiveButton("Edit") { _, _ ->
            // Implementasi logika untuk membuka file explorer atau tindakan lainnya jika ingin mengedit
            openFileExplorer(fieldName)
        }
        dialog.setNegativeButton("Hapus") { _, _ ->
            // Implementasi logika untuk menghapus
            deleteFile(fieldName)
        }
        dialog.setNeutralButton("Batal") { _, _ ->
            // Tidak melakukan apa-apa jika memilih batal
        }
        dialog.show()
    }

    private fun deleteFile(fieldName: String) {
        try {
            // Implementasi logika untuk menghapus
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser

            if (user != null) {
                val userRef = FirebaseFirestore.getInstance().collection("users").document(user.uid)

                // Menampilkan loading bar sebelum proses penghapusan
                when (fieldName) {
                    "file_resume" -> loadingProgressBar1?.visibility = View.VISIBLE
                    "file_portofolio" -> loadingProgressBar2?.visibility = View.VISIBLE
                    "file_certificate" -> loadingProgressBar3?.visibility = View.VISIBLE
                }

                userRef.get().addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Mengambil URL file dari Firestore
                        val fileUrl = document.getString(fieldName)

                        // Jika file ada, hapus dari Firestore dan Firebase Storage
                        if (!fileUrl.isNullOrBlank()) {
                            // Hapus dari Firestore
                            userRef.update(fieldName, FieldValue.delete())
                                .addOnSuccessListener {
                                    // Hapus dari Firebase Storage
                                    val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(fileUrl)
                                    storageRef.delete()
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                context,
                                                "File ${convertFieldNameToFile(fieldName)} berhasil dihapus",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(
                                                context,
                                                "Gagal menghapus file dari Firebase Storage: $e",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Gagal menghapus $fieldName dari Firestore: $e", Toast.LENGTH_SHORT).show()
                                }
                                .addOnCompleteListener {
                                    // Menyembunyikan loading bar setelah penghapusan selesai
                                    when (fieldName) {
                                        "file_resume" -> loadingProgressBar1?.visibility = View.GONE
                                        "file_portofolio" -> loadingProgressBar2?.visibility = View.GONE
                                        "file_certificate" -> loadingProgressBar3?.visibility = View.GONE
                                    }
                                }
                        } else {
                            // Handle jika file tidak ada
                            Toast.makeText(context, "$fieldName tidak tersedia", Toast.LENGTH_SHORT).show()
                            // Menyembunyikan loading bar karena penghapusan tidak terjadi
                            when (fieldName) {
                                "file_resume" -> loadingProgressBar1?.visibility = View.GONE
                                "file_portofolio" -> loadingProgressBar2?.visibility = View.GONE
                                "file_certificate" -> loadingProgressBar3?.visibility = View.GONE
                            }
                        }
                    }
                }
            } else {
                // Handle the case where the user is not authenticated
                Toast.makeText(context, "User not authenticated.", Toast.LENGTH_SHORT).show()

                // Menyembunyikan loading bar karena penghapusan tidak terjadi
                when (fieldName) {
                    "file_resume" -> loadingProgressBar1?.visibility = View.GONE
                    "file_portofolio" -> loadingProgressBar2?.visibility = View.GONE
                    "file_certificate" -> loadingProgressBar3?.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()

            // Menyembunyikan loading bar karena penghapusan tidak terjadi
            when (fieldName) {
                "file_resume" -> loadingProgressBar1?.visibility = View.GONE
                "file_portofolio" -> loadingProgressBar2?.visibility = View.GONE
                "file_certificate" -> loadingProgressBar3?.visibility = View.GONE
            }
            Toast.makeText(context, "Terjadi kesalahan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFileExplorer(fieldName: String) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf" // Hanya file PDF yang diizinkan
        startActivityForResult(intent, PICK_FILE_REQUEST)
        currentFieldName = fieldName // Pindahkan inisialisasi currentFieldName ke sini
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { fileUri ->
                val fieldName = currentFieldName
                if (fieldName != null) {
                    selectedFileUri = fileUri
                    // Lakukan proses upload ke Firebase Storage
                    uploadFileToFirebaseStorage(fieldName)

                    // Reset currentFieldName menjadi null setelah penggunaannya
                    currentFieldName = null
                } else {
                    // Handle jika fieldName null
                    Toast.makeText(context, "Error: fieldName is null", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun uploadFileToFirebaseStorage(fieldName: String) {
        try {
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser

            if (user != null) {
                selectedFileUri?.let { uri ->
                    // Memeriksa tipe MIME file
                    val mimeType = context?.contentResolver?.getType(uri)
                    if (mimeType == "application/pdf") { // Hanya mengizinkan file PDF
                        val currentDate = getCurrentDateForFileName()
                        val randomFileName = generateRandomFileName(10)
                        val fileName = "${fieldName}_${currentDate}$randomFileName.pdf"

                        val storageReference: StorageReference =
                            FirebaseStorage.getInstance().getReference("users/${user.uid}/$fileName")

                        // Menampilkan loading bar sebelum upload
                        when (fieldName) {
                            "file_resume" -> loadingProgressBar1?.visibility = View.VISIBLE
                            "file_portofolio" -> loadingProgressBar2?.visibility = View.VISIBLE
                            "file_certificate" -> loadingProgressBar3?.visibility = View.VISIBLE
                        }

                        storageReference.putFile(uri)
                            .addOnSuccessListener { taskSnapshot ->
                                taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUrl ->
                                    // Mendapatkan URL download file setelah berhasil diunggah
                                    val fileUrl = downloadUrl.toString()

                                    // Memperbarui dokumen pengguna di Firebase Firestore dengan URL
                                    val firestore = FirebaseFirestore.getInstance()
                                    val userRef = firestore.collection("users").document(user.uid)

                                    // Mengambil URL file lama dari Firestore untuk dihapus
                                    userRef.get().addOnSuccessListener { document ->
                                        if (document != null && document.exists()) {
                                            val oldFileUrl = document.getString(fieldName)

                                            // Jika file lama ada, hapus dari Firestore dan Firebase Storage
                                            if (!oldFileUrl.isNullOrBlank()) {
                                                // Hapus dari Firestore
                                                userRef.update(fieldName, FieldValue.delete())
                                                    .addOnSuccessListener {
                                                        // Hapus dari Firebase Storage
                                                        val oldStorageRef =
                                                            FirebaseStorage.getInstance().getReferenceFromUrl(oldFileUrl)
                                                        oldStorageRef.delete()
                                                            .addOnSuccessListener {
                                                                // Hapus berhasil, lanjutkan dengan menyimpan URL baru
                                                                userRef.update(fieldName, fileUrl)
                                                                    .addOnSuccessListener {
                                                                        Toast.makeText(
                                                                            context,
                                                                            "File ${convertFieldNameToFile(fieldName)} berhasil diunggah",
                                                                            Toast.LENGTH_SHORT
                                                                        ).show()
                                                                    }
                                                                    .addOnFailureListener { e ->
                                                                        Toast.makeText(
                                                                            context,
                                                                            "Gagal menyimpan URL $fieldName: $e",
                                                                            Toast.LENGTH_SHORT
                                                                        ).show()
                                                                    }
                                                            }
                                                            .addOnFailureListener { e ->
                                                                Toast.makeText(
                                                                    context,
                                                                    "Gagal menghapus file lama dari Firebase Storage: $e",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Toast.makeText(
                                                            context,
                                                            "Gagal menghapus $fieldName dari Firestore: $e",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                    .addOnCompleteListener {
                                                        // Menyembunyikan loading bar setelah operasi selesai
                                                        when (fieldName) {
                                                            "file_resume" -> loadingProgressBar1?.visibility =
                                                                View.GONE
                                                            "file_portofolio" -> loadingProgressBar2?.visibility =
                                                                View.GONE
                                                            "file_certificate" -> loadingProgressBar3?.visibility =
                                                                View.GONE
                                                        }
                                                    }
                                            } else {
                                                // File lama tidak ada, lanjutkan dengan menyimpan URL baru
                                                userRef.update(fieldName, fileUrl)
                                                    .addOnSuccessListener {
                                                        Toast.makeText(
                                                            context,
                                                            "File ${convertFieldNameToFile(fieldName)} berhasil diunggah",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Toast.makeText(
                                                            context,
                                                            "Gagal menyimpan URL $fieldName: $e",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                    .addOnCompleteListener {
                                                        // Menyembunyikan loading bar setelah operasi selesai
                                                        when (fieldName) {
                                                            "file_resume" -> loadingProgressBar1?.visibility =
                                                                View.GONE
                                                            "file_portofolio" -> loadingProgressBar2?.visibility =
                                                                View.GONE
                                                            "file_certificate" -> loadingProgressBar3?.visibility =
                                                                View.GONE
                                                        }
                                                    }
                                            }
                                        }
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Gagal mengunggah file: $e", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(context, "Format file tidak didukung. Hanya file PDF yang diizinkan.", Toast.LENGTH_SHORT).show()
                        // Menyembunyikan loading bar karena upload tidak terjadi
                        when (fieldName) {
                            "file_resume" -> loadingProgressBar1?.visibility = View.GONE
                            "file_portofolio" -> loadingProgressBar2?.visibility = View.GONE
                            "file_certificate" -> loadingProgressBar3?.visibility = View.GONE
                            else -> {
                            }
                        }
                    }
                }
            } else {
                // Handle the case where the user is not authenticated
                Toast.makeText(context, "User not authenticated.", Toast.LENGTH_SHORT).show()
                // Menyembunyikan loading bar karena upload tidak terjadi
                when (fieldName) {
                    "file_resume" -> loadingProgressBar1?.visibility = View.GONE
                    "file_portofolio" -> loadingProgressBar2?.visibility = View.GONE
                    "file_certificate" -> loadingProgressBar3?.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Terjadi kesalahan", Toast.LENGTH_SHORT).show()
            // Menyembunyikan loading bar karena upload tidak terjadi
            when (fieldName) {
                "file_resume" -> loadingProgressBar1?.visibility = View.GONE
                "file_portofolio" -> loadingProgressBar2?.visibility = View.GONE
                "file_certificate" -> loadingProgressBar3?.visibility = View.GONE
            }
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}