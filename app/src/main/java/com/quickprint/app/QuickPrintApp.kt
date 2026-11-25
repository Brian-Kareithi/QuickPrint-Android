package com.quickprint.app

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.quickprint.app.ui.chat.ChatScreen
import com.quickprint.app.ui.components.AmbientBackdrop
import com.quickprint.app.ui.home.HomeScreen
import com.quickprint.app.ui.shop.ShopScreen

object Routes {
    const val HOME = "home"
    const val SHOP = "shop/{slug}"
    fun shop(slug: String) = "shop/$slug"
    const val CHAT = "chat/{shopId}?shopName={shopName}&order={order}&name={name}"
    fun chat(shopId: String, shopName: String, order: String? = null, name: String? = null) =
        "chat/${Uri.encode(shopId)}?shopName=${Uri.encode(shopName)}&order=${Uri.encode(order.orEmpty())}&name=${Uri.encode(name.orEmpty())}"
}

/** Something outside the app asked us to open a screen. */
sealed interface AppLink {
    /** A QuickPrint shop link or QR code (https://…/shop/<slug>). */
    data class Shop(val slug: String) : AppLink
    /** A tapped "new message" notification. */
    data class Chat(val shopId: String, val shopName: String) : AppLink
}

/** Content never gets wider than this, so tablets and landscape phones stay readable. */
private val MaxContentWidth = 640.dp
private const val NAV_MS = 260

@Composable
fun QuickPrintApp(link: AppLink?, onLinkHandled: () -> Unit) {
    val nav = rememberNavController()

    LaunchedEffect(link) {
        when (link) {
            is AppLink.Shop -> nav.navigate(Routes.shop(link.slug)) { launchSingleTop = true }
            is AppLink.Chat -> nav.navigate(Routes.chat(link.shopId, link.shopName)) { launchSingleTop = true }
            null -> return@LaunchedEffect
        }
        onLinkHandled()
    }

    // Surface (not Box) so all text defaults to the theme's onBackground colour in light and dark mode.
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            AmbientBackdrop()
            NavHost(
                navController = nav,
                startDestination = Routes.HOME,
                modifier = Modifier.widthIn(max = MaxContentWidth).fillMaxSize(),
                // Subtle: a short slide (1/8 of the width) with a cross-fade.
                enterTransition = { slideIntoContainer(SlideDirection.Start, tween(NAV_MS, easing = FastOutSlowInEasing)) { it / 8 } + fadeIn(tween(NAV_MS)) },
                exitTransition = { slideOutOfContainer(SlideDirection.Start, tween(NAV_MS, easing = FastOutSlowInEasing)) { it / 8 } + fadeOut(tween(NAV_MS / 2)) },
                popEnterTransition = { slideIntoContainer(SlideDirection.End, tween(NAV_MS, easing = FastOutSlowInEasing)) { it / 8 } + fadeIn(tween(NAV_MS)) },
                popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(NAV_MS, easing = FastOutSlowInEasing)) { it / 8 } + fadeOut(tween(NAV_MS / 2)) },
            ) {
                composable(Routes.HOME) {
                    HomeScreen(onOpenShop = { slug -> nav.navigate(Routes.shop(slug)) { launchSingleTop = true } })
                }
                composable(Routes.SHOP) { entry ->
                    val slug = entry.arguments?.getString("slug").orEmpty()
                    ShopScreen(
                        slug = slug,
                        onBack = { if (!nav.popBackStack()) nav.navigate(Routes.HOME) },
                        onHome = { nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } },
                        onChat = { shopId, shopName, order, name -> nav.navigate(Routes.chat(shopId, shopName, order, name)) { launchSingleTop = true } },
                    )
                }
                composable(Routes.CHAT) { entry ->
                    val args = entry.arguments
                    ChatScreen(
                        shopId = args?.getString("shopId").orEmpty(),
                        shopName = args?.getString("shopName").orEmpty(),
                        orderNumber = args?.getString("order")?.takeIf { it.isNotBlank() },
                        customerName = args?.getString("name")?.takeIf { it.isNotBlank() },
                        onBack = { if (!nav.popBackStack()) nav.navigate(Routes.HOME) },
                    )
                }
            }
        }
    }
}
