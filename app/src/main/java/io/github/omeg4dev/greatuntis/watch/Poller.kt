package io.github.omeg4dev.greatuntis.watch

import android.content.Context
import android.util.Log
import io.github.omeg4dev.greatuntis.GreatUntisApp
import io.github.omeg4dev.greatuntis.model.Change
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.widget.WidgetUpdater
import java.time.LocalDateTime

data class PollSummary(
    val success: Boolean,
    val days: Int = 0,
    val activeChanges: Int = 0,
    val notifiedChanges: Int = 0,
    val durationMs: Long = 0,
    val errorClass: String? = null,
    val active: Map<String, Change> = emptyMap(),
)

object Poller {
    private const val TAG = "GreatUntis"

    suspend fun pollOnce(context: Context): PollSummary {
        val t0 = System.currentTimeMillis()
        val app = context.applicationContext as? GreatUntisApp
        if (app == null) {
            Log.i(TAG, "poll error IllegalStateException")
            return PollSummary(success = false, errorClass = "IllegalStateException")
        }
        val repo = app.repo

        val pollResult: Result<List<Day>> = repo.pollWindow()
        val elapsed = System.currentTimeMillis() - t0

        val windowDays = pollResult.getOrNull()
        if (windowDays == null) {
            val exception = pollResult.exceptionOrNull()
            val errName = exception?.let { it::class.java.simpleName } ?: "UnknownException"
            Log.i(TAG, "poll error $errName")
            return PollSummary(success = false, durationMs = elapsed, errorClass = errName)
        }

        val now = LocalDateTime.now()
        // Without notification permission nothing can be shown: keep the old state so the changes
        // are still "new" once the user allows notifications.
        if (!androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.i(TAG, "poll ok days=${windowDays.size} notifications disabled, state kept")
            return PollSummary(success = true, days = windowDays.size, durationMs = elapsed)
        }
        val firstRun = !WatchState.exists(context)
        val previous = WatchState.load(context)
        val settings = repo.settings.value
        val timeGrid = repo.masterData.value?.timeGrid ?: emptyList()

        val detectorResult = ChangeDetector(settings, timeGrid).detect(windowDays, now, previous)
        // First poll after login: only urgent changes; quiet ones become the silent baseline.
        val toNotify = if (firstRun) detectorResult.toNotify.filter { it.urgent } else detectorResult.toNotify
        // Changes that are gone while their lesson is still in the window were reverted: drop their notification.
        val windowKeys = windowDays.flatMap { day -> day.lessons.map { it.key } }.toSet()
        val reverted = previous.filterKeys { it !in detectorResult.active }
            .filterValues { it.lessonKey != null && it.lessonKey in windowKeys }.keys
        Notifier.cancelChanges(context, reverted)
        Notifier.notifyChanges(context, toNotify)
        WatchState.save(context, detectorResult.active)

        val summary = PollSummary(
            success = true,
            days = windowDays.size,
            activeChanges = detectorResult.active.size,
            notifiedChanges = toNotify.size,
            durationMs = elapsed,
            active = detectorResult.active,
        )

        WidgetUpdater.requestUpdate(context)

        Log.i(TAG, "poll ok days=${summary.days} changes=${summary.activeChanges} notified=${summary.notifiedChanges} ms=${summary.durationMs}")
        return summary
    }
}
