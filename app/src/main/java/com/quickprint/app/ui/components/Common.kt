package com.quickprint.app.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quickprint.app.BuildConfig
import com.quickprint.app.ui.theme.Frost
import com.quickprint.app.ui.theme.Ink
import com.quickprint.app.ui.theme.LocalQpExtras

val TERMS_URL = BuildConfig.WEB_BASE_URL.trimEnd('/') + "/terms"

/** Opens a web page in a Chrome Custom Tab (falls back to any browser). */
fun openWeb(context: Context, url: String) {
    val uri = Uri.parse(url)
    try {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder().setToolbarColor(Ink.toArgb()).build())
            .build()
            .launchUrl(context, uri)
    } catch (_: ActivityNotFoundException) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
}

/** The deep-blue rounded header used across the website, with its soft glow. */
@Composable
fun BrandPanel(
    modifier: Modifier = Modifier,
    glow: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit,
) {
    val extras = LocalQpExtras.current
    val shape = RoundedCornerShape(24.dp)
    // Light mode keeps the website's soft glow; dark mode is flat (solid panel + hairline border).
    val surface = if (extras.isDark) {
        modifier.fillMaxWidth().clip(shape).background(extras.panel).border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    } else {
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(extras.panel)
            .background(Brush.radialGradient(listOf(glow.copy(alpha = 0.55f), Color.Transparent), center = Offset(900f, 0f), radius = 520f))
            .background(Brush.radialGradient(listOf(Frost.copy(alpha = 0.18f), Color.Transparent), center = Offset(0f, 600f), radius = 480f))
    }
    Box(surface) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    // Frosted "liquid glass" card (see Modifier.glass).
    Column(modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp), alpha = if (LocalQpExtras.current.isDark) 0.9f else 0.86f, elevation = 6.dp).padding(18.dp), content = content)
}

@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    val err = MaterialTheme.colorScheme.error
    Text(
        message,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(err.copy(alpha = 0.10f))
            .border(1.dp, err.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        color = err,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
fun TermsLink(modifier: Modifier = Modifier, label: String = "Terms & Privacy Policy") {
    val context = LocalContext.current
    TextButton(onClick = { openWeb(context, TERMS_URL) }, modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}

/** Simple QR-code glyph (Material's core icon set has none). */
@Composable
fun QrGlyph(color: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val box = s * 0.38f
        val stroke = s * 0.09f
        val r = CornerRadius(s * 0.06f)
        listOf(Offset(0f, 0f), Offset(s - box, 0f), Offset(0f, s - box)).forEach { o ->
            drawRoundRect(color, o + Offset(stroke / 2, stroke / 2), Size(box - stroke, box - stroke), r, style = Stroke(stroke))
            drawRect(color, o + Offset(box * 0.32f, box * 0.32f), Size(box * 0.36f, box * 0.36f))
        }
        val d = s * 0.14f
        drawRect(color, Offset(s - box + d * 0.2f, s - box + d * 0.2f), Size(d, d))
        drawRect(color, Offset(s - d, s - d), Size(d, d))
        drawRect(color, Offset(s - box + d * 1.4f, s - d * 1.6f), Size(d, d))
    }
}

@Composable
fun CenteredBox(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) =
    Box(modifier, contentAlignment = androidx.compose.ui.Alignment.Center, content = content)
