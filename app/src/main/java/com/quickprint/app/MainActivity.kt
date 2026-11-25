package com.quickprint.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quickprint.app.data.ShopLink
import com.quickprint.app.data.ShopLinks
import com.quickprint.app.notifications.ChatChecks
import com.quickprint.app.notifications.ChatChecker
import com.quickprint.app.data.Prefs
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.quickprint.app.notifications.ChatNotifications
import com.quickprint.app.ui.theme.QuickprintTheme

class MainActivity : ComponentActivity() {
    private var link by mutableStateOf<AppLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ChatNotifications.ensureChannel(this)
        ChatChecks.schedule(this)
        if (savedInstanceState == null) handleIntent(intent)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    if (Prefs(this@MainActivity).allChats().isNotEmpty()) ChatChecker.checkAll(applicationContext)
                    delay(10_000)
                }
            }
        }
        setContent {
            QuickprintTheme {
                QuickPrintApp(link = link, onLinkHandled = { link = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val shopId = intent?.getStringExtra(ChatNotifications.EXTRA_SHOP_ID)
        if (shopId != null) {
            link = AppLink.Chat(shopId, intent.getStringExtra(ChatNotifications.EXTRA_SHOP_NAME) ?: "Print shop")
            return
        }
        val parsed = ShopLinks.parse(intent?.dataString)
        if (parsed is ShopLink.Slug) link = AppLink.Shop(parsed.slug)
    }
}
