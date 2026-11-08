package io.github.omeg4dev.greatuntis.watch

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.github.omeg4dev.greatuntis.GreatUntisApp
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.Holiday
import io.github.omeg4dev.greatuntis.model.LessonStatus
import io.github.omeg4dev.greatuntis.model.Settings
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

data class WindowSchedule(
    val isInsideWindow: Boolean,
    val windowToday: ClosedRange<LocalDateTime>?,
    val nextWindowStart: LocalDateTime,
)

object WatchScheduler {
    private const val ALARM_REQ_CODE = 2001
    private const val WORK_NAME_EVENING = "evening"

    fun isSchoolDay(date: LocalDate, holidays: List<Holiday>): Boolean {
        val dow = date.dayOfWeek
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return false
        }
        return holidays.none { date in it.start..it.end }
    }

    fun computeWindowToday(
        date: LocalDate,
        settings: Settings,
        days: List<Day>,
        holidays: List<Holiday>,
    ): ClosedRange<LocalDateTime>? {
        if (!isSchoolDay(date, holidays)) return null
        val day = days.firstOrNull { it.date == date }
        val effectiveLessons = day?.lessons?.filter { it.status != LessonStatus.CANCELLED } ?: emptyList()
        val endTime = if (effectiveLessons.isNotEmpty()) {
            effectiveLessons.maxOf { it.end }.plusMinutes(10)
        } else {
            LocalTime.of(16, 0)
        }
        return LocalDateTime.of(date, settings.schoolStart)..LocalDateTime.of(date, endTime)
    }

    fun computeNextWindowStart(
        now: LocalDateTime,
        settings: Settings,
        days: List<Day>,
        holidays: List<Holiday>,
    ): LocalDateTime {
        val today = now.toLocalDate()
        if (isSchoolDay(today, holidays)) {
            val todayStart = LocalDateTime.of(today, settings.schoolStart)
            if (now < todayStart) {
                return todayStart
            }
        }
        var candidate = today.plusDays(1)
        while (!isSchoolDay(candidate, holidays)) {
            candidate = candidate.plusDays(1)
        }
        return LocalDateTime.of(candidate, settings.schoolStart)
    }

    fun computeSchedule(
        now: LocalDateTime,
        settings: Settings,
        days: List<Day>,
        holidays: List<Holiday>,
    ): WindowSchedule {
        val today = now.toLocalDate()
        val windowToday = computeWindowToday(today, settings, days, holidays)
        val isInside = windowToday != null && now >= windowToday.start && now <= windowToday.endInclusive
        val nextStart = computeNextWindowStart(now, settings, days, holidays)
        return WindowSchedule(
            isInsideWindow = isInside,
            windowToday = windowToday,
            nextWindowStart = nextStart,
        )
    }

    fun reschedule(context: Context) {
        val app = context.applicationContext as? GreatUntisApp ?: return
        val repo = app.repo
        val settings = repo.settings.value
        val account = repo.account.value

        if (!settings.watchEnabled || account == null) {
            cancel(context)
            return
        }

        val now = LocalDateTime.now()
        val schedule = computeSchedule(
            now = now,
            settings = settings,
            days = repo.days.value,
            holidays = repo.masterData.value?.holidays ?: emptyList(),
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context,
            ALARM_REQ_CODE,
            Intent(context, WatchAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        if (schedule.isInsideWindow) {
            val serviceIntent = Intent(context, WatchService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
            alarmManager.cancel(pi)
        } else {
            context.stopService(Intent(context, WatchService::class.java))
            val triggerAtMillis = schedule.nextWindowStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } catch (e: SecurityException) {
                // If exact alarm permission missing fallback
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val eveningWork = PeriodicWorkRequestBuilder<EveningWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_EVENING,
            ExistingPeriodicWorkPolicy.UPDATE,
            eveningWork,
        )
    }

    fun cancel(context: Context) {
        context.stopService(Intent(context, WatchService::class.java))

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context,
            ALARM_REQ_CODE,
            Intent(context, WatchAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pi)

        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_EVENING)
    }
}
