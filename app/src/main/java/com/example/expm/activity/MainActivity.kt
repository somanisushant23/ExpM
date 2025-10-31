package com.example.expm.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expm.R
import com.example.expm.adapter.EntryAdapter
import com.example.expm.viewmodel.MainViewModel
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var adapter: EntryAdapter
    private lateinit var viewModel: MainViewModel
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.nav_transactions)

        // Setup Navigation Drawer
        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        // Find header's version TextView (we'll update it when drawer opens)
        val headerView = navigationView.getHeaderView(0)
        val tvVersion = headerView.findViewById<TextView>(R.id.tv_version)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)

        // Update version when drawer opens (keeps behavior consistent)
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: android.view.View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {}
            override fun onDrawerClosed(drawerView: android.view.View) {}
            override fun onDrawerOpened(drawerView: android.view.View) {
                // Use BuildConfig which reflects versionName from build.gradle at build time

            }
        })

        toggle.syncState()

        // Set default selection
        navigationView.setCheckedItem(R.id.nav_transactions)

        // Handle back button for drawer
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

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
        val chipGroup = findViewById<ChipGroup>(R.id.chip_group_filter)

        // Handle chip filter selection
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                // If no chip is selected, default to "All"
                viewModel.setFilterType("All")
            } else {
                when (checkedIds[0]) {
                    R.id.chip_all -> viewModel.setFilterType("All")
                    R.id.chip_expenses -> viewModel.setFilterType("Expense")
                    R.id.chip_income -> viewModel.setFilterType("Income")
                }
            }
        }

        viewModel.entriesForCurrentMonth.observe(this) { filtered ->
            adapter.submitList(filtered)

            tvEmpty.visibility = if (filtered.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE

            // Calculate and display total amount based on current filter
            val currentFilter = when (chipGroup.checkedChipId) {
                R.id.chip_expenses -> "Expense"
                R.id.chip_income -> "Income"
                else -> "All"
            }

            when (currentFilter) {
                "Expense" -> {
                    val totalExpenses = filtered.sumOf { it.amount }
                    val text = String.format(java.util.Locale.getDefault(), "Total Expenses: Rs %.0f", totalExpenses)
                    val spannable = SpannableString(text)
                    spannable.setSpan(ForegroundColorSpan(Color.parseColor("#D32F2F")), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    tvTotalAmount.text = spannable
                }
                "Income" -> {
                    val totalIncome = filtered.sumOf { it.amount }
                    val text = String.format(java.util.Locale.getDefault(), "Total Income: Rs %.0f", totalIncome)
                    val spannable = SpannableString(text)
                    spannable.setSpan(ForegroundColorSpan(Color.parseColor("#388E3C")), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    tvTotalAmount.text = spannable
                }
                else -> {
                    val totalExpenses = filtered.filter { it.type == "Expense" }.sumOf { it.amount }
                    val totalIncome = filtered.filter { it.type == "Income" }.sumOf { it.amount }
                    val text = String.format(java.util.Locale.getDefault(), "Expenses: Rs %.0f | Income: Rs %.0f", totalExpenses, totalIncome)
                    val spannable = SpannableString(text)

                    // Color "Expenses: Rs X" in red
                    val expensesEnd = text.indexOf(" |")
                    if (expensesEnd != -1) {
                        spannable.setSpan(ForegroundColorSpan(Color.parseColor("#D32F2F")), 0, expensesEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }

                    // Color "Income: Rs X" in green
                    val incomeStart = text.indexOf("Income:")
                    if (incomeStart != -1) {
                        spannable.setSpan(ForegroundColorSpan(Color.parseColor("#388E3C")), incomeStart, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }

                    tvTotalAmount.text = spannable
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_transactions -> {
                supportActionBar?.title = getString(R.string.nav_transactions)
                Toast.makeText(this, "Transactions", Toast.LENGTH_SHORT).show()
                // Already on this screen
            }
            R.id.nav_analytics -> {
                val intent = Intent(this, AnalyticsActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_settings -> {
                supportActionBar?.title = getString(R.string.nav_settings)
                Toast.makeText(this, "Settings - Coming Soon!", Toast.LENGTH_SHORT).show()
                // TODO: Navigate to Settings Activity
            }
            R.id.nav_about -> {
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}
