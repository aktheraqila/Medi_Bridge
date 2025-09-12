package com.aas.medi_bridge.Activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.aas.medi_bridge.Adapter.TopDoctorAdapter3
import com.aas.medi_bridge.ViewModel.MainviewModel
import com.aas.medi_bridge.databinding.ActivityDoctorListBinding

class DoctorListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDoctorListBinding
    private val viewModel = MainviewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val specialization = intent.getStringExtra("specialization") ?: ""
        binding.doctorListTitle.text = specialization

        // Back button functionality
        binding.backBtn.setOnClickListener {
            finish()
        }

        // Observe doctors from ViewModel
        viewModel.doctors.observe(this) { allDoctors ->

            // Use robust filtering: trim and contains (case-insensitive)
            val filtered = allDoctors.filter {
                it.specialization.trim().equals(specialization.trim(), ignoreCase = true) ||
                it.specialization.trim().contains(specialization.trim(), ignoreCase = true) ||
                specialization.trim().contains(it.specialization.trim(), ignoreCase = true)
            }

            if (filtered.isEmpty()) {
                binding.emptyStateTxt.visibility = android.view.View.VISIBLE
            } else {
                binding.emptyStateTxt.visibility = android.view.View.GONE
            }

            // Use TopDoctorAdapter3 for the new design with hospital names
            val adapter = TopDoctorAdapter3(filtered.toMutableList())
            binding.doctorRecyclerView.layoutManager = LinearLayoutManager(this)
            binding.doctorRecyclerView.adapter = adapter
        }
        viewModel.loadDoctors()
    }
}
