package com.quickprint.app.data

import org.json.JSONObject
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** A choice shown to the customer; `value` is what the backend expects. */
data class Choice(val value: String, val label: String)

object PrintChoices {
    val color = listOf(Choice("BLACK_WHITE", "B&W"), Choice("FULL_COLOR", "Color"))
    val paper = listOf(Choice("A4", "A4"), Choice("A3", "A3"), Choice("LETTER", "Letter"), Choice("LEGAL", "Legal"))
    val layout = listOf(Choice("SINGLE_SIDED", "Single-sided"), Choice("DOUBLE_SIDED", "Double-sided"))
    val quality = listOf(Choice("DRAFT", "Draft"), Choice("STANDARD", "Standard"), Choice("HIGH_QUALITY", "High"))
    val binding = listOf(Choice("NONE", "None"), Choice("SPIRAL", "Spiral"), Choice("COMB", "Comb"), Choice("TAPE", "Tape"), Choice("HARDCOVER", "Hardcover"))
    val finishing = listOf(Choice("NONE", "None"), Choice("STAPLING", "Staple"), Choice("LAMINATION", "Laminate"), Choice("FOLDING", "Fold"), Choice("HOLE_PUNCH", "Punch"))
}

data class PrintOptions(
    val pageCount: Int = 1,
    val copies: Int = 1,
    val colorMode: String = "BLACK_WHITE",
    val paperSize: String = "A4",
    val layout: String = "SINGLE_SIDED",
    val printQuality: String = "STANDARD",
    val bindingType: String = "NONE",
    val finishingType: String = "NONE",
) {
    fun toJson(): JSONObject = JSONObject()
        .put("pageCount", pageCount).put("copies", copies)
        .put("colorMode", colorMode).put("paperSize", paperSize)
        .put("layout", layout).put("printQuality", printQuality)
        .put("bindingType", bindingType).put("finishingType", finishingType)
}

/** A file already uploaded to the shop, plus the customer's print options for it. */
data class UploadedFile(
    val id: String,
    val name: String,
    val size: Long,
    val options: PrintOptions,
)

data class PriceBreakdown(
    val printingCost: Double,
    val bindingCost: Double,
    val finishingCost: Double,
    val taxAmount: Double,
    val serviceFee: Double,
    val totalAmount: Double,
) {
    companion object {
        fun from(o: JSONObject) = PriceBreakdown(
            o.optDouble("printingCost", 0.0), o.optDouble("bindingCost", 0.0), o.optDouble("finishingCost", 0.0),
            o.optDouble("taxAmount", 0.0), o.optDouble("serviceFee", 0.0), o.optDouble("totalAmount", 0.0),
        )
    }
}

data class CustomerDetails(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val paymentMethod: String = "",
    val notes: String = "",
)

data class PlacedOrder(
    val id: String,
    val orderNumber: String,
    val status: String,
    val totalAmount: Double,
    val paymentMethod: String?,
    val readyInMinutes: Int?,
) {
    companion object {
        fun from(o: JSONObject): PlacedOrder {
            val minutes = runCatching {
                val created = parseIso(o.getString("createdAt"))
                val eta = parseIso(o.getString("estimatedCompletion"))
                ((eta - created) / 60000).toInt().coerceAtLeast(1)
            }.getOrNull()
            return PlacedOrder(o.getString("id"), o.getString("orderNumber"), o.optString("status", "PENDING"), o.optDouble("totalAmount", 0.0), o.str("paymentMethod"), minutes)
        }
    }
}

object Payments {
    /** Same rules as the website (lib/payments.ts). */
    fun options(shop: Shop): List<Choice> {
        val opts = mutableListOf<Choice>()
        if (shop.acceptCash) opts += Choice("CASH_ON_PICKUP", "Cash on pickup")
        if (!shop.mpesaNumber.isNullOrBlank()) opts += Choice("MPESA", "M-Pesa")
        if (!shop.airtelNumber.isNullOrBlank()) opts += Choice("AIRTEL_MONEY", "Airtel Money")
        if (opts.isEmpty()) opts += Choice("CASH_ON_PICKUP", "Pay at the shop")
        return opts
    }

    fun instructions(shop: Shop, method: String, amount: Double): List<String> {
        val lines = mutableListOf<String>()
        when {
            method == "MPESA" && !shop.mpesaNumber.isNullOrBlank() -> {
                lines += when (shop.mpesaMethod) {
                    "TILL" -> "Lipa na M-Pesa > Buy Goods > Till number ${shop.mpesaNumber}"
                    "PAYBILL" -> "Lipa na M-Pesa > Paybill > Business no. ${shop.mpesaNumber}" + (shop.mpesaAccountRef?.let { ", Account no. $it" } ?: "")
                    else -> "Send money to ${shop.mpesaNumber}"
                }
                shop.mpesaAccountName?.let { lines += "Name shown: $it" }
            }
            method == "AIRTEL_MONEY" && !shop.airtelNumber.isNullOrBlank() -> lines += "Send Airtel Money to ${shop.airtelNumber}"
            method == "CASH_ON_PICKUP" -> lines += "Pay in cash when you collect your prints."
        }
        if (method != "CASH_ON_PICKUP") lines += "Amount: ${money(amount, shop.currency)}"
        if (!shop.bankAccountNumber.isNullOrBlank()) {
            lines += "Bank: " + listOfNotNull(shop.bankName, shop.bankAccountName, shop.bankAccountNumber).joinToString(" · ")
        }
        shop.paymentNotes?.let { lines += it }
        return lines
    }
}

fun money(amount: Double, currency: String = "KES"): String {
    val f = NumberFormat.getCurrencyInstance(Locale("en", "KE"))
    runCatching { f.currency = Currency.getInstance(currency) }
    return f.format(amount)
}

/** Parses the backend's ISO-8601 UTC timestamps (e.g. 2026-11-25T08:57:16.639Z) to epoch millis. Works on API 24. */
fun parseIso(value: String): Long {
    val pattern = if (value.contains('.')) "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" else "yyyy-MM-dd'T'HH:mm:ss'Z'"
    val f = java.text.SimpleDateFormat(pattern, Locale.US)
    f.timeZone = java.util.TimeZone.getTimeZone("UTC")
    return f.parse(value)!!.time
}

fun fileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}
