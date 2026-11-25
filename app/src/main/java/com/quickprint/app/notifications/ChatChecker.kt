package com.quickprint.app.notifications

import android.content.Context
import android.util.Log
import com.quickprint.app.data.ApiException
import com.quickprint.app.data.ChatRepository
import com.quickprint.app.data.Prefs

/** Shared "is there a new shop reply?" check used by the listening service and the background worker. */
object ChatChecker {
    private const val TAG = "QuickPrintChat"

    /** Checks every chat on this phone; shows a heads-up notification for each new shop reply. Returns how many were new. */
    suspend fun checkAll(context: Context): Int {
        val prefs = Prefs(context)
        var fresh = 0
        for ((shopId, token) in prefs.allChats()) {
            if (ChatVisibility.shopId == shopId) continue // the customer is looking at this chat
            try {
                val status = ChatRepository.status(token)
                val id = status.lastShopMessageId
                if (status.unread > 0 && id != null && id != prefs.lastNotified(shopId)) {
                    ChatNotifications.show(
                        context,
                        shopId = shopId,
                        shopName = prefs.chatShopName(shopId) ?: "Print shop",
                        message = status.lastShopMessage.orEmpty(),
                        unread = status.unread,
                        alias = status.alias,
                    )
                    prefs.setLastNotified(shopId, id)
                    fresh++
                }
            } catch (e: ApiException) {
                Log.d(TAG, "check failed for shop=$shopId: ${e.status} ${e.message}")
                if (e.status == 401) prefs.setChatToken(shopId, null) // chat expired/deleted
            }
        }
        return fresh
    }
}
