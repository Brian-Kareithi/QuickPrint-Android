package com.example.quickprint

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.quickprint.data.ShopLink
import com.example.quickprint.data.ShopLinks
import com.example.quickprint.ui.theme.QuickprintTheme

class MainActivity : ComponentActivity() {
    private var deepLinkSlug by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            QuickprintTheme {
                QuickPrintApp(deepLinkSlug = deepLinkSlug, onDeepLinkHandled = { deepLinkSlug = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** A shop link (https://…/shop/<slug>) tapped in a browser, chat or camera app opens that shop. */
    private fun handleIntent(intent: Intent?) {
        val link = ShopLinks.parse(intent?.dataString)
        if (link is ShopLink.Slug) deepLinkSlug = link.slug
    }
}
