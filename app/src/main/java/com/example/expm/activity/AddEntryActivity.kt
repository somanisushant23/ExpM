package com.example.expm.activity

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.expm.R
import com.example.expm.data.Entry
import com.example.expm.viewmodel.AddEntryViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddEntryActivity : AppCompatActivity() {
    private lateinit var viewModel: AddEntryViewModel
    private var currentEntry: Entry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_entry)

        val etTitle = findViewById<EditText>(R.id.et_title)
        val etAmount = findViewById<EditText>(R.id.et_amount)
        val etNotes = findViewById<EditText>(R.id.et_notes)
        val etDate = findViewById<EditText>(R.id.et_date)
        val spinnerType = findViewById<Spinner>(R.id.spinner_type)
        val spinnerCategory = findViewById<Spinner>(R.id.spinner_category)
        val btnSave = findViewById<Button>(R.id.btn_save)
        val btnDelete = findViewById<Button>(R.id.btn_delete)

        // Obtain ViewModel
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(AddEntryViewModel::class.java)

        // Setup date helpers (reusable calendar)
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        etDate.setText(dateFormat.format(calendar.time))

        etDate.setOnClickListener {
            val y = calendar.get(Calendar.YEAR)
            val m = calendar.get(Calendar.MONTH)
            val d = calendar.get(Calendar.DAY_OF_MONTH)
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                etDate.setText(dateFormat.format(calendar.time))
            }, y, m, d).show()
        }

        // Types adapter
        ArrayAdapter.createFromResource(
            this,
            R.array.entry_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerType.adapter = adapter
        }

        // Category adapters for income and expense
        val expenseAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.expense_categories,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val incomeAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.income_categories,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // When type changes, swap category adapter
        spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 1) { // Income
                    spinnerCategory.adapter = incomeAdapter
                    spinnerCategory.setSelection(0)
                } else { // Expense
                    spinnerCategory.adapter = expenseAdapter
                    spinnerCategory.setSelection(0)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Force initial selection to trigger category population (default to first type)
        spinnerType.setSelection(0)

        // Observe insertion result (for create mode)
        viewModel.insertResult.observe(this) { insertedId ->
            if (insertedId == null) return@observe
            if (insertedId > 0L) {
                Toast.makeText(this@AddEntryActivity, "Saved successfully", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(this@AddEntryActivity, "Failed to save entry", Toast.LENGTH_SHORT).show()
            }
            viewModel.clearResults()
        }

        // Observe update/delete results
        viewModel.operationResult.observe(this) { ok ->
            if (ok == null) return@observe
            if (ok) {
                Toast.makeText(this@AddEntryActivity, "Operation successful", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(this@AddEntryActivity, "Operation failed", Toast.LENGTH_SHORT).show()
            }
            viewModel.clearResults()
        }

        // Determine if we're editing an existing entry
        val entryId = intent.getLongExtra("entry_id", -1L)
        if (entryId >= 0L) {
            // Edit mode: show delete, load entry and populate fields
            btnDelete.visibility = View.VISIBLE

            viewModel.getEntry(entryId).observe(this) { entry ->
                if (entry == null) {
                    Toast.makeText(this, "Entry not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@observe
                }
                currentEntry = entry
                etTitle.setText(entry.title)
                etAmount.setText(entry.amount.toString())
                etNotes.setText(entry.notes)
                // set date and update calendar used by date picker
                try {
                    val parsed = dateFormat.parse(entry.date)
                    if (parsed != null) {
                        calendar.time = parsed
                        etDate.setText(dateFormat.format(calendar.time))
                    } else {
                        etDate.setText(entry.date)
                    }
                } catch (_: Exception) {
                    etDate.setText(entry.date)
                }
                // set type
                val typePos = if (entry.type.equals("Income", ignoreCase = true)) 1 else 0
                spinnerType.setSelection(typePos)
                // ensure category is selected after adapter is set by onItemSelectedListener
                spinnerCategory.post {
                    val adapter = spinnerCategory.adapter
                    if (adapter != null) {
                        for (i in 0 until adapter.count) {
                            if (adapter.getItem(i)?.toString()?.equals(entry.category, true) == true) {
                                spinnerCategory.setSelection(i)
                                break
                            }
                        }
                    }
                }
            }

            btnSave.setOnClickListener {
                // Update existing entry
                val title = etTitle.text.toString().trim()
                val amountStr = etAmount.text.toString().trim()
                val notes = etNotes.text.toString().trim()
                val date = etDate.text.toString().trim()
                val type = spinnerType.selectedItem?.toString() ?: ""
                val category = spinnerCategory.selectedItem?.toString() ?: ""

                if (title.isEmpty() || amountStr.isEmpty()) {
                    Toast.makeText(this, "Please enter title and amount", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val amount = try { amountStr.toDouble() } catch (_: NumberFormatException) {
                    Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show(); return@setOnClickListener }

                val updated = currentEntry?.copy(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    date = date,
                    notes = notes
                )
                if (updated != null) viewModel.updateEntry(updated)
            }

            btnDelete.setOnClickListener {
                val entry = currentEntry
                if (entry != null) viewModel.deleteEntry(entry)
            }

        } else {
            // Create mode: hide delete, insertion on save
            btnDelete.visibility = View.GONE

            btnSave.setOnClickListener {
                val title = etTitle.text.toString().trim()
                val amountStr = etAmount.text.toString().trim()
                val notes = etNotes.text.toString().trim()
                val date = etDate.text.toString().trim()
                val type = spinnerType.selectedItem?.toString() ?: ""
                val category = spinnerCategory.selectedItem?.toString() ?: ""

                if (title.isEmpty() || amountStr.isEmpty()) {
                    Toast.makeText(this, "Please enter title and amount", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val amount = try { amountStr.toDouble() } catch (_: NumberFormatException) { Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show(); return@setOnClickListener }

                val entry = Entry(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    date = date,
                    notes = notes
                )

                viewModel.insertEntry(entry)
            }
        }
    }
}
