package io.github.omeg4dev.greatuntis.watch

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.omeg4dev.greatuntis.GreatUntisApp
import java.time.LocalDateTime

class EveningWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? GreatUntisApp ?: return Result.success()
        val repo = app.repo
        val settings = repo.settings.value
        if (!settings.watchEnabled || repo.account.value == null) return Result.success()

        val now = LocalDateTime.now()
        val nowTime = now.toLocalTime()
        val isEvening = nowTime >= settings.eveningStart && nowTime <= settings.eveningEnd

        val schedule = WatchScheduler.computeSchedule(
            now,
            settings,
            repo.days.value,
            repo.masterData.value?.holidays ?: emptyList(),
        )

        if (isEvening) {
            Poller.pollOnce(applicationContext)
        } else if (schedule.isInsideWindow) {
            if (!WatchService.isRunning) {
                WatchScheduler.reschedule(applicationContext)
            }
        }
        return Result.success()
    }
}
