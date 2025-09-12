package com.aas.medi_bridge.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aas.medi_bridge.Domain.AppointmentNotification
import com.aas.medi_bridge.R
import com.aas.medi_bridge.databinding.ViewholderNotificationBinding

class NotificationAdapter(private val notifications: MutableList<AppointmentNotification>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ViewholderNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int {
        return notifications.size
    }

    fun updateNotifications(newNotifications: List<AppointmentNotification>) {
        notifications.clear()
        notifications.addAll(newNotifications)
        notifyDataSetChanged()
    }

    class NotificationViewHolder(private val binding: ViewholderNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: AppointmentNotification) {
            binding.apply {
                tvDoctorName.text = "${notification.doctorName}"
                tvAppointmentDate.text = notification.appointmentDate
                tvAppointmentTime.text = notification.appointmentTime
                tvPatientName.text = "Patient: ${notification.patientName}"
                tvStatus.text = notification.status.uppercase()

                // Set status color based on appointment status
                when (notification.status.lowercase()) {
                    "pending" -> {
                        tvStatus.setTextColor(itemView.context.getColor(R.color.purple))
                        statusIndicator.setBackgroundColor(itemView.context.getColor(R.color.purple))
                    }
                    "confirmed" -> {
                        tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                        statusIndicator.setBackgroundColor(itemView.context.getColor(android.R.color.holo_green_dark))
                    }
                    "cancelled" -> {
                        tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                        statusIndicator.setBackgroundColor(itemView.context.getColor(android.R.color.holo_red_dark))
                    }
                    else -> {
                        tvStatus.setTextColor(itemView.context.getColor(R.color.darkgrey))
                        statusIndicator.setBackgroundColor(itemView.context.getColor(R.color.darkgrey))
                    }
                }

                // Create the detailed message format
                val formattedMessage = "Dear ${notification.patientName},\n\n" +
                        "Your appointment has been confirmed with ${notification.doctorName}.\n\n" +
                        "Date: ${notification.appointmentDate}\n" +
                        "Time: ${notification.appointmentTime}\n\n" +
                        "Please arrive 15 minutes before your appointed time.\n" +
                        "\nThank you!"

                tvMessage.text = formattedMessage
            }
        }
    }
}
