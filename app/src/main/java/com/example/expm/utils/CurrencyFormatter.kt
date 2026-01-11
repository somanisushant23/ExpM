package com.example.expm.utils

import java.util.Locale

object CurrencyFormatter {

    fun formatIndianCurrency(amount: Double): String {
        val roundedAmount = amount.toLong()

        if (roundedAmount == 0L) return "0"

        val crores = roundedAmount / 10000000
        val lakhs = (roundedAmount % 10000000) / 100000
        val thousands = (roundedAmount % 100000) / 1000
        val hundreds = roundedAmount % 1000

        val parts = mutableListOf<String>()

        if (crores > 0) {
            parts.add("$crores Crore${if (crores > 1) "s" else ""}")
        }
        if (lakhs > 0) {
            parts.add("$lakhs Lakh${if (lakhs > 1) "s" else ""}")
        }
        if (thousands > 0) {
            parts.add("$thousands Thousand")
        }
        if (hundreds > 0 || parts.isEmpty()) {
            if (parts.isEmpty()) {
                parts.add(String.format(Locale.getDefault(), "%.2f", amount))
            } else {
                parts.add("$hundreds")
            }
        }

        return parts.joinToString(" ")
    }
}

