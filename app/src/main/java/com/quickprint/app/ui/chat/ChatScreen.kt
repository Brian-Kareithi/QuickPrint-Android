package com.quickprint.app.ui.chat

import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.clickable
import com.quickprint.app.ui.components.glass
import com.quickprint.app.ui.components.ChatSkeleton
import com.quickprint.app.ui.components.FadeUp
import com.quickprint.app.ui.components.AnimatedError
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
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import com.quickprint.app.notifications.ChatVisibility
import com.quickprint.app.notifications.NotificationPermissionPrompt
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quickprint.app.data.CUSTOMER_QUICK_MESSAGES
import com.quickprint.app.data.ChatMessage
import com.quickprint.app.data.MAX_CHAT_MESSAGE
import com.quickprint.app.data.parseIso
import com.quickprint.app.ui.components.ErrorBanner
import com.quickprint.app.ui.components.TermsLink
import com.quickprint.app.ui.theme.LocalQpExtras
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

private const val POLL_MS = 4000L

@Composable
fun ChatScreen(shopId: String, shopName: String, orderNumber: String?, customerName: String?, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as Application
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val vm: ChatViewModel = viewModel(key = "chat-$shopId") { ChatViewModel(app, shopId, shopName, orderNumber, customerName) }

    DisposableEffect(lifecycle, shopId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> ChatVisibility.shopId = shopId
                Lifecycle.Event.ON_PAUSE -> if (ChatVisibility.shopId == shopId) ChatVisibility.shopId = null
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (ChatVisibility.shopId == shopId) ChatVisibility.shopId = null
        }
    }
    NotificationPermissionPrompt(ask = vm.hasSent)
    val list = rememberLazyListState()

    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(POLL_MS)
                vm.refresh()
            }
        }
    }
    val density = LocalDensity.current
    var topBarPx by remember { mutableIntStateOf(0) }
    var bottomBarPx by remember { mutableIntStateOf(0) }
    val gap = 16.dp
    val topSpace = with(density) { topBarPx.toDp() } + gap
    val bottomSpace = with(density) { bottomBarPx.toDp() } + gap

    LaunchedEffect(vm.messages.size, bottomBarPx) {
        if (vm.messages.isNotEmpty()) list.animateScrollToItem(vm.messages.lastIndex)
    }

    Box(Modifier.fillMaxSize().imePadding()) {
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            when {
                vm.loading -> Box(Modifier.padding(top = topSpace)) { ChatSkeleton() }
                vm.messages.isEmpty() -> EmptyChat(shopName, enabled = !vm.sending, top = topSpace, bottom = bottomSpace) { vm.send(it) }
                else -> LazyColumn(
                    state = list,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = topSpace, bottom = bottomSpace),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(vm.messages, key = { it.id }) { Box(Modifier.animateItem()) { Bubble(it) } }
                }
            }
        }

        Row(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth()
                .onSizeChanged { topBarPx = it.height }
                .glass(RoundedCornerShape(28.dp), tint = LocalQpExtras.current.panel, alpha = 0.9f)
                .padding(start = 4.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.12f), modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(vm.conversation?.emoji ?: "💬", fontSize = 21.sp) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(shopName, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth()
                .onSizeChanged { bottomBarPx = it.height },
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AnimatedError(vm.error)
            Row(
                Modifier.fillMaxWidth().glass(RoundedCornerShape(30.dp)).padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                TextField(
                    value = vm.draft,
                    onValueChange = { vm.draft = it.take(MAX_CHAT_MESSAGE) },
                    placeholder = { Text("Type a message…") },
                    modifier = Modifier.weight(1f),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
                val canSend = !vm.sending && vm.draft.isNotBlank()
                val sendScale by animateFloatAsState(if (canSend) 1f else 0.88f, label = "send")
                FilledIconButton(
                    onClick = { vm.send() },
                    enabled = canSend,
                    modifier = Modifier.size(50.dp).graphicsLayer { scaleX = sendScale; scaleY = sendScale },
                ) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyChat(shopName: String, enabled: Boolean, top: Dp, bottom: Dp, onQuick: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState(), reverseScrolling = true).padding(start = 20.dp, end = 20.dp, top = top, bottom = bottom), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
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
            CUSTOMER_QUICK_MESSAGES.forEachIndexed { i, q ->
                FadeUp(i) {
                    Text(
                        q,
                        Modifier.glass(CircleShape, elevation = 4.dp).clickable(enabled = enabled) { onQuick(q) }.padding(horizontal = 14.dp, vertical = 9.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
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
