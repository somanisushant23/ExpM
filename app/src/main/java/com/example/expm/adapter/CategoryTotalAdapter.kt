package com.example.expm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.expm.R
import com.example.expm.viewmodel.CategoryTotal
import java.util.Locale

class CategoryTotalAdapter : ListAdapter<CategoryTotal, CategoryTotalAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category_total, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategoryName: TextView = itemView.findViewById(R.id.tv_category_name)
        private val tvCategoryAmount: TextView = itemView.findViewById(R.id.tv_category_amount)

        fun bind(categoryTotal: CategoryTotal) {
            tvCategoryName.text = categoryTotal.category
            tvCategoryAmount.text = String.format(Locale.getDefault(), "Rs %.0f", categoryTotal.total)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CategoryTotal>() {
        override fun areItemsTheSame(oldItem: CategoryTotal, newItem: CategoryTotal): Boolean =
            oldItem.category.equals(newItem.category, true)
        override fun areContentsTheSame(oldItem: CategoryTotal, newItem: CategoryTotal): Boolean =
            oldItem == newItem
    }
}

