package com.example.expm.utils

object AppUtils {
    fun formatTimestampToDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
        val date = java.util.Date(timestamp)
        return sdf.format(date)
    }

    fun isTimestampInCurrentMonth(timestamp: Long): Boolean {
        val calendar = java.util.Calendar.getInstance()
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        val currentMonth = calendar.get(java.util.Calendar.MONTH)

        calendar.time = java.util.Date(timestamp)
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH)

        return year == currentYear && month == currentMonth
    }

    fun dateToTimestamp(dateString: String): Long {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return try {
            val date = sdf.parse(dateString)
            date?.time ?: throw IllegalArgumentException("Invalid date format")
        } catch (e: Exception) {
            throw IllegalArgumentException("Error parsing date: ${e.message}", e)
        }
    }

    fun dateToTimestamp2(dateString: String): Long {
       val sdf = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
        return try {
            val date = sdf.parse(dateString)
            date?.time ?: throw IllegalArgumentException("Invalid date format")
        } catch (e: Exception) {
            throw IllegalArgumentException("Error parsing date: ${e.message}", e)
        }
    }
}