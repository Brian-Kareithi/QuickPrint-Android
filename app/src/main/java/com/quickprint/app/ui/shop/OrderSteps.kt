package com.quickprint.app.ui.shop

import com.quickprint.app.ui.components.glass
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.quickprint.app.ui.components.FadeUp
import com.quickprint.app.ui.components.pressScale
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickprint.app.data.Choice
import com.quickprint.app.data.DEFAULT_EXTENSIONS
import com.quickprint.app.data.Payments
import com.quickprint.app.data.PlacedOrder
import com.quickprint.app.data.PrintChoices
import com.quickprint.app.data.Shop
import com.quickprint.app.data.UploadedFile
import com.quickprint.app.data.fileSize
import com.quickprint.app.data.money
import com.quickprint.app.ui.components.BrandPanel
import com.quickprint.app.ui.components.Card
import com.quickprint.app.ui.components.TermsLink
import com.quickprint.app.ui.theme.LocalQpExtras

private val ButtonShape = RoundedCornerShape(14.dp)

// ---------------------------------------------------------------- Upload

@Composable
fun UploadStep(shop: Shop, vm: OrderViewModel) {
    val mimeTypes = shop.allowedMimeTypes.ifEmpty { listOf("*/*") }.toTypedArray()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris -> vm.addFiles(uris) }
    val busy = vm.uploading != null
    val extensions = shop.allowedExtensions.ifEmpty { DEFAULT_EXTENSIONS }

    Surface(
        onClick = { if (!busy) picker.launch(mimeTypes) },
        enabled = !busy,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = if (vm.files.isEmpty()) 32.dp else 18.dp, horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(if (vm.files.isEmpty()) "Tap to choose documents" else "Add more files", style = MaterialTheme.typography.titleSmall)
            if (vm.files.isEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(extensions.joinToString(", ") { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your files go straight to ${shop.name} and are permanently deleted about an hour after your order is ready.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    vm.uploading?.let { u ->
        Card {
            Text("Uploading ${u.name}", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { u.progress }, modifier = Modifier.fillMaxWidth())
        }
    }

    vm.files.forEachIndexed { i, f -> key(f.id) { FadeUp(i) { FileRow(f, onRemove = { vm.removeFile(f.id) }) } } }

    if (vm.files.isNotEmpty()) {
        Button(onClick = { vm.error = null; vm.step = Step.OPTIONS }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(50.dp), shape = ButtonShape) {
            Text("Continue (${vm.files.size} file${if (vm.files.size > 1) "s" else ""})")
        }
    }

    Text(
        "By uploading you agree to our",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
    TermsLink(Modifier.fillMaxWidth())
}

@Composable
private fun FileRow(f: UploadedFile, onRemove: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color.Transparent, modifier = Modifier.glass(RoundedCornerShape(20.dp), elevation = 4.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FileBadge(f.name)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(f.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${fileSize(f.size)} · ${f.options.pageCount} page${if (f.options.pageCount > 1) "s" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, "Remove ${f.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun FileBadge(name: String) {
    val ext = name.substringAfterLast('.', "").uppercase().take(4).ifEmpty { "FILE" }
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), modifier = Modifier.size(40.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(ext, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

// ---------------------------------------------------------------- Options

@Composable
fun OptionsStep(vm: OrderViewModel) {
    vm.files.forEach { f ->
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FileBadge(f.name)
                Spacer(Modifier.width(10.dp))
                Text(f.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stepper("Pages in document", f.options.pageCount, Modifier.weight(1f)) { v -> vm.updateOptions(f.id) { it.copy(pageCount = v) } }
                Stepper("Copies", f.options.copies, Modifier.weight(1f)) { v -> vm.updateOptions(f.id) { it.copy(copies = v) } }
            }
            ChoiceRow("Color", PrintChoices.color, f.options.colorMode) { v -> vm.updateOptions(f.id) { it.copy(colorMode = v) } }
            ChoiceRow("Paper", PrintChoices.paper, f.options.paperSize) { v -> vm.updateOptions(f.id) { it.copy(paperSize = v) } }
            ChoiceRow("Layout", PrintChoices.layout, f.options.layout) { v -> vm.updateOptions(f.id) { it.copy(layout = v) } }
            ChoiceRow("Quality", PrintChoices.quality, f.options.printQuality) { v -> vm.updateOptions(f.id) { it.copy(printQuality = v) } }
            ChoiceRow("Binding", PrintChoices.binding, f.options.bindingType) { v -> vm.updateOptions(f.id) { it.copy(bindingType = v) } }
            ChoiceRow("Finishing", PrintChoices.finishing, f.options.finishingType) { v -> vm.updateOptions(f.id) { it.copy(finishingType = v) } }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { vm.back() }, modifier = Modifier.weight(1f).height(50.dp), shape = ButtonShape) { Text("Back") }
        Button(onClick = { vm.review() }, enabled = !vm.busy, modifier = Modifier.weight(2f).height(50.dp), shape = ButtonShape) {
            if (vm.busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text("Review order")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceRow(label: String, choices: List<Choice>, selected: String, onSelect: (String) -> Unit) {
    Spacer(Modifier.height(10.dp))
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEach { c ->
            FilterChip(
                selected = c.value == selected,
                onClick = { onSelect(c.value) },
                label = { Text(c.label) },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = LocalQpExtras.current.panel,
                    selectedLabelColor = Color.White,
                ),
            )
        }
    }
}

@Composable
private fun Stepper(label: String, value: Int, modifier: Modifier = Modifier, onChange: (Int) -> Unit) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Surface(shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), color = MaterialTheme.colorScheme.surface) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onChange((value - 1).coerceAtLeast(1)) }, enabled = value > 1) { Text("−", fontSize = 20.sp) }
                Text("$value", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { onChange((value + 1).coerceAtMost(9999)) }) { Text("+", fontSize = 20.sp) }
            }
        }
    }
}

// ---------------------------------------------------------------- Review

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewStep(shop: Shop, vm: OrderViewModel) {
    Card {
        Text("Order summary", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        vm.files.forEachIndexed { i, f ->
            if (i > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row {
                Text("${f.name} (${f.options.copies}×)", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                Text(if (f.options.colorMode == "FULL_COLOR") "Color" else "B&W", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    vm.pricing?.let { p ->
        Card {
            PriceLine("Printing", money(p.printingCost, shop.currency))
            PriceLine("Binding", money(p.bindingCost, shop.currency))
            PriceLine("Finishing", money(p.finishingCost, shop.currency))
            PriceLine("Tax (VAT)", money(p.taxAmount, shop.currency))
            PriceLine("Service fee", money(p.serviceFee, shop.currency))
            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row {
                Text("Total", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(money(p.totalAmount, shop.currency), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    Card {
        Text("Your details", style = MaterialTheme.typography.titleSmall)
        Text("Optional, but they help the shop reach you about your order.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Field("Name", vm.details.name, KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)) { vm.details = vm.details.copy(name = it) }
        Field("Email", vm.details.email, KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)) { vm.details = vm.details.copy(email = it) }
        Field("Phone", vm.details.phone, KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)) { vm.details = vm.details.copy(phone = it) }
        Field("Note for the shop", vm.details.notes, KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), singleLine = false) { vm.details = vm.details.copy(notes = it.take(500)) }

        Spacer(Modifier.height(10.dp))
        Text("Payment method", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Payments.options(shop).forEach { c ->
                FilterChip(
                    selected = vm.details.paymentMethod == c.value,
                    onClick = { vm.details = vm.details.copy(paymentMethod = c.value) },
                    label = { Text(c.label) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LocalQpExtras.current.panel, selectedLabelColor = Color.White),
                )
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { vm.back() }, modifier = Modifier.weight(1f).height(52.dp), shape = ButtonShape) { Text("Back") }
        Button(onClick = { vm.placeOrder() }, enabled = !vm.busy, modifier = Modifier.weight(2f).height(52.dp), shape = ButtonShape) {
            if (vm.busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text("Place order · ${money(vm.pricing?.totalAmount ?: 0.0, shop.currency)}", maxLines = 1)
        }
    }
}

@Composable
private fun PriceLine(label: String, value: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Field(label: String, value: String, keyboard: KeyboardOptions, singleLine: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        keyboardOptions = keyboard,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    )
}

// ---------------------------------------------------------------- Done

@Composable
fun DoneStep(shop: Shop, order: PlacedOrder, vm: OrderViewModel, onHome: () -> Unit, onChat: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                // The tick springs in once.
                val tick = remember { Animatable(0.4f) }
                LaunchedEffect(Unit) { tick.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
                Surface(shape = CircleShape, color = LocalQpExtras.current.success.copy(alpha = 0.12f), modifier = Modifier.size(64.dp).graphicsLayer { scaleX = tick.value; scaleY = tick.value }) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Check, null, tint = LocalQpExtras.current.success, modifier = Modifier.size(32.dp)) }
                }
                Spacer(Modifier.height(12.dp))
                Text("Order placed!", style = MaterialTheme.typography.titleLarge)
                Text(order.orderNumber, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("Show this number at the counter", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        PriceLine("Total", money(order.totalAmount, shop.currency))
                        PriceLine("Status", order.status.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() })
                        order.readyInMinutes?.let { PriceLine("Ready in about", "$it min") }
                    }
                }
            }
        }

        BrandPanel {
            Text("How to pay", color = Color.White, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Payments.instructions(shop, order.paymentMethod ?: vm.details.paymentMethod, order.totalAmount).forEach {
                Text(it, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(6.dp))
            Text("Show your order number to the cashier when you pay.", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
        }

        Text(
            "Your files are permanently deleted about an hour after your order is ready.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        val chatPress = remember { MutableInteractionSource() }
        Surface(
            onClick = onChat,
            interactionSource = chatPress,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth().pressScale(chatPress),
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("💬", fontSize = 22.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Message ${shop.name}", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Sent the wrong file or want to cancel? Chat with the shop.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        OutlinedButton(onClick = { vm.startOver() }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = ButtonShape) { Text("Send more files to ${shop.name}", maxLines = 1, overflow = TextOverflow.Ellipsis) }
        Button(onClick = onHome, modifier = Modifier.fillMaxWidth().height(50.dp), shape = ButtonShape) { Text("Done") }
    }
}

// ---------------------------------------------------------------- Closed

@Composable
fun ClosedState(shop: Shop, onRetry: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BrandPanel(glow = brandColor(shop.primaryColor, MaterialTheme.colorScheme.primary)) {
            Text(shop.name.uppercase(), color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
            Text("We're closed right now.", color = Color.White, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "New print orders are paused. Come back a little later and you can send your files straight from your phone.",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(14.dp))
            shop.phone?.let { ContactRow(Icons.Filled.Call, it) { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$it"))) } }
            shop.email?.let { ContactRow(Icons.Filled.Email, it) { runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$it"))) } } }
            shop.address?.let { ContactRow(Icons.Filled.LocationOn, it, null) }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = ButtonShape,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = com.quickprint.app.ui.theme.Ink),
            ) {
                Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Check again")
            }
        }
    }
}

@Composable
private fun ContactRow(icon: ImageVector, label: String, onClick: (() -> Unit)?) {
    val content = @Composable {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
        }
    }
    val shape = RoundedCornerShape(16.dp)
    val bg = Color.White.copy(alpha = 0.06f)
    Box(Modifier.padding(vertical = 4.dp)) {
        if (onClick != null) Surface(onClick = onClick, shape = shape, color = bg, modifier = Modifier.fillMaxWidth()) { content() }
        else Surface(shape = shape, color = bg, modifier = Modifier.fillMaxWidth()) { content() }
    }
}
