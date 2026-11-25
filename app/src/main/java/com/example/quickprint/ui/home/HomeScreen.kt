package com.example.quickprint.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quickprint.data.Prefs
import com.example.quickprint.data.ShopLink
import com.example.quickprint.data.ShopLinks
import com.example.quickprint.data.ShopRepository
import com.example.quickprint.ui.components.BrandPanel
import com.example.quickprint.ui.components.Card
import com.example.quickprint.ui.components.ErrorBanner
import com.example.quickprint.ui.components.QrGlyph
import com.example.quickprint.ui.components.TermsLink
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(onOpenShop: (slug: String) -> Unit) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val prefs = remember { Prefs(context) }
    val recent = remember { prefs.recentShop }

    var code by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun resolve(link: ShopLink) {
        error = null
        when (link) {
            is ShopLink.Slug -> onOpenShop(link.slug)
            is ShopLink.Code -> {
                busy = true
                scope.launch {
                    try {
                        onOpenShop(ShopRepository.slugForCode(link.code))
                    } catch (e: Exception) {
                        error = e.message ?: "Could not find that shop"
                    } finally {
                        busy = false
                    }
                }
            }
        }
    }

    fun connect() {
        focus.clearFocus()
        if (code.length != 5) { error = "The shop code is 5 characters."; return }
        resolve(ShopLink.Code(code.uppercase()))
    }

    fun scan() {
        error = null
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options).startScan()
            .addOnSuccessListener { barcode ->
                val link = ShopLinks.parse(barcode.rawValue)
                if (link == null) error = "That QR code isn't a QuickPrint shop code. Scan the code on the shop's poster."
                else resolve(link)
            }
            .addOnFailureListener {
                error = "The QR scanner couldn't start. Make sure Google Play services is up to date, or enter the shop code instead."
            }
    }

    Box(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BrandPanel {
                Text("QUICKPRINT", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                Text("Print from your phone.", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Connect to a cyber or print shop, send your documents, and collect your prints. No account needed.",
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Card {
                Text("Enter the shop code", style = MaterialTheme.typography.titleMedium)
                Text(
                    "The 5-character code is on the shop's counter poster, or ask the attendant.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { v -> code = v.filter { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' }.take(5); error = null },
                    visualTransformation = UpperCaseTransformation,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("K7M2P", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = codeStyle(), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
                    textStyle = codeStyle(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Go, autoCorrectEnabled = false),
                    keyboardActions = KeyboardActions(onGo = { connect() }),
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { connect() }, enabled = !busy && code.length == 5, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) {
                    if (busy) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(10.dp)); Text("Checking...")
                    } else Text("Connect")
                }

                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(Modifier.weight(1f))
                    Text("  or  ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(Modifier.weight(1f))
                }

                OutlinedButton(onClick = { scan() }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) {
                    QrGlyph(MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text("Scan the shop's QR code")
                }
            }

            error?.let { ErrorBanner(it) }

            if (recent != null) {
                Surface(
                    onClick = { onOpenShop(recent.slug) },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).padding(0.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Last shop you used", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(recent.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            recent.address?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Open", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Your files go only to the shop you choose and are deleted automatically after your order.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                TermsLink()
            }
        }
    }
}

@Composable
private fun codeStyle() = TextStyle(
    fontSize = 28.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 10.sp,
    textAlign = TextAlign.Center,
    color = MaterialTheme.colorScheme.onSurface,
)

/** Shows the code in capitals without rewriting what the keyboard is composing. */
private object UpperCaseTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString) = TransformedText(AnnotatedString(text.text.uppercase()), OffsetMapping.Identity)
}
