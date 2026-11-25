package com.example.quickprint.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject

/** Small on-device storage: last shop used. Chat keys live in a separate, non-backed-up file. */
class Prefs(context: Context) {
    private val app = context.applicationContext
    private val main = app.getSharedPreferences("quickprint", Context.MODE_PRIVATE)

    /** Excluded from cloud backup and device transfer (see res/xml backup rules). */
    private val chat = app.getSharedPreferences("quickprint_chat", Context.MODE_PRIVATE)

    var recentShop: RecentShop?
        get() = main.getString("recent_shop", null)?.let {
            runCatching {
                val o = JSONObject(it)
                RecentShop(o.getString("name"), o.getString("slug"), o.str("address"))
            }.getOrNull()
        }
        set(value) = main.edit {
            if (value == null) remove("recent_shop")
            else putString("recent_shop", JSONObject().put("name", value.name).put("slug", value.slug).put("address", value.address).toString())
        }

    fun chatToken(shopId: String): String? = chat.getString("token_$shopId", null)

    fun setChatToken(shopId: String, token: String?) = chat.edit {
        if (token == null) remove("token_$shopId") else putString("token_$shopId", token)
    }
}
