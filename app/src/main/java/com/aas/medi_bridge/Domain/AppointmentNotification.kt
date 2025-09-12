package com.aas.medi_bridge.Domain

data class AppointmentNotification(
    val id: String = "",
    val patientName: String = "",
    val patientPhone: String = "",
    val appointmentDate: String = "",
    val appointmentTime: String = "",
    val doctorId: String = "",
    val doctorEmail: String = "",
    val doctorName: String = "",
    val status: String = "",
    val symptoms: String = "",
    val timestamp: Long = 0L
)
