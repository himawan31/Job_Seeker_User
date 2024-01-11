package com.example.jobseeker

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdapterListSearch (val context: Context, var searchList: ArrayList<DataListSearch?>): RecyclerView.Adapter<AdapterListSearch.MyViewHolder>() {

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val category_name = view.findViewById<TextView>(R.id.daftarSpesialisasi)
        val jobCountTextView: TextView = view.findViewById(R.id.jumlahjobs)
        val categoryImageView: ImageView = view.findViewById(R.id.daftarImgSearch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.fetch_list_search, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = searchList[position]
        holder.category_name.text = item?.category_name
        holder.jobCountTextView.text = "${item?.job_count} pekerjaan"

        // Logika untuk menentukan ikon berdasarkan nama kategori
        when (item?.category_name) {
            "Desain" -> holder.categoryImageView.setImageResource(R.drawable.ic_design)
            "Pendidikan" -> holder.categoryImageView.setImageResource(R.drawable.ic_pendidikan)
            "Restoran" -> holder.categoryImageView.setImageResource(R.drawable.ic_restoran)
            "Kesehatan" -> holder.categoryImageView.setImageResource(R.drawable.ic_health)
            "Programmer" -> holder.categoryImageView.setImageResource(R.drawable.ic_programmer)
            "Keuangan" -> holder.categoryImageView.setImageResource(R.drawable.ic_keuangan)

            // Tambahkan kategori lain jika diperlukan
            else -> {
                // Default icon jika kategori tidak dikenali
                holder.categoryImageView.setImageResource(R.drawable.image_work)
            }
        }
    }

    override fun getItemCount(): Int {
        return searchList.size
    }

    fun updateData(newList: ArrayList<DataListSearch?>) {
        searchList = newList
        notifyDataSetChanged()
    }
}