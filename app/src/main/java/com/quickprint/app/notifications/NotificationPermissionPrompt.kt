package com.quickprint.app.notifications

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

@Composable
fun NotificationPermissionPrompt(ask: Boolean) {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            ChatNotifications.ensureChannel(context)
            ChatChecks.boost(context)
        }
    }

    LaunchedEffect(ask) {
        if (!ask || Build.VERSION.SDK_INT < 33 || ChatNotifications.canNotify(context)) return@LaunchedEffect
        val prefs = context.getSharedPreferences("quickprint", Context.MODE_PRIVATE)
        if (prefs.getBoolean("asked_notifications", false)) return@LaunchedEffect
        prefs.edit { putBoolean("asked_notifications", true) }
        showRationale = true
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Get notified when the shop replies?") },
            text = { Text("We'll let you know when the shop answers your message, even if the app is closed. Nothing else.") },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text("Turn on") }
            },
            dismissButton = { TextButton(onClick = { showRationale = false }) { Text("Not now") } },
        )
    }
}
