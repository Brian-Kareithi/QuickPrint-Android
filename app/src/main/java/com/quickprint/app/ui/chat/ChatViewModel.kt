package com.quickprint.app.ui.chat

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickprint.app.data.ApiException
import com.quickprint.app.data.ChatConversation
import com.quickprint.app.data.ChatMessage
import com.quickprint.app.data.ChatRepository
import com.quickprint.app.data.MAX_CHAT_MESSAGE
import com.quickprint.app.data.Prefs
import com.quickprint.app.notifications.ChatChecks
import com.quickprint.app.notifications.ChatNotifications
import kotlinx.coroutines.launch

class ChatViewModel(
    app: Application,
    private val shopId: String,
    private val shopName: String,
    private val orderNumber: String?,
    private val customerName: String?,
) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private var token: String? = prefs.chatToken(shopId)

    var conversation by mutableStateOf<ChatConversation?>(null); private set
    var messages by mutableStateOf<List<ChatMessage>>(emptyList()); private set
    var draft by mutableStateOf("")
    var sending by mutableStateOf(false); private set
    var loading by mutableStateOf(token != null); private set
    var error by mutableStateOf<String?>(null)

    var hasSent by mutableStateOf(false); private set

    init {
        if (token != null) prefs.setChatShopName(shopId, shopName)
        viewModelScope.launch {
            val t = token ?: return@launch
            try {
                val (c, list) = ChatRepository.mine(t)
                conversation = c
                messages = list
                markSeen()
                if (!orderNumber.isNullOrBlank() && c.orderNumber != orderNumber) linkOrder(t, orderNumber)
            } catch (e: ApiException) {
                handle(e)
            } finally {
                loading = false
            }
        }
    }

    suspend fun refresh() {
        val t = token ?: return
        try {
            val (c, fresh) = ChatRepository.mine(t, messages.lastOrNull()?.createdAt)
            conversation = c
            merge(fresh)
        } catch (e: ApiException) {
            if (e.status == 401) handle(e)
        }
    }

    fun send(quick: String? = null) {
        val body = (quick ?: draft).trim()
        if (body.isEmpty() || sending) return
        if (body.length > MAX_CHAT_MESSAGE) { error = "Messages can be at most $MAX_CHAT_MESSAGE characters."; return }
        sending = true
        error = null
        viewModelScope.launch {
            try {
                val t = token ?: ChatRepository.start(shopId, orderNumber, customerName).let { (newToken, c) ->
                    token = newToken
                    prefs.setChatToken(shopId, newToken)
                    prefs.setChatShopName(shopId, shopName)
                    conversation = c
                    merge(ChatRepository.mine(newToken).second)
                    newToken
                }
                merge(listOf(ChatRepository.send(t, body)))
                if (quick == null) draft = ""
                hasSent = true
                ChatChecks.boost(getApplication())
            } catch (e: ApiException) {
                handle(e)
            } finally {
                sending = false
            }
        }
    }

    private suspend fun linkOrder(t: String, number: String) {
        runCatching { conversation = ChatRepository.linkOrder(t, number) }
        runCatching { messages = ChatRepository.mine(t).second }
    }

    private fun merge(incoming: List<ChatMessage>) {
        if (incoming.isEmpty()) return
        val seen = messages.mapTo(HashSet()) { it.id }
        val fresh = incoming.filter { it.id !in seen }
        if (fresh.isNotEmpty()) messages = (messages + fresh).sortedBy { it.createdAt }
        markSeen()
    }

    private fun markSeen() {
        messages.lastOrNull { it.sender == "SHOP" }?.let { prefs.setLastNotified(shopId, it.id) }
        ChatNotifications.cancel(getApplication(), shopId)
    }

    private fun handle(e: ApiException) {
        if (e.status == 401) {
            prefs.setChatToken(shopId, null)
            token = null
            conversation = null
            messages = emptyList()
            error = "Your previous chat has expired. Send a message to start a new one."
        } else {
            error = e.message
        }
    }
}
