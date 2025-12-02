package com.example.expm.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expm.R
import com.example.expm.adapter.InvestmentAdapter
import com.example.expm.viewmodel.InvestmentsViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Locale

class InvestmentsActivity : AppCompatActivity() {

    private lateinit var viewModel: InvestmentsViewModel
    private lateinit var adapter: InvestmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_investments)

        // Setup Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Enable back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.nav_investments)

        // Initialize views
        val tvTotalInvested = findViewById<TextView>(R.id.tv_total_invested)
        val tvNoInvestments = findViewById<TextView>(R.id.tv_no_investments)
        val fabAddInvestment = findViewById<FloatingActionButton>(R.id.fab_add_investment)
        val recyclerInvestments = findViewById<RecyclerView>(R.id.recycler_investments)

        // Setup RecyclerView
        adapter = InvestmentAdapter { _ ->
            // Handle investment item click - for now, you could open AddInvestmentActivity in edit mode
            // TODO: Implement edit functionality if needed
        }
        recyclerInvestments.layoutManager = LinearLayoutManager(this)
        recyclerInvestments.adapter = adapter

        // FAB click listener
        fabAddInvestment.setOnClickListener {
            val intent = Intent(this, AddInvestmentActivity::class.java)
            startActivity(intent)
        }

        // Initialize ViewModel
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(InvestmentsViewModel::class.java)

        // Observe data
        viewModel.investmentEntries.observe(this) { entries ->
            val totalInvested = entries.sumOf { it.amount }

            // For now, we'll show the total invested amount
            // In a real app, you would calculate returns based on your business logic
            tvTotalInvested.text = String.format(Locale.getDefault(), "Rs %d", totalInvested)

            // Update adapter with investments
            adapter.submitList(entries)

            // Show/hide empty message and RecyclerView
            if (entries.isEmpty()) {
                tvNoInvestments.visibility = View.VISIBLE
                recyclerInvestments.visibility = View.GONE
            } else {
                tvNoInvestments.visibility = View.GONE
                recyclerInvestments.visibility = View.VISIBLE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

