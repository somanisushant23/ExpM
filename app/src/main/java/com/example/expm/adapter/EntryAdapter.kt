package com.example.expm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.expm.R
import com.example.expm.data.Entry
import java.text.NumberFormat
import java.util.Locale

class EntryAdapter(private val onItemClick: (Entry) -> Unit) : ListAdapter<Entry, EntryAdapter.EntryViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_entry, parent, false)
        return EntryViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EntryViewHolder(itemView: View, private val onItemClick: (Entry) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_category)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val tvType: TextView = itemView.findViewById(R.id.tv_type)

        fun bind(entry: Entry) {
            tvTitle.text = entry.title
            val nf = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            nf.maximumFractionDigits = 0
            try {
                tvAmount.text = nf.format(entry.amount)
            } catch (_: Exception) {
                tvAmount.text = entry.amount.toString()
            }
            tvCategory.text = entry.category
            tvDate.text = entry.date
            tvType.text = entry.type

            itemView.setOnClickListener { onItemClick(entry) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Entry>() {
        override fun areItemsTheSame(oldItem: Entry, newItem: Entry): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Entry, newItem: Entry): Boolean = oldItem == newItem
    }
}
