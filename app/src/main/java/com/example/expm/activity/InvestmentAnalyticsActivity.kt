package com.example.expm.activity

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expm.R
import com.example.expm.adapter.InvestmentTypeAdapter
import com.example.expm.adapter.MaturityTimelineAdapter
import com.example.expm.network.models.InvestmentResponse
import com.example.expm.network.Resource
import com.example.expm.viewmodel.InvestmentsViewModel
import com.example.expm.utils.CurrencyFormatter
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InvestmentAnalyticsActivity : BaseActivity() {

    private lateinit var viewModel: InvestmentsViewModel
    private lateinit var investmentTypeAdapter: InvestmentTypeAdapter
    private lateinit var maturityTimelineAdapter: MaturityTimelineAdapter
    private lateinit var barChart: HorizontalBarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_investment_analytics)

        // Enable back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.investment_analytics)

        // Initialize views
        val tvTotalInvested = findViewById<TextView>(R.id.tv_total_invested)
        val tvNoInvestmentsType = findViewById<TextView>(R.id.tv_no_investments_type)
        val tvNoMaturityData = findViewById<TextView>(R.id.tv_no_maturity_data)

        // Setup RecyclerViews
        val recyclerInvestmentByType = findViewById<RecyclerView>(R.id.recycler_investment_by_type)
        investmentTypeAdapter = InvestmentTypeAdapter()
        recyclerInvestmentByType.layoutManager = LinearLayoutManager(this)
        recyclerInvestmentByType.adapter = investmentTypeAdapter

        // Setup BarChart
        barChart = findViewById(R.id.pie_chart_investment_type)
        setupBarChart()

        val recyclerMaturityTimeline = findViewById<RecyclerView>(R.id.recycler_maturity_timeline)
        maturityTimelineAdapter = MaturityTimelineAdapter()
        recyclerMaturityTimeline.layoutManager = LinearLayoutManager(this)
        recyclerMaturityTimeline.adapter = maturityTimelineAdapter

        // Initialize ViewModel
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(InvestmentsViewModel::class.java)

        // Observe investments data
        viewModel.investmentsState.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    val investments = resource.data ?: emptyList()
                    updateAnalytics(
                        investments,
                        tvTotalInvested,
                        tvNoInvestmentsType,
                        tvNoMaturityData
                    )
                }
                is Resource.Error -> {
                    // Show error state - set all values to zero
                    tvTotalInvested.text = getString(R.string.rs_zero)
                    tvNoInvestmentsType.visibility = View.VISIBLE
                    tvNoMaturityData.visibility = View.VISIBLE
                    recyclerInvestmentByType.visibility = View.GONE
                    recyclerMaturityTimeline.visibility = View.GONE
                }
                else -> {
                    // Loading or null state - do nothing
                }
            }
        }

        // Fetch investments
        viewModel.getInvestments()
    }

    private fun updateAnalytics(
        investments: List<InvestmentResponse>,
        tvTotalInvested: TextView,
        tvNoInvestmentsType: TextView,
        tvNoMaturityData: TextView
    ) {
        if (investments.isEmpty()) {
            tvTotalInvested.text = getString(R.string.rs_zero)
            tvNoInvestmentsType.visibility = View.VISIBLE
            tvNoMaturityData.visibility = View.VISIBLE
            barChart.visibility = View.GONE
            findViewById<RecyclerView>(R.id.recycler_investment_by_type).visibility = View.GONE
            findViewById<RecyclerView>(R.id.recycler_maturity_timeline).visibility = View.GONE
            return
        }

        // Calculate total invested
        val totalInvested = investments.sumOf {
            it.amount.replace(",", "").toDoubleOrNull() ?: 0.0
        }

        // Update UI
        tvTotalInvested.text = "Rs ${CurrencyFormatter.formatIndianCurrency(totalInvested)}"

        // Group investments by type
        val investmentsByType = investments.groupBy { it.investmentType.name }
            .map { (type, typeInvestments) ->
                val total = typeInvestments.sumOf {
                    it.amount.replace(",", "").toDoubleOrNull() ?: 0.0
                }
                val displayName = when (type) {
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
                Pair(displayName, total)
            }
            .sortedByDescending { it.second }

        if (investmentsByType.isEmpty()) {
            tvNoInvestmentsType.visibility = View.VISIBLE
            barChart.visibility = View.GONE
            findViewById<RecyclerView>(R.id.recycler_investment_by_type).visibility = View.GONE
        } else {
            tvNoInvestmentsType.visibility = View.GONE
            barChart.visibility = View.VISIBLE
            findViewById<RecyclerView>(R.id.recycler_investment_by_type).visibility = View.VISIBLE
            investmentTypeAdapter.submitList(investmentsByType)
            updateBarChart(investmentsByType)
        }

        // Get maturity timeline (investments maturing in next 6 months)
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val currentTime = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 6)
        val next6MonthsTime = calendar.timeInMillis

        val maturingInvestments = investments.mapNotNull { investment ->
            try {
                val maturityDate = dateFormat.parse(investment.maturityDate)
                maturityDate?.let {
                    if (it.time in currentTime..next6MonthsTime) {
                        Triple(
                            investment.description ?: "Investment",
                            investment.maturityDate,
                            investment.amount.replace(",", "").toDoubleOrNull() ?: 0.0
                        )
                    } else null
                }
            } catch (_: Exception) {
                null
            }
        }.sortedBy {
            try {
                dateFormat.parse(it.second)?.time ?: Long.MAX_VALUE
            } catch (_: Exception) {
                Long.MAX_VALUE
            }
        }

        if (maturingInvestments.isEmpty()) {
            tvNoMaturityData.visibility = View.VISIBLE
            findViewById<RecyclerView>(R.id.recycler_maturity_timeline).visibility = View.GONE
        } else {
            tvNoMaturityData.visibility = View.GONE
            findViewById<RecyclerView>(R.id.recycler_maturity_timeline).visibility = View.VISIBLE
            maturityTimelineAdapter.submitList(maturingInvestments)
        }
    }

    private fun setupBarChart() {
        barChart.apply {
            description.isEnabled = false
            //setMaxVisibleValueCount(8)
            setDrawGridBackground(false)
            setDrawBarShadow(false)

            // X-axis configuration
            xAxis.apply {
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                isGranularityEnabled = true
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return ""
                    }
                }
            }

            // Y-axis (left) configuration
            axisLeft.apply {
                axisMinimum = 0f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return ""
                    }
                }
            }

            // Right Y-axis disabled
            axisRight.isEnabled = false

            // Legend configuration
            legend.apply {
                isEnabled = true
                textSize = 12f
                form = com.github.mikephil.charting.components.Legend.LegendForm.SQUARE
            }

            setTouchEnabled(true)
            isHighlightPerTapEnabled = true
            animateY(1000)
        }
    }

    private fun updateBarChart(investmentsByType: List<Pair<String, Double>>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        // Calculate total for percentage
        val total = investmentsByType.sumOf { it.second }

        // Create bar entries with percentages
        investmentsByType.forEachIndexed { index, (type, amount) ->
            val percentage = if (total > 0) (amount / total * 100).toFloat() else 0f
            entries.add(BarEntry(index.toFloat(), percentage))
            labels.add(type)
        }

        // Create dataset
        val dataSet = BarDataSet(entries, "Investment Distribution %").apply {
            colors = listOf(
                Color.rgb(33, 150, 243),   // Blue
                Color.rgb(76, 175, 80),    // Green
                Color.rgb(255, 152, 0),    // Orange
                Color.rgb(156, 39, 176),   // Purple
                Color.rgb(244, 67, 54),    // Red
                Color.rgb(0, 188, 212),    // Cyan
                Color.rgb(255, 235, 59),   // Yellow
                Color.rgb(121, 85, 72),    // Brown
                Color.rgb(96, 125, 139)    // Blue Grey
            ).let {
                (0 until investmentsByType.size).map { idx -> it[idx % it.size] }
            }
            valueTextSize = 11f
            valueTextColor = Color.BLACK
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format("%.1f%%", value)
                }
            }
        }

        val data = BarData(dataSet).apply {
            barWidth = 0.6f
        }

        barChart.apply {
            this.data = data
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return if (index >= 0 && index < labels.size) labels[index] else ""
                }
            }
            xAxis.labelCount = labels.size
            invalidate()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

