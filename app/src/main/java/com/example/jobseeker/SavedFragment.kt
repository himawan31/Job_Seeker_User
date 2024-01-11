package com.example.jobseeker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class SavedFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var savedList : ArrayList<DataListSaved>
    private lateinit var shimmerFrameLayout: ShimmerFrameLayout
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var userId: String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_saved, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewSaved)
        savedList = ArrayList()
        shimmerFrameLayout = view.findViewById(R.id.shimmerLayoutSaved)
        val layoutNoData: LinearLayout = view.findViewById(R.id.dataNotFound)

        userId = auth.currentUser?.uid

        // Hanya mengambil data dari koleksi "savedJobs" di dalam koleksi "users"
        userId?.let { uid ->
            db.collection("users").document(uid).collection("savedJobs")
                .addSnapshotListener { querySnapshot, exception ->
                    if (exception != null) {
                        // Handle kesalahan jika terjadi
                        Log.e("SavedFragment", "Error fetching saved jobs data", exception)
                        return@addSnapshotListener
                    }

                    // Hapus semua item sebelum menambahkan yang baru
                    savedList.clear()

                    // Ambil data pekerjaan dari koleksi "savedJobs"
                    for (document in querySnapshot!!.documents) {
                        val image = document.getString("job_image") ?: ""
                        val jobName = document.getString("job_name") ?: ""
                        val location = document.getString("location") ?: ""
                        val workingTime = document.getString("working_time") ?: ""
                        val salary = document.getString("salary") ?: ""

                        savedList.add(
                            DataListSaved(
                                document.id,
                                image,
                                jobName,
                                workingTime,
                                salary,
                                location
                            )
                        )
                    }

                    // Tampilkan data pada RecyclerView
                    populateData()

                    // Tampilkan atau sembunyikan pesan "Data tidak tersedia"
                    layoutNoData.visibility = if (savedList.isEmpty()) View.VISIBLE else View.GONE

                    shimmerFrameLayout.stopShimmer()
                    shimmerFrameLayout.visibility = View.GONE
                }
        }
        return view
    }

    private fun populateData() {
        if (isAdded) { // Periksa apakah fragment masih terpasang
            val linearLayout = LinearLayoutManager(activity)
            linearLayout.stackFromEnd = true
            linearLayout.reverseLayout = true
            recyclerView.layoutManager = linearLayout

            // Buat adapter dan set onItemClickListener
            val adapter = AdapterListSaved(requireActivity(), savedList)
            adapter.setOnItemClickListener(object : AdapterListSaved.OnItemClickListener {
                override fun onItemClick(position: Int, documentId: String) {
                    // Ambil data dari item yang diklik
                    val clickedItem = savedList[position]

                    // Implementasikan logika untuk membuka DetailJobActivity
                    moveToDetailJobActivity(
                        documentId,
                        clickedItem.nama_pekerjaan,
                        clickedItem.lokasi_pekerjaan
                    )
                }
            })

            recyclerView.adapter = adapter
            adapter.notifyDataSetChanged()
        }
    }

    private fun moveToDetailJobActivity(documentId: String, jobName: String, location: String) {
        val intent = Intent(requireContext(), DetailJobActivity::class.java)
        // Sertakan data yang diperlukan untuk DetailJobActivity
        intent.putExtra("documentId", documentId)
        intent.putExtra("jobName", jobName)
        intent.putExtra("location", location)
        startActivity(intent)
    }
}