package com.aas.medi_bridge.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aas.medi_bridge.Activity.DetailActivity
import com.aas.medi_bridge.Domain.DoctorsModel
import com.aas.medi_bridge.databinding.ViewholderTopDoctorBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

class TopDoctorAdapter(val items: MutableList<DoctorsModel>): RecyclerView.Adapter<TopDoctorAdapter.ViewHolder>() {
    private var context: Context?=null

    class ViewHolder(val binding: ViewholderTopDoctorBinding):
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context=parent.context
        val binding=
            ViewholderTopDoctorBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            val doctor = items[position]

            // Safely bind text fields with null checks
            holder.binding.nameTxt.text = doctor.name.takeIf { it.isNotBlank() } ?: "Unknown Doctor"
            holder.binding.specialTxt.text = doctor.specialization.takeIf { it.isNotBlank() } ?: "General Practice"

            // Safely get doctor image with proper fallback chain
            var imageUrl = ""
            when {
                doctor.image.isNotBlank() -> imageUrl = doctor.image
                doctor.chambers.isNotEmpty() && doctor.chambers[0].image.isNotBlank() -> imageUrl = doctor.chambers[0].image
                else -> imageUrl = "" // Will use placeholder
            }

            // Convert Imgur URL to direct image URL if needed
            if (imageUrl.isNotBlank() && imageUrl.contains("imgur.com") && !imageUrl.contains("i.imgur.com")) {
                val imageId = imageUrl.substringAfterLast("/")
                imageUrl = "https://i.imgur.com/$imageId.jpg"
            }

            // Load image with better error handling
            Glide.with(holder.itemView.context)
                .load(imageUrl.takeIf { it.isNotBlank() })
                .transform(CenterCrop(), RoundedCorners(16))
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(holder.binding.imageview6)

            holder.itemView.setOnClickListener {
                val intent = Intent(context, DetailActivity::class.java)
                intent.putExtra("Object", doctor)
                context?.startActivity(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("TopDoctorAdapter", "Error binding doctor at position $position: ${e.message}")
            // Set fallback values to prevent blank items
            holder.binding.nameTxt.text = "Error Loading Doctor"
            holder.binding.specialTxt.text = "Please try again"
        }
    }

    override fun getItemCount(): Int = items.size

}