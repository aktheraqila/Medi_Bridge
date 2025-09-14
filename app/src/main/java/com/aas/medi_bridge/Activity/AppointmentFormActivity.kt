//package com.aas.medi_bridge.Activity
//
//import android.app.DatePickerDialog
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.content.Context
//import android.graphics.Color
//import android.os.Build
//import android.os.Bundle
//import android.view.View
//import android.view.ViewGroup
//import android.widget.AdapterView
//import android.widget.ArrayAdapter
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.NotificationCompat
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.aas.medi_bridge.Adapter.DateChipAdapter
//import com.aas.medi_bridge.Adapter.TimeChipAdapter
//import com.aas.medi_bridge.Adapter.TimeSlot
//import com.aas.medi_bridge.Domain.DoctorsModel
//import com.aas.medi_bridge.R
//import com.aas.medi_bridge.databinding.ActivityAppointmentFormBinding
//import com.google.firebase.database.DataSnapshot
//import com.google.firebase.database.DatabaseError
//import com.google.firebase.database.FirebaseDatabase
//import com.google.firebase.database.ValueEventListener
//import java.text.SimpleDateFormat
//import java.util.*
//import kotlin.collections.getValue
//
//class AppointmentFormActivity : AppCompatActivity() {
//    private lateinit var binding: ActivityAppointmentFormBinding
//    private lateinit var item: DoctorsModel
//    private var selectedDate: String = ""
//    private var selectedTime: String = ""
//    private var bookedSlots: MutableMap<String, Set<String>> = mutableMapOf() // date -> set of booked times
//    private var isDateSelected: Boolean = false // Track if user has manually selected a date
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityAppointmentFormBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // Get doctor data from intent
//        item = intent.getParcelableExtra("Object") ?: return
//
//        setupDoctorInfo()
//        setupSpinners()
//        setupForm()
//        setupDatePicker()
//        setupAvailableDatesAndTimes()
//    }
//
//    private fun setupDoctorInfo() {
//        // Set doctor name in the form
//        binding.tvDoctorName.text = item.name
//
//        // Load doctor image with corner radius
//        var imageUrl = item.image
//
//        // Fallback to first chamber image if main image is blank
//        if (imageUrl.isBlank() && item.chambers.isNotEmpty()) {
//            imageUrl = item.chambers[0].image
//        }
//
//        // Convert Imgur URL to direct image URL if needed
//        if (imageUrl.isNotBlank() && imageUrl.contains("imgur.com") && !imageUrl.contains("i.imgur.com")) {
//            val imageId = imageUrl.substringAfterLast("/")
//            imageUrl = "https://i.imgur.com/$imageId.jpg"
//        }
//
//        // Load image with corner radius using Glide
//        com.bumptech.glide.Glide.with(this)
//            .load(imageUrl)
//            .transform(
//                com.bumptech.glide.load.resource.bitmap.CenterCrop(),
//                com.bumptech.glide.load.resource.bitmap.RoundedCorners(24)
//            )
//            .placeholder(R.drawable.blank_profile)
//            .error(R.drawable.blank_profile)
//            .into(binding.ivMedicalIcon)
//    }
//
//    private fun setupAvailableDatesAndTimes() {
//        val availableDates = generateAvailableDates()
//        val dateAdapter = DateChipAdapter(availableDates) { date ->
//            selectedDate = date
//            isDateSelected = true // Mark that user has selected a date
//            selectedTime = "" // Reset selected time when date changes
//
//            // Load booked appointments for this doctor and date, then update time slots
//            loadBookedAppointments(date)
//        }
//
//        binding.availableDatesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
//        binding.availableDatesRecyclerView.adapter = dateAdapter
//
//        // DO NOT automatically select first date - let user choose
//        // Clear time slots initially
//        clearTimeSlots()
//    }
//
//    private fun clearTimeSlots() {
//        // Clear the time slots RecyclerView
//        binding.availableTimesRecyclerView.adapter = null
//    }
//
//    private fun loadBookedAppointments(date: String) {
//        val database = FirebaseDatabase.getInstance()
//        val appointmentsRef = database.getReference("appointments")
//
//        // Query appointments for this doctor on the selected date
//        appointmentsRef.addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val bookedTimes = mutableSetOf<String>()
//
//                for (appointmentSnapshot in snapshot.children) {
//                    val doctorId = appointmentSnapshot.child("doctorId").getValue(String::class.java)
//                    val doctorName = appointmentSnapshot.child("doctorName").getValue(String::class.java)
//                    val appointmentDate = appointmentSnapshot.child("appointmentDate").getValue(String::class.java)
//                    val appointmentTime = appointmentSnapshot.child("appointmentTime").getValue(String::class.java)
//                    val status = appointmentSnapshot.child("status").getValue(String::class.java)
//
//                    // Use doctor name for comparison with fallback to doctorId
//                    val isDoctorMatch = doctorName == item.name || doctorId == item.name
//
//                    if (isDoctorMatch && appointmentDate == date &&
//                        appointmentTime != null && status != "cancelled") {
//                        bookedTimes.add(appointmentTime)
//                    }
//                }
//
//                // Store booked slots for this date
//                bookedSlots[date] = bookedTimes
//
//                // Only update time slots if a date has been selected by the user
//                if (isDateSelected) {
//                    updateTimeSlots(date, bookedTimes)
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Toast.makeText(this@AppointmentFormActivity,
//                    "Failed to load booking information: ${error.message}", Toast.LENGTH_SHORT).show()
//
//                // Only show default time slots if user has selected a date
//                if (isDateSelected) {
//                    updateTimeSlots(date, emptySet())
//                }
//            }
//        })
//    }
//
//    private fun updateTimeSlots(date: String, bookedTimes: Set<String>) {
//        val allAvailableTimes = getAvailableTimesForDoctor()
//
//        // Create TimeSlot objects with booking status
//        val timeSlots = allAvailableTimes.map { time ->
//            TimeSlot(time = time, isBooked = bookedTimes.contains(time))
//        }
//
//        val timeAdapter = TimeChipAdapter(timeSlots) { time ->
//            selectedTime = time
//        }
//
//        binding.availableTimesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
//        binding.availableTimesRecyclerView.adapter = timeAdapter
//    }
//
//    private fun generateAvailableDates(): List<String> {
//        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
//        val dates = mutableListOf<String>()
//        val calendar = Calendar.getInstance()
//
//        // Get closed days from visiting hours
//        val closedDays = getClosedDaysFromVisitingHours()
//
//        // Generate next 14 days excluding closed days
//        for (i in 0 until 14) {
//            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
//            val dayName = when (dayOfWeek) {
//                Calendar.SUNDAY -> "sunday"
//                Calendar.MONDAY -> "monday"
//                Calendar.TUESDAY -> "tuesday"
//                Calendar.WEDNESDAY -> "wednesday"
//                Calendar.THURSDAY -> "thursday"
//                Calendar.FRIDAY -> "friday"
//                Calendar.SATURDAY -> "saturday"
//                else -> ""
//            }
//
//            // Only add if not a closed day
//            if (!closedDays.contains(dayName)) {
//                dates.add(dateFormat.format(calendar.time))
//            }
//
//            calendar.add(Calendar.DAY_OF_MONTH, 1)
//        }
//
//        return dates
//    }
//
//    private fun getClosedDaysFromVisitingHours(): List<String> {
//        val visitingHour = when {
//            item.visiting_hour.isNotBlank() -> item.visiting_hour
//            item.chambers.isNotEmpty() && item.chambers[0].visiting_hour.isNotBlank() -> item.chambers[0].visiting_hour
//            else -> ""
//        }
//
//        val closedDays = mutableListOf<String>()
//
//        if (visitingHour.contains("closed:", ignoreCase = true)) {
//            val closedPart = visitingHour.substringAfter("closed:", "").trim()
//            when {
//                closedPart.contains("friday", ignoreCase = true) -> closedDays.add("friday")
//                closedPart.contains("saturday", ignoreCase = true) -> closedDays.add("saturday")
//                closedPart.contains("sunday", ignoreCase = true) -> closedDays.add("sunday")
//                closedPart.contains("monday", ignoreCase = true) -> closedDays.add("monday")
//                closedPart.contains("tuesday", ignoreCase = true) -> closedDays.add("tuesday")
//                closedPart.contains("wednesday", ignoreCase = true) -> closedDays.add("wednesday")
//                closedPart.contains("thursday", ignoreCase = true) -> closedDays.add("thursday")
//            }
//        }
//
//        return closedDays
//    }
//
//    private fun getAvailableTimesForDoctor(): List<String> {
//        val visitingHour = when {
//            item.visiting_hour.isNotBlank() -> item.visiting_hour
//            item.chambers.isNotEmpty() && item.chambers[0].visiting_hour.isNotBlank() -> item.chambers[0].visiting_hour
//            else -> "9am to 5pm"
//        }
//
//        return parseVisitingHoursToTimeSlots(visitingHour)
//    }
//
//    private fun parseVisitingHoursToTimeSlots(visitingHours: String): List<String> {
//        val timeSlots = mutableListOf<String>()
//
//        try {
//            // Clean the visiting hours string - remove parentheses content first
//            val cleanHours = visitingHours.replace(Regex("\\([^)]*\\)"), "").trim()
//
//            // Handle multiple time ranges separated by "&"
//            val timeRanges = cleanHours.split("&")
//
//            for (range in timeRanges) {
//                val trimmedRange = range.trim()
//
//                // Extract start and end times using " to " as separator
//                val parts = trimmedRange.split(" to ")
//                if (parts.size == 2) {
//                    val startTimeRaw = parts[0].trim()
//                    val endTimeRaw = parts[1].trim()
//
//                    val startTime = normalizeTimeFormat(startTimeRaw)
//                    val endTime = normalizeTimeFormat(endTimeRaw)
//
//                    val slots = generateTimeSlots(startTime, endTime)
//                    timeSlots.addAll(slots)
//                }
//            }
//        } catch (e: Exception) {
//            // Fallback to default times
//            timeSlots.addAll(listOf("9:00 AM", "10:00 AM", "11:00 AM", "2:00 PM", "3:00 PM", "4:00 PM"))
//        }
//
//        val result = timeSlots.ifEmpty { listOf("9:00 AM", "10:00 AM", "11:00 AM", "2:00 PM", "3:00 PM", "4:00 PM") }
//        return result
//    }
//
//    private fun normalizeTimeFormat(time: String): String {
//        // Remove any extra spaces and unwanted characters but keep the basic time format
//        val cleaned = time.trim().replace(Regex("\\s+"), " ")
//
//        // Handle different time formats from database
//        return when {
//            // Format: "7pm" -> "7:00 PM"
//            cleaned.matches(Regex("\\d{1,2}pm", RegexOption.IGNORE_CASE)) -> {
//                val hour = cleaned.replace(Regex("[pm]", RegexOption.IGNORE_CASE), "")
//                "$hour:00 PM"
//            }
//            // Format: "7am" -> "7:00 AM"
//            cleaned.matches(Regex("\\d{1,2}am", RegexOption.IGNORE_CASE)) -> {
//                val hour = cleaned.replace(Regex("[am]", RegexOption.IGNORE_CASE), "")
//                "$hour:00 AM"
//            }
//            // Format: "10:30pm" -> "10:30 PM"
//            cleaned.matches(Regex("\\d{1,2}:\\d{2}pm", RegexOption.IGNORE_CASE)) -> {
//                cleaned.replace(Regex("pm", RegexOption.IGNORE_CASE), " PM")
//            }
//            // Format: "10:30am" -> "10:30 AM"
//            cleaned.matches(Regex("\\d{1,2}:\\d{2}am", RegexOption.IGNORE_CASE)) -> {
//                cleaned.replace(Regex("am", RegexOption.IGNORE_CASE), " AM")
//            }
//            // If already in correct format, return as is
//            else -> cleaned
//        }
//    }
//
//    private fun generateTimeSlots(startTime: String, endTime: String): List<String> {
//        val timeSlots = mutableListOf<String>()
//        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
//
//        try {
//            val start = sdf.parse(startTime) ?: return emptyList()
//            val end = sdf.parse(endTime) ?: return emptyList()
//
//            val calendar = Calendar.getInstance()
//            calendar.time = start
//
//            while (calendar.time.before(end)) {
//                timeSlots.add(sdf.format(calendar.time))
//                calendar.add(Calendar.MINUTE, 15) // 15-minute intervals
//            }
//        } catch (e: Exception) {
//            // Handle error silently
//        }
//
//        return timeSlots
//    }
//
//    private fun setupSpinners() {
//        // Setup Gender Spinner with placeholder
//        val genderOptions = arrayOf("Please select", "Male", "Female")
//
//        // Create custom adapter with simpler approach
//        val genderAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, genderOptions) {
//
//            override fun isEnabled(position: Int): Boolean {
//                // Disable the first item (placeholder)
//                return position != 0
//            }
//
//            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
//                val view = super.getView(position, convertView, parent)
//                val textView = view as TextView
//
//                // Style the main display text
//                textView.setPadding(16, 16, 16, 16)
//                textView.textSize = 16f
//
//                if (position == 0) {
//                    // Set placeholder text color to gray
//                    textView.setTextColor(Color.parseColor("#9CA3AF"))
//                } else {
//                    textView.setTextColor(Color.parseColor("#1f2937"))
//                }
//
//                return view
//            }
//
//            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
//                val view = super.getDropDownView(position, convertView, parent)
//                val textView = view as TextView
//
//                if (position == 0) {
//                    // Style placeholder in dropdown - make it look disabled
//                    textView.setPadding(16, 8, 16, 8)
//                    textView.textSize = 14f
//                    textView.setTextColor(Color.parseColor("#9CA3AF"))
//                    textView.setBackgroundColor(Color.parseColor("#f9fafb"))
//                    textView.isEnabled = false
//                } else {
//                    // Style normal dropdown items
//                    textView.setPadding(16, 12, 16, 12)
//                    textView.textSize = 16f
//                    textView.setTextColor(Color.parseColor("#1f2937"))
//                    textView.setBackgroundColor(Color.parseColor("#ffffff"))
//                    textView.isEnabled = true
//                }
//
//                return view
//            }
//        }
//
//        // Set dropdown resource and apply to spinner
//        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        binding.spinnerGender.adapter = genderAdapter
//
//        // Customize spinner appearance
//        binding.spinnerGender.apply {
//            dropDownVerticalOffset = 0
//            dropDownWidth = ViewGroup.LayoutParams.MATCH_PARENT
//            setBackgroundResource(R.drawable.rounded_edittext_background)
//            setPadding(16, 0, 16, 0)
//        }
//
//        // Add selection listener
//        binding.spinnerGender.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//                if (position > 0) { // Ignore placeholder selection
//                    val selectedGender = genderOptions[position]
//                    android.util.Log.d("AppointmentForm", "Gender selected: $selectedGender")
//
//                    // Update the text color after selection
//                    (view as? TextView)?.setTextColor(Color.parseColor("#1f2937"))
//                }
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>?) {
//                android.util.Log.d("AppointmentForm", "No gender selected")
//            }
//        }
//    }
//
//    private fun setupForm() {
//        // Set up red asterisks for required fields
//        setupRedAsterisks()
//
//        // Submit button click listener
//        binding.btnSubmit.setOnClickListener {
//            if (validateForm()) {
//                collectFormData()
//
//                // Show appointment confirmation notification using actual doctor data
//                showAppointmentConfirmationNotification(
//                    item.name,
//                    item.specialization,
//                    selectedDate,
//                    selectedTime
//                )
//
//                finish()
//            }
//        }
//    }
//
//    private fun setupDatePicker() {
//        binding.etDateOfBirth.setOnClickListener {
//            val calendar = Calendar.getInstance()
//            val year = calendar.get(Calendar.YEAR)
//            val month = calendar.get(Calendar.MONTH)
//            val day = calendar.get(Calendar.DAY_OF_MONTH)
//
//            val datePickerDialog = DatePickerDialog(
//                this,
//                { _, selectedYear, selectedMonth, selectedDay ->
//                    val formattedDate = String.format(Locale.getDefault(), "%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear)
//                    binding.etDateOfBirth.setText(formattedDate)
//                },
//                year, month, day
//            )
//
//            // Set maximum date to today (no future dates for birth date)
//            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
//
//            // Set minimum date to 100 years ago
//            val minCalendar = Calendar.getInstance()
//            minCalendar.add(Calendar.YEAR, -100)
//            datePickerDialog.datePicker.minDate = minCalendar.timeInMillis
//
//            datePickerDialog.show()
//        }
//    }
//
//    private fun setupRedAsterisks() {
//        // Helper function to create text with red asterisk
//        fun createLabelWithRedAsterisk(text: String): android.text.SpannableString {
//            val spannableString = android.text.SpannableString("$text *")
//            val foregroundColorSpan = android.text.style.ForegroundColorSpan(Color.parseColor("#dc2626"))
//            spannableString.setSpan(foregroundColorSpan, text.length + 1, spannableString.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
//            return spannableString
//        }
//
//        // Apply red asterisks to required field labels
//        binding.tvFirstNameLabel.text = createLabelWithRedAsterisk("First Name")
//        binding.tvLastNameLabel.text = createLabelWithRedAsterisk("Last Name")
//        binding.tvDobLabel.text = createLabelWithRedAsterisk("Date of Birth")
//        binding.tvGenderLabel.text = createLabelWithRedAsterisk("Gender")
//        binding.tvContactLabel.text = createLabelWithRedAsterisk("Contact Number")
//        // Add red asterisks for Available Dates and Available Times
//        binding.tvAvailableDatesLabel.text = createLabelWithRedAsterisk("Available Dates")
//        binding.tvAvailableTimesLabel.text = createLabelWithRedAsterisk("Available Times")
//    }
//
//    private fun validateForm(): Boolean {
//        val firstName = binding.etFirstName.text.toString().trim()
//        val lastName = binding.etLastName.text.toString().trim()
//        val dob = binding.etDateOfBirth.text.toString().trim()
//        val contactNumber = binding.etContactNumber.text.toString().trim()
//
//        when {
//            firstName.isEmpty() -> {
//                binding.etFirstName.error = "First name is required"
//                binding.etFirstName.requestFocus()
//                return false
//            }
//            lastName.isEmpty() -> {
//                binding.etLastName.error = "Last name is required"
//                binding.etLastName.requestFocus()
//                return false
//            }
//            dob.isEmpty() -> {
//                Toast.makeText(this, "Please select date of birth", Toast.LENGTH_SHORT).show()
//                return false
//            }
//            contactNumber.isEmpty() -> {
//                binding.etContactNumber.error = "Contact number is required"
//                binding.etContactNumber.requestFocus()
//                return false
//            }
//            binding.spinnerGender.selectedItemPosition == 0 -> {
//                Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show()
//                return false
//            }
//            selectedDate.isEmpty() -> {
//                Toast.makeText(this, "Please select an appointment date", Toast.LENGTH_SHORT).show()
//                return false
//            }
//            selectedTime.isEmpty() -> {
//                Toast.makeText(this, "Please select an appointment time", Toast.LENGTH_SHORT).show()
//                return false
//            }
//        }
//        return true
//    }
//
//    private fun collectFormData() {
//        val firstName = binding.etFirstName.text.toString().trim()
//        val lastName = binding.etLastName.text.toString().trim()
//        val dob = binding.etDateOfBirth.text.toString().trim()
//        val gender = binding.spinnerGender.selectedItem.toString()
//        val contactNumber = binding.etContactNumber.text.toString().trim()
//        val email = binding.etEmail.text.toString().trim()
//        val message = binding.etMessage.text.toString().trim()
//
//        // Use the selected date and time from the RecyclerViews
//        val doctorName = item.name
//        val doctorSpecialization = item.specialization
//        val patientName = "$firstName $lastName"
//
//        // Log the collected data
//        android.util.Log.d("AppointmentForm", "=== Appointment Data ===")
//        android.util.Log.d("AppointmentForm", "Doctor: $doctorName")
//        android.util.Log.d("AppointmentForm", "Patient: $patientName")
//        android.util.Log.d("AppointmentForm", "DOB: $dob")
//        android.util.Log.d("AppointmentForm", "Gender: $gender")
//        android.util.Log.d("AppointmentForm", "Contact: $contactNumber")
//        android.util.Log.d("AppointmentForm", "Email: $email")
//        android.util.Log.d("AppointmentForm", "Message: $message")
//        android.util.Log.d("AppointmentForm", "Date: $selectedDate")
//        android.util.Log.d("AppointmentForm", "Time: $selectedTime")
//        android.util.Log.d("AppointmentForm", "========================")
//
//        // Save appointment to Firebase database
//        saveAppointmentToFirebase(firstName, lastName, contactNumber, email, message, doctorName, doctorSpecialization)
//
//        // Also save appointment notification locally for backup
//        saveAppointmentNotificationLocally(doctorName, patientName, doctorSpecialization)
//    }
//
//    private fun saveAppointmentToFirebase(
//        firstName: String,
//        lastName: String,
//        contactNumber: String,
//        email: String,
//        message: String,
//        doctorName: String,
//        doctorSpecialization: String
//    ) {
//        try {
//            val database = FirebaseDatabase.getInstance()
//            val appointmentsRef = database.getReference("appointments")
//
//            // Generate unique appointment ID
//            val appointmentId = appointmentsRef.push().key
//            if (appointmentId == null) {
//                android.util.Log.e("AppointmentForm", "Failed to generate appointment ID")
//                Toast.makeText(this@AppointmentFormActivity, "Failed to create appointment ID", Toast.LENGTH_SHORT).show()
//                return
//            }
//
//            // Create appointment object matching AppointmentModel structure exactly
//            val appointmentData = mapOf(
//                "id" to appointmentId,
//                "patientName" to "$firstName $lastName",
//                "patientPhone" to contactNumber,
//                "appointmentDate" to selectedDate,
//                "appointmentTime" to selectedTime,
//                "doctorId" to doctorName, // Use doctor ID if available, otherwise doctor name
//                "doctorEmail" to email, // Use doctor email if available
//                "doctorName" to doctorName,
//                "status" to "pending",
//                "symptoms" to message,
//                "timestamp" to System.currentTimeMillis()
//            )
//
//            android.util.Log.d("AppointmentForm", "Attempting to save appointment data: $appointmentData")
//
//            // Save to Firebase with improved error handling
//            appointmentsRef.child(appointmentId).setValue(appointmentData)
//                .addOnSuccessListener {
//                    android.util.Log.d("AppointmentForm", "Appointment saved successfully with ID: $appointmentId")
//                    Toast.makeText(this@AppointmentFormActivity, "Appointment booked successfully!", Toast.LENGTH_SHORT).show()
//                }
//                .addOnFailureListener { error ->
//                    android.util.Log.e("AppointmentForm", "Failed to save appointment: ${error.message}")
//
//                    val errorMessage = when {
//                        error.message?.contains("permission", ignoreCase = true) == true ->
//                            "Database permission denied. Please check Firebase security rules."
//                        error.message?.contains("network", ignoreCase = true) == true ->
//                            "Network error. Please check your internet connection."
//                        else -> "Failed to book appointment: ${error.message}"
//                    }
//
//                    Toast.makeText(this@AppointmentFormActivity, errorMessage, Toast.LENGTH_LONG).show()
//                }
//        } catch (e: Exception) {
//            android.util.Log.e("AppointmentForm", "Exception in saveAppointmentToFirebase: ${e.message}")
//            Toast.makeText(this@AppointmentFormActivity, "Error occurred while booking appointment", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun saveAppointmentNotificationLocally(
//        doctorName: String,
//        patientName: String,
//        doctorSpecialization: String
//    ) {
//        try {
//            // Use a simple direct approach to save the notification
//            val sharedPreferences = getSharedPreferences("appointment_notifications", MODE_PRIVATE)
//
//            // Create a simple appointment string instead of JSON object
//            val appointmentData = "$patientName|$doctorName|$selectedDate|$selectedTime|${System.currentTimeMillis()}"
//
//            // Get existing appointments and add the new one
//            val existingAppointments = sharedPreferences.getStringSet("appointments_simple", mutableSetOf()) ?: mutableSetOf()
//
//            val updatedAppointments = existingAppointments.toMutableSet()
//            updatedAppointments.add(appointmentData)
//
//            sharedPreferences.edit()
//                .putStringSet("appointments_simple", updatedAppointments)
//                .commit()
//
//            Toast.makeText(this, "Appointment booked successfully!", Toast.LENGTH_SHORT).show()
//
//        } catch (e: Exception) {
//            Toast.makeText(this, "Appointment booked, but failed to save notification", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun showAppointmentConfirmationNotification(doctorName: String, specialization: String, date: String, time: String) {
//        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        val channelId = "appointment_channel"
//
//        // Create notification channel for Android O and above
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                channelId,
//                "Appointment Notifications",
//                NotificationManager.IMPORTANCE_HIGH
//            )
//            channel.description = "Notifications for appointment confirmations"
//            notificationManager.createNotificationChannel(channel)
//        }
//
//        // Create the detailed message with doctor info, specialization, date, time and reminder
//        val patientName = "${binding.etFirstName.text.toString().trim()} ${binding.etLastName.text.toString().trim()}"
//        val message = "Dear $patientName,\n\nYour appointment has been confirmed with $doctorName from $specialization department.\n\nDate: $date\nTime: $time\n\nPlease arrive 15 minutes before your appointed time.\n\nThank you!"
//
//        val notification = NotificationCompat.Builder(this, channelId)
//            .setSmallIcon(R.drawable.medi_bridge_logo) // Use your app logo
//            .setContentTitle("Appointment Confirmed - Dr. $doctorName")
//            .setContentText("$specialization Department • $date at $time")
//            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setAutoCancel(true)
//            .build()
//
//        notificationManager.notify(1, notification)
//
//        // Also show a success toast
//        Toast.makeText(this, "Appointment confirmed! Check notification for details.", Toast.LENGTH_LONG).show()
//    }
//}

package com.aas.medi_bridge.Activity

import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.aas.medi_bridge.Adapter.DateChipAdapter
import com.aas.medi_bridge.Adapter.TimeChipAdapter
import com.aas.medi_bridge.Adapter.TimeSlot
import com.aas.medi_bridge.Domain.DoctorsModel
import com.aas.medi_bridge.R
import com.aas.medi_bridge.databinding.ActivityAppointmentFormBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.getValue

class AppointmentFormActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAppointmentFormBinding
    private lateinit var item: DoctorsModel
    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var bookedSlots: MutableMap<String, Set<String>> = mutableMapOf() // date -> set of booked times
    private var isDateSelected: Boolean = false // Track if user has manually selected a date

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppointmentFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get doctor data from intent
        item = intent.getParcelableExtra("Object") ?: return

        setupDoctorInfo()
        setupSpinners()
        setupForm()
        setupDatePicker()
        setupAvailableDatesAndTimes()
    }

    private fun setupDoctorInfo() {
        // Set doctor name in the form
        binding.tvDoctorName.text = item.name

        // Load doctor image with corner radius
        var imageUrl = item.image

        // Fallback to first chamber image if main image is blank
        if (imageUrl.isBlank() && item.chambers.isNotEmpty()) {
            imageUrl = item.chambers[0].image
        }

        // Convert Imgur URL to direct image URL if needed
        if (imageUrl.isNotBlank() && imageUrl.contains("imgur.com") && !imageUrl.contains("i.imgur.com")) {
            val imageId = imageUrl.substringAfterLast("/")
            imageUrl = "https://i.imgur.com/$imageId.jpg"
        }

        // Load image with corner radius using Glide
        com.bumptech.glide.Glide.with(this)
            .load(imageUrl)
            .transform(
                com.bumptech.glide.load.resource.bitmap.CenterCrop(),
                com.bumptech.glide.load.resource.bitmap.RoundedCorners(24)
            )
            .placeholder(R.drawable.blank_profile)
            .error(R.drawable.blank_profile)
            .into(binding.ivMedicalIcon)
    }

    private fun setupAvailableDatesAndTimes() {
        val availableDates = generateAvailableDates()
        val dateAdapter = DateChipAdapter(availableDates) { date ->
            selectedDate = date
            isDateSelected = true // Mark that user has selected a date
            selectedTime = "" // Reset selected time when date changes

            // Load booked appointments for this doctor and date, then update time slots
            loadBookedAppointments(date)
        }

        binding.availableDatesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.availableDatesRecyclerView.adapter = dateAdapter

        // DO NOT automatically select first date - let user choose
        // Clear time slots initially
        clearTimeSlots()
    }



    private fun loadBookedAppointments(date: String) {
        val database = FirebaseDatabase.getInstance()
        val appointmentsRef = database.getReference("appointments")

        // Add this debug log to see what's happening
        android.util.Log.d("AppointmentForm", "Loading appointments for date: $date, doctor: ${item.name}")

        appointmentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bookedTimes = mutableSetOf<String>()

                // Add debug log to see all appointments
                android.util.Log.d("AppointmentForm", "Total appointments in database: ${snapshot.childrenCount}")

                for (appointmentSnapshot in snapshot.children) {
                    val doctorId = appointmentSnapshot.child("doctorId").getValue(String::class.java)
                    val doctorName = appointmentSnapshot.child("doctorName").getValue(String::class.java)
                    val appointmentDate = appointmentSnapshot.child("appointmentDate").getValue(String::class.java)
                    val appointmentTime = appointmentSnapshot.child("appointmentTime").getValue(String::class.java)
                    val status = appointmentSnapshot.child("status").getValue(String::class.java)

                    // Add detailed logging
                    android.util.Log.d("AppointmentForm", "Checking appointment - Doctor: $doctorName, Date: $appointmentDate, Time: $appointmentTime, Status: $status")

                    val isDoctorMatch = doctorName == item.name || doctorId == item.name

                    if (isDoctorMatch && appointmentDate == date &&
                        appointmentTime != null && status != "cancelled") {
                        bookedTimes.add(appointmentTime)
                        android.util.Log.d("AppointmentForm", "Found booked time: $appointmentTime for doctor: $doctorName")
                    }
                }

                // Log final booked times
                android.util.Log.d("AppointmentForm", "Final booked times for $date: $bookedTimes")

                bookedSlots[date] = bookedTimes

                if (isDateSelected) {
                    updateTimeSlots(date, bookedTimes)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("AppointmentForm", "Database error: ${error.message}")
                Toast.makeText(this@AppointmentFormActivity,
                    "Failed to load booking information: ${error.message}", Toast.LENGTH_SHORT).show()

                if (isDateSelected) {
                    updateTimeSlots(date, emptySet())
                }
            }
        })
    }


    private fun updateTimeSlots(date: String, bookedTimes: Set<String>) {
        val allAvailableTimes = getAvailableTimesForDoctor()

        // Create TimeSlot objects with booking status
        val timeSlots = allAvailableTimes.map { time ->
            TimeSlot(time = time, isBooked = bookedTimes.contains(time))
        }

        val timeAdapter = TimeChipAdapter(timeSlots) { time ->
            selectedTime = time
        }

        binding.availableTimesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.availableTimesRecyclerView.adapter = timeAdapter
    }

    private fun clearTimeSlots() {
        // Show all available times initially (before date selection)
        val allAvailableTimes = getAvailableTimesForDoctor()
        val timeSlots = allAvailableTimes.map { time ->
            TimeSlot(time = time, isBooked = false)
        }

        val timeAdapter = TimeChipAdapter(timeSlots) { time ->
            if (!isDateSelected) {
                Toast.makeText(this, "Please select a date first", Toast.LENGTH_SHORT).show()
            } else {
                selectedTime = time
            }
        }

        binding.availableTimesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.availableTimesRecyclerView.adapter = timeAdapter
    }


    private fun generateAvailableDates(): List<String> {
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        val dates = mutableListOf<String>()
        val calendar = Calendar.getInstance()

        // Get closed days from visiting hours
        val closedDays = getClosedDaysFromVisitingHours()

        // Generate next 14 days excluding closed days
        for (i in 0 until 14) {
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val dayName = when (dayOfWeek) {
                Calendar.SUNDAY -> "sunday"
                Calendar.MONDAY -> "monday"
                Calendar.TUESDAY -> "tuesday"
                Calendar.WEDNESDAY -> "wednesday"
                Calendar.THURSDAY -> "thursday"
                Calendar.FRIDAY -> "friday"
                Calendar.SATURDAY -> "saturday"
                else -> ""
            }

            // Only add if not a closed day
            if (!closedDays.contains(dayName)) {
                dates.add(dateFormat.format(calendar.time))
            }

            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return dates
    }

    private fun getClosedDaysFromVisitingHours(): List<String> {
        val visitingHour = when {
            item.visiting_hour.isNotBlank() -> item.visiting_hour
            item.chambers.isNotEmpty() && item.chambers[0].visiting_hour.isNotBlank() -> item.chambers[0].visiting_hour
            else -> ""
        }

        val closedDays = mutableListOf<String>()

        if (visitingHour.contains("closed:", ignoreCase = true)) {
            val closedPart = visitingHour.substringAfter("closed:", "").trim()
            when {
                closedPart.contains("friday", ignoreCase = true) -> closedDays.add("friday")
                closedPart.contains("saturday", ignoreCase = true) -> closedDays.add("saturday")
                closedPart.contains("sunday", ignoreCase = true) -> closedDays.add("sunday")
                closedPart.contains("monday", ignoreCase = true) -> closedDays.add("monday")
                closedPart.contains("tuesday", ignoreCase = true) -> closedDays.add("tuesday")
                closedPart.contains("wednesday", ignoreCase = true) -> closedDays.add("wednesday")
                closedPart.contains("thursday", ignoreCase = true) -> closedDays.add("thursday")
            }
        }

        return closedDays
    }

    private fun getAvailableTimesForDoctor(): List<String> {
        val visitingHour = when {
            item.visiting_hour.isNotBlank() -> item.visiting_hour
            item.chambers.isNotEmpty() && item.chambers[0].visiting_hour.isNotBlank() -> item.chambers[0].visiting_hour
            else -> "9am to 5pm"
        }

        return parseVisitingHoursToTimeSlots(visitingHour)
    }

    private fun parseVisitingHoursToTimeSlots(visitingHours: String): List<String> {
        val timeSlots = mutableListOf<String>()

        try {
            // Clean the visiting hours string - remove parentheses content first
            val cleanHours = visitingHours.replace(Regex("\\([^)]*\\)"), "").trim()

            // Handle multiple time ranges separated by "&"
            val timeRanges = cleanHours.split("&")

            for (range in timeRanges) {
                val trimmedRange = range.trim()

                // Extract start and end times using " to " as separator
                val parts = trimmedRange.split(" to ")
                if (parts.size == 2) {
                    val startTimeRaw = parts[0].trim()
                    val endTimeRaw = parts[1].trim()

                    val startTime = normalizeTimeFormat(startTimeRaw)
                    val endTime = normalizeTimeFormat(endTimeRaw)

                    val slots = generateTimeSlots(startTime, endTime)
                    timeSlots.addAll(slots)
                }
            }
        } catch (e: Exception) {
            // Fallback to default times
            timeSlots.addAll(listOf("9:00 AM", "10:00 AM", "11:00 AM", "2:00 PM", "3:00 PM", "4:00 PM"))
        }

        val result = timeSlots.ifEmpty { listOf("9:00 AM", "10:00 AM", "11:00 AM", "2:00 PM", "3:00 PM", "4:00 PM") }
        return result
    }

    private fun normalizeTimeFormat(time: String): String {
        // Remove any extra spaces and unwanted characters but keep the basic time format
        val cleaned = time.trim().replace(Regex("\\s+"), " ")

        // Handle different time formats from database
        return when {
            // Format: "7pm" -> "7:00 PM"
            cleaned.matches(Regex("\\d{1,2}pm", RegexOption.IGNORE_CASE)) -> {
                val hour = cleaned.replace(Regex("[pm]", RegexOption.IGNORE_CASE), "")
                "$hour:00 PM"
            }
            // Format: "7am" -> "7:00 AM"
            cleaned.matches(Regex("\\d{1,2}am", RegexOption.IGNORE_CASE)) -> {
                val hour = cleaned.replace(Regex("[am]", RegexOption.IGNORE_CASE), "")
                "$hour:00 AM"
            }
            // Format: "10:30pm" -> "10:30 PM"
            cleaned.matches(Regex("\\d{1,2}:\\d{2}pm", RegexOption.IGNORE_CASE)) -> {
                cleaned.replace(Regex("pm", RegexOption.IGNORE_CASE), " PM")
            }
            // Format: "10:30am" -> "10:30 AM"
            cleaned.matches(Regex("\\d{1,2}:\\d{2}am", RegexOption.IGNORE_CASE)) -> {
                cleaned.replace(Regex("am", RegexOption.IGNORE_CASE), " AM")
            }
            // If already in correct format, return as is
            else -> cleaned
        }
    }

    private fun generateTimeSlots(startTime: String, endTime: String): List<String> {
        val timeSlots = mutableListOf<String>()
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())

        try {
            val start = sdf.parse(startTime) ?: return emptyList()
            val end = sdf.parse(endTime) ?: return emptyList()

            val calendar = Calendar.getInstance()
            calendar.time = start

            while (calendar.time.before(end)) {
                timeSlots.add(sdf.format(calendar.time))
                calendar.add(Calendar.MINUTE, 15) // 15-minute intervals
            }
        } catch (e: Exception) {
            // Handle error silently
        }

        return timeSlots
    }

    private fun setupSpinners() {

        // Setup Gender Spinner with placeholder
        val genderOptions = arrayOf("Please select", "Male", "Female")

        // Create custom adapter with simpler approach
    
        val genderAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, genderOptions) {

            override fun isEnabled(position: Int): Boolean {
                return position != 0
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view as TextView


                // Style the main display text

                textView.setPadding(16, 16, 16, 16)
                textView.textSize = 16f

                if (position == 0) {
                    textView.setTextColor(Color.parseColor("#9CA3AF"))
                } else {
                    textView.setTextColor(Color.parseColor("#1f2937"))
                }

                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view as TextView

                if (position == 0) {
                    // Style placeholder in dropdown - make it look disabled
                    textView.setPadding(16, 8, 16, 8)
                    textView.textSize = 14f
                    textView.setTextColor(Color.parseColor("#9CA3AF"))
                    textView.setBackgroundColor(Color.parseColor("#f9fafb"))
                    textView.isEnabled = false
                } else {
                    // Style normal dropdown items
                    textView.setPadding(16, 12, 16, 12)
                    textView.textSize = 16f
                    textView.setTextColor(Color.parseColor("#1f2937"))
                    textView.setBackgroundColor(Color.parseColor("#ffffff"))
                    textView.isEnabled = true
                }


                return view
            }
        }


        // Set dropdown resource and apply to spinner
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGender.adapter = genderAdapter

        // Customize spinner appearance
        binding.spinnerGender.apply {
            dropDownVerticalOffset = 0
            dropDownWidth = ViewGroup.LayoutParams.MATCH_PARENT
            setBackgroundResource(R.drawable.rounded_edittext_background)
            setPadding(16, 0, 16, 0)
        }

        // Add selection listener
        binding.spinnerGender.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val selectedGender = genderOptions[position]
                    android.util.Log.d("AppointmentForm", "Gender selected: $selectedGender")

                    // Update the text color after selection

                    (view as? TextView)?.setTextColor(Color.parseColor("#1f2937"))
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                android.util.Log.d("AppointmentForm", "No gender selected")
            }
        }
    }

    private fun setupForm() {
        // Set up red asterisks for required fields
        setupRedAsterisks()

        // Submit button click listener
        binding.btnSubmit.setOnClickListener {
            if (validateForm()) {
                collectFormData()

                // Show appointment confirmation notification using actual doctor data
                showAppointmentConfirmationNotification(
                    item.name,
                    item.specialization,
                    selectedDate,
                    selectedTime
                )

                finish()
            }
        }
    }

    private fun setupDatePicker() {
        binding.etDateOfBirth.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val formattedDate = String.format(Locale.getDefault(), "%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear)
                    binding.etDateOfBirth.setText(formattedDate)
                },
                year, month, day
            )

            // Set maximum date to today (no future dates for birth date)
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

            // Set minimum date to 100 years ago
            val minCalendar = Calendar.getInstance()
            minCalendar.add(Calendar.YEAR, -100)
            datePickerDialog.datePicker.minDate = minCalendar.timeInMillis

            datePickerDialog.show()
        }
    }

    private fun setupRedAsterisks() {
        // Helper function to create text with red asterisk
        fun createLabelWithRedAsterisk(text: String): android.text.SpannableString {
            val spannableString = android.text.SpannableString("$text *")
            val foregroundColorSpan = android.text.style.ForegroundColorSpan(Color.parseColor("#dc2626"))
            spannableString.setSpan(foregroundColorSpan, text.length + 1, spannableString.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return spannableString
        }

        // Apply red asterisks to required field labels
        binding.tvFirstNameLabel.text = createLabelWithRedAsterisk("First Name")
        binding.tvLastNameLabel.text = createLabelWithRedAsterisk("Last Name")
        binding.tvDobLabel.text = createLabelWithRedAsterisk("Date of Birth")
        binding.tvGenderLabel.text = createLabelWithRedAsterisk("Gender")
        binding.tvContactLabel.text = createLabelWithRedAsterisk("Contact Number")
        // Add red asterisks for Available Dates and Available Times
        binding.tvAvailableDatesLabel.text = createLabelWithRedAsterisk("Available Dates")
        binding.tvAvailableTimesLabel.text = createLabelWithRedAsterisk("Available Times")
    }

    private fun validateForm(): Boolean {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val dob = binding.etDateOfBirth.text.toString().trim()
        val contactNumber = binding.etContactNumber.text.toString().trim()

        when {
            firstName.isEmpty() -> {
                binding.etFirstName.error = "First name is required"
                binding.etFirstName.requestFocus()
                return false
            }
            lastName.isEmpty() -> {
                binding.etLastName.error = "Last name is required"
                binding.etLastName.requestFocus()
                return false
            }
            dob.isEmpty() -> {
                Toast.makeText(this, "Please select date of birth", Toast.LENGTH_SHORT).show()
                return false
            }
            contactNumber.isEmpty() -> {
                binding.etContactNumber.error = "Contact number is required"
                binding.etContactNumber.requestFocus()
                return false
            }
            binding.spinnerGender.selectedItemPosition == 0 -> {
                Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show()
                return false
            }
            selectedDate.isEmpty() -> {
                Toast.makeText(this, "Please select an appointment date", Toast.LENGTH_SHORT).show()
                return false
            }
            selectedTime.isEmpty() -> {
                Toast.makeText(this, "Please select an appointment time", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun collectFormData() {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val dob = binding.etDateOfBirth.text.toString().trim()
        val gender = binding.spinnerGender.selectedItem.toString()
        val contactNumber = binding.etContactNumber.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val message = binding.etMessage.text.toString().trim()

        // Use the selected date and time from the RecyclerViews
        val doctorName = item.name
        val doctorSpecialization = item.specialization
        val patientName = "$firstName $lastName"

        // Log the collected data
        android.util.Log.d("AppointmentForm", "=== Appointment Data ===")
        android.util.Log.d("AppointmentForm", "Doctor: $doctorName")
        android.util.Log.d("AppointmentForm", "Patient: $patientName")
        android.util.Log.d("AppointmentForm", "DOB: $dob")
        android.util.Log.d("AppointmentForm", "Gender: $gender")
        android.util.Log.d("AppointmentForm", "Contact: $contactNumber")
        android.util.Log.d("AppointmentForm", "Email: $email")
        android.util.Log.d("AppointmentForm", "Message: $message")
        android.util.Log.d("AppointmentForm", "Date: $selectedDate")
        android.util.Log.d("AppointmentForm", "Time: $selectedTime")
        android.util.Log.d("AppointmentForm", "========================")

        // Save appointment to Firebase database
        saveAppointmentToFirebase(firstName, lastName, contactNumber, email, message, doctorName, doctorSpecialization)

        // Also save appointment notification locally for backup
        saveAppointmentNotificationLocally(doctorName, patientName, doctorSpecialization)
    }

    private fun saveAppointmentToFirebase(
        firstName: String,
        lastName: String,
        contactNumber: String,
        email: String,
        message: String,
        doctorName: String,
        doctorSpecialization: String
    ) {
        try {
            val database = FirebaseDatabase.getInstance()
            val appointmentsRef = database.getReference("appointments")

            // Generate unique appointment ID
            val appointmentId = appointmentsRef.push().key
            if (appointmentId == null) {
                android.util.Log.e("AppointmentForm", "Failed to generate appointment ID")
                Toast.makeText(this@AppointmentFormActivity, "Failed to create appointment ID", Toast.LENGTH_SHORT).show()
                return
            }

            // Create appointment object with correct doctor information
            val appointmentData = mapOf(
                "id" to appointmentId,
                "patientName" to "$firstName $lastName",
                "patientPhone" to contactNumber,
                "appointmentDate" to selectedDate,
                "appointmentTime" to selectedTime,
                "doctorId" to doctorName, // Use doctor name as ID since DoctorsModel has no unique ID field
                "doctorEmail" to "", // DoctorsModel has no email field
                "doctorName" to doctorName,
                "status" to "pending",
                "symptoms" to message,
                "timestamp" to System.currentTimeMillis()
            )

            android.util.Log.d("AppointmentForm", "Attempting to save appointment data: $appointmentData")

            // Save to Firebase with improved error handling
            appointmentsRef.child(appointmentId).setValue(appointmentData)
                .addOnSuccessListener {
                    android.util.Log.d("AppointmentForm", "Appointment saved successfully with ID: $appointmentId")
                    Toast.makeText(this@AppointmentFormActivity, "Appointment booked successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { error ->
                    android.util.Log.e("AppointmentForm", "Failed to save appointment: ${error.message}")

                    val errorMessage = when {
                        error.message?.contains("permission", ignoreCase = true) == true ->
                            "Database permission denied. Please check Firebase security rules."
                        error.message?.contains("network", ignoreCase = true) == true ->
                            "Network error. Please check your internet connection."
                        else -> "Failed to book appointment: ${error.message}"
                    }

                    Toast.makeText(this@AppointmentFormActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            android.util.Log.e("AppointmentForm", "Exception in saveAppointmentToFirebase: ${e.message}")
            Toast.makeText(this@AppointmentFormActivity, "Error occurred while booking appointment", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveAppointmentNotificationLocally(
        doctorName: String,
        patientName: String,
        doctorSpecialization: String
    ) {
        try {
            // Use a simple direct approach to save the notification
            val sharedPreferences = getSharedPreferences("appointment_notifications", MODE_PRIVATE)

            // Create a simple appointment string instead of JSON object
            val appointmentData = "$patientName|$doctorName|$selectedDate|$selectedTime|${System.currentTimeMillis()}"

            // Get existing appointments and add the new one
            val existingAppointments = sharedPreferences.getStringSet("appointments_simple", mutableSetOf()) ?: mutableSetOf()

            val updatedAppointments = existingAppointments.toMutableSet()
            updatedAppointments.add(appointmentData)

            sharedPreferences.edit()
                .putStringSet("appointments_simple", updatedAppointments)
                .commit()

            Toast.makeText(this, "Appointment booked successfully!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Appointment booked, but failed to save notification", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAppointmentConfirmationNotification(doctorName: String, specialization: String, date: String, time: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "appointment_channel"

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Appointment Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notifications for appointment confirmations"
            notificationManager.createNotificationChannel(channel)
        }

        // Create the detailed message with doctor info, specialization, date, time and reminder
        val patientName = "${binding.etFirstName.text.toString().trim()} ${binding.etLastName.text.toString().trim()}"
        val message = "Dear $patientName,\n\nYour appointment has been confirmed with $doctorName from $specialization department.\n\nDate: $date\nTime: $time\n\nPlease arrive 15 minutes before your appointed time.\n\nThank you!"

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.medi_bridge_logo) // Use your app logo
            .setContentTitle("Appointment Confirmed - $doctorName")
            .setContentText("$specialization Department • $date at $time")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)

        // Also show a success toast
        Toast.makeText(this, "Appointment confirmed! Check notification for details.", Toast.LENGTH_LONG).show()
    }
}