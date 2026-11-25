package com.example.quickprint.ui.chat

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quickprint.data.CUSTOMER_QUICK_MESSAGES
import com.example.quickprint.data.ChatMessage
import com.example.quickprint.data.MAX_CHAT_MESSAGE
import com.example.quickprint.data.parseIso
import com.example.quickprint.ui.components.ErrorBanner
import com.example.quickprint.ui.components.TermsLink
import com.example.quickprint.ui.theme.LocalQpExtras
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

private const val POLL_MS = 4000L

@Composable
fun ChatScreen(shopId: String, shopName: String, orderNumber: String?, customerName: String?, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as Application
    val vm: ChatViewModel = viewModel(key = "chat-$shopId") { ChatViewModel(app, shopId, orderNumber, customerName) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val list = rememberLazyListState()

    // New messages arrive by polling while the chat is on screen (stops in the background).
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(POLL_MS)
                vm.refresh()
            }
        }
    }
    LaunchedEffect(vm.messages.size) {
        if (vm.messages.isNotEmpty()) list.animateScrollToItem(vm.messages.lastIndex)
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        // Header
        Surface(color = LocalQpExtras.current.panel, contentColor = Color.White) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.12f), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(vm.conversation?.emoji ?: "💬", fontSize = 20.sp) }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(shopName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val c = vm.conversation
                    Text(
                        if (c != null) "You're chatting as ${c.alias}" + (c.orderNumber?.let { " · $it" } ?: "") else "Ask about your order",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                vm.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                vm.messages.isEmpty() -> EmptyChat(shopName, enabled = !vm.sending) { vm.send(it) }
                else -> LazyColumn(
                    state = list,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(vm.messages, key = { it.id }) { Bubble(it) }
                }
            }
        }

        vm.error?.let { ErrorBanner(it, Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column {
                Row(Modifier.padding(start = 12.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.Bottom) {
                    OutlinedTextField(
                        value = vm.draft,
                        onValueChange = { vm.draft = it.take(MAX_CHAT_MESSAGE) },
                        placeholder = { Text("Type a message…") },
                        modifier = Modifier.weight(1f),
                        maxLines = 5,
                        shape = RoundedCornerShape(22.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(onClick = { vm.send() }, enabled = !vm.sending && vm.draft.isNotBlank(), modifier = Modifier.size(52.dp)) {
                        if (vm.sending) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Icon(Icons.AutoMirrored.Filled.Send, "Send")
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text("Chats are deleted after 30 days of inactivity.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TermsLink(label = "Privacy")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyChat(shopName: String, enabled: Boolean, onQuick: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🍍🍌🥭", fontSize = 32.sp)
        Spacer(Modifier.height(10.dp))
        Text(
            "Cancelled your request or sent the wrong file? Message $shopName here. You'll get a fun nickname so they can find you, no account needed.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CUSTOMER_QUICK_MESSAGES.forEach { q ->
                Surface(
                    onClick = { onQuick(q) },
                    enabled = enabled,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Text(q, Modifier.padding(horizontal = 14.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun Bubble(m: ChatMessage) {
    if (m.sender == "SYSTEM") {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(m.body, Modifier.padding(horizontal = 12.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
        return
    }
    val mine = m.sender == "CUSTOMER"
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
        Surface(
            shape = RoundedCornerShape(18.dp, 18.dp, if (mine) 4.dp else 18.dp, if (mine) 18.dp else 4.dp),
            color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            border = if (mine) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(m.body, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
        }
        val time = runCatching { DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(parseIso(m.createdAt))) }.getOrDefault("")
        Text(
            (if (!mine && m.senderName != null) "${m.senderName} · " else "") + time,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
}
