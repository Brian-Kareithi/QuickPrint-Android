package com.example.quickprint.ui.chat

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickprint.data.ApiException
import com.example.quickprint.data.ChatConversation
import com.example.quickprint.data.ChatMessage
import com.example.quickprint.data.ChatRepository
import com.example.quickprint.data.MAX_CHAT_MESSAGE
import com.example.quickprint.data.Prefs
import kotlinx.coroutines.launch

class ChatViewModel(
    app: Application,
    private val shopId: String,
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

    init {
        viewModelScope.launch {
            val t = token ?: return@launch
            try {
                val (c, list) = ChatRepository.mine(t)
                conversation = c
                messages = list
                if (!orderNumber.isNullOrBlank() && c.orderNumber != orderNumber) linkOrder(t, orderNumber)
            } catch (e: ApiException) {
                handle(e)
            } finally {
                loading = false
            }
        }
    }

    /** Fetch only messages newer than the last one we have. Called on a timer while the chat is visible. */
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

    /** Sends [quick] (a one-tap message) or, when null, whatever is typed in the box. */
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
                    conversation = c
                    // Pick up the "Chat started…" system message.
                    merge(ChatRepository.mine(newToken).second)
                    newToken
                }
                merge(listOf(ChatRepository.send(t, body)))
                if (quick == null) draft = ""
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
    }

    private fun handle(e: ApiException) {
        if (e.status == 401) {
            // The chat was deleted (30 days inactive) or the key is invalid: start fresh next time.
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
