package com.quickprint.app.ui.shop

import com.quickprint.app.ui.components.glass
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.core.tween
import com.quickprint.app.ui.components.AnimatedError
import com.quickprint.app.ui.components.ShopSkeleton
import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quickprint.app.data.Shop
import com.quickprint.app.ui.components.BrandPanel
import com.quickprint.app.ui.components.CenteredBox
import com.quickprint.app.ui.components.ErrorBanner
import com.quickprint.app.ui.theme.LocalQpExtras

fun brandColor(hex: String?, fallback: Color): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)

@Composable
fun ShopScreen(
    slug: String,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onChat: (shopId: String, shopName: String, orderNumber: String?, customerName: String?) -> Unit,
) {
    val app = LocalContext.current.applicationContext as Application
    val vm: OrderViewModel = viewModel(key = "order-$slug") { OrderViewModel(app, slug) }
    val openChat = { s: Shop -> onChat(s.id, s.name, vm.order?.orderNumber, vm.order?.customerName ?: vm.details.name.ifBlank { null }) }

    BackHandler(enabled = vm.step == Step.OPTIONS || vm.step == Step.REVIEW) { vm.back() }

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth().glass(RoundedCornerShape(28.dp), elevation = 6.dp).padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { if (!vm.back()) onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text(vm.shop?.name ?: "", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            vm.shop?.let { s ->
                TextButton(onClick = { openChat(s) }) {
                    Text("💬", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("Chat")
                }
            }
        }

        val shop = vm.shop
        when {
            shop == null && vm.loadError == null -> ShopSkeleton()
            shop == null -> CenteredBox(Modifier.fillMaxSize().padding(24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (vm.notFound) "Shop not found" else "Couldn't load this shop", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (vm.notFound) "This print shop doesn't exist or is inactive. Check the code and try again." else vm.loadError.orEmpty(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { if (vm.notFound) onBack() else vm.load() }) { Text(if (vm.notFound) "Back" else "Try again") }
                }
            }
            vm.step == Step.DONE && vm.order != null -> DoneStep(shop, vm.order!!, vm, onHome, onChat = { openChat(shop) })
            !shop.isAcceptingOrders && vm.files.isEmpty() -> ClosedState(shop, onRetry = { vm.load() })
            else -> {
                val brand = brandColor(shop.primaryColor, MaterialTheme.colorScheme.primary)
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    ShopHeader(shop, brand)
                    StepBar(vm.step)
                    AnimatedError(vm.error)
                    AnimatedContent(
                        targetState = vm.step,
                        transitionSpec = {
                            val forward = targetState.ordinal > initialState.ordinal
                            (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { w -> if (forward) w / 10 else -w / 10 }) togetherWith
                                fadeOut(tween(120))
                        },
                        label = "step",
                    ) { step ->
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            when (step) {
                                Step.UPLOAD -> UploadStep(shop, vm)
                                Step.OPTIONS -> OptionsStep(vm)
                                Step.REVIEW -> ReviewStep(shop, vm)
                                Step.DONE -> Unit
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun StepBar(step: Step) {
    val steps = listOf(Step.UPLOAD to "Upload", Step.OPTIONS to "Options", Step.REVIEW to "Review")
    val current = steps.indexOfFirst { it.first == step }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        steps.forEachIndexed { i, (_, label) ->
            val active = i == current
            val done = i < current
            Surface(
                shape = CircleShape,
                color = when {
                    active -> LocalQpExtras.current.panel
                    done -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else -> Color.Transparent
                },
                modifier = if (!active && !done) Modifier.glass(CircleShape, elevation = 3.dp) else Modifier,
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (done) Icon(Icons.Filled.Check, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    else Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            active -> Color.White
                            done -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            if (i < steps.lastIndex) Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
fun ShopHeader(shop: Shop, brand: Color) {
    BrandPanel(glow = brand) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(shop.name, color = Color.White, style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(shop.welcomeMessage ?: "Upload your documents for printing", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
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
