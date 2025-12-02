package com.example.expm.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
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
        adapter = InvestmentAdapter { investment ->
            // Handle investment item click - open AddInvestmentActivity in edit mode
            val intent = Intent(this, AddInvestmentActivity::class.java).apply {
                putExtra("INVESTMENT_ID", investment.id)
                putExtra("INVESTMENT_TITLE", investment.title)
                putExtra("INVESTMENT_AMOUNT", investment.amount)
                putExtra("INVESTMENT_TYPE", investment.type)
                putExtra("INVESTMENT_RETURN_RATE", investment.returnRate)
                putExtra("INVESTMENT_PRINCIPAL_DATE", investment.principalDateTimestamp)
                putExtra("INVESTMENT_MATURITY_DATE", investment.maturityDateTimestamp)
                putExtra("INVESTMENT_NOTES", investment.notes)
                putExtra("INVESTMENT_CLIENT_ID", investment.clientId)
                putExtra("INVESTMENT_REMOTE_ID", investment.remoteId)
            }
            startActivity(intent)
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_investments, menu)

        // Setup SearchView
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView

        searchView?.apply {
            queryHint = getString(R.string.search_hint)

            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    viewModel.setSearchQuery(query ?: "")
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel.setSearchQuery(newText ?: "")
                    return true
                }
            })

            // Clear search when SearchView is collapsed
            setOnCloseListener {
                viewModel.setSearchQuery("")
                false
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sort_date_desc -> {
                viewModel.setSortOrder("date_desc")
                Toast.makeText(this, "Sorted by Date (Newest First)", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.sort_date_asc -> {
                viewModel.setSortOrder("date_asc")
                Toast.makeText(this, "Sorted by Date (Oldest First)", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.sort_amount_desc -> {
                viewModel.setSortOrder("amount_desc")
                Toast.makeText(this, "Sorted by Amount (Highest First)", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.sort_amount_asc -> {
                viewModel.setSortOrder("amount_asc")
                Toast.makeText(this, "Sorted by Amount (Lowest First)", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

