package io.github.omeg4dev.greatuntis.watch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.omeg4dev.greatuntis.MainActivity
import io.github.omeg4dev.greatuntis.R
import io.github.omeg4dev.greatuntis.model.Change

object Notifier {
    const val CHANNEL_URGENT = "urgent"
    const val CHANNEL_INFO = "info"
    const val CHANNEL_WATCH = "watch"
    const val GROUP_CHANGES = "changes"
    const val SUMMARY_ID = 42

    fun createChannels(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val urgentChannel = NotificationChannel(
            CHANNEL_URGENT,
            "Dringende Änderungen",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            vibrationPattern = longArrayOf(0, 250, 150, 250)
            enableVibration(true)
            enableLights(true)
        }

        val infoChannel = NotificationChannel(
            CHANNEL_INFO,
            "Weitere Änderungen",
            NotificationManager.IMPORTANCE_DEFAULT,
        )

        val watchChannel = NotificationChannel(
            CHANNEL_WATCH,
            "Überwachung",
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            setShowBadge(false)
        }

        notificationManager.createNotificationChannels(listOf(urgentChannel, infoChannel, watchChannel))
    }

    /** Removes posted notifications of changes that are no longer true (e.g. Foyer reverted). */
    fun cancelChanges(context: Context, fingerprints: Collection<String>) {
        val notificationManager = NotificationManagerCompat.from(context)
        for (fp in fingerprints) notificationManager.cancel(fp.hashCode())
    }

    fun notifyChanges(context: Context, changes: List<Change>) {
        if (changes.isEmpty()) return
        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) return

        for (change in changes) {
            val channelId = if (change.urgent) CHANNEL_URGENT else CHANNEL_INFO
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("screen", "heute")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                change.fingerprint.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle(change.title)
                .setContentText(change.text)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setGroup(GROUP_CHANGES)

            if (change.urgent) {
                builder.setCategory(NotificationCompat.CATEGORY_REMINDER)
            }

            try {
                notificationManager.notify(change.fingerprint.hashCode(), builder.build())
            } catch (e: SecurityException) {
                // Ignore if notifications denied
            }
        }

        if (changes.size >= 2) {
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("screen", "heute")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                SUMMARY_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val summary = NotificationCompat.Builder(context, CHANNEL_INFO)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle("Stundenplanänderungen")
                .setContentText("${changes.size} neue Änderungen")
                .setGroup(GROUP_CHANGES)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            try {
                notificationManager.notify(SUMMARY_ID, summary)
            } catch (e: SecurityException) {
                // Ignore
            }
        }
    }
}
