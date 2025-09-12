package com.aas.medi_bridge.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aas.medi_bridge.Domain.CategoryModel
import com.aas.medi_bridge.Domain.DoctorsModel
import com.aas.medi_bridge.Domain.Chamber
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainviewModel(): ViewModel() {

    private val firebaseDatabase = FirebaseDatabase.getInstance()

    private val _catagory = MutableLiveData<MutableList<CategoryModel>>()
    private val _doctors = MutableLiveData<MutableList<DoctorsModel>>()

    val category: LiveData<MutableList<CategoryModel>> = _catagory
    val doctors: LiveData<MutableList<DoctorsModel>> = _doctors

    init {
        // Add connection status logging
        android.util.Log.d("MainviewModel", "MainviewModel initialized")
        android.util.Log.d("MainviewModel", "Firebase database instance: ${firebaseDatabase}")

        // Test Firebase connection
        val connectedRef = firebaseDatabase.getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                android.util.Log.d("MainviewModel", "Firebase connected: $connected")
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("MainviewModel", "Firebase connection check failed: ${error.message}")
            }
        })
    }

    fun loadCategory() {
        android.util.Log.d("MainviewModel", "Starting loadCategory()")
        val Ref = firebaseDatabase.getReference("Category")
        Ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                android.util.Log.d("MainviewModel", "Category onDataChange - exists: ${snapshot.exists()}, children: ${snapshot.childrenCount}")
                val lists = mutableListOf<CategoryModel>()
                for (childSnapshot in snapshot.children) {
                    val list = childSnapshot.getValue(CategoryModel::class.java)
                    if (list != null) {
                        lists.add(list)
                        android.util.Log.d("MainviewModel", "Added category: ${list.name}")
                    }
                }

                android.util.Log.d("MainviewModel", "Setting category value with ${lists.size} items")
                _catagory.value = lists
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("MainviewModel", "Category database error: ${error.message}")
            }
        })
    }

    fun loadDoctors() {
        android.util.Log.d("MainviewModel", "Starting loadDoctors()")
        val originalDoctorsRef = firebaseDatabase.getReference("Doctors")
        val newDoctorsRef = firebaseDatabase.getReference("doctors")

        // First load original doctors from "Doctors" node
        originalDoctorsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                android.util.Log.d("MainviewModel", "Original Doctors onDataChange - exists: ${snapshot.exists()}, children: ${snapshot.childrenCount}")
                val originalDoctorsList = mutableListOf<DoctorsModel>()

                for (childSnapshot in snapshot.children) {
                    val doctor = childSnapshot.getValue(DoctorsModel::class.java)
                    if (doctor != null) {
                        originalDoctorsList.add(doctor)
                        android.util.Log.d("MainviewModel", "Added original doctor: ${doctor.name}")
                    }
                }

                // Then load new doctors from "doctors" node and combine
                loadAndCombineNewDoctors(originalDoctorsList)
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("MainviewModel", "Original doctors database error: ${error.message}")
            }
        })
    }

    private fun loadAndCombineNewDoctors(originalDoctors: MutableList<DoctorsModel>) {
        android.util.Log.d("MainviewModel", "Loading new doctors from 'doctors' node")
        val newDoctorsRef = firebaseDatabase.getReference("doctors")

        newDoctorsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                android.util.Log.d("MainviewModel", "New doctors onDataChange - exists: ${snapshot.exists()}, children: ${snapshot.childrenCount}")
                val newDoctorsList = mutableListOf<DoctorsModel>()

                for (childSnapshot in snapshot.children) {
                    val doctorData = childSnapshot.value as? Map<String, Any>
                    if (doctorData != null) {
                        // Check if doctor is approved
                        val approved = doctorData["approved"] as? Boolean ?: false
                        val status = doctorData["status"] as? String ?: "pending"

                        if (approved && status == "approved") {
                            // Convert the new doctor data to DoctorsModel format
                            val doctor = convertNewDoctorToDoctorsModel(doctorData)
                            if (doctor != null) {
                                newDoctorsList.add(doctor)
                                android.util.Log.d("MainviewModel", "Added new approved doctor: ${doctor.name}")
                            }
                        } else {
                            android.util.Log.d("MainviewModel", "Skipped unapproved doctor: ${doctorData["name"]}")
                        }
                    }
                }

                // Combine original doctors + new doctors
                val combinedList = originalDoctors.toMutableList()
                combinedList.addAll(newDoctorsList)

                android.util.Log.d("MainviewModel", "Combined doctors list: ${combinedList.size} total (${originalDoctors.size} original + ${newDoctorsList.size} new)")
                _doctors.value = combinedList
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("MainviewModel", "New doctors database error: ${error.message}")
                // If new doctors fail to load, at least show original doctors
                _doctors.value = originalDoctors
            }
        })
    }

    private fun convertNewDoctorToDoctorsModel(doctorData: Map<String, Any>): DoctorsModel? {
        return try {
            val chambers = doctorData["chambers"] as? List<Map<String, Any>> ?: emptyList()
            val chambersList = chambers.map { chamberMap ->
                Chamber(
                    name = chamberMap["name"] as? String ?: "",
                    address = chamberMap["address"] as? String ?: "",
                    visiting_hour = chamberMap["visiting_hour"] as? String ?: "",
                    appointment_number = chamberMap["appointment_number"] as? String ?: "",
                    location = chamberMap["location"] as? String ?: "",
                    image = chamberMap["image"] as? String ?: ""
                )
            }

            DoctorsModel(
                name = doctorData["name"] as? String ?: "",
                specialization = doctorData["specialization"] as? String ?: "",
                degrees = doctorData["degrees"] as? String ?: "",
                designation = doctorData["designation"] as? String ?: "",
                city = doctorData["city"] as? String ?: "",
                patients = "0", // Default for new doctors
                rating = 0.0, // Default for new doctors
                image = "", // No image for new doctors initially
                bio = "", // No bio for new doctors initially
                address = chambersList.firstOrNull()?.address ?: "",
                experience = 0, // Default for new doctors
                mobile = "", // Not stored in new doctor registration
                site = "", // Not stored in new doctor registration
                location = chambersList.firstOrNull()?.location ?: "",
                chambers = chambersList,
                visiting_hour = chambersList.firstOrNull()?.visiting_hour ?: ""
            )
        } catch (e: Exception) {
            android.util.Log.e("MainviewModel", "Error converting new doctor data: ${e.message}")
            null
        }
    }
}