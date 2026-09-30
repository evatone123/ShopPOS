package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {
    private val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    private val decimalFormat = DecimalFormat("#,##0.00", symbols)

    /**
     * Formats integer minor units (pesewas) into a display string.
     * E.g. 1050L -> "GHS 10.50"
     */
    fun formatPesewas(pesewas: Long, currency: String = "GHS"): String {
        val amount = pesewas / 100.0
        return "$currency ${decimalFormat.format(amount)}"
    }

    /**
     * Converts a string representation (e.g. "10.50" or "10,50") into integer pesewas.
     */
    fun toPesewas(amountStr: String): Long {
        if (amountStr.isBlank()) return 0L
        val cleaned = amountStr.replace(",", ".").trim()
        val parsed = cleaned.toDoubleOrNull() ?: 0.0
        return Math.round(parsed * 100.0)
    }

    /**
     * Converts pesewas to a simple decimal string for editing in TextFields (e.g. "10.50")
     */
    fun pesewasToEditableString(pesewas: Long): String {
        val amount = pesewas / 100.0
        return String.format(Locale.US, "%.2f", amount)
    }
}
