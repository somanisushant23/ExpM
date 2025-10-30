package com.example.expm.activity

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expm.R
import com.example.expm.adapter.CategoryTotalAdapter
import com.example.expm.adapter.MonthlyTrendAdapter
import com.example.expm.viewmodel.AnalyticsViewModel
import java.util.Locale

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var viewModel: AnalyticsViewModel
    private lateinit var expenseAdapter: CategoryTotalAdapter
    private lateinit var monthlyAdapter: MonthlyTrendAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        // Enable back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.nav_analytics)

        // Initialize views
        val tvTotalExpenses = findViewById<TextView>(R.id.tv_total_expenses)
        val tvTotalIncome = findViewById<TextView>(R.id.tv_total_income)
        val tvNetBalance = findViewById<TextView>(R.id.tv_net_balance)
        val tvNoExpenses = findViewById<TextView>(R.id.tv_no_expenses)

        // Setup RecyclerViews
        val recyclerExpenseCategories = findViewById<RecyclerView>(R.id.recycler_expense_categories)
        expenseAdapter = CategoryTotalAdapter()
        recyclerExpenseCategories.layoutManager = LinearLayoutManager(this)
        recyclerExpenseCategories.adapter = expenseAdapter


        val recyclerMonthlyTrends = findViewById<RecyclerView>(R.id.recycler_monthly_trends)
        monthlyAdapter = MonthlyTrendAdapter()
        recyclerMonthlyTrends.layoutManager = LinearLayoutManager(this)
        recyclerMonthlyTrends.adapter = monthlyAdapter

        // Initialize ViewModel
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(AnalyticsViewModel::class.java)

        // Observe data
        viewModel.entriesForCurrentMonth.observe(this) { entries ->
            val totalExpenses = entries.filter { it.type == "Expense" }.sumOf { it.amount }
            val totalIncome = entries.filter { it.type == "Income" }.sumOf { it.amount }
            val netBalance = totalIncome - totalExpenses

            tvTotalExpenses.text = String.format(Locale.getDefault(), "Rs %.0f", totalExpenses)
            tvTotalIncome.text = String.format(Locale.getDefault(), "Rs %.0f", totalIncome)
            tvNetBalance.text = String.format(Locale.getDefault(), "Rs %.0f", netBalance)

            // Color code net balance
            if (netBalance >= 0) {
                tvNetBalance.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            } else {
                tvNetBalance.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            }
        }

        viewModel.expenseByCategoryForCurrentMonth.observe(this) { categories ->
            if (categories.isEmpty()) {
                tvNoExpenses.visibility = View.VISIBLE
                recyclerExpenseCategories.visibility = View.GONE
            } else {
                tvNoExpenses.visibility = View.GONE
                recyclerExpenseCategories.visibility = View.VISIBLE
                expenseAdapter.submitList(categories)
            }
        }


        viewModel.monthlyTrends.observe(this) { trends ->
            monthlyAdapter.submitList(trends)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

