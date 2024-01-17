package com.example.jobseeker

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class HomeFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdapterListHome
    private lateinit var homeList : ArrayList<DataListHome?>
    private lateinit var shimmerFrameLayout: ShimmerFrameLayout
    private val db = FirebaseFirestore.getInstance()
    private lateinit var edSearch: EditText
    private lateinit var btnSearch: Button
    private lateinit var originalHomeList: ArrayList<DataListHome?>
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
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
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)

        // Set LayoutManager ke RecyclerView
        val layoutManager = LinearLayoutManager(activity)
        recyclerView.layoutManager = layoutManager

        // Set Adapter ke RecyclerView
        adapter = AdapterListHome(requireActivity(), originalHomeList)
        recyclerView.adapter = adapter

        // Atur listener untuk refresh
        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }

        fetchDataFromFirestore()

        btnSearch.setOnClickListener {
            // Panggil metode untuk melakukan pencarian
            searchJobs(edSearch.text.toString())
        }

        return view
    }

    private fun refreshData() {
        // Lakukan penarikan data baru dari Firestore saat di-refresh
        fetchDataFromFirestore()
        // Berhenti animasi refresh
        swipeRefreshLayout.isRefreshing = false
    }

    private fun fetchDataFromFirestore() {
        db.collection("job_vacancies")
            .get()
            .addOnSuccessListener { querySnapshot ->
                homeList.clear()
                originalHomeList.clear()

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

                    originalHomeList.addAll(homeList)

                    // Jika fragment sudah di-attach dan activity tidak null, update data
                    if (isAdded && activity != null) {
                        populateData()
                    }

                    shimmerFrameLayout.stopShimmer()
                    shimmerFrameLayout.visibility = View.GONE
                }
            }
            .addOnFailureListener { exception ->
                Log.e("HomeFragment", "Error getting job vacancies", exception)
            }
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

        // Tampilkan atau sembunyikan LinearLayout dataNotFound
        val dataNotFoundLayout = view?.findViewById<LinearLayout>(R.id.dataNotFound)
        dataNotFoundLayout?.visibility = if (homeList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun populateData() {
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Jika dalam mode landscape, set GridLayoutManager dengan 2 kolom
            val gridLayoutManager = GridLayoutManager(activity, 2)
            recyclerView.layoutManager = gridLayoutManager
        } else {
            // Jika dalam mode potrait, biarkan LayoutManager sesuai dengan default vertical
            val linearLayoutManager = LinearLayoutManager(activity)
            linearLayoutManager.stackFromEnd = true
            linearLayoutManager.reverseLayout = true
            recyclerView.layoutManager = linearLayoutManager
        }

        // Gunakan originalHomeList untuk inisialisasi adapter
        val adp = AdapterListHome(requireActivity(), originalHomeList)
        recyclerView.adapter = adp
    }
}