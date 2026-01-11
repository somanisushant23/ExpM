package com.example.expm.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.expm.R
import com.example.expm.adapter.InvestmentsAdapter
import com.example.expm.adapter.InvestmentGroupedItem
import com.example.expm.network.Resource
import com.example.expm.network.models.InvestmentResponse
import com.example.expm.viewmodel.InvestmentsViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Locale

class InvestmentsActivity : BaseActivity() {

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
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        val btnViewAnalytics = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_view_analytics)

        // Analytics button click listener
        btnViewAnalytics.setOnClickListener {
            val intent = Intent(this, InvestmentAnalyticsActivity::class.java)
            startActivity(intent)
        }

        // Setup SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener {
            viewModel.getInvestments()
        }

        // Setup RecyclerView
        investmentsAdapter = InvestmentsAdapter { investment ->
            // Open AddInvestmentActivity with investment details
            val intent = Intent(this, AddInvestmentActivity::class.java).apply {
                putExtra("INVESTMENT_ID", investment.id)
                putExtra("INVESTMENT_AMOUNT", investment.amount.replace(",", "").toIntOrNull() ?: 0)
                putExtra("INVESTMENT_RETURN_RATE", investment.expectedReturnRate)
                putExtra("INVESTMENT_ACCOUNT_NUMBER", investment.investmentAccountNumber)
                putExtra("INVESTMENT_NOTES", investment.description)
                putExtra("INVESTMENT_CLIENT_ID", investment.clientId)
                putExtra("INVESTMENT_INSTUTION_NAME", investment.institutionName)

                // Map InvestmentType enum to display name
                val investmentTypeDisplay = when (investment.investmentType.name) {
                    "FIXED_DEPOSIT" -> "Fixed Deposit"
                    "RECURRING_DEPOSIT" -> "Recurring Deposit"
                    "EPF" -> "EPF"
                    "PPF" -> "PPF"
                    "SSY" -> "SSY"
                    "MUTUAL_FUNDS" -> "Mutual Fund"
                    "SHARES" -> "Stocks"
                    "GOLD" -> "Gold"
                    else -> "Others"
                }
                putExtra("INVESTMENT_TYPE", investmentTypeDisplay)

                // Parse and pass creation date timestamp
                try {
                    val dateFormat = java.text.SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    val creationDate = dateFormat.parse(investment.creationDate)
                    creationDate?.let {
                        putExtra("INVESTMENT_PRINCIPAL_DATE", it.time)
                    }
                } catch (e: Exception) {
                    // Use transactionDateTimestamp as fallback
                    putExtra("INVESTMENT_PRINCIPAL_DATE", investment.transactionDateTimestamp)
                }

                // Parse and pass maturity date timestamp
                try {
                    val dateFormat = java.text.SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    val maturityDate = dateFormat.parse(investment.maturityDate)
                    maturityDate?.let {
                        putExtra("INVESTMENT_MATURITY_DATE", it.time)
                    }
                } catch (e: Exception) {
                    // No maturity date set
                }
            }
            startActivity(intent)
        }
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
                    // Show loading state (but don't hide views if refreshing)
                    if (!swipeRefresh.isRefreshing) {
                        tvNoInvestments.visibility = View.GONE
                        recyclerInvestments.visibility = View.GONE
                    }
                }
                is Resource.Success -> {
                    // Stop refresh animation
                    swipeRefresh.isRefreshing = false

                    val investments = resource.data
                    if (investments.isNullOrEmpty()) {
                        // Show empty state
                        tvNoInvestments.visibility = View.VISIBLE
                        recyclerInvestments.visibility = View.GONE
                    } else {
                        // Group and sort investments by type
                        val groupedItems = groupAndSortInvestments(investments)

                        // Show data
                        tvNoInvestments.visibility = View.GONE
                        recyclerInvestments.visibility = View.VISIBLE
                        investmentsAdapter.submitList(groupedItems)
                    }
                }
                is Resource.Error -> {
                    // Stop refresh animation
                    swipeRefresh.isRefreshing = false

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
                    swipeRefresh.isRefreshing = false
                }
            }
        }

        // Fetch investments
        viewModel.getInvestments()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_investments, menu)

        // Setup SearchView
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView

        searchView?.apply {
            queryHint = "Search investments"

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

    override fun onResume() {
        super.onResume()
        // Refresh investments when returning to this activity
        viewModel.getInvestments()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun groupAndSortInvestments(investments: List<InvestmentResponse>): List<InvestmentGroupedItem> {
        // Group investments by their type name
        val groupedMap = investments.groupBy { it.investmentType.name }

        // Sort groups alphabetically by investment type name
        val sortedGroups = groupedMap.toSortedMap()

        // Build the result list with headers and items
        val result = mutableListOf<InvestmentGroupedItem>()

        for ((investmentType, investmentsList) in sortedGroups) {
            // Add header for this investment type
            result.add(InvestmentGroupedItem.Header(investmentType))

            // Add all investments of this type
            for (investment in investmentsList) {
                result.add(InvestmentGroupedItem.Item(investment))
            }
        }

        return result
    }
}
