package com.aas.medi_bridge.Activity

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.aas.medi_bridge.Adapter.TopDoctorAdapter3
import com.aas.medi_bridge.ViewModel.MainviewModel
import com.aas.medi_bridge.databinding.ActivityTopDoctorsBinding

class TopDoctorsActivity : BaseActivity() {
    private lateinit var binding: ActivityTopDoctorsBinding
    private lateinit var viewModel: MainviewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTopDoctorsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this).get(MainviewModel::class.java)
        initDoctor()
    }

    private fun initDoctor() {
        binding.progressBarTopDoctor.visibility = View.VISIBLE
        viewModel.doctors.observe(this) { doctors ->
            try {
                if (doctors != null && doctors.isNotEmpty()) {
                    binding.viewTopDoctor.layoutManager = LinearLayoutManager(this@TopDoctorsActivity, LinearLayoutManager.VERTICAL, false)
                    binding.viewTopDoctor.adapter = TopDoctorAdapter3(doctors.toMutableList())
                } else {
                    // Handle empty or null doctor list
                    android.util.Log.w("TopDoctorsActivity", "No doctors data available")
                    binding.viewTopDoctor.adapter = TopDoctorAdapter3(mutableListOf())
                }
                binding.progressBarTopDoctor.visibility = View.GONE
            } catch (e: Exception) {
                android.util.Log.e("TopDoctorsActivity", "Error loading doctors: ${e.message}")
                binding.progressBarTopDoctor.visibility = View.GONE
            }
        }
        viewModel.loadDoctors()
        binding.backBtn.setOnClickListener { finish() }
    }
}