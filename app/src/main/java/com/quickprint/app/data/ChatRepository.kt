package com.quickprint.app.data

import android.net.Uri
import org.json.JSONObject

data class ChatMessage(
    val id: String,
    val sender: String,
    val senderName: String?,
    val body: String,
    val createdAt: String,
) {
    companion object {
        fun from(o: JSONObject) = ChatMessage(o.getString("id"), o.getString("sender"), o.str("senderName"), o.optString("body"), o.getString("createdAt"))
    }
}

data class ChatConversation(
    val id: String,
    val alias: String,
    val emoji: String,
    val orderNumber: String?,
    val status: String,
) {
    companion object {
        fun from(o: JSONObject) = ChatConversation(o.getString("id"), o.optString("alias"), o.optString("emoji", "💬"), o.str("orderNumber"), o.optString("status", "OPEN"))
    }
}

data class ChatStatus(val unread: Int, val lastShopMessageId: String?, val lastShopMessage: String?, val alias: String)

const val MAX_CHAT_MESSAGE = 2000

val CUSTOMER_QUICK_MESSAGES = listOf(
    "I want to cancel my order",
    "I sent the wrong document",
    "When will my order be ready?",
    "I need to change my print options",
)

object ChatRepository {
    private fun auth(token: String) = mapOf("X-Chat-Token" to token)

    suspend fun start(shopId: String, orderNumber: String?, customerName: String?): Pair<String, ChatConversation> {
        val body = JSONObject()
        orderNumber?.takeIf { it.isNotBlank() }?.let { body.put("orderNumber", it) }
        customerName?.takeIf { it.isNotBlank() }?.let { body.put("customerName", it) }
        val o = JSONObject(Api.send("POST", "/chat/shops/${Uri.encode(shopId)}/start", body))
        return o.getString("token") to ChatConversation.from(o.getJSONObject("conversation"))
    }

    suspend fun mine(token: String, after: String? = null): Pair<ChatConversation, List<ChatMessage>> {
        val path = "/chat/me" + (after?.let { "?after=${Uri.encode(it)}" } ?: "")
        val o = JSONObject(Api.get(path, auth(token)))
        val arr = o.getJSONArray("messages")
        return ChatConversation.from(o.getJSONObject("conversation")) to (0 until arr.length()).map { ChatMessage.from(arr.getJSONObject(it)) }
    }

    suspend fun status(token: String): ChatStatus {
        val o = JSONObject(Api.get("/chat/me/status", auth(token)))
        val c = o.getJSONObject("conversation")
        val last = o.optJSONObject("lastShopMessage")
        return ChatStatus(
            unread = c.optInt("unread", 0),
            lastShopMessageId = last?.optString("id"),
            lastShopMessage = last?.optString("body"),
            alias = c.optString("alias"),
        )
    }

    suspend fun send(token: String, body: String): ChatMessage =
        ChatMessage.from(JSONObject(Api.send("POST", "/chat/me/messages", JSONObject().put("body", body), auth(token))))

    suspend fun linkOrder(token: String, orderNumber: String): ChatConversation =
        ChatConversation.from(JSONObject(Api.send("PATCH", "/chat/me/order", JSONObject().put("orderNumber", orderNumber), auth(token))))
}
