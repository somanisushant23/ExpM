package com.example.expm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.expm.R
import com.example.expm.network.models.InvestmentResponse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InvestmentsAdapter(
    private val onItemClick: (InvestmentResponse) -> Unit = {}
) : ListAdapter<InvestmentResponse, InvestmentsAdapter.InvestmentViewHolder>(InvestmentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvestmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_investment, parent, false)
        return InvestmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: InvestmentViewHolder, position: Int) {
        val investment = getItem(position)
        holder.bind(investment)
        holder.itemView.setOnClickListener {
            onItemClick(investment)
        }
    }

    class InvestmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_investment_description)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_investment_amount)
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_investment_category)
        private val tvType: TextView = itemView.findViewById(R.id.tv_investment_type)
        private val viewTypeIndicator: View = itemView.findViewById(R.id.view_type_indicator)

        fun bind(investment: InvestmentResponse) {
            tvDescription.text = investment.investmentType.name

            // Format amount
            try {
                val amount = investment.amount
                tvAmount.text = amount
            } catch (_: Exception) {
                tvAmount.text = itemView.context.getString(R.string.rs_zero)
            }

            // Set category
            tvCategory.text = investment.creationDate

            // Set transaction type
            "@ ${investment.expectedReturnRate}".also { tvType.text = it }

            // Show indicator if maturity date is within 3 months
            viewTypeIndicator.visibility = if (isMaturityWithinThreeMonths(investment.maturityDate)) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        private fun isMaturityWithinThreeMonths(maturityDateStr: String): Boolean {
            return try {
                val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val maturityDate = dateFormat.parse(maturityDateStr) ?: return false

                val currentDate = Calendar.getInstance().time
                val threeMonthsFromNow = Calendar.getInstance().apply {
                    add(Calendar.MONTH, 3)
                }.time

                // Check if maturity date is between now and 3 months from now
                maturityDate.after(currentDate) && maturityDate.before(threeMonthsFromNow)
            } catch (_: Exception) {
                false
            }
        }
    }

    class InvestmentDiffCallback : DiffUtil.ItemCallback<InvestmentResponse>() {
        override fun areItemsTheSame(oldItem: InvestmentResponse, newItem: InvestmentResponse): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: InvestmentResponse, newItem: InvestmentResponse): Boolean {
            return oldItem == newItem
        }
    }
}

