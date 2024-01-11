package com.example.jobseeker

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore


class SearchFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdapterListSearch

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewSearch)
        adapter = AdapterListSearch(requireContext(), ArrayList())

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter

        fetchDataFromFirestore()

        return view
    }

    private fun fetchDataFromFirestore() {
        // Initialize Firestore
        val firestore = FirebaseFirestore.getInstance()
        val categoriesRef = firestore.collection("categories")

        // Fetch data from Firestore
        categoriesRef.get()
            .addOnSuccessListener { categoryDocuments ->
                val searchList = ArrayList<DataListSearch?>()

                for (categoryDocument in categoryDocuments) {
                    val categoryName = categoryDocument.getString("category_name")

                    // Membuat objek DataListSearch dengan kategori
                    val dataListSearch = DataListSearch(category_name = categoryName ?: "", job_count = 0)

                    // Mendapatkan jumlah pekerjaan dari koleksi pekerjaan yang memiliki kategori tertentu
                    val jobsRef = firestore.collection("job_vacancies").whereEqualTo("category", categoryName)

                    jobsRef.get()
                        .addOnSuccessListener { jobDocuments ->
                            // Menambahkan jumlah pekerjaan ke dalam objek DataListSearch
                            val jobCount = jobDocuments.size()
                            Log.d("Firestore", "Category: $categoryName, Job Count: $jobCount")

                            // Menambahkan objek ke dalam list
                            dataListSearch.job_count = jobCount
                            searchList.add(dataListSearch)

                            // Memperbarui adapter dengan data baru
                            adapter.updateData(searchList)
                        }
                        .addOnFailureListener { exception ->
                            Log.e("Firestore", "Error getting job documents: ", exception)
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error getting category documents: ", exception)
            }
    }
}