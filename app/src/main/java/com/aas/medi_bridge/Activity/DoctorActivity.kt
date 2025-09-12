package com.aas.medi_bridge.Activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.core.content.edit
import com.aas.medi_bridge.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.*
import kotlin.or

class DoctorActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageView
    private lateinit var formTitle: TextView
    private lateinit var formSubtitle: TextView
    private lateinit var loginContainer: CardView

    // Login fields
    private lateinit var loginFieldsContainer: LinearLayout
    private lateinit var loginNameEditText: TextInputEditText
    private lateinit var loginIdEditText: TextInputEditText

    // Signup fields
    private lateinit var signupFieldsContainer: LinearLayout
    private lateinit var signupNameEditText: TextInputEditText
    private lateinit var signupEmailEditText: TextInputEditText // Re-add email field
    private lateinit var signupDegreesEditText: TextInputEditText
    private lateinit var signupSpecialtyEditText: TextInputEditText
    private lateinit var signupDesignationEditText: TextInputEditText
    private lateinit var signupChamberNameEditText: TextInputEditText
    private lateinit var signupChamberAddressEditText: TextInputEditText
    private lateinit var signupVisitingHourEditText: TextInputEditText
    private lateinit var signupAppointmentNumberEditText: TextInputEditText

    // Action buttons
    private lateinit var primaryActionBtn: AppCompatButton
    private lateinit var switchModeText: TextView

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var database: DatabaseReference

    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor)

        initViews()
        initFirebase()
        setupClickListeners()
        checkIfLoggedIn()
    }

    private fun initViews() {
        backBtn = findViewById(R.id.backBtn)
        formTitle = findViewById(R.id.formTitle)
        formSubtitle = findViewById(R.id.formSubtitle)
        loginContainer = findViewById(R.id.loginContainer)

        // Login fields
        loginFieldsContainer = findViewById(R.id.loginFieldsContainer)
        loginNameEditText = findViewById(R.id.loginNameEditText)
        loginIdEditText = findViewById(R.id.loginIdEditText)

        // Signup fields
        signupFieldsContainer = findViewById(R.id.signupFieldsContainer)
        signupNameEditText = findViewById(R.id.signupNameEditText)
        signupEmailEditText = findViewById(R.id.signupEmailEditText) // Re-add email field
        signupDegreesEditText = findViewById(R.id.signupDegreesEditText)
        signupSpecialtyEditText = findViewById(R.id.signupSpecialtyEditText)
        signupDesignationEditText = findViewById(R.id.signupDesignationEditText)
        signupChamberNameEditText = findViewById(R.id.signupChamberNameEditText)
        signupChamberAddressEditText = findViewById(R.id.signupChamberAddressEditText)
        signupVisitingHourEditText = findViewById(R.id.signupVisitingHourEditText)
        signupAppointmentNumberEditText = findViewById(R.id.signupAppointmentNumberEditText)

        // Action buttons
        primaryActionBtn = findViewById(R.id.primaryActionBtn)
        switchModeText = findViewById(R.id.switchModeText)

        sharedPreferences = getSharedPreferences("DoctorPrefs", MODE_PRIVATE)
    }

    private fun initFirebase() {
        database = FirebaseDatabase.getInstance().reference
    }

    private fun setupClickListeners() {
        backBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        primaryActionBtn.setOnClickListener {
            if (isLoginMode) {
                performLogin()
            } else {
                performSignup()
            }
        }

        switchModeText.setOnClickListener {
            toggleMode()
        }
    }

    private fun checkIfLoggedIn() {
        val doctorName = sharedPreferences.getString("doctor_name", null)
        val doctorId = sharedPreferences.getString("doctor_id", null)

        if (!doctorName.isNullOrEmpty() && !doctorId.isNullOrEmpty()) {
            navigateToDashboard()
        }
    }

    private fun toggleMode() {
        isLoginMode = !isLoginMode

        if (isLoginMode) {
            // Switch to login mode
            formTitle.text = getString(R.string.login_to_account)
            formSubtitle.text = getString(R.string.enter_credentials)
            primaryActionBtn.text = getString(R.string.login)
            switchModeText.text = getString(R.string.dont_have_account_signup)
            loginFieldsContainer.visibility = View.VISIBLE
            signupFieldsContainer.visibility = View.GONE
        } else {
            // Switch to signup mode
            formTitle.text = getString(R.string.create_new_account)
            formSubtitle.text = getString(R.string.fill_info_register)
            primaryActionBtn.text = getString(R.string.signup)
            switchModeText.text = getString(R.string.already_have_account_login)
            loginFieldsContainer.visibility = View.GONE
            signupFieldsContainer.visibility = View.VISIBLE
        }
    }

    private fun performLogin() {
        val name = loginNameEditText.text.toString().trim()
        val id = loginIdEditText.text.toString().trim()

        if (name.isEmpty() || id.isEmpty()) {
            Toast.makeText(this, "Please fill in both Doctor Name and Doctor ID", Toast.LENGTH_SHORT).show()
            return
        }

        // First check the new registrations node ("doctors")
        database.child("doctors").child(id).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    handleLoginFromNewRegistrations(snapshot, name, id)
                } else {
                    // If not found in new registrations, check original JSON structure ("Doctors")
                    checkOriginalDoctorsNode(name, id)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DoctorActivity, "Login failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleLoginFromNewRegistrations(snapshot: DataSnapshot, name: String, id: String) {
        val doctorName = snapshot.child("name").getValue(String::class.java)
        val doctorEmail = snapshot.child("email").getValue(String::class.java)
        val approved = snapshot.child("approved").getValue(Boolean::class.java) ?: false
        val status = snapshot.child("status").getValue(String::class.java) ?: "pending"

        android.util.Log.d("DoctorLogin", "New registration found: $doctorName, email: $doctorEmail, approved: $approved, status: $status")

        if (doctorName != null && doctorName.equals(name, ignoreCase = true)) {
            when {
                status == "rejected" -> {
                    Toast.makeText(this, "Your registration has been rejected. Please contact admin.", Toast.LENGTH_LONG).show()
                }
                !approved || status == "pending" -> {
                    Toast.makeText(this, "Your account is pending admin approval. Please wait for approval notification.", Toast.LENGTH_LONG).show()
                }
                status == "approved" -> {
                    saveLoginInfo(doctorName, id, doctorEmail ?: "")
                    navigateToDashboard()
                    Toast.makeText(this, "Welcome back, $doctorName!", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, "Account status unknown. Please contact admin.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Invalid credentials. Please check your name and ID.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkOriginalDoctorsNode(name: String, id: String) {
        // Check in the original "Doctors" node from JSON
        database.child("Doctors").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var doctorFound = false

                for (doctorSnapshot in snapshot.children) {
                    val doctorName = doctorSnapshot.child("name").getValue(String::class.java)
                    val doctorEmail = doctorSnapshot.child("email").getValue(String::class.java)

                    if (doctorName != null && doctorName.equals(name, ignoreCase = true)) {
                        doctorFound = true
                        saveLoginInfo(doctorName, id, doctorEmail ?: "")
                        navigateToDashboard()
                        Toast.makeText(this@DoctorActivity, "Welcome back, $doctorName!", Toast.LENGTH_SHORT).show()
                        break
                    }
                }

                if (!doctorFound) {
                    Toast.makeText(this@DoctorActivity, "Invalid credentials. Please check your name and ID.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DoctorActivity, "Login failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun performSignup() {
        // Get the doctor name from the signup field
        val name = signupNameEditText.text.toString().trim()
        val email = signupEmailEditText.text.toString().trim() // Get email
        val degrees = signupDegreesEditText.text.toString().trim()
        val specialty = signupSpecialtyEditText.text.toString().trim()
        val designation = signupDesignationEditText.text.toString().trim()
        val chamberName = signupChamberNameEditText.text.toString().trim()
        val chamberAddress = signupChamberAddressEditText.text.toString().trim()
        val visitingHour = signupVisitingHourEditText.text.toString().trim()
        val appointmentNumber = signupAppointmentNumberEditText.text.toString().trim()

        // Validate all required fields (including email)
        if (name.isEmpty() || email.isEmpty() || degrees.isEmpty() || specialty.isEmpty() ||
            designation.isEmpty() || chamberName.isEmpty() ||
            chamberAddress.isEmpty() || visitingHour.isEmpty() || appointmentNumber.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields marked with *", Toast.LENGTH_SHORT).show()
            return
        }

        // Generate unique doctor ID
        val doctorId = generateDoctorId()

        // Create doctor data with email (no Firebase Auth)
        val doctorData = hashMapOf(
            "name" to name,
            "id" to doctorId,
            "email" to email, // Include email
            "degrees" to degrees,
            "specialization" to specialty,
            "specialty" to specialty, // Keep both for compatibility
            "designation" to designation,
            "city" to extractCityFromWorkplace(chamberAddress),
            "chambers" to listOf(
                hashMapOf(
                    "name" to chamberName,
                    "address" to chamberAddress,
                    "visiting_hour" to visitingHour,
                    "appointment_number" to appointmentNumber,
                    "location" to "", // Can be added later
                    "image" to "" // Can be added later
                )
            ),
            "status" to "pending", // Pending approval
            "registrationDate" to System.currentTimeMillis(),
            "approved" to false
        )

        // Save directly to Firebase Database (no Firebase Auth needed)
        database.child("doctors").child(doctorId).setValue(doctorData)
            .addOnSuccessListener {
                // Start listening for approval notifications
                com.aas.medi_bridge.Service.DoctorNotificationService.startListeningForApproval(this@DoctorActivity, doctorId)

                // Show the generated Doctor ID to the user
                Toast.makeText(this@DoctorActivity,
                    "Registration successful! Your Doctor ID is: $doctorId\n" +
                            "Please save this ID for login. You will be notified once approved.",
                    Toast.LENGTH_LONG).show()

                // Also show an alert dialog with the ID so user can copy it
                showDoctorIdDialog(doctorId, name)

                // Clear form and switch to login mode
                clearAllFormFields()
                toggleMode() // Switch back to login
            }
            .addOnFailureListener { error ->
                Toast.makeText(this@DoctorActivity,
                    "Failed to save doctor data: ${error.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun generateDoctorId(): String {
        val timestamp = System.currentTimeMillis().toString().takeLast(6)
        return "DR${timestamp}"
    }

    private fun extractCityFromWorkplace(workplace: String): String {
        // Simple city extraction - can be enhanced
        return when {
            workplace.contains("Dhaka", ignoreCase = true) -> "Dhaka"
            workplace.contains("Chittagong", ignoreCase = true) -> "Chittagong"
            workplace.contains("Sylhet", ignoreCase = true) -> "Sylhet"
            workplace.contains("Rajshahi", ignoreCase = true) -> "Rajshahi"
            workplace.contains("Khulna", ignoreCase = true) -> "Khulna"
            workplace.contains("Barisal", ignoreCase = true) -> "Barisal"
            workplace.contains("Rangpur", ignoreCase = true) -> "Rangpur"
            else -> "Other"
        }
    }

    private fun sendDoctorIdViaEmail(doctorId: String, email: String) {
        // This would integrate with an email service like SendGrid or Firebase Functions
        // For now, we'll just log it
        android.util.Log.d("DoctorSignup", "Doctor ID $doctorId should be sent to $email")
    }

    private fun saveLoginInfo(name: String, id: String, email: String = "") {
        sharedPreferences.edit {
            putString("doctor_name", name)
            putString("doctor_id", id)
            putString("doctor_email", email)
        }
        android.util.Log.d("DoctorLogin", "Saved login info - Name: $name, ID: $id, Email: $email")
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showDoctorIdDialog(doctorId: String, doctorName: String) {
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Registration Successful!")
            .setMessage("Dear $doctorName,\n\nYour Doctor ID is: $doctorId\n\nPlease save this ID for future login. You will be notified once your account is approved by admin.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()

        alertDialog.show()
    }

    private fun clearAllFormFields() {
        signupNameEditText.text?.clear()
        signupEmailEditText.text?.clear() // Clear email
        signupDegreesEditText.text?.clear()
        signupSpecialtyEditText.text?.clear()
        signupDesignationEditText.text?.clear()
        signupChamberNameEditText.text?.clear()
        signupChamberAddressEditText.text?.clear()
        signupVisitingHourEditText.text?.clear()
        signupAppointmentNumberEditText.text?.clear()

        loginNameEditText.text?.clear()
        loginIdEditText.text?.clear()
    }
}