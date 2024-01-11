package com.example.jobseeker

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class AdapterListHome (val context: Context, val homeList: ArrayList<DataListHome?>): RecyclerView.Adapter<AdapterListHome.MyViewHolder>() {
    private val auth = FirebaseAuth.getInstance()
    private val userId = auth.currentUser?.uid
    private val db = FirebaseFirestore.getInstance()
    private val userRef = db.collection("users").document(userId ?: "")

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image_pekerjaan = view.findViewById<ImageView>(R.id.image_pekerjaan)
        val nama_pekerjaan = view.findViewById<TextView>(R.id.nama_pekerjaan)
        val lokasi_pekerjaan = view.findViewById<TextView>(R.id.lokasi_pekerjaan)
        val btn_saved = view.findViewById<ImageView>(R.id.btnSaved)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.fetch_list_home, parent, false)
        return AdapterListHome.MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = homeList[position]

        val userId = auth.currentUser?.uid

        if (userId != null) {
            // Operasi yang melibatkan userId
            if (currentItem?.image_pekerjaan!!.isNotEmpty()) {
                Picasso.get()
                    .load(currentItem?.image_pekerjaan)
                    .error(R.drawable.image_home) // Gambar error jika gagal memuat gambar
                    .into(holder.image_pekerjaan)
            } else {
                // Jika path gambar kosong, tampilkan gambar placeholder atau gambar lain yang sesuai
                holder.image_pekerjaan.setImageResource(R.drawable.image_home_1)
            }

            holder.nama_pekerjaan.text = currentItem?.nama_pekerjaan
            holder.lokasi_pekerjaan.text = currentItem?.lokasi_pekerjaan

            holder.itemView.setOnClickListener {
                onItemClickListener?.onItemClick(position)
                // Pindah ke DetailJobActivity dengan mengirim ID pekerjaan
                currentItem?.documentId?.let { documentId ->
                    moveToDetailJobActivity(documentId)
                }
            }

            currentItem?.documentId?.let { documentId ->
                val savedJobsRef = userRef.collection("savedJobs").document(documentId)

                savedJobsRef.get()
                    .addOnSuccessListener { documentSnapshot ->
                        val isJobSaved = documentSnapshot.exists()

                        // Setel gambar btn_saved sesuai dengan keberadaan dokumen
                        if (isJobSaved) {
                            holder.btn_saved.setImageResource(R.drawable.image_save)
                        } else {
                            holder.btn_saved.setImageResource(R.drawable.image_save_no_fill)
                        }
                    }
                    .addOnFailureListener { e ->
                        // Handle kesalahan jika ada
                        Log.e("AdapterListHome", "Error getting saved status", e)
                    }
            }

            holder.btn_saved.setOnClickListener {
                val currentItem =
                    homeList[position] ?: return@setOnClickListener // Handle jika currentItem null

                val documentId = currentItem.documentId ?: return@setOnClickListener

                val savedJobsRef = userRef.collection("savedJobs").document(documentId)

                val savedJobData = hashMapOf(
                    "job_image" to currentItem.image_pekerjaan,
                    "job_name" to currentItem.nama_pekerjaan,
                    "location" to currentItem.lokasi_pekerjaan,
                    "working_time" to currentItem.salary,  // Tambahkan working_time ke savedJobs
                    "salary" to currentItem.working_time,  // Tambahkan salary ke savedJobs
                )

                savedJobsRef.get()
                    .addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            // Dokumen sudah ada, hapus job dari koleksi "savedJobs"
                            savedJobsRef.delete()
                                .addOnSuccessListener {
                                    Log.d(
                                        "AdapterListHome",
                                        "Job removed from savedJobs successfully"
                                    )

                                    // Update UI setelah menghapus
                                    holder.btn_saved.setImageResource(R.drawable.image_save_no_fill)
                                }
                                .addOnFailureListener { e ->
                                    Log.e(
                                        "AdapterListHome",
                                        "Error removing job from savedJobs",
                                        e
                                    )
                                }
                        } else {
                            // Dokumen belum ada, tambahkan job ke koleksi "savedJobs"
                            savedJobsRef.set(savedJobData)
                                .addOnSuccessListener {
                                    Log.d(
                                        "AdapterListHome",
                                        "Job added to savedJobs successfully"
                                    )

                                    // Update UI setelah menambahkan
                                    holder.btn_saved.setImageResource(R.drawable.image_save)
                                }
                                .addOnFailureListener { e ->
                                    Log.e(
                                        "AdapterListHome",
                                        "Error adding job to savedJobs",
                                        e
                                    )
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        // Handle kesalahan jika ada
                        Log.e(
                            "AdapterListHome",
                            "Error checking existence of document in savedJobs",
                            e
                        )
                    }
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    private var onItemClickListener: OnItemClickListener? = null

    private fun moveToDetailJobActivity(documentId: String) {
        val intent = Intent(context, DetailJobActivity::class.java)

        // Ambil data dari currentItem sesuai dengan documentId
        val currentItem = homeList.firstOrNull { it?.documentId == documentId }

        currentItem?.let {
            // Tambahkan data ke intent
            intent.putExtra("jobName", it.nama_pekerjaan)
            intent.putExtra("location", it.lokasi_pekerjaan)
            intent.putExtra("documentId", it.documentId)

            // Tambahan: Kirim data lain yang diperlukan ke DetailJobActivity jika perlu

            // Start activity dengan intent yang sudah berisi data
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return homeList.size
    }
}