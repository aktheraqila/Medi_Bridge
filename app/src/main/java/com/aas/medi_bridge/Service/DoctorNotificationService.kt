package com.aas.medi_bridge.Service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.aas.medi_bridge.Activity.DoctorActivity
import com.aas.medi_bridge.R
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.ConcurrentHashMap

object DoctorNotificationService {

    private const val CHANNEL_ID = "doctor_approval_channel"
    private const val NOTIFICATION_ID = 1001
    private const val POLLING_INTERVAL_MS = 30_000L // 30 seconds
    private const val ERROR_POLLING_INTERVAL_MS = 60_000L // 1 minute on error
    private const val MAX_POLLING_DURATION_MS = 10 * 60 * 1000L // 10 minutes
    private const val PREFS_NAME = "DoctorPrefs"
    private const val TAG = "DoctorNotification"

    // Track active polling handlers to avoid duplicates
    private val activePollingHandlers = ConcurrentHashMap<String, Handler>()

    fun startListeningForApproval(context: Context, doctorId: String) {
        // Prevent duplicate polling for the same doctor
        if (activePollingHandlers.containsKey(doctorId)) {
            Log.d(TAG, "Already polling for doctor $doctorId")
            return
        }

        val prefs = getSharedPreferences(context)

        // Check if already approved to avoid unnecessary polling
        if (prefs.getBoolean(getApprovedKey(doctorId), false)) {
            Log.d(TAG, "Doctor $doctorId already approved, skipping polling")
            return
        }

        startPollingForApproval(context, doctorId)
    }

    private fun startPollingForApproval(context: Context, doctorId: String) {
        val database = FirebaseDatabase.getInstance().reference
        val handler = Handler(Looper.getMainLooper())
        val prefs = getSharedPreferences(context)

        // Store handler reference
        activePollingHandlers[doctorId] = handler

        // Initialize start time
        prefs.edit { putLong(getStartTimeKey(doctorId), System.currentTimeMillis()) }

        val pollRunnable = createPollRunnable(context, doctorId, database, handler, prefs)

        Log.d(TAG, "Starting polling for doctor $doctorId")
        handler.post(pollRunnable)
    }

    private fun createPollRunnable(
        context: Context,
        doctorId: String,
        database: com.google.firebase.database.DatabaseReference,
        handler: Handler,
        prefs: SharedPreferences
    ): Runnable = object : Runnable {
        override fun run() {
            if (!shouldContinuePolling(doctorId, prefs)) {
                stopPolling(doctorId)
                return
            }

            database.child("doctors").child(doctorId)
                .get()
                .addOnSuccessListener { snapshot ->
                    handlePollingSuccess(context, doctorId, snapshot, handler, prefs, this)
                }
                .addOnFailureListener { error ->
                    handlePollingError(doctorId, error, handler, prefs, this)
                }
        }
    }

    private fun handlePollingSuccess(
        context: Context,
        doctorId: String,
        snapshot: com.google.firebase.database.DataSnapshot,
        handler: Handler,
        prefs: SharedPreferences,
        runnable: Runnable
    ) {
        try {
            val approved = snapshot.child("approved").getValue(Boolean::class.java) ?: false
            val status = snapshot.child("status").getValue(String::class.java) ?: "pending"
            val wasApproved = prefs.getBoolean(getApprovedKey(doctorId), false)

            Log.d(TAG, "Doctor $doctorId status: approved=$approved, status=$status")

            when {
                approved && status == "approved" && !wasApproved -> {
                    handleDoctorApproved(context, doctorId, prefs)
                    stopPolling(doctorId)
                }
                shouldContinuePolling(doctorId, prefs) -> {
                    scheduleNextPoll(handler, runnable, POLLING_INTERVAL_MS)
                }
                else -> {
                    Log.d(TAG, "Polling timeout reached for doctor $doctorId")
                    stopPolling(doctorId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing approval status for doctor $doctorId", e)
            if (shouldContinuePolling(doctorId, prefs)) {
                scheduleNextPoll(handler, runnable, POLLING_INTERVAL_MS)
            } else {
                stopPolling(doctorId)
            }
        }
    }

    private fun handlePollingError(
        doctorId: String,
        error: Exception,
        handler: Handler,
        prefs: SharedPreferences,
        runnable: Runnable
    ) {
        Log.e(TAG, "Failed to check approval for doctor $doctorId", error)

        if (shouldContinuePolling(doctorId, prefs)) {
            scheduleNextPoll(handler, runnable, ERROR_POLLING_INTERVAL_MS)
        } else {
            stopPolling(doctorId)
        }
    }

    private fun handleDoctorApproved(context: Context, doctorId: String, prefs: SharedPreferences) {
        showApprovalNotification(context, doctorId)
        prefs.edit { putBoolean(getApprovedKey(doctorId), true) }
        Log.d(TAG, "Approval notification sent for doctor $doctorId")
    }

    private fun shouldContinuePolling(doctorId: String, prefs: SharedPreferences): Boolean {
        val startTime = prefs.getLong(getStartTimeKey(doctorId), System.currentTimeMillis())
        val elapsedTime = System.currentTimeMillis() - startTime
        return elapsedTime < MAX_POLLING_DURATION_MS && activePollingHandlers.containsKey(doctorId)
    }

    private fun scheduleNextPoll(handler: Handler, runnable: Runnable, delayMs: Long) {
        handler.postDelayed(runnable, delayMs)
    }

    private fun stopPolling(doctorId: String) {
        activePollingHandlers.remove(doctorId)?.removeCallbacksAndMessages(null)
        Log.d(TAG, "Stopped polling for doctor $doctorId")
    }

    private fun showApprovalNotification(context: Context, doctorId: String) {
        createNotificationChannel(context)

        val intent = Intent(context, DoctorActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.medibridge_logo)
            .setContentTitle("Doctor Registration Approved!")
            .setContentText("Your doctor account has been approved. You can now login with your Doctor ID: $doctorId")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Doctor Approval Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for doctor registration approval"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun stopListeningForApproval(context: Context, doctorId: String) {
        stopPolling(doctorId)
        val prefs = getSharedPreferences(context)
        prefs.edit { remove(getStartTimeKey(doctorId)) }
    }

    fun clearApprovalStatus(context: Context, doctorId: String) {
        val prefs = getSharedPreferences(context)
        prefs.edit {
            remove(getApprovedKey(doctorId))
            remove(getStartTimeKey(doctorId))
        }
        Log.d(TAG, "Cleared approval status for doctor $doctorId")
    }

    // Helper methods for consistent key generation
    private fun getApprovedKey(doctorId: String) = "was_approved_$doctorId"
    private fun getStartTimeKey(doctorId: String) = "polling_start_$doctorId"
    private fun getSharedPreferences(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
