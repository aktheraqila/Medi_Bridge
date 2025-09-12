package com.aas.medi_bridge.Adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aas.medi_bridge.Domain.CategoryModel
import com.aas.medi_bridge.databinding.ViewholderCategoryBinding
import android.content.Context
import android.view.LayoutInflater
import com.bumptech.glide.Glide

class CategoryAdapter(
    val items: MutableList<CategoryModel>,
    private val onItemClick: ((CategoryModel) -> Unit)? = null
):
    RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {
    private lateinit var context: Context
    inner class ViewHolder(val binding: ViewholderCategoryBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoryAdapter.ViewHolder {
        context=parent.context
        val binding= ViewholderCategoryBinding.inflate(LayoutInflater.from(context),parent,false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryAdapter.ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.titleTxt.text = item.name

        // Convert Imgur URL to direct image URL
        val imageUrl = if (item.picture.contains("imgur.com") && !item.picture.contains("i.imgur.com")) {
            // Convert imgur.com/abc to i.imgur.com/abc.jpg
            val imageId = item.picture.substringAfterLast("/")
            "https://i.imgur.com/$imageId.jpg"
        } else {
            item.picture
        }
        Glide.with(context)
            .load(imageUrl)
            .into(holder.binding.img)

        holder.binding.root.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int {
        return items.size // Return the size of the items list
    }

}