package io.github.omeg4dev.greatuntis.watch

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import io.github.omeg4dev.greatuntis.GreatUntisApp
import io.github.omeg4dev.greatuntis.MainActivity
import io.github.omeg4dev.greatuntis.R
import io.github.omeg4dev.greatuntis.data.UntisRepository
import io.github.omeg4dev.greatuntis.model.LessonStatus
import io.github.omeg4dev.greatuntis.model.Settings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class WatchService : LifecycleService() {

    private var loopJob: Job? = null
    @Volatile private var force: Boolean = false
    private var lastPollTime: LocalTime? = null
    private var lastSummary: PollSummary? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        val notification = buildForegroundNotification("Initialisiere…")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.getBooleanExtra("force", false) == true) {
            force = true
        }

        if (intent?.action == ACTION_POLL_NOW) {
            lifecycleScope.launch {
                runPollAndUpdate()
            }
        }

        if (loopJob == null || loopJob?.isActive == false) {
            startLoop()
        }

        return START_STICKY
    }

    private fun startLoop() {
        loopJob = lifecycleScope.launch {
            var backoff = 0
            while (isActive) {
                val app = applicationContext as? GreatUntisApp
                val repo = app?.repo
                val settings = repo?.settings?.value ?: Settings()
                val now = LocalDateTime.now()

                if (!force) {
                    val schedule = WatchScheduler.computeSchedule(
                        now = now,
                        settings = settings,
                        days = repo?.days?.value ?: emptyList(),
                        holidays = repo?.masterData?.value?.holidays ?: emptyList(),
                    )
                    if (!schedule.isInsideWindow) {
                        stopSelf()
                        WatchScheduler.reschedule(applicationContext)
                        break
                    }
                }

                if (hasValidatedNetwork()) {
                    val summary = Poller.pollOnce(applicationContext)
                    lastSummary = summary
                    lastPollTime = LocalTime.now()

                    if (summary.success) {
                        backoff = 0
                    } else {
                        backoff = when (backoff) {
                            0 -> 60
                            60 -> 120
                            120 -> 240
                            else -> 300
                        }
                    }
                }

                updateForegroundNotification()

                val delaySeconds = if (backoff > 0) backoff else settings.pollSeconds
                delay(delaySeconds * 1000L)
            }
        }
    }

    private suspend fun runPollAndUpdate() {
        if (hasValidatedNetwork()) {
            val summary = Poller.pollOnce(applicationContext)
            lastSummary = summary
            lastPollTime = LocalTime.now()
            updateForegroundNotification()
        }
    }

    private fun hasValidatedNetwork(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun updateForegroundNotification() {
        val app = applicationContext as? GreatUntisApp
        val repo = app?.repo ?: return
        val text = formatStatusText(repo, lastPollTime, lastSummary)
        val notification = buildForegroundNotification(text)
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Notifications disabled
        }
    }

    private fun buildForegroundNotification(statusText: String): Notification {
        val pollIntent = Intent(this, WatchService::class.java).apply {
            action = ACTION_POLL_NOW
        }
        val pollPendingIntent = PendingIntent.getService(
            this,
            1,
            pollIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val openIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("screen", "heute")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, Notifier.CHANNEL_WATCH)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle("Überwachung aktiv")
            .setContentText(statusText)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .addAction(0, "Jetzt prüfen", pollPendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        loopJob?.cancel()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_POLL_NOW = "io.github.omeg4dev.greatuntis.POLL_NOW"
        @Volatile var isRunning: Boolean = false

        fun formatStatusText(repo: UntisRepository, lastPollTime: LocalTime?, lastSummary: PollSummary?): String {
            val now = LocalTime.now()
            val today = LocalDate.now()
            val todayLessons = repo.days.value.firstOrNull { it.date == today }?.lessons ?: emptyList()
            val upcoming = todayLessons
                .filter { it.status != LessonStatus.CANCELLED && it.end > now }
                .minByOrNull { it.start }

            if (upcoming != null) {
                val roomName = upcoming.rooms.firstOrNull()?.let {
                    it.current?.shortName ?: it.removed?.shortName
                } ?: "Foyer"
                val timeStr = upcoming.start.format(DateTimeFormatter.ofPattern("HH:mm"))
                return "Als Nächstes: ${upcoming.subject} · $roomName · $timeStr"
            }

            val timeStr = (lastPollTime ?: now).format(DateTimeFormatter.ofPattern("HH:mm"))
            return if (lastSummary != null && lastSummary.activeChanges > 0) {
                "Letzte Prüfung $timeStr · ${lastSummary.activeChanges} Änderungen"
            } else {
                "Letzte Prüfung $timeStr · keine Änderungen"
            }
        }
    }
}
