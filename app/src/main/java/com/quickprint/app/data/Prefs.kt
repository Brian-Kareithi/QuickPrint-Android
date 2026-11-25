package com.quickprint.app.data

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
        if (token == null) {
            remove("token_$shopId"); remove("name_$shopId"); remove("notified_$shopId")
        } else putString("token_$shopId", token)
    }

    /** Shop name for notifications ("Campus Prints: Your order is ready"). */
    fun chatShopName(shopId: String): String? = chat.getString("name_$shopId", null)
    fun setChatShopName(shopId: String, name: String) = chat.edit { putString("name_$shopId", name) }

    /** Every shop this phone has an active chat with: shopId -> token. */
    fun allChats(): Map<String, String> = chat.all
        .filterKeys { it.startsWith("token_") }
        .mapNotNull { (k, v) -> (v as? String)?.let { k.removePrefix("token_") to it } }
        .toMap()

    /** Id of the last shop reply we already showed a notification for (avoids repeats). */
    fun lastNotified(shopId: String): String? = chat.getString("notified_$shopId", null)
    fun setLastNotified(shopId: String, messageId: String) = chat.edit { putString("notified_$shopId", messageId) }

    /** Until this time (epoch ms) background checks run about every minute instead of every 15. */
    var fastCheckUntil: Long
        get() = main.getLong("fast_check_until", 0L)
        set(value) = main.edit { putLong("fast_check_until", value) }
}
