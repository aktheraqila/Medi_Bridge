# 🏥 Doctor Finder & Appointment App

A modern doctor discovery and appointment booking app focused on simplicity and accessibility — no login required.

---

## 🚀 Features

- **Browse doctors** with name, specialization, and hospital details.
- **Search and filter** by city, specialization, or hospital.
- **Tap to view full doctor profile** with schedule and details.
- **Book appointments** directly from the profile page.
- **View upcoming appointments** from a simple dashboard.
- No login or signup required for access.

---

## 💻 Tech Stack

- **Frontend**: Kotlin (Android)
- **Architecture**: MVVM 
- **UI**: XML layouts (Material Design)
- **Backend**: Firebase Firestore (for real-time data)
- **Storage**: Firebase Storage (for doctor images)

---

## 🗂 Project Structure

```
app/
├─ java/com/example/appointmentapp/
│   ├─ ui/
│   │   ├─ dashboard/
│   │   │   └─ DashboardActivity.java
│   │   ├─ doctor/
│   │   │   └─ DoctorProfileActivity.java
│   │   └─ appointments/
│   │       └─ MyAppointmentsActivity.java
│   ├─ viewmodels/
│   │   └─ AppointmentViewModel.java
│   ├─ repository/
│   │   └─ FirebaseRepository.java
│   └─ model/
│       ├─ Doctor.java
│       └─ Appointment.java
└─ res/
    ├─ layout/
    │   ├─ activity_dashboard.xml
    │   ├─ activity_doctor_profile.xml
    │   └─ activity_my_appointments.xml
    └─ values/
        └─ styles.xml

---

## 🔧 Setup Instructions

1. Clone this repository.
2. Create a Firebase project and enable Firestore and Storage.
3. Download `google-services.json` and place it in your `app/` folder.
4. Add doctor and appointment sample data to Firestore.
5. Build and run the app in Android Studio.

---

## 📱 Usage

- Open the app to view all available doctors.
- Use filters or search bar to refine results.
- Tap a doctor’s card to view details.
- Choose a time slot and book an appointment.
- See your booked appointments from the home screen.

---
