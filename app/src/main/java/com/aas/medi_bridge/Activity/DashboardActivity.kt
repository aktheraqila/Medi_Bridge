package com.aas.medi_bridge.Activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aas.medi_bridge.Adapter.AppointmentAdapter
import com.aas.medi_bridge.R
import com.aas.medi_bridge.Domain.AppointmentModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.or

class DashboardActivity : AppCompatActivity() {

    private lateinit var welcomeText: TextView
    private lateinit var appointmentsRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var backBtn: ImageView
    private lateinit var logoutBtn: ImageView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var appointmentAdapter: AppointmentAdapter
    private val appointments = mutableListOf<AppointmentModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        initViews()
        setupClickListeners()
        loadDoctorInfo()
        loadAppointments()

        // Apply window insets to the root view instead of non-existent "main" view
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initViews() {
        welcomeText = findViewById(R.id.welcomeText)
        appointmentsRecyclerView = findViewById(R.id.appointmentsRecyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        backBtn = findViewById(R.id.backBtn)
        logoutBtn = findViewById(R.id.logoutBtn)

        sharedPreferences = getSharedPreferences("DoctorPrefs", MODE_PRIVATE)

        // Setup RecyclerView
        appointmentsRecyclerView.layoutManager = LinearLayoutManager(this)
        appointmentAdapter = AppointmentAdapter(appointments)

        // Enable doctor mode with status change callback
        appointmentAdapter.enableDoctorMode { appointment, newStatus ->
            handleAppointmentStatusChange(appointment, newStatus)
        }

        appointmentsRecyclerView.adapter = appointmentAdapter
    }

    private fun setupClickListeners() {
        // Fix back button to navigate properly
        backBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        logoutBtn.setOnClickListener {
            logout()
        }
    }

    private fun handleAppointmentStatusChange(appointment: AppointmentModel, newStatus: String) {
        // Update the appointment status locally
        appointmentAdapter.updateAppointmentStatus(appointment.id, newStatus)

        // Show a toast to confirm the action
        val statusText = when (newStatus) {
            "done" -> "marked as completed"
            "missed" -> "marked as missed"
            else -> "status updated"
        }

        android.widget.Toast.makeText(
            this,
            "Appointment for ${appointment.patientName} $statusText",
            android.widget.Toast.LENGTH_SHORT
        ).show()

        android.util.Log.d("DashboardActivity", "Appointment ${appointment.id} status changed to: $newStatus")
    }

    private fun logout() {
        try {
            android.util.Log.d("DashboardActivity", "Starting logout process...")

            // Clear stored doctor info first
            val editor = sharedPreferences.edit()
            editor.clear()
            editor.apply()
            android.util.Log.d("DashboardActivity", "SharedPreferences cleared")

            // Sign out from Firebase Auth (if signed in)
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                android.util.Log.d("DashboardActivity", "Firebase auth sign out completed")
            } catch (authError: Exception) {
                android.util.Log.w("DashboardActivity", "Firebase auth sign out failed (might not be signed in): ${authError.message}")
            }

            // Go back to DoctorActivity (login screen) with proper flags
            val intent = Intent(this@DashboardActivity, DoctorActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

            android.util.Log.d("DashboardActivity", "Logout completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("DashboardActivity", "Error during logout: ${e.message}")
            // Even if there's an error, try to go back to login
            val intent = Intent(this@DashboardActivity, DoctorActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadDoctorInfo() {
        val doctorName = sharedPreferences.getString("doctor_name", "Doctor")
        val doctorSpecialty = sharedPreferences.getString("doctor_specialty", "General Practice")
        welcomeText.text = "Welcome, $doctorName"
    }

    private fun loadAppointments() {
        val doctorName = sharedPreferences.getString("doctor_name", "")

        if (doctorName.isNullOrEmpty()) {
            android.util.Log.e("DashboardActivity", "No doctor name found in preferences")
            showEmptyState()
            return
        }

        android.util.Log.d("DashboardActivity", "Loading appointments for doctor: $doctorName")

        val database = FirebaseDatabase.getInstance()
        val appointmentsRef = database.getReference("appointments")

        appointmentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                android.util.Log.d("DashboardActivity", "Appointments snapshot received, children: ${snapshot.childrenCount}")

                appointments.clear()

                for (appointmentSnapshot in snapshot.children) {
                    try {
                        val appointment = appointmentSnapshot.getValue(AppointmentModel::class.java)
                        if (appointment != null) {
                            // SIMPLE name-based filtering: Only match by doctor name
                            val matchesByName = appointment.doctorName.equals(doctorName, ignoreCase = true)

                            if (matchesByName) {
                                // Filter appointments for the current week
                                if (isAppointmentInCurrentWeek(appointment.appointmentDate)) {
                                    appointments.add(appointment)
                                    android.util.Log.d("DashboardActivity", "✅ Added appointment for patient: ${appointment.patientName} on ${appointment.appointmentDate} (Doctor: ${appointment.doctorName})")
                                }
                            } else {
                                android.util.Log.d("DashboardActivity", "❌ Filtered out appointment - Doctor: ${appointment.doctorName}, Expected: $doctorName")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("DashboardActivity", "Error parsing appointment: ${e.message}")
                    }
                }

                // Sort appointments by date and time
                appointments.sortWith { a1, a2 ->
                    when {
                        a1.appointmentDate != a2.appointmentDate -> a1.appointmentDate.compareTo(a2.appointmentDate)
                        else -> a1.appointmentTime.compareTo(a2.appointmentTime)
                    }
                }

                android.util.Log.d("DashboardActivity", "Total weekly appointments loaded: ${appointments.size}")

                runOnUiThread {
                    if (appointments.isEmpty()) {
                        showEmptyState()
                    } else {
                        showAppointments()
                    }
                    appointmentAdapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("DashboardActivity", "Failed to load appointments: ${error.message}")
                runOnUiThread {
                    showEmptyState()
                }
            }
        })
    }

    private fun isAppointmentInCurrentWeek(appointmentDate: String): Boolean {
        try {
            // Simple week check - can be enhanced with proper date parsing
            val currentTimeMillis = System.currentTimeMillis()
            val oneWeekInMillis = 7 * 24 * 60 * 60 * 1000L

            // For now, show all appointments (can be refined based on date format)
            // This assumes appointments are for the current week by default
            return true
        } catch (e: Exception) {
            android.util.Log.e("DashboardActivity", "Error checking appointment date: ${e.message}")
            return true // Show all appointments if date parsing fails
        }
    }

    private fun showEmptyState() {
        appointmentsRecyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.VISIBLE
    }

    private fun showAppointments() {
        appointmentsRecyclerView.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE
    }
}