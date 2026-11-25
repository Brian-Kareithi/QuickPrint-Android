package com.quickprint.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.quickprint.app.MainActivity
import com.quickprint.app.R
import com.quickprint.app.ui.theme.Ocean
import androidx.compose.ui.graphics.toArgb

/** Which shop's chat is on screen right now (no notification needed for it). */
object ChatVisibility {
    @Volatile var shopId: String? = null
}

object ChatNotifications {
    const val CHANNEL_ID = "chat_messages"
    const val EXTRA_SHOP_ID = "open_chat_shop_id"
    const val EXTRA_SHOP_NAME = "open_chat_shop_name"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Messages from shops", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Replies from print shops about your orders"
            },
        )
    }

    fun canNotify(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** "Campus Prints · You're Bubbly Apple" — tapping opens that chat. */
    fun show(context: Context, shopId: String, shopName: String, message: String, unread: Int, alias: String) {
        if (!canNotify(context)) return
        ensureChannel(context)
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SHOP_ID, shopId)
            putExtra(EXTRA_SHOP_NAME, shopName)
        }
        val pending = PendingIntent.getActivity(
            context, shopId.hashCode(), open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_quickprint)
            .setColor(Ocean.toArgb())
            .setContentTitle(shopName)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSubText(if (unread > 1) "$unread new messages" else alias.takeIf { it.isNotBlank() }?.let { "You're $it" })
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // heads-up popup (pre-Android 8 uses this)
            .setDefaults(NotificationCompat.DEFAULT_ALL)    // sound + vibration
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_quickprint)
                    .setContentTitle(shopName)
                    .setContentText("New message")
                    .build(),
            )
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(shopId.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call.
        }
    }

    fun cancel(context: Context, shopId: String) = NotificationManagerCompat.from(context).cancel(shopId.hashCode())
}
