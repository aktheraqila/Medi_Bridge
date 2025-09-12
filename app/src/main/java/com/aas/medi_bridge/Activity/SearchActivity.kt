package com.aas.medi_bridge.Activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.aas.medi_bridge.Adapter.TopDoctorAdapter3
import com.aas.medi_bridge.Domain.DoctorsModel
import com.aas.medi_bridge.ViewModel.MainviewModel
import com.aas.medi_bridge.databinding.ActivitySearchBinding

class SearchActivity : BaseActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val viewModel = MainviewModel()
    private var allDoctors: MutableList<DoctorsModel> = mutableListOf()
    private lateinit var searchResultsAdapter: TopDoctorAdapter3
    private lateinit var allDoctorsAdapter: TopDoctorAdapter3 // Updated to use TopDoctorAdapter3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initSearch()
        loadDoctors()
        setupRecyclerViews() // Updated method name
    }

    private fun setupRecyclerViews() {
        // Setup search results RecyclerView
        searchResultsAdapter = TopDoctorAdapter3(mutableListOf())
        binding.searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.searchResultsRecyclerView.adapter = searchResultsAdapter

        // Setup all doctors RecyclerView (the main one that shows all doctors)
        allDoctorsAdapter = TopDoctorAdapter3(mutableListOf())
        binding.doctorRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.doctorRecyclerView.adapter = allDoctorsAdapter
    }

    private fun initSearch() {
        // Set up search functionality - search immediately as user types
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performSearch(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Back button functionality
        binding.backBtn.setOnClickListener {
            finish()
        }

        // Auto-focus on search field when activity opens
        binding.searchEditText.requestFocus()
    }

    private fun loadDoctors() {
        // Show progress
        binding.progressCard.visibility = View.VISIBLE
        binding.doctorCard.visibility = View.GONE
        binding.searchResultsCard.visibility = View.GONE
        binding.searchHintContainer.visibility = View.GONE

        viewModel.doctors.observe(this) { doctorsList ->
            allDoctors = doctorsList
            binding.progressCard.visibility = View.GONE
            android.util.Log.d("SearchActivity", "Loaded ${allDoctors.size} doctors")

            // Show all doctors initially when data is loaded
            showAllDoctors()
        }
        viewModel.loadDoctors()
    }

    private fun showAllDoctors() {
        // Show the all doctors container and hide others
        binding.doctorCard.visibility = View.VISIBLE
        binding.searchResultsCard.visibility = View.GONE
        binding.searchHintContainer.visibility = View.GONE

        // Update the all doctors adapter
        allDoctorsAdapter.apply {
            items.clear()
            items.addAll(allDoctors)
            notifyDataSetChanged()
        }

        android.util.Log.d("SearchActivity", "Displaying all ${allDoctors.size} doctors")
    }

    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            // Show all doctors when search is empty
            showAllDoctors()
            return
        }

        // Search across ALL relevant fields
        val matchingDoctors = allDoctors.filter { doctor ->
            val hospitalName = if (doctor.chambers.isNotEmpty()) {
                doctor.chambers[0].name
            } else {
                ""
            }

            doctor.name.contains(query, ignoreCase = true) ||
                    doctor.specialization.contains(query, ignoreCase = true) ||
                    doctor.designation.contains(query, ignoreCase = true) ||
                    hospitalName.contains(query, ignoreCase = true) ||
                    doctor.address.contains(query, ignoreCase = true) ||
                    doctor.location.contains(query, ignoreCase = true)
        }

        android.util.Log.d("SearchActivity", "Found ${matchingDoctors.size} matches for '$query'")

        // Show results or no results message
        if (matchingDoctors.isEmpty()) {
            // Show no results state
            binding.doctorCard.visibility = View.GONE
            binding.searchResultsCard.visibility = View.GONE
            binding.searchHintContainer.visibility = View.VISIBLE
            binding.searchHintText.text = "No doctors found for \"$query\"\nTry different keywords"
        } else {
            // Show search results
            binding.doctorCard.visibility = View.GONE
            binding.searchResultsCard.visibility = View.VISIBLE
            binding.searchHintContainer.visibility = View.GONE

            searchResultsAdapter.apply {
                items.clear()
                items.addAll(matchingDoctors)
                notifyDataSetChanged()
            }
        }
    }
}
