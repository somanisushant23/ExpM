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
) : ListAdapter<InvestmentGroupedItem, RecyclerView.ViewHolder>(InvestmentGroupedDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is InvestmentGroupedItem.Header -> VIEW_TYPE_HEADER
            is InvestmentGroupedItem.Item -> VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_investment_header, parent, false)
                InvestmentHeaderViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_investment, parent, false)
                InvestmentViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (item) {
            is InvestmentGroupedItem.Header -> {
                (holder as InvestmentHeaderViewHolder).bind(item.investmentType)
            }
            is InvestmentGroupedItem.Item -> {
                val viewHolder = holder as InvestmentViewHolder
                viewHolder.bind(item.investment)
                viewHolder.itemView.setOnClickListener {
                    onItemClick(item.investment)
                }
            }
        }
    }

    class InvestmentHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSectionHeader: TextView = itemView.findViewById(R.id.tv_section_header)

        fun bind(investmentType: String) {
            tvSectionHeader.text = investmentType
        }
    }

    class InvestmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_investment_description)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_investment_amount)
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_investment_category)
        private val tvType: TextView = itemView.findViewById(R.id.tv_investment_type)
        private val viewTypeIndicator: View = itemView.findViewById(R.id.view_type_indicator)

        fun bind(investment: InvestmentResponse) {
            //tvDescription.text = investment.investmentType.name
            tvDescription.text = investment.institutionName

            // Format amount
            try {
                val amount = investment.amount
                tvAmount.text = amount
            } catch (_: Exception) {
                tvAmount.text = itemView.context.getString(R.string.rs_zero)
            }

            // Set category
            tvCategory.text = "Maturity:" + investment.maturityDate

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

    class InvestmentGroupedDiffCallback : DiffUtil.ItemCallback<InvestmentGroupedItem>() {
        override fun areItemsTheSame(oldItem: InvestmentGroupedItem, newItem: InvestmentGroupedItem): Boolean {
            return when {
                oldItem is InvestmentGroupedItem.Header && newItem is InvestmentGroupedItem.Header ->
                    oldItem.investmentType == newItem.investmentType
                oldItem is InvestmentGroupedItem.Item && newItem is InvestmentGroupedItem.Item ->
                    oldItem.investment.id == newItem.investment.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: InvestmentGroupedItem, newItem: InvestmentGroupedItem): Boolean {
            return oldItem == newItem
        }
    }
}

