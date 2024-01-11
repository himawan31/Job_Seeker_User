package com.example.jobseeker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.jobseeker.databinding.ActivityDetailJobBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class DetailJobActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailJobBinding
    private val db = FirebaseFirestore.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailJobBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imageBack.setOnClickListener {
            onBackPressed()
        }

        // Ambil data dari intent
        val jobName = intent.getStringExtra("jobName")
        val location = intent.getStringExtra("location")
        val documentId = intent.getStringExtra("documentId") ?: ""

        // Tampilkan data pada elemen UI yang sesuai (contoh: TextView)
        binding.jobName.text = jobName
        binding.location.text = location

        // Tambahan: Tampilkan data lainnya jika perlu

        // Ambil data dari Firestore
        val jobVacanciesRef = db.collection("job_vacancies").document(documentId)

        jobVacanciesRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Ambil data dari Firestore
                    val jobImage = documentSnapshot.getString("job_image")
                    val companyName = documentSnapshot.getString("company_name") ?: ""
                    val workingTime = documentSnapshot.getString("working_time") ?: ""
                    val employees = documentSnapshot.getString("employees") ?: ""
                    val salary = documentSnapshot.getString("salary") ?: ""

                    // Ambil data sebagai List<String> dari Firestore
                    val benefitList = documentSnapshot.get("benefit") as? List<String>
                    val jobDescriptionList = documentSnapshot.get("job_description") as? List<String>
                    val jobRequirementsList = documentSnapshot.get("job_requirements") as? List<String>

                    // Konversi List<String> ke String untuk ditampilkan pada elemen UI
                    val benefit = formatListWithNumbers(benefitList)
                    val jobDescription = formatListWithNumbers(jobDescriptionList)
                    val jobRequirements = formatListWithNumbers(jobRequirementsList)

                    // Tampilkan data pada elemen UI yang sesuai
                    binding.namaPerusahaan.text = companyName
                    binding.waktuKerja.text = workingTime
                    binding.jumlahKaryawan.text = employees
                    binding.gajiPekerjaan.text = salary
                    binding.benefit.text = benefit
                    binding.deskripsiPekerjaan.text = jobDescription
                    binding.persyaratanPekerjaan.text = jobRequirements

                    // Muat gambar menggunakan Picasso
                    if (jobImage != null && jobImage.isNotEmpty()) {
                        Picasso.get()
                            .load(jobImage)
                            .error(R.drawable.image_home)
                            .into(binding.imagePekerjaan)
                    } else {
                        binding.imagePekerjaan.setImageResource(R.drawable.image_home)
                    }
                }
            }
            .addOnFailureListener { e ->
                // Handle kesalahan jika ada
                e.printStackTrace()
            }

        binding.ApplyForJob.setOnClickListener {
            val companyName = binding.namaPerusahaan.text.toString()
            val workingTime = binding.waktuKerja.text.toString()
            val employees = binding.jumlahKaryawan.text.toString()
            val salary = binding.gajiPekerjaan.text.toString()

            // Mendapatkan data dari Firestore kembali
            val jobVacanciesRef = db.collection("job_vacancies").document(documentId)

            jobVacanciesRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        // Ambil path gambar dari Firestore
                        val jobImage = documentSnapshot.getString("job_image")

                        val intent = Intent(this, ApplyForJobActivity::class.java)

                        intent.putExtra("jobName", jobName)
                        intent.putExtra("location", location)
                        intent.putExtra("jobImage", jobImage) // Kirim path gambar ke activity selanjutnya
                        intent.putExtra("companyName", companyName)
                        intent.putExtra("workingTime", workingTime)
                        intent.putExtra("employees", employees)
                        intent.putExtra("salary", salary)

                        startActivity(intent)
                    }
                }
                .addOnFailureListener { e ->
                    // Tangani kesalahan jika ada
                    e.printStackTrace()
                }
        }
    }

    // Fungsi untuk memformat List<String> dengan nomor urut atau pesan "Data Tidak Tersedia"
    private fun formatListWithNumbers(list: List<String>?): String {
        return if (list.isNullOrEmpty()) {
            "Data Tidak Tersedia"
        } else {
            val formattedList = StringBuilder()
            for ((index, item) in list.withIndex()) {
                // Tambahkan nomor urut dan elemen ke formattedList
                formattedList.append("${index + 1}. $item\n")
            }
            formattedList.toString()
        }
    }
}