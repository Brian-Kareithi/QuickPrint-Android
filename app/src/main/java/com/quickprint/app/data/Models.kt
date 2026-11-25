package com.quickprint.app.data

import org.json.JSONObject

data class Shop(
    val id: String,
    val name: String,
    val slug: String,
    val welcomeMessage: String?,
    val address: String?,
    val phone: String?,
    val email: String?,
    val currency: String,
    val isAcceptingOrders: Boolean,
    val primaryColor: String?,
    val logoUrl: String?,
    val acceptCash: Boolean,
    val mpesaMethod: String?,
    val mpesaNumber: String?,
    val mpesaAccountRef: String?,
    val mpesaAccountName: String?,
    val airtelNumber: String?,
    val bankName: String?,
    val bankAccountName: String?,
    val bankAccountNumber: String?,
    val paymentNotes: String?,
    /** Lower-case extensions the shop accepts, e.g. "pdf", "docx". Empty = use defaults. */
    val allowedExtensions: List<String>,
    val allowedMimeTypes: List<String>,
) {
    companion object {
        fun from(o: JSONObject): Shop {
            val branding = o.optJSONObject("branding")
            val types = o.optJSONArray("allowedFileTypes")
            val ext = mutableListOf<String>()
            val mimes = mutableListOf<String>()
            if (types != null) for (i in 0 until types.length()) {
                val t = types.getJSONObject(i)
                t.str("extension")?.let { ext += it.lowercase() }
                t.str("mimeType")?.let { mimes += it }
            }
            return Shop(
                id = o.getString("id"),
                name = o.optString("name"),
                slug = o.optString("slug"),
                welcomeMessage = o.str("welcomeMessage"),
                address = o.str("address"),
                phone = o.str("phone"),
                email = o.str("email"),
                currency = o.str("currency") ?: "KES",
                isAcceptingOrders = o.optBoolean("isAcceptingOrders", true),
                primaryColor = branding?.str("primaryColor"),
                logoUrl = branding?.str("logoUrl"),
                acceptCash = o.optBoolean("acceptCash", true),
                mpesaMethod = o.str("mpesaMethod"),
                mpesaNumber = o.str("mpesaNumber"),
                mpesaAccountRef = o.str("mpesaAccountRef"),
                mpesaAccountName = o.str("mpesaAccountName"),
                airtelNumber = o.str("airtelNumber"),
                bankName = o.str("bankName"),
                bankAccountName = o.str("bankAccountName"),
                bankAccountNumber = o.str("bankAccountNumber"),
                paymentNotes = o.str("paymentNotes"),
                allowedExtensions = ext,
                allowedMimeTypes = mimes.distinct(),
            )
        }
    }
}

/** A shop the customer used before, remembered on the device for one-tap access. */
data class RecentShop(val name: String, val slug: String, val address: String?)
