package com.aas.medi_bridge.Adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aas.medi_bridge.Activity.DetailActivity
import com.aas.medi_bridge.Domain.DoctorsModel
import com.aas.medi_bridge.databinding.ViewholderTopDoctor3Binding
import com.bumptech.glide.Glide

class DeptDoctorAdapter(val items: MutableList<DoctorsModel>) : RecyclerView.Adapter<DeptDoctorAdapter.ViewHolder>() {
    private var context: Context? = null

    class ViewHolder(val binding: ViewholderTopDoctor3Binding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val binding = ViewholderTopDoctor3Binding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doctor = items[position]
        Log.d("DeptDoctorAdapter", "Binding doctor: ${doctor.name}, specialization: ${doctor.specialization}, rating: ${doctor.rating}")
        holder.binding.nameTxt.text = doctor.name
        holder.binding.specialTxt.text = doctor.specialization

        // Fix: Ensure rating is a valid Float
        val ratingFloat = when (doctor.rating) {
            is Float -> doctor.rating
            is Double -> (doctor.rating as Double).toFloat()
            is Int -> (doctor.rating as Int).toFloat()
            is String -> doctor.rating.toFloatOrNull() ?: 0f
            else -> 0f
        }
        // Load image
        var imageUrl = doctor.image
        if (imageUrl.isBlank() && doctor.chambers.isNotEmpty()) {
            imageUrl = doctor.chambers[0].image
        }
        if (imageUrl.isNotBlank() && imageUrl.contains("imgur.com") && !imageUrl.contains("i.imgur.com")) {
            val imageId = imageUrl.substringAfterLast("/")
            imageUrl = "https://i.imgur.com/$imageId.jpg"
        }
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_gallery)
            .centerCrop()
            .into(holder.binding.img)
        // Make Appointment button
        holder.binding.makeBtn.setOnClickListener {
            val intent = Intent(holder.itemView.context, DetailActivity::class.java)
            intent.putExtra("Object", doctor)
            holder.itemView.context.startActivity(intent)
        }
        // Card click opens details
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, DetailActivity::class.java)
            intent.putExtra("Object", doctor)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size
}
