package com.aas.medi_bridge.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aas.medi_bridge.R
import com.aas.medi_bridge.databinding.ItemTimeChipBinding

data class TimeSlot(
    val time: String,
    val isBooked: Boolean = false
)

class TimeChipAdapter(
    private val timeSlots: List<TimeSlot>,
    private val onTimeSelected: (String) -> Unit
) : RecyclerView.Adapter<TimeChipAdapter.TimeViewHolder>() {

    private var selectedPosition = -1

    inner class TimeViewHolder(private val binding: ItemTimeChipBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(timeSlot: TimeSlot, position: Int) {
            binding.timeText.text = timeSlot.time

            val context = binding.root.context

            // Set visual state based on booking status
            when {
                timeSlot.isBooked -> {
                    // Booked slot - dark red background with white text
                    binding.timeText.background = ContextCompat.getDrawable(context, R.drawable.time_slot_booked)
                    binding.timeText.setTextColor(ContextCompat.getColor(context, R.color.white))
                    binding.root.isClickable = false
                    binding.root.isFocusable = false
                    binding.root.isEnabled = false
                }
                position == selectedPosition -> {
                    // Selected available slot - purple background with white text
                    binding.timeText.background = ContextCompat.getDrawable(context, R.drawable.time_slot_selected)
                    binding.timeText.setTextColor(ContextCompat.getColor(context, R.color.white))
                    binding.root.isEnabled = true
                }
                else -> {
                    // Available unselected slot - white background with black text
                    binding.timeText.background = ContextCompat.getDrawable(context, R.drawable.time_slot_available)
                    binding.timeText.setTextColor(ContextCompat.getColor(context, R.color.black))
                    binding.root.isEnabled = true
                }
            }

            // Set click listener only for available slots
            if (!timeSlot.isBooked) {
                binding.root.setOnClickListener {
                    val previousPosition = selectedPosition
                    selectedPosition = position

                    // Refresh the previously selected item
                    if (previousPosition != -1) {
                        notifyItemChanged(previousPosition)
                    }
                    // Refresh the newly selected item
                    notifyItemChanged(selectedPosition)

                    onTimeSelected(timeSlot.time)
                }
                binding.root.isClickable = true
                binding.root.isFocusable = true
            } else {
                binding.root.setOnClickListener(null)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeViewHolder {
        val binding = ItemTimeChipBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TimeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimeViewHolder, position: Int) {
        holder.bind(timeSlots[position], position)
    }

    override fun getItemCount(): Int = timeSlots.size

    // Method to update booking status
    fun updateBookingStatus(bookedTimes: Set<String>) {
        timeSlots.forEachIndexed { index, timeSlot ->
            if (bookedTimes.contains(timeSlot.time)) {
                (timeSlots as MutableList)[index] = timeSlot.copy(isBooked = true)
            }
        }
        notifyDataSetChanged()
    }
}