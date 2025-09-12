package com.aas.medi_bridge.Activity

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.aas.medi_bridge.Adapter.NotificationAdapter
import com.aas.medi_bridge.Domain.AppointmentNotification
import com.aas.medi_bridge.databinding.ActivityNotificationBinding
import androidx.core.content.edit

class NotificationActivity : BaseActivity() {

    private lateinit var binding: ActivityNotificationBinding
    private lateinit var notificationAdapter: NotificationAdapter
    private val appointmentsList = mutableListOf<AppointmentNotification>()
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var readNotificationsPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadLocalNotifications()
    }

    override fun onResume() {
        super.onResume()
        loadLocalNotifications()
    }

    private fun setupUI() {
        // Set up toolbar
        binding.backBtn.setOnClickListener {
            finish()
        }

        // Set up RecyclerView for notifications
        notificationAdapter = NotificationAdapter(appointmentsList)
        binding.notificationRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notificationRecyclerView.adapter = notificationAdapter

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("appointment_notifications", MODE_PRIVATE)
        readNotificationsPrefs = getSharedPreferences("read_notifications", MODE_PRIVATE)
    }

    private fun loadLocalNotifications() {
        // Show loading initially
        showLoading()

        // Load appointments with a short delay to see loading state
        binding.root.postDelayed({
            try {
                // Load appointments from the simple string format
                val appointmentsStringSet = sharedPreferences.getStringSet("appointments_simple", mutableSetOf()) ?: mutableSetOf()

                appointmentsList.clear()

                // Parse each appointment string
                appointmentsStringSet.forEach { appointmentString ->
                    try {
                        val parts = appointmentString.split("|")

                        if (parts.size >= 5) {
                            val patientName = parts[0].trim()
                            val doctorName = parts[1].trim()
                            val appointmentDate = parts[2].trim()
                            val appointmentTime = parts[3].trim()
                            val timestamp = parts[4].toLongOrNull() ?: System.currentTimeMillis()

                            val appointment = AppointmentNotification(
                                id = timestamp.toString(),
                                patientName = patientName,
                                patientPhone = "",
                                appointmentDate = appointmentDate,
                                appointmentTime = appointmentTime,
                                doctorId = doctorName,
                                doctorEmail = "",
                                doctorName = doctorName,
                                status = "confirmed",
                                symptoms = "Appointment booked successfully",
                                timestamp = timestamp
                            )

                            appointmentsList.add(appointment)
                        }
                    } catch (e: Exception) {
                        // Skip invalid appointment entries
                    }
                }

                // Sort by timestamp (newest first)
                appointmentsList.sortByDescending { it.timestamp }

                // Mark all notifications as read when activity is opened
                markAllNotificationsAsRead()

                // Hide loading and update UI
                hideLoading()

                if (appointmentsList.isEmpty()) {
                    showEmptyState()
                } else {
                    showNotificationsList()

                    // Update the adapter with new data
                    if (::notificationAdapter.isInitialized) {
                        try {
                            notificationAdapter.updateNotifications(appointmentsList.toList())
                        } catch (e: Exception) {
                            // Handle error silently
                        }
                    }

                    // Update notification count to 0 since all are now read
                    updateNotificationCount(0)
                }

            } catch (e: Exception) {
                hideLoading()
                showEmptyState()
            }
        }, 300) // Short delay to show loading
    }

    private fun markAllNotificationsAsRead() {
        val editor = readNotificationsPrefs.edit()
        appointmentsList.forEach { notification ->
            editor.putBoolean(notification.id, true)
        }
        editor.apply()
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateLayout.visibility = View.GONE
        binding.notificationRecyclerView.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }

    private fun showEmptyState() {
        binding.progressBar.visibility = View.GONE
        binding.notificationRecyclerView.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.VISIBLE
    }

    private fun showNotificationsList() {
        binding.progressBar.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        binding.notificationRecyclerView.visibility = View.VISIBLE
    }

    private fun updateNotificationCount(count: Int) {
        val sharedPref = getSharedPreferences("notifications", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("notification_count", count)
            apply()
        }
    }

    companion object {
        // Simple method to save appointment notifications using the string format
        fun saveAppointmentNotification(
            context: android.content.Context,
            doctorName: String,
            patientName: String,
            appointmentDate: String,
            appointmentTime: String,
            specialization: String = ""
        ) {
            try {
                val sharedPreferences = context.getSharedPreferences("appointment_notifications", android.content.Context.MODE_PRIVATE)

                // Create appointment string in the same format as AppointmentFormActivity
                val appointmentData = "$patientName|$doctorName|$appointmentDate|$appointmentTime|${System.currentTimeMillis()}"

                // Get existing appointments
                val existingAppointments = sharedPreferences.getStringSet("appointments_simple", mutableSetOf()) ?: mutableSetOf()
                val updatedAppointments = existingAppointments.toMutableSet()
                updatedAppointments.add(appointmentData)

                // Save back to SharedPreferences
                sharedPreferences.edit(commit = true) {
                    putStringSet("appointments_simple", updatedAppointments)
                }

                // Update notification count in MainActivity
                updateUnreadNotificationCount(context)

            } catch (e: Exception) {
                // Handle error silently
            }
        }

        private fun updateUnreadNotificationCount(context: android.content.Context) {
            try {
                val appointmentsPrefs = context.getSharedPreferences("appointment_notifications", android.content.Context.MODE_PRIVATE)
                val readNotificationsPrefs = context.getSharedPreferences("read_notifications", android.content.Context.MODE_PRIVATE)
                val notificationCountPrefs = context.getSharedPreferences("notifications", android.content.Context.MODE_PRIVATE)

                // Get all appointments
                val appointmentsStringSet = appointmentsPrefs.getStringSet("appointments_simple", mutableSetOf()) ?: mutableSetOf()
                
                // Count unread notifications
                var unreadCount = 0
                appointmentsStringSet.forEach { appointmentString ->
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

                // Update notification count
                notificationCountPrefs.edit().putInt("notification_count", unreadCount).apply()

            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
}
