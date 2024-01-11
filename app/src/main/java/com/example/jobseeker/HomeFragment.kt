package com.example.jobseeker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class HomeFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var homeList : ArrayList<DataListHome?>
    private lateinit var shimmerFrameLayout: ShimmerFrameLayout
    private val db = FirebaseFirestore.getInstance()
    private lateinit var edSearch: EditText
    private lateinit var btnSearch: Button
    private lateinit var originalHomeList: ArrayList<DataListHome?>
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewHome)
        homeList = ArrayList<DataListHome?>()
        originalHomeList = ArrayList()
        shimmerFrameLayout = view.findViewById<ShimmerFrameLayout>(R.id.shimmerLayoutMemberHome)
        edSearch = view.findViewById(R.id.edSearch)
        btnSearch = view.findViewById(R.id.btnSearch)

        // Ambil data dari Firestore
        db.collection("job_vacancies")
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot != null) {
                    for (document in querySnapshot.documents) {
                        val image = document.getString("job_image") ?: ""
                        val jobName = document.getString("job_name") ?: ""
                        val location = document.getString("location") ?: ""
                        var salary = document.getString("salary") ?: ""
                        var working_time = document.getString("working_time") ?: ""
                        val documentId = document.id

                        homeList.add(DataListHome(documentId, image, jobName, location, working_time, salary, null))
                    }

                    // Simpan data asli untuk pencarian ulang
                    originalHomeList.addAll(homeList)

                    if (isAdded && activity != null) {
                        populateData()
                    }

                    shimmerFrameLayout.stopShimmer()
                    shimmerFrameLayout.visibility = View.GONE
                }
            }
            .addOnFailureListener { exception ->
                // Handle kesalahan jika terjadi
                Log.e("HomeFragment", "Error getting job vacancies", exception)
            }

        btnSearch.setOnClickListener {
            // Panggil metode untuk melakukan pencarian
            searchJobs(edSearch.text.toString())
        }

        return view
    }

    private fun searchJobs(keyword: String) {
        homeList.clear()

        // Lakukan pencarian berdasarkan nama pekerjaan pada data asli
        for (job in originalHomeList) {
            val jobName = job?.nama_pekerjaan ?: ""
            if (jobName.contains(keyword, ignoreCase = true)) {
                // Tambahkan ke hasil pencarian
                homeList.add(job)
            }
        }

        // Update RecyclerView dengan hasil pencarian
        val linearLayout = LinearLayoutManager(activity)
        linearLayout.stackFromEnd = true
        linearLayout.reverseLayout = true
        recyclerView.layoutManager = linearLayout

        val adp = AdapterListHome(requireActivity(), homeList)
        recyclerView.adapter = adp
    }

    private fun populateData() {
        val linearLayout = LinearLayoutManager(activity)
        linearLayout.stackFromEnd = true
        linearLayout.reverseLayout = true
        recyclerView.layoutManager = linearLayout

        // Gunakan originalHomeList untuk inisialisasi adapter
        val adp = AdapterListHome(requireActivity(), originalHomeList)
        recyclerView.adapter = adp
    }
}