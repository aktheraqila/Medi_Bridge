package com.aas.medi_bridge.Domain

import android.os.Parcel
import android.os.Parcelable

data class DoctorsModel(
    val name: String = "",
    val specialization: String = "",
    val degrees: String = "",
    val designation: String = "",
    val city: String = "",
    val patients: String = "",
    val rating: Double = 0.0,
    val image: String = "",
    val bio: String = "",
    val address: String = "",
    val experience: Int = 0,
    val mobile: String = "",
    val site: String = "",
    val location: String = "",
    val chambers: List<Chamber> = emptyList(),
    val visiting_hour: String = ""
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.createTypedArrayList(Chamber.CREATOR) ?: emptyList(),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(specialization)
        parcel.writeString(degrees)
        parcel.writeString(designation)
        parcel.writeString(city)
        parcel.writeString(patients)
        parcel.writeDouble(rating)
        parcel.writeString(image)
        parcel.writeString(bio)
        parcel.writeString(address)
        parcel.writeInt(experience)
        parcel.writeString(mobile)
        parcel.writeString(site)
        parcel.writeString(location)
        parcel.writeTypedList(chambers)
        parcel.writeString(visiting_hour)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DoctorsModel> {
        override fun createFromParcel(parcel: Parcel): DoctorsModel {
            return DoctorsModel(parcel)
        }

        override fun newArray(size: Int): Array<DoctorsModel?> {
            return arrayOfNulls(size)
        }
    }
}

data class Chamber(
    val name: String = "",
    val address: String = "",
    val appointment_number: String? = null,
    val visiting_hour: String = "",
    val location: String = "",
    val image: String = ""
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(address)
        parcel.writeString(appointment_number)
        parcel.writeString(visiting_hour)
        parcel.writeString(location)
        parcel.writeString(image)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Chamber> {
        override fun createFromParcel(parcel: Parcel): Chamber {
            return Chamber(parcel)
        }

        override fun newArray(size: Int): Array<Chamber?> {
            return arrayOfNulls(size)
        }
    }
}
