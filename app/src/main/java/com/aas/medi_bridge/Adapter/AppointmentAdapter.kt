package com.aas.medi_bridge.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aas.medi_bridge.Domain.AppointmentModel
import com.aas.medi_bridge.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.FirebaseDatabase

class AppointmentAdapter(val appointments: MutableList<AppointmentModel>) :
    RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder>() {

    // Add doctor mode functionality
    var isDoctorMode: Boolean = false
    var onStatusChanged: ((AppointmentModel, String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.viewholder_appointment, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        val appointment = appointments[position]
        holder.bind(appointment)
    }

    override fun getItemCount(): Int = appointments.size

    fun enableDoctorMode(callback: (AppointmentModel, String) -> Unit) {
        isDoctorMode = true
        onStatusChanged = callback
    }

    inner class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val patientNameTxt: TextView = itemView.findViewById(R.id.patientNameTxt)
        private val appointmentDateTxt: TextView = itemView.findViewById(R.id.appointmentDateTxt)
        private val appointmentTimeTxt: TextView = itemView.findViewById(R.id.appointmentTimeTxt)
        private val patientPhoneTxt: TextView = itemView.findViewById(R.id.patientPhoneTxt)
        private val symptomsTxt: TextView = itemView.findViewById(R.id.symptomsTxt)
        private val statusTxt: TextView = itemView.findViewById(R.id.statusTxt)

        // Add doctor interaction views
        private val doctorActionContainer: LinearLayout? = itemView.findViewById(R.id.doctorActionContainer)
        private val markDoneBtn: MaterialButton? = itemView.findViewById(R.id.markDoneBtn)
        private val markMissedBtn: MaterialButton? = itemView.findViewById(R.id.markMissedBtn)

        fun bind(appointment: AppointmentModel) {
            patientNameTxt.text = appointment.patientName
            appointmentDateTxt.text = appointment.appointmentDate
            appointmentTimeTxt.text = appointment.appointmentTime
            patientPhoneTxt.text = appointment.patientPhone
            symptomsTxt.text = if (appointment.symptoms.isNotBlank()) appointment.symptoms else "No symptoms provided"

            // Enhanced status display
            updateStatus(appointment)

            // Setup doctor buttons
            setupDoctorButtons(appointment)
        }

        private fun updateStatus(appointment: AppointmentModel) {
            val card = itemView as CardView

            when (appointment.status.lowercase()) {
                "pending" -> {
                    statusTxt.text = "Pending"
                    statusTxt.setTextColor(ContextCompat.getColor(itemView.context, R.color.purple))
                    card.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
                }
                "done" -> {
                    statusTxt.text = "Completed"
                    statusTxt.setTextColor(ContextCompat.getColor(itemView.context, R.color.green))
                    card.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.light_green))
                }
                "missed" -> {
                    statusTxt.text = "Missed"
                    statusTxt.setTextColor(android.graphics.Color.RED)
                    card.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.light_red))
                }
                "scheduled" -> {
                    statusTxt.text = "Scheduled"
                    statusTxt.setTextColor(itemView.context.getColor(R.color.purple))
                    card.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
                }
                "completed" -> {
                    statusTxt.text = "Completed"
                    statusTxt.setTextColor(itemView.context.getColor(R.color.darkgrey))
                    card.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
                }
                "cancelled" -> {
                    statusTxt.text = "Cancelled"
                    statusTxt.setTextColor(android.graphics.Color.RED)
                    card.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
                }
                else -> {
                    statusTxt.text = appointment.status.capitalize()
                    statusTxt.setTextColor(itemView.context.getColor(R.color.purple))
                    card.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
                }
            }
        }

        private fun setupDoctorButtons(appointment: AppointmentModel) {
            if (isDoctorMode && appointment.status.lowercase() == "pending" &&
                doctorActionContainer != null && markDoneBtn != null && markMissedBtn != null) {

                doctorActionContainer.visibility = View.VISIBLE

                markDoneBtn.setOnClickListener {
                    updateAppointmentStatus(appointment, "done")
                }

                markMissedBtn.setOnClickListener {
                    updateAppointmentStatus(appointment, "missed")
                }
            } else {
                doctorActionContainer?.visibility = View.GONE
            }
        }

        private fun updateAppointmentStatus(appointment: AppointmentModel, newStatus: String) {
            FirebaseDatabase.getInstance()
                .getReference("appointments")
                .child(appointment.id)
                .child("status")
                .setValue(newStatus)
                .addOnSuccessListener {
                    onStatusChanged?.invoke(appointment, newStatus)
                }
        }
    }

    fun updateAppointmentStatus(appointmentId: String, newStatus: String) {
        val index = appointments.indexOfFirst { it.id == appointmentId }
        if (index != -1) {
            appointments[index] = appointments[index].copy(status = newStatus)
            notifyItemChanged(index)
        }
    }
}