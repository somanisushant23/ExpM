package com.example.expm.activity

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.example.expm.R
import com.example.expm.data.AppDatabase
import com.example.expm.data.Utility
import com.example.expm.utils.BiometricHelper
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var btnSelectStartDate: Button
    private lateinit var btnSelectEndDate: Button
    private lateinit var btnSaveDateRange: Button
    private lateinit var switchBiometric: SwitchMaterial
    private lateinit var database: AppDatabase
    private lateinit var biometricHelper: BiometricHelper

    private var startDateTimestamp: Long? = null
    private var endDateTimestamp: Long? = null

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Setup Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.nav_settings)
            setDisplayHomeAsUpEnabled(true)
        }

        // Initialize views
        tvStartDate = findViewById(R.id.tv_start_date)
        tvEndDate = findViewById(R.id.tv_end_date)
        btnSelectStartDate = findViewById(R.id.btn_select_start_date)
        btnSelectEndDate = findViewById(R.id.btn_select_end_date)
        btnSaveDateRange = findViewById(R.id.btn_save_date_range)
        switchBiometric = findViewById(R.id.switch_biometric)

        // Initialize database and biometric helper
        database = AppDatabase.getInstance(applicationContext)
        biometricHelper = BiometricHelper(this)

        // Load existing date range
        loadDateRange()

        // Setup biometric switch
        setupBiometricSwitch()

        // Set up click listeners
        btnSelectStartDate.setOnClickListener {
            showDatePicker { timestamp ->
                startDateTimestamp = timestamp
                tvStartDate.text = dateFormat.format(Date(timestamp))
            }
        }

        btnSelectEndDate.setOnClickListener {
            showDatePicker { timestamp ->
                endDateTimestamp = timestamp
                tvEndDate.text = dateFormat.format(Date(timestamp))
            }
        }

        btnSaveDateRange.setOnClickListener {
            saveDateRange()
        }
    }

    /**
     * Setup biometric authentication switch
     */
    private fun setupBiometricSwitch() {
        // Check if biometric or device credential is available
        val isBiometricAvailable = biometricHelper.isBiometricOrDeviceCredentialAvailable()

        if (!isBiometricAvailable) {
            // Disable switch if biometric/device credential is not available
            switchBiometric.isEnabled = false
            switchBiometric.isChecked = false
            Toast.makeText(this, "Biometric or device credential authentication not available on this device", Toast.LENGTH_SHORT).show()
        } else {
            // Load current setting
            switchBiometric.isChecked = biometricHelper.isBiometricEnabled()

            // Handle switch changes
            switchBiometric.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Enable biometric - show authentication prompt to verify
                    biometricHelper.showBiometricOrDeviceCredentialPrompt(
                        onSuccess = {
                            biometricHelper.setBiometricEnabled(true)
                            Toast.makeText(this, "Biometric/PIN authentication enabled", Toast.LENGTH_SHORT).show()
                        },
                        onError = { _ ->
                            // User cancelled, revert switch
                            switchBiometric.isChecked = false
                            Toast.makeText(this, "Authentication setup cancelled", Toast.LENGTH_SHORT).show()
                        },
                        onFailed = {
                            // Authentication failed, revert switch
                            switchBiometric.isChecked = false
                            Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    // Disable biometric
                    biometricHelper.setBiometricEnabled(false)
                    Toast.makeText(this, "Biometric/PIN authentication disabled", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Load existing date range from database
     */
    private fun loadDateRange() {
        lifecycleScope.launch {
            val startDateUtility = withContext(Dispatchers.IO) {
                database.utilityDao().getByKey("start_date")
            }
            val endDateUtility = withContext(Dispatchers.IO) {
                database.utilityDao().getByKey("end_date")
            }

            startDateUtility?.let {
                startDateTimestamp = it.data_value.toLongOrNull()
                startDateTimestamp?.let { timestamp ->
                    tvStartDate.text = dateFormat.format(Date(timestamp))
                }
            }

            endDateUtility?.let {
                endDateTimestamp = it.data_value.toLongOrNull()
                endDateTimestamp?.let { timestamp ->
                    tvEndDate.text = dateFormat.format(Date(timestamp))
                }
            }
        }
    }

    /**
     * Show date picker dialog
     */
    private fun showDatePicker(onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                onDateSelected(calendar.timeInMillis)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    /**
     * Save date range to database
     */
    private fun saveDateRange() {
        if (startDateTimestamp == null || endDateTimestamp == null) {
            Toast.makeText(this, "Please select both start and end dates", Toast.LENGTH_SHORT).show()
            return
        }

        if (startDateTimestamp!! > endDateTimestamp!!) {
            Toast.makeText(this, "Start date must be before end date", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Check if entries already exist
                    val existingStartDate = database.utilityDao().getByKey("start_date")
                    val existingEndDate = database.utilityDao().getByKey("end_date")

                    val currentTime = System.currentTimeMillis()

                    if (existingStartDate != null) {
                        // Update existing
                        database.utilityDao().update(
                            existingStartDate.copy(
                                data_value = startDateTimestamp.toString(),
                                updated_on = currentTime
                            )
                        )
                    } else {
                        // Insert new
                        database.utilityDao().insert(
                            Utility(
                                data_key = "start_date",
                                data_value = startDateTimestamp.toString(),
                                created_on = currentTime,
                                updated_on = currentTime,
                                isActive = true
                            )
                        )
                    }

                    if (existingEndDate != null) {
                        // Update existing
                        database.utilityDao().update(
                            existingEndDate.copy(
                                data_value = endDateTimestamp.toString(),
                                updated_on = currentTime
                            )
                        )
                    } else {
                        // Insert new
                        database.utilityDao().insert(
                            Utility(
                                data_key = "end_date",
                                data_value = endDateTimestamp.toString(),
                                created_on = currentTime,
                                updated_on = currentTime,
                                isActive = true
                            )
                        )
                    }
                }

                Toast.makeText(this@SettingsActivity, "Date range saved successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Error saving date range: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

