package com.example.expm.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.expm.R
import com.example.expm.data.Investment
import com.example.expm.utils.AppUtils
import java.text.NumberFormat
import java.util.Locale

class InvestmentAdapter(private val onItemClick: (Investment) -> Unit) : ListAdapter<Investment, InvestmentAdapter.InvestmentViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvestmentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_investment, parent, false)
        return InvestmentViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: InvestmentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class InvestmentViewHolder(itemView: View, private val onItemClick: (Investment) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)
        private val tvType: TextView = itemView.findViewById(R.id.tv_type)
        private val tvMaturityDate: TextView = itemView.findViewById(R.id.tv_maturity_date)

        fun bind(investment: Investment) {
            tvTitle.text = investment.title
            val nf = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            nf.maximumFractionDigits = 0
            try {
                tvAmount.text = nf.format(investment.amount)
            } catch (_: Exception) {
                tvAmount.text = investment.amount.toString()
            }
            tvType.text = investment.type
            tvMaturityDate.text = "Maturity: ${AppUtils.formatTimestampToDate(investment.maturityDateTimestamp)}"

            itemView.setOnClickListener { onItemClick(investment) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Investment>() {
        override fun areItemsTheSame(oldItem: Investment, newItem: Investment): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Investment, newItem: Investment): Boolean = oldItem == newItem
    }
}

