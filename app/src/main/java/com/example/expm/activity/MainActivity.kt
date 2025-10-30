package com.example.expm.activity

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expm.R
import com.example.expm.adapter.EntryAdapter
import com.example.expm.viewmodel.MainViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: EntryAdapter
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fab = findViewById<FloatingActionButton>(R.id.fab_add)
        fab.setOnClickListener {
            startActivity(Intent(this, AddEntryActivity::class.java))
        }

        // RecyclerView setup with item click -> open AddEntryActivity in edit mode
        val recycler = findViewById<RecyclerView>(R.id.recycler_entries)
        adapter = EntryAdapter { entry ->
            val intent = Intent(this, AddEntryActivity::class.java).apply {
                putExtra("entry_id", entry.id)
            }
            startActivity(intent)
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // Obtain ViewModel and observe entries for current month
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(MainViewModel::class.java)


        val tvEmpty = findViewById<TextView>(R.id.tv_empty)
        val tvTotalAmount = findViewById<TextView>(R.id.tv_total_amount)

        viewModel.entriesForCurrentMonth.observe(this) { filtered ->
            adapter.submitList(filtered)

            tvEmpty.visibility = if (filtered.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE

            // Calculate and display total amount for expenses only
            val totalExpenses = filtered.filter { it.type == "Expense" }.sumOf { it.amount }
            tvTotalAmount.text = String.format(java.util.Locale.getDefault(), "Total Expenses: Rs %.0f", totalExpenses)
        }
    }
}
