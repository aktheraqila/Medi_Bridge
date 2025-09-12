package com.aas.medi_bridge.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.aas.medi_bridge.Adapter.CategoryAdapter
import com.aas.medi_bridge.Adapter.TopDoctorAdapter
import com.aas.medi_bridge.Domain.DoctorsModel
import com.aas.medi_bridge.R
import com.aas.medi_bridge.ViewModel.MainviewModel
import com.aas.medi_bridge.databinding.ActivityMainBinding


class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel = MainviewModel()

    private var allDoctors: MutableList<DoctorsModel> = mutableListOf()
    private lateinit var doctorAdapter: TopDoctorAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intCategroy()
        initDoctor()
        setupBottomNavigation()
    }

    private fun initDoctor() {
        android.util.Log.d("MainActivity", "=== STARTING DOCTOR INITIALIZATION ===")
        binding.progressBarTopDoctor.visibility = View.VISIBLE
        viewModel.doctors.observe(this) { doctorsList ->
            android.util.Log.d("MainActivity", "Doctors LiveData changed. Data received: ${doctorsList != null}")
            android.util.Log.d("MainActivity", "Doctors count: ${doctorsList?.size ?: 0}")

            try {
                if (doctorsList != null && doctorsList.isNotEmpty()) {
                    allDoctors = doctorsList
                    // Show only first 4 doctors in the main screen
                    val limitedDoctors = allDoctors.take(6).toMutableList()
                    android.util.Log.d("MainActivity", "Creating adapter with ${limitedDoctors.size} doctors")
                    doctorAdapter = TopDoctorAdapter(limitedDoctors)
                    binding.recyclerViewTopDoctor.layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
                    binding.recyclerViewTopDoctor.adapter = doctorAdapter
                    android.util.Log.d("MainActivity", "Doctor adapter set successfully")
                } else {
                    allDoctors = mutableListOf()
                    android.util.Log.w("MainActivity", "No doctors data available - showing empty state")
                }
                binding.progressBarTopDoctor.visibility = View.GONE
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error initializing doctors: ${e.message}")
                binding.progressBarTopDoctor.visibility = View.GONE
            }
        }

        android.util.Log.d("MainActivity", "Calling viewModel.loadDoctors()")
        viewModel.loadDoctors()

        binding.doctorListTxt.setOnClickListener {
            val intent = Intent(this, TopDoctorsActivity::class.java)
            startActivity(intent)
        }

        // Search functionality moved to SearchActivity via bottom navigation
    }

    private fun intCategroy() {
        android.util.Log.d("MainActivity", "=== STARTING CATEGORY INITIALIZATION ===")
        binding.progressBarCategory.visibility = View.VISIBLE
        viewModel.category.observe(this) { categoryList ->
            android.util.Log.d("MainActivity", "Categories LiveData changed. Data received: ${categoryList != null}")
            android.util.Log.d("MainActivity", "Categories count: ${categoryList?.size ?: 0}")

            try {
                if (categoryList != null && categoryList.isNotEmpty()) {
                    binding.viewCategory.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                    binding.viewCategory.adapter = CategoryAdapter(categoryList.toMutableList()) { category ->
                        // On specialization click, start DoctorListActivity
                        val intent = Intent(this, DoctorListActivity::class.java)
                        intent.putExtra("specialization", category.name)
                        startActivity(intent)
                    }
                    android.util.Log.d("MainActivity", "Category adapter set successfully")
                } else {
                    android.util.Log.w("MainActivity", "No categories data available - showing empty state")
                }
                binding.progressBarCategory.visibility = View.GONE
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error initializing categories: ${e.message}")
                binding.progressBarCategory.visibility = View.GONE
            }
        }

        android.util.Log.d("MainActivity", "Calling viewModel.loadCategory()")
        viewModel.loadCategory()
    }

    private fun setupBottomNavigation() {
        // Home button - already selected by default
        binding.homeLayout.setOnClickListener {
            // Already on home, just refresh the current state
            resetBottomNavigation()
            setActiveNavItem("home")
        }

        // Search button
        binding.searchLayout.setOnClickListener {
            resetBottomNavigation()
            setActiveNavItem("doctors")
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        // Notification button
        binding.notificationLayout.setOnClickListener {
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)
        }

        // Doctor button - navigate to doctor login
        binding.doctorLayout.setOnClickListener {
            val intent = Intent(this, DoctorActivity::class.java)
            startActivity(intent)
        }

        // Set home as default active
        setActiveNavItem("home")
    }

    private fun resetBottomNavigation() {
        // Reset all icons to black and text to dark grey
        binding.imageViewHome.setImageResource(R.drawable.home_hospital_black)
        binding.textViewHome.setTextColor(getColor(R.color.darkgrey))
        binding.textViewHome.setTypeface(null, android.graphics.Typeface.NORMAL)

        binding.imageViewStethecope.setImageResource(R.drawable.stethoscopes)
        binding.textViewSearch.setTextColor(getColor(R.color.darkgrey))

        binding.imageViewNotification.setImageResource(R.drawable.notification_black)
        binding.textViewNotification.setTextColor(getColor(R.color.darkgrey))

        binding.imageViewDoctor.setImageResource(R.drawable.doctor_account_black)
        binding.textViewDoctor.setTextColor(getColor(R.color.darkgrey))
    }

    private fun setActiveNavItem(activeItem: String) {
        when (activeItem) {
            "home" -> {
                binding.imageViewHome.setImageResource(R.drawable.home_hospital_purple)
                binding.textViewHome.setTextColor(getColor(R.color.purple))
                binding.textViewHome.setTypeface(null, android.graphics.Typeface.BOLD)
            }
            "doctors" -> {
                binding.imageViewStethecope.setImageResource(R.drawable.stethoscopes_purple)
                binding.textViewSearch.setTextColor(getColor(R.color.purple))
            }
            "notification" -> {
                binding.imageViewNotification.setImageResource(R.drawable.notification_purple)
                binding.textViewNotification.setTextColor(getColor(R.color.purple))
            }
            "doctor" -> {
                binding.imageViewDoctor.setImageResource(R.drawable.doctor_account_purple)
                binding.textViewDoctor.setTextColor(getColor(R.color.purple))
            }
        }
    }

    // Function to update notification badge count
    private fun updateNotificationBadge(count: Int) {
        if (count > 0) {
            binding.notificationBadge.visibility = View.VISIBLE
            binding.notificationBadge.text = if (count > 9) "9+" else count.toString()
        } else {
            binding.notificationBadge.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        // Reset navigation state when returning to main activity
        resetBottomNavigation()
        setActiveNavItem("home")

        // Load and show notification count
        loadNotificationCount()
    }

    private fun loadNotificationCount() {
        val sharedPref = getSharedPreferences("notifications", MODE_PRIVATE)
        val notificationCount = sharedPref.getInt("notification_count", 0)
        updateNotificationBadge(notificationCount)

        // Also calculate unread count to ensure accuracy
        calculateAndUpdateUnreadCount()
    }

    private fun calculateAndUpdateUnreadCount() {
        try {
            val appointmentsPrefs = getSharedPreferences("appointment_notifications", MODE_PRIVATE)
            val readNotificationsPrefs = getSharedPreferences("read_notifications", MODE_PRIVATE)

            // Get all appointments
            val appointmentsStringSet = appointmentsPrefs.getStringSet("appointments_simple", mutableSetOf()) ?: mutableSetOf()

            // Count unread notifications
            var unreadCount = 0
            for (appointmentString in appointmentsStringSet) {
                try {
                    val parts = appointmentString.split("|")
                    if (parts.size >= 5) {
                        val timestamp = parts[4].toLongOrNull() ?: System.currentTimeMillis()
                        val notificationId = timestamp.toString()

                        // Check if this notification is read
                        val isRead = readNotificationsPrefs.getBoolean(notificationId, false)
                        if (!isRead) {
                            unreadCount++
                        }
                    }
                } catch (e: Exception) {
                    // Skip invalid entries
                }
            }

            // Update the badge with accurate unread count
            updateNotificationBadge(unreadCount)

            // Save the accurate count
            val sharedPref = getSharedPreferences("notifications", MODE_PRIVATE)
            with(sharedPref.edit()) {
                putInt("notification_count", unreadCount)
                apply()
            }

        } catch (e: Exception) {
            // Handle error silently
        }
    }
}