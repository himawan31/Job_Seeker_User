package com.example.jobseeker

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.jobseeker.databinding.BottomsheetFragmentBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class BottomSheetFragment : BottomSheetDialogFragment(){

    private var _binding: BottomsheetFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var btnDeleteApply: CardView
    private lateinit var getKeterangan: TextView
    private lateinit var getNamaFile: TextView
    private lateinit var getTanggalInput: TextView
    private lateinit var getStatus: TextView
    private lateinit var imageStatus: ImageView
    private val db = FirebaseFirestore.getInstance()

    override fun getTheme(): Int = R.style.Theme_AppBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getKeterangan = binding.getKeterangan
        getNamaFile = binding.getNamaFile
        getTanggalInput = binding.getTanggalInput
        getStatus = binding.getStatus
        btnDeleteApply = binding.btnDeleteApply
        imageStatus = binding.imageStatus

        btnDeleteApply.setOnClickListener {
            val jobName = arguments?.getString("jobName")

            if (jobName != null) {
                // Hapus data lamaran berdasarkan nama pekerjaan
                deleteApplication(jobName)

            } else {
                // Tindakan jika nama pekerjaan tidak tersedia
            }
            dismiss()
        }

        // Dapatkan nama pekerjaan dari bundle
        val userID = FirebaseAuth.getInstance().currentUser?.uid

        val jobName = arguments?.getString("jobName")

        if (jobName != null && userID != null) {
            fetchDataFromFirebase(jobName, userID)
        } else {
            // Tindakan jika nama pekerjaan atau userID tidak tersedia
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun deleteApplication(jobName: String) {
        // Hapus data lamaran berdasarkan nama pekerjaan dari Firestore
        db.collection("apply_job")
            .whereEqualTo("jobName", jobName)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    // Ambil URL file dari dokumen
                    val fileUrl = document.getString("fileUrl")

                    // Hapus file dari penyimpanan Firebase
                    if (!fileUrl.isNullOrEmpty()) {
                        deleteFileFromStorage(fileUrl)
                    }

                    // Hapus dokumen dari Firestore
                    db.collection("apply_job")
                        .document(document.id)
                        .delete()
                        .addOnSuccessListener {
                            Log.d("BottomSheetFragment", "DocumentSnapshot successfully deleted!")
                        }
                        .addOnFailureListener { e ->
                            Log.w("BottomSheetFragment", "Error deleting document", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w("BottomSheetFragment", "Error getting documents", e)
            }
    }

    private fun deleteFileFromStorage(fileUrl: String) {
        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(fileUrl)

        storageReference.delete()
            .addOnSuccessListener {
                Log.d("BottomSheetFragment", "File successfully deleted from storage!")
            }
            .addOnFailureListener { e ->
                Log.w("BottomSheetFragment", "Error deleting file from storage", e)
            }
    }

    private fun fetchDataFromFirebase(jobName: String, userID: String) {
        if (isAdded) {  // Memastikan fragment masih terkait dengan aktivitas
            // Menggunakan whereEqualTo untuk mendapatkan dokumen dengan nama pekerjaan dan ID pengguna yang sesuai
            db.collection("apply_job")
                .whereEqualTo("jobName", jobName)
                .whereEqualTo("userId", userID)
                .limit(1)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val documentSnapshot = documents.documents[0]

                        // Ambil data dari dokumen
                        val description = documentSnapshot.getString("description")
                        val date = documentSnapshot.getString("date")
                        val fileUrl = documentSnapshot.getString("fileUrl")
                        val status = documentSnapshot.getString("status")

                        // Cek apakah data tidak null atau kosong sebelum menetapkan ke TextView
                        if (!description.isNullOrEmpty() && !date.isNullOrEmpty() && !fileUrl.isNullOrEmpty() && !status.isNullOrEmpty()) {
                            // Mendapatkan nama file dari URL menggunakan metode getFileName
                            val fileName = getFileName(Uri.parse(fileUrl))

                            // Setel data ke dalam TextView
                            getKeterangan.text = "$description"
                            getNamaFile.text = "$fileName"  // Menggunakan nama file, bukan URL
                            getTanggalInput.text = "$date"
                            getStatus.text = "$status"

                            // Mengubah warna teks pada TextView getStatus jika status adalah "Diterima"
                            if (status.equals("Diterima", ignoreCase = true)) {
                                // Ganti dengan warna hijau yang diinginkan
                                getStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorAccepted))
                                // Ganti src pada ImageView
                                imageStatus.setImageResource(R.drawable.baseline_check_circle)
                                // hilangkan cardView btnDeteleApply
                                btnDeleteApply.visibility = View.GONE
                            } else if (status.equals("Ditolak", ignoreCase = true)) {
                                // Mengubah warna teks pada TextView getStatus jika status adalah "Ditolak"
                                getStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorRejected)) // Ganti dengan warna merah yang diinginkan
                                // Ganti src pada ImageView
                                imageStatus.setImageResource(R.drawable.baseline_cancel)
                                // hilangkan cardView btnDeteleApply
                                btnDeleteApply.visibility = View.GONE
                            } else {
                                // Set warna default jika status bukan "Diterima" atau "Ditolak"
                                getStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.defaultTextColor))
                                // Ganti src pada ImageView ke gambar default atau yang diinginkan
                                imageStatus.setImageResource(R.drawable.baseline_hourglass_top) // Sesuaikan dengan sumber gambar yang diinginkan
                            }
                        } else {
                            // Tindakan yang perlu diambil jika data tidak lengkap
                            Log.e("BottomSheetFragment", "Data is incomplete or empty")
                        }
                    } else {
                        // Dokumen tidak ditemukan
                        // Tindakan yang perlu diambil jika dokumen tidak ditemukan
                        Log.e("BottomSheetFragment", "Document not found")
                    }
                }
                .addOnFailureListener { e ->
                    // Tangani kesalahan saat mengambil data dari Firebase
                    // Tindakan yang perlu diambil jika terjadi kesalahan
                    e.printStackTrace() // Tampilkan log kesalahan
                    Log.e("BottomSheetFragment", "Error fetching data from Firestore: $e")
                }
        }
    }

    private fun getFileNameFromPath(path: String?): String? {
        return path?.substringAfterLast("/")
    }

    private fun getFileName(uri: Uri): String? {
        val fileNameFromPath = getFileNameFromPath(uri.lastPathSegment)
        return fileNameFromPath ?: uri.lastPathSegment
    }
}