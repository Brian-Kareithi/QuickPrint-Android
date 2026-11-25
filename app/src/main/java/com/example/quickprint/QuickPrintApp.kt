package com.example.quickprint

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.quickprint.ui.home.HomeScreen
import com.example.quickprint.ui.shop.ShopScreen

object Routes {
    const val HOME = "home"
    const val SHOP = "shop/{slug}"
    fun shop(slug: String) = "shop/$slug"
}

/**
 * @param deepLinkSlug a shop opened from a QuickPrint link/QR outside the app.
 * @param onDeepLinkHandled called once the deep link has been navigated to.
 */
@Composable
fun QuickPrintApp(deepLinkSlug: String?, onDeepLinkHandled: () -> Unit) {
    val nav = rememberNavController()

    LaunchedEffect(deepLinkSlug) {
        if (deepLinkSlug != null) {
            nav.navigate(Routes.shop(deepLinkSlug)) { launchSingleTop = true }
            onDeepLinkHandled()
        }
    }

    // Surface (not Box) so all text defaults to the theme's onBackground colour in light and dark mode.
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        NavHost(navController = nav, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                HomeScreen(onOpenShop = { slug -> nav.navigate(Routes.shop(slug)) { launchSingleTop = true } })
            }
            composable(Routes.SHOP) { entry ->
                val slug = entry.arguments?.getString("slug").orEmpty()
                ShopScreen(
                    slug = slug,
                    onBack = { if (!nav.popBackStack()) nav.navigate(Routes.HOME) },
                    onHome = { nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } },
                )
            }
        }
    }
}
