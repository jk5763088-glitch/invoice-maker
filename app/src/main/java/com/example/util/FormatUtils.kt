package com.example.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class SupportedCurrency(
    val code: String,
    val symbol: String,
    val label: String
) {
    PKR("PKR", "Rs.", "Pakistani Rupee (PKR)"),
    USD("USD", "$", "US Dollar (USD)"),
    EUR("EUR", "€", "Euro (EUR)"),
    GBP("GBP", "£", "British Pound (GBP)"),
    AED("AED", "AED", "UAE Dirham (AED)"),
    SAR("SAR", "SAR", "Saudi Riyal (SAR)");

    companion object {
        fun fromCode(code: String): SupportedCurrency {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: USD
        }
    }
}

enum class InvoiceTemplate(
    val id: String,
    val title: String,
    val subtitle: String,
    val isPremium: Boolean = false
) {
    SIMPLE("SIMPLE", "Simple", "Clean & classic minimal layout for fast printing", false),
    MODERN("MODERN", "Modern", "Bold header banner with tinted rows & summary box", false),
    BUSINESS("BUSINESS", "Business", "Formal corporate grid layout with signature area", false),
    EXECUTIVE("EXECUTIVE", "Executive Pro", "Premium dark-gold accent executive layout", true);

    companion object {
        fun fromId(id: String): InvoiceTemplate {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MODERN
        }
    }
}

enum class PaymentStatus(val key: String, val label: String) {
    PAID("PAID", "Paid"),
    UNPAID("UNPAID", "Unpaid"),
    PARTIAL("PARTIAL", "Partial");

    companion object {
        fun fromKey(key: String): PaymentStatus {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: UNPAID
        }
    }
}

object FormatUtils {
    private val numberFormat = DecimalFormat("#,##0.00")
    private val qtyFormat = DecimalFormat("#,##0.##")

    fun formatMoney(amount: Double, currencyCode: String): String {
        val currency = SupportedCurrency.fromCode(currencyCode)
        val safeAmount = if (amount.isNaN() || amount.isInfinite()) 0.0 else amount
        val formatted = numberFormat.format(safeAmount)
        return when (currency) {
            SupportedCurrency.USD, SupportedCurrency.EUR, SupportedCurrency.GBP ->
                "${currency.symbol}$formatted"
            else -> "${currency.code} $formatted"
        }
    }

    fun formatQuantity(qty: Double): String {
        val safeQty = if (qty.isNaN() || qty.isInfinite()) 0.0 else qty
        return qtyFormat.format(safeQty)
    }

    fun todayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun dueDateString(daysAhead: Int = 15): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, daysAhead)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(cal.time)
    }

    fun parseDateToMillis(dateStr: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    fun formatInvoiceNumber(prefix: String, number: Int): String {
        val cleanPrefix = if (prefix.isBlank()) "INV-" else prefix
        return "$cleanPrefix${number.toString().padStart(4, '0')}"
    }

    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return true // Optional unless entered
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return emailRegex.matches(email.trim())
    }
}
