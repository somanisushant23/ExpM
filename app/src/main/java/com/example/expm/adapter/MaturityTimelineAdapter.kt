package com.example.expm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.expm.R
import java.util.Locale

class MaturityTimelineAdapter : ListAdapter<Triple<String, String, Double>, MaturityTimelineAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_maturity_timeline, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvInvestmentName: TextView = itemView.findViewById(R.id.tv_investment_name)
        private val tvMaturityDate: TextView = itemView.findViewById(R.id.tv_maturity_date)
        private val tvMaturityAmount: TextView = itemView.findViewById(R.id.tv_maturity_amount)

        fun bind(maturityItem: Triple<String, String, Double>) {
            tvInvestmentName.text = maturityItem.first
            tvMaturityDate.text = maturityItem.second
            tvMaturityAmount.text = String.format(Locale.getDefault(), "Rs %.2f", maturityItem.third)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Triple<String, String, Double>>() {
        override fun areItemsTheSame(oldItem: Triple<String, String, Double>, newItem: Triple<String, String, Double>): Boolean =
            oldItem.first == newItem.first && oldItem.second == newItem.second

        override fun areContentsTheSame(oldItem: Triple<String, String, Double>, newItem: Triple<String, String, Double>): Boolean =
            oldItem == newItem
    }
}

