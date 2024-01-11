package com.example.jobseeker

import android.content.Context
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

class AdapterListSaved(val context: Context, val savedList: ArrayList<DataListSaved>): RecyclerView.Adapter<AdapterListSaved.MyViewHolder>() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userId: String?
        get() {
            val user = auth.currentUser
            return user?.uid
        }

    interface OnItemClickListener {
        fun onItemClick(position: Int, documentId: String)
    }

    private var onItemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        onItemClickListener = listener
    }

    class MyViewHolder(view: View): RecyclerView.ViewHolder(view){
        val image_pekerjaan = view.findViewById<ImageView>(R.id.imagePekerjaan)
        val nama_pekerjaan = view.findViewById<TextView>(R.id.namaPekerjaan)
        val waktu_kerja = view.findViewById<TextView>(R.id.waktuKerja)
        val lokasi_pekerjaan = view.findViewById<TextView>(R.id.lokasiPekerjaan)
        val gaji_pekerjaan = view.findViewById<TextView>(R.id.gajiPekerjaan)
        val btn_saved = view.findViewById<ImageView>(R.id.btnSaved)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.fetch_list_saved, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentJob = savedList[position]

        // Set data ke view pada ViewHolder
        holder.nama_pekerjaan.text = currentJob.nama_pekerjaan
        holder.waktu_kerja.text = currentJob.waktu_kerja
        holder.lokasi_pekerjaan.text = currentJob.lokasi_pekerjaan
        holder.gaji_pekerjaan.text = currentJob.gaji_pekerjaan

        // Mengecek dan memuat gambar menggunakan Picasso atau menetapkan gambar default
        if (currentJob.image_pekerjaan.isNotBlank()) {
            Picasso.get()
                .load(currentJob.image_pekerjaan)
                .into(holder.image_pekerjaan)
        } else {
            holder.image_pekerjaan.setImageResource(R.drawable.image_home_1)
        }

        // Mengecek apakah pekerjaan sudah disimpan oleh pengguna
        val savedJobsRef = db.collection("users").document(userId!!)
            .collection("savedJobs").document(currentJob.documentId)
        savedJobsRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Pekerjaan sudah disimpan
                    holder.btn_saved.setImageResource(R.drawable.image_save)
                } else {
                    // Pekerjaan belum disimpan
                    holder.btn_saved.setImageResource(R.drawable.image_save_no_fill)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("AdapterListSaved", "Error saat mengecek apakah pekerjaan sudah disimpan", exception)
            }

        // Menetapkan onClickListener untuk tombol simpan
        holder.btn_saved.setOnClickListener {
            // Implementasikan logika untuk menyimpan atau tidak menyimpan pekerjaan
            toggleSavedState(currentJob.documentId, position)
        }

        // Menambahkan onClickListener untuk membuka DetailJobActivity
        holder.itemView.setOnClickListener {
            onItemClickListener?.onItemClick(position, currentJob.documentId)
        }
    }

    private fun toggleSavedState(documentId: String, position: Int) {
        val savedJobsRef = db.collection("users").document(userId!!)
            .collection("savedJobs").document(documentId)

        savedJobsRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Pekerjaan sudah disimpan, hapus dari koleksi
                    savedJobsRef.delete()
                        .addOnSuccessListener {
                            Log.d("AdapterListSaved", "Pekerjaan dihapus dari savedJobs: $documentId")

                            // Pemeriksaan ukuran savedList sebelum menghapus
                            if (position >= 0 && position < savedList.size) {
                                savedList.removeAt(position)
                                notifyDataSetChanged()
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("AdapterListSaved", "Error saat menghapus pekerjaan dari savedJobs", exception)
                        }
                } else {
                    // Pekerjaan belum disimpan, simpan ke dalam koleksi
                    // Anda dapat menambahkan logika simpan di sini jika diperlukan
                }
            }
            .addOnFailureListener { exception ->
                Log.e("AdapterListSaved", "Error saat mengecek apakah pekerjaan sudah disimpan", exception)
            }
    }


    override fun getItemCount(): Int {
        return savedList.size
    }
}