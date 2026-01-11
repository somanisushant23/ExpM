package com.example.expm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.expm.R
import com.example.expm.utils.CurrencyFormatter

class InvestmentTypeAdapter : ListAdapter<Pair<String, Double>, InvestmentTypeAdapter.ViewHolder>(DiffCallback()) {

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

        fun bind(investmentType: Pair<String, Double>) {
            tvCategoryName.text = investmentType.first
            tvCategoryAmount.text = "Rs ${CurrencyFormatter.formatIndianCurrency(investmentType.second)}"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Pair<String, Double>>() {
        override fun areItemsTheSame(oldItem: Pair<String, Double>, newItem: Pair<String, Double>): Boolean =
            oldItem.first == newItem.first

        override fun areContentsTheSame(oldItem: Pair<String, Double>, newItem: Pair<String, Double>): Boolean =
            oldItem == newItem
    }
}

