package com.example.expm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.expm.R
import com.example.expm.viewmodel.MonthlyData
import java.util.Locale

class MonthlyTrendAdapter : ListAdapter<MonthlyData, MonthlyTrendAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_monthly_trend, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMonth: TextView = itemView.findViewById(R.id.tv_month)
        private val tvMonthExpense: TextView = itemView.findViewById(R.id.tv_month_expense)
        private val tvMonthIncome: TextView = itemView.findViewById(R.id.tv_month_income)

        fun bind(monthlyData: MonthlyData) {
            tvMonth.text = monthlyData.month
            tvMonthExpense.text = String.format(Locale.getDefault(), "Rs %.0f", monthlyData.totalExpense)
            tvMonthIncome.text = String.format(Locale.getDefault(), "Rs %.0f", monthlyData.totalIncome)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MonthlyData>() {
        override fun areItemsTheSame(oldItem: MonthlyData, newItem: MonthlyData): Boolean =
            oldItem.month == newItem.month
        override fun areContentsTheSame(oldItem: MonthlyData, newItem: MonthlyData): Boolean =
            oldItem == newItem
    }
}

