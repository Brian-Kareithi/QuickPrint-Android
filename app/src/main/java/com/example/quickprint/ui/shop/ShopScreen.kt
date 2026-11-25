package com.example.quickprint.ui.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.quickprint.data.ApiException
import com.example.quickprint.data.Prefs
import com.example.quickprint.data.RecentShop
import com.example.quickprint.data.Shop
import com.example.quickprint.data.ShopRepository
import com.example.quickprint.ui.components.BrandPanel
import com.example.quickprint.ui.components.CenteredBox
import com.example.quickprint.ui.components.ErrorBanner
import com.example.quickprint.ui.components.TermsLink

/** Parses a "#RRGGBB" brand colour from the shop's branding settings. */
fun brandColor(hex: String?, fallback: Color): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)

@Composable
fun ShopScreen(slug: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var shop by remember { mutableStateOf<Shop?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var notFound by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(slug, reload) {
        error = null
        try {
            val s = ShopRepository.bySlug(slug)
            shop = s
            Prefs(context).recentShop = RecentShop(s.name, s.slug, s.address)
        } catch (e: ApiException) {
            notFound = e.status == 404
            error = e.message
        }
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text(shop?.name ?: "", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        val s = shop
        when {
            s == null && error == null -> CenteredBox(Modifier.fillMaxSize()) { CircularProgressIndicator() }
            s == null -> CenteredBox(Modifier.fillMaxSize().padding(24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (notFound) "Shop not found" else "Couldn't load this shop", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (notFound) "This print shop doesn't exist or is inactive. Check the code and try again." else error.orEmpty(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { if (notFound) onBack() else reload++ }) { Text(if (notFound) "Back" else "Try again") }
                }
            }
            else -> ShopContent(s, onRetry = { reload++ })
        }
    }
}

@Composable
private fun ShopContent(shop: Shop, onRetry: () -> Unit) {
    val brand = brandColor(shop.primaryColor, MaterialTheme.colorScheme.primary)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ShopHeader(shop, brand)
        if (!shop.isAcceptingOrders) {
            ErrorBanner("${shop.name} isn't taking new print orders right now. Come back a little later.")
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Check again") }
        } else {
            Text(
                "Connected! File upload is coming in the next update.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        TermsLink(Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
fun ShopHeader(shop: Shop, brand: Color) {
    BrandPanel(glow = brand) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(shop.name, color = Color.White, style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(
                    shop.welcomeMessage ?: "Upload your documents for printing",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                )
                shop.address?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.width(12.dp))
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.12f)) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = if (shop.isAcceptingOrders) Color(0xFF34D399) else Color(0xFFF59E0B), modifier = Modifier.size(7.dp)) {}
                    Spacer(Modifier.width(6.dp))
                    Text(if (shop.isAcceptingOrders) "Open" else "Closed", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
