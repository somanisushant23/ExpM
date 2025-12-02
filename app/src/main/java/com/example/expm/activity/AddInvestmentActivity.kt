package com.example.expm.activity

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.expm.R
import com.example.expm.data.Entry
import com.example.expm.data.Investment
import com.example.expm.utils.AppUtils
import com.example.expm.viewmodel.AddInvestmentViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class AddInvestmentActivity : AppCompatActivity() {
    private lateinit var viewModel: AddInvestmentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_investment)

        // Enable back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.add_investment)

        val etTitle = findViewById<EditText>(R.id.et_title)
        val etAmount = findViewById<EditText>(R.id.et_amount)
        val etReturnRate = findViewById<EditText>(R.id.et_return_rate)
        val etDate = findViewById<EditText>(R.id.et_date)
        val etMaturityDate = findViewById<EditText>(R.id.et_maturity_date)
        val etNotes = findViewById<EditText>(R.id.et_notes)
        val spinnerCategory = findViewById<Spinner>(R.id.spinner_category)
        val btnSave = findViewById<Button>(R.id.btn_save)

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

        // Save button click listener
        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
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

            // Create investment entry (as Expense type with investment category)
            val investment = Investment(
                title = title,
                amount = amount,
                type = category,
                principalDateTimestamp = dateTimestamp,
                returnRate = returnRate,
                maturityDateTimestamp = AppUtils.dateToTimestamp2(maturityDateStr),
                notes = notes.trim(),
                remoteId = 0,
                clientId = UUID.randomUUID().toString()
            )

            // Insert entry
            viewModel.insertInvestment(investment)
            Toast.makeText(this, "Investment added successfully", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

