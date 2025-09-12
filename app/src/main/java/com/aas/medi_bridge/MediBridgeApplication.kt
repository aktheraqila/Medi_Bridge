package com.aas.medi_bridge

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class MediBridgeApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Enable offline persistence for Firebase Database
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        android.util.Log.d("MediBridgeApp", "Firebase initialized successfully")
    }
}
