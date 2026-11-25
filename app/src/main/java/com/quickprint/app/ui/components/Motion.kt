package com.quickprint.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quickprint.app.ui.theme.LocalQpExtras

@Composable
fun FadeUp(index: Int = 0, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        modifier = modifier,
        enter = fadeIn(tween(280, delayMillis = 35 * index.coerceAtMost(10))) +
            slideInVertically(tween(320, delayMillis = 35 * index.coerceAtMost(10))) { it / 6 },
    ) { content() }
}

fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "press")
    graphicsLayer { scaleX = scale; scaleY = scale }
}

@Composable
fun AnimatedError(message: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = fadeIn(tween(200)) + expandVertically(tween(220)),
        exit = fadeOut(tween(150)) + shrinkVertically(tween(180)),
    ) {
        val last = remember { arrayOf("") }
        if (message != null) last[0] = message
        ErrorBanner(last[0])
    }
}

fun Modifier.skeleton(shape: Shape = RoundedCornerShape(12.dp), onPanel: Boolean = false): Modifier = composed {
    val base = if (onPanel) Color.White.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant
    val transition = rememberInfiniteTransition(label = "skeleton")
    if (onPanel) {
        val alpha by transition.animateFloat(1f, 0.55f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "pulse")
        clip(shape).background(base.copy(alpha = base.alpha * alpha))
    } else {
        val x by transition.animateFloat(-1f, 2f, infiniteRepeatable(tween(1400)), label = "shimmer")
        clip(shape).background(base).background(
            Brush.linearGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = 0.6f), Color.Transparent),
                start = androidx.compose.ui.geometry.Offset(x * 600f, 0f),
                end = androidx.compose.ui.geometry.Offset(x * 600f + 400f, 0f),
            ),
        )
    }
}

@Composable
fun Bone(width: Dp? = null, height: Dp, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(12.dp), onPanel: Boolean = false) {
    Box((if (width != null) modifier.width(width) else modifier.fillMaxWidth()).height(height).skeleton(shape, onPanel))
}

@Composable
fun ShopSkeleton() {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(LocalQpExtras.current.panel).padding(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Bone(200.dp, 28.dp, onPanel = true)
                Bone(240.dp, 12.dp, shape = CircleShape, onPanel = true)
                Bone(140.dp, 12.dp, shape = CircleShape, onPanel = true)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(3) { i -> if (i > 0) Spacer(Modifier.width(8.dp)); Bone(86.dp, 30.dp, shape = CircleShape) }
        }
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).padding(vertical = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Bone(52.dp, 52.dp, shape = RoundedCornerShape(16.dp))
                Bone(180.dp, 14.dp, shape = CircleShape)
                Bone(220.dp, 10.dp, shape = CircleShape)
            }
        }
    }
}

@Composable
fun ChatSkeleton() {
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Bone(240.dp, 22.dp, Modifier.align(Alignment.CenterHorizontally), CircleShape)
        listOf(false to 220.dp, true to 160.dp, false to 250.dp, true to 120.dp).forEach { (mine, w) ->
            Bone(
                w, 40.dp,
                Modifier.align(if (mine) Alignment.End else Alignment.Start),
                RoundedCornerShape(18.dp, 18.dp, if (mine) 4.dp else 18.dp, if (mine) 18.dp else 4.dp),
            )
        }
    }
}

fun Modifier.glass(
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color? = null,
    alpha: Float? = null,
    elevation: Dp = 10.dp,
): Modifier = composed {
    val base = tint ?: MaterialTheme.colorScheme.surface
    val a = alpha ?: 0.7f
    val edge = Color.White.copy(alpha = if (tint != null) 0.16f else 0.7f)
    this
        .shadow(elevation, shape, ambientColor = Color(0x33003249), spotColor = Color(0x33003249))
        .clip(shape)
        .background(base.copy(alpha = a))
        .border(1.dp, edge, shape)
}

@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "ambient")
    val a by t.animateFloat(0f, 1f, infiniteRepeatable(tween(26_000), RepeatMode.Reverse), label = "a")
    val b by t.animateFloat(0f, 1f, infiniteRepeatable(tween(32_000), RepeatMode.Reverse), label = "b")
    androidx.compose.foundation.Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val r1 = maxOf(w, h) * 0.62f
        val c1 = androidx.compose.ui.geometry.Offset(w * (0.95f - 0.12f * a), h * (0.05f + 0.06f * a))
        drawCircle(
            Brush.radialGradient(listOf(Color(0x4D007EA7), Color(0x339AD1D4), Color.Transparent), center = c1, radius = r1),
            radius = r1, center = c1,
        )
        val r2 = maxOf(w, h) * 0.55f
        val c2 = androidx.compose.ui.geometry.Offset(w * (0.02f + 0.12f * b), h * (0.98f - 0.07f * b))
        drawCircle(
            Brush.radialGradient(listOf(Color(0x599AD1D4), Color(0x1A003249), Color.Transparent), center = c2, radius = r2),
            radius = r2, center = c2,
        )
    }
}
