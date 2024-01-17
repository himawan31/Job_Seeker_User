package com.example.jobseeker

import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore


class SearchFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdapterListSearch
    private lateinit var searchBar: EditText
    private lateinit var originalSearchList: ArrayList<DataListSearch?>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewSearch)
        adapter = AdapterListSearch(requireContext(), ArrayList())

        // Set LayoutManager dengan jumlah kolom berdasarkan orientasi layar
        val columns = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else 2
        recyclerView.layoutManager = GridLayoutManager(requireContext(), columns)
        recyclerView.adapter = adapter

        searchBar = view.findViewById(R.id.searchBar)

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No action needed before text changes
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No action needed when text is changing
            }

            override fun afterTextChanged(s: Editable?) {
                // When text changes, filter categories based on the entered text
                filterCategories(s.toString())
            }
        })

        fetchDataFromFirestore()

        return view
    }

    private fun filterCategories(query: String) {
        if (::originalSearchList.isInitialized) {
            val filteredList = ArrayList<DataListSearch?>()
            for (data in originalSearchList) {
                if (data?.category_name?.toLowerCase()?.contains(query.toLowerCase()) == true) {
                    filteredList.add(data)
                }
            }
            adapter.updateData(filteredList)
        } else {
            Log.e("SearchFragment", "originalSearchList has not been initialized yet.")
        }
    }

    private fun fetchDataFromFirestore() {
        val firestore = FirebaseFirestore.getInstance()
        val categoriesRef = firestore.collection("categories")

        categoriesRef.get()
            .addOnSuccessListener { categoryDocuments ->
                originalSearchList = ArrayList()

                for (categoryDocument in categoryDocuments) {
                    val categoryName = categoryDocument.getString("category_name")
                    val dataListSearch = DataListSearch(category_name = categoryName ?: "", job_count = 0)
                    originalSearchList.add(dataListSearch)
                }

                adapter.updateData(originalSearchList)
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error getting category documents: ", exception)
            }
    }
}