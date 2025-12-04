package com.example.expm.activity

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.expm.R
import com.example.expm.network.models.InvestmentType
import com.example.expm.viewmodel.AddInvestmentViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class AddInvestmentActivity : AppCompatActivity() {
    private lateinit var viewModel: AddInvestmentViewModel
    private var editingInvestmentId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_investment)

        // Check if editing existing investment
        val isEditing = intent.hasExtra("INVESTMENT_ID")

        // Enable back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(if (isEditing) R.string.edit_investment else R.string.add_investment)

        val tvScreenTitle = findViewById<TextView>(R.id.tv_screen_title)
        val etAmount = findViewById<EditText>(R.id.et_amount)
        val etReturnRate = findViewById<EditText>(R.id.et_return_rate)
        val etDate = findViewById<EditText>(R.id.et_date)
        val etMaturityDate = findViewById<EditText>(R.id.et_maturity_date)
        val etNotes = findViewById<EditText>(R.id.et_notes)
        val spinnerCategory = findViewById<Spinner>(R.id.spinner_category)
        val btnSave = findViewById<Button>(R.id.btn_save)
        val btnDelete = findViewById<Button>(R.id.btn_delete)

        // Set screen title dynamically
        tvScreenTitle.text = getString(if (isEditing) R.string.edit_investment else R.string.add_investment)

        // Show delete button only when editing
        if (isEditing) {
            btnDelete.visibility = android.view.View.VISIBLE
        }

        // Obtain ViewModel
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(AddInvestmentViewModel::class.java)

        // Setup date helpers
        val calendar = Calendar.getInstance()
        val maturityCalendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        etDate.setText(dateFormat.format(calendar.time))

        // Investment Date picker
        etDate.setOnClickListener {
            val y = calendar.get(Calendar.YEAR)
            val m = calendar.get(Calendar.MONTH)
            val d = calendar.get(Calendar.DAY_OF_MONTH)
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                etDate.setText(dateFormat.format(calendar.time))
            }, y, m, d).show()
        }

        // Maturity Date picker (Optional)
        etMaturityDate.setOnClickListener {
            val y = maturityCalendar.get(Calendar.YEAR)
            val m = maturityCalendar.get(Calendar.MONTH)
            val d = maturityCalendar.get(Calendar.DAY_OF_MONTH)
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                maturityCalendar.set(year, month, dayOfMonth)
                etMaturityDate.setText(dateFormat.format(maturityCalendar.time))
            }, y, m, d).show()
        }

        // Clear maturity date on long click
        etMaturityDate.setOnLongClickListener {
            etMaturityDate.setText("")
            Toast.makeText(this, "Maturity date cleared", Toast.LENGTH_SHORT).show()
            true
        }

        // Investment categories adapter
        ArrayAdapter.createFromResource(
            this,
            R.array.investment_categories,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategory.adapter = adapter
        }

        // If editing, populate fields with existing data
        if (isEditing) {
            editingInvestmentId = intent.getLongExtra("INVESTMENT_ID", 0)
            etAmount.setText(intent.getIntExtra("INVESTMENT_AMOUNT", 0).toString())
            etReturnRate.setText(intent.getFloatExtra("INVESTMENT_RETURN_RATE", 0f).toString())
            etNotes.setText(intent.getStringExtra("INVESTMENT_NOTES"))

            // Set principal date
            val principalDate = intent.getLongExtra("INVESTMENT_PRINCIPAL_DATE", 0)
            if (principalDate > 0) {
                calendar.timeInMillis = principalDate
                etDate.setText(dateFormat.format(calendar.time))
            }

            // Set maturity date
            val maturityDate = intent.getLongExtra("INVESTMENT_MATURITY_DATE", 0)
            if (maturityDate > 0) {
                maturityCalendar.timeInMillis = maturityDate
                etMaturityDate.setText(dateFormat.format(maturityCalendar.time))
            }

            // Set spinner selection
            val investmentType = intent.getStringExtra("INVESTMENT_TYPE")
            val categories = resources.getStringArray(R.array.investment_categories)
            val categoryIndex = categories.indexOf(investmentType)
            if (categoryIndex >= 0) {
                spinnerCategory.setSelection(categoryIndex)
            }
        }

        // Observe postInvestmentState
        viewModel.postInvestmentState.observe(this) { resource ->
            when (resource) {
                is com.example.expm.network.Resource.Loading -> {
                    btnSave.isEnabled = false
                    btnSave.text = "Saving..."
                }
                is com.example.expm.network.Resource.Success -> {
                    Toast.makeText(this, "Investment saved successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is com.example.expm.network.Resource.Error -> {
                    btnSave.isEnabled = true
                    btnSave.text = "Save"
                    Toast.makeText(this, "Error: ${resource.message}", Toast.LENGTH_LONG).show()
                }
                null -> {
                    btnSave.isEnabled = true
                    btnSave.text = "Save"
                }
            }
        }

        // Save button click listener
        btnSave.setOnClickListener {
            val amountStr = etAmount.text.toString().trim()
            val returnRateStr = etReturnRate.text.toString().trim()
            val category = spinnerCategory.selectedItem.toString()
            val notes = etNotes.text.toString().trim()
            val dateTimestamp = calendar.timeInMillis
            val maturityDateStr = etMaturityDate.text.toString().trim()

            // Validation
            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toIntOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Parse and round return rate to 2 decimal places
            val returnRate = if (returnRateStr.isNotEmpty()) {
                val rate = returnRateStr.toFloatOrNull() ?: 0f
                String.format(Locale.US, "%.2f", rate).toFloat()
            } else {
                0f
            }

            // Format transaction date to "dd-MM-yyyy" format
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val transactionDate = dateFormat.format(calendar.time)
            val creationDate = transactionDate // Use same date as creation date
            val maturityDate = if (maturityDateStr.isNotEmpty()) {
                maturityDateStr
            } else {
                transactionDate // Default to transaction date if not set
            }

            // Map category string to InvestmentType enum
            val investmentType = when (category) {
                "Fixed Deposit" -> InvestmentType.FIXED_DEPOSIT
                "Recurring Deposit" -> InvestmentType.RECURRING_DEPOSIT
                "EPF" -> InvestmentType.EPF
                "PPF" -> InvestmentType.PPF
                "SSY" -> InvestmentType.SSY
                "Mutual Fund" -> InvestmentType.MUTUAL_FUNDS
                "Stocks" -> InvestmentType.SHARES
                "Gold" -> InvestmentType.GOLD
                else -> InvestmentType.OTHER
            }

            // Generate clientId
            val clientId = UUID.randomUUID().toString()

            // Call ViewModel to post investment
            viewModel.postInvestment(
                amount = amount,
                expectedReturnRate = returnRate,
                investmentType = investmentType,
                creationDate = creationDate,
                maturityDate = maturityDate,
                transactionDate = transactionDate,
                description = notes.ifEmpty { null },
                clientId = clientId
            )
        }

        // Delete button click listener
        btnDelete.setOnClickListener {
            if (editingInvestmentId != null) {
                // Show confirmation dialog
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete Investment")
                    .setMessage("Are you sure you want to delete this investment?")
                    .setPositiveButton("Delete") { _, _ ->

                        finish()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

