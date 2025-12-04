package com.example.expm.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expm.R
import com.example.expm.adapter.InvestmentsAdapter
import com.example.expm.network.Resource
import com.example.expm.viewmodel.InvestmentsViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.NumberFormat
import java.util.Locale

class InvestmentsActivity : AppCompatActivity() {

    private lateinit var viewModel: InvestmentsViewModel
    private lateinit var investmentsAdapter: InvestmentsAdapter

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
        val tvNoInvestments = findViewById<TextView>(R.id.tv_no_investments)
        val fabAddInvestment = findViewById<FloatingActionButton>(R.id.fab_add_investment)
        val recyclerInvestments = findViewById<RecyclerView>(R.id.recycler_investments)

        // Setup RecyclerView
        investmentsAdapter = InvestmentsAdapter()
        recyclerInvestments.apply {
            layoutManager = LinearLayoutManager(this@InvestmentsActivity)
            adapter = investmentsAdapter
            setHasFixedSize(true)
        }

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

        // Observe investments data
        viewModel.investmentsState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Show loading state
                    tvNoInvestments.visibility = View.GONE
                    recyclerInvestments.visibility = View.GONE
                }
                is Resource.Success -> {
                    val investments = resource.data
                    if (investments.isNullOrEmpty()) {
                        // Show empty state
                        tvNoInvestments.visibility = View.VISIBLE
                        recyclerInvestments.visibility = View.GONE
                    } else {
                        // Show data
                        tvNoInvestments.visibility = View.GONE
                        recyclerInvestments.visibility = View.VISIBLE
                        investmentsAdapter.submitList(investments)
                    }
                }
                is Resource.Error -> {
                    // Show error message
                    tvNoInvestments.visibility = View.VISIBLE
                    recyclerInvestments.visibility = View.GONE
                    Toast.makeText(
                        this,
                        resource.message ?: "Failed to load investments",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {
                    // Handle null state
                }
            }
        }

        // Fetch investments
        viewModel.getInvestments()

    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

