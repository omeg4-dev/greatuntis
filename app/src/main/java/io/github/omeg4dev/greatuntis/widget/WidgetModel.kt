package io.github.omeg4dev.greatuntis.widget

import io.github.omeg4dev.greatuntis.calendar.FreeCalendar
import io.github.omeg4dev.greatuntis.calendar.FreeKind
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.DayStatus
import io.github.omeg4dev.greatuntis.model.TimeUnit
import io.github.omeg4dev.greatuntis.timetable.DayAnalysis
import io.github.omeg4dev.greatuntis.timetable.FreeReason
import io.github.omeg4dev.greatuntis.timetable.LessonKind
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

sealed interface WidgetState {
    data class Lesson(
        val label: String,
        val title: String,
        val room: String,
        val timeText: String,
        val note: String? = null,
    ) : WidgetState

    data class Free(
        val title: String,
        val subtitle: String,
        val isHoliday: Boolean = false,
    ) : WidgetState

    data class Done(
        val nextText: String,
    ) : WidgetState

    data object NoData : WidgetState
}

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun formatGermanDay(date: LocalDate): String {
    val dow = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "Mo"
        DayOfWeek.TUESDAY -> "Di"
        DayOfWeek.WEDNESDAY -> "Mi"
        DayOfWeek.THURSDAY -> "Do"
        DayOfWeek.FRIDAY -> "Fr"
        DayOfWeek.SATURDAY -> "Sa"
        DayOfWeek.SUNDAY -> "So"
        null -> ""
    }
    val formatter = DateTimeFormatter.ofPattern("dd.MM.")
    return "$dow ${date.format(formatter)}"
}

fun ferienCountdown(today: LocalDate, calendar: FreeCalendar): String? {
    val nextFerien = calendar.nextFerien(today) ?: return null
    val schoolDays = calendar.schoolDaysBetween(today, nextFerien.start)
    val unitText = if (schoolDays == 1) "Schultag" else "Schultage"
    return "Noch $schoolDays $unitText bis ${nextFerien.title}"
}

fun ferien(today: LocalDate, calendar: FreeCalendar): String? = ferienCountdown(today, calendar)

fun widgetState(
    now: LocalDateTime,
    days: List<Day>,
    grid: List<TimeUnit>,
    foyerPrefix: String,
    calendar: FreeCalendar,
): WidgetState {
    val todayDate = now.toLocalDate()
    val currentTime = now.toLocalTime()

    val todayDay = days.firstOrNull { it.date == todayDate }
    val freeDay = calendar.freeDay(todayDate)

    val plan = if (todayDay != null) {
        DayAnalysis.analyze(todayDay, grid, foyerPrefix)
    } else {
        null
    }

    val isFreeByCalendar = freeDay != null
    val isWholeDayFree = plan?.wholeDayFree == true
    if (isFreeByCalendar || isWholeDayFree) {
        val title = if (freeDay != null) {
            freeDay.name
        } else {
            plan?.wholeDayFreeReason ?: "Kein Unterricht"
        }
        var nextSchool = todayDate.plusDays(1)
        while (calendar.freeDay(nextSchool) != null) {
            nextSchool = nextSchool.plusDays(1)
        }
        val subtitle = "Schule wieder ${formatGermanDay(nextSchool)}"
        val isHoliday = freeDay?.kind == FreeKind.FERIEN || freeDay?.kind == FreeKind.FEIERTAG
        return WidgetState.Free(title = title, subtitle = subtitle, isHoliday = isHoliday)
    }

    if (plan == null || (!plan.hasSchool && todayDay?.status == DayStatus.NO_DATA && todayDay.lessons.isEmpty())) {
        return WidgetState.NoData
    }

    val effectivePlaced = plan.placed.filter {
        it.view.kind != LessonKind.CANCELLED && !it.view.isFreeEvent
    }.sortedBy { it.view.lesson.start }

    if (effectivePlaced.isEmpty()) {
        return WidgetState.NoData
    }

    val note: String? = when {
        plan.startsLater && plan.effectiveStart != null && plan.regularStart != null ->
            "Beginn ${plan.effectiveStart.format(TIME_FORMATTER)} statt ${plan.regularStart.format(TIME_FORMATTER)}"
        plan.endsEarlier && plan.effectiveEnd != null && plan.regularEnd != null ->
            "Schluss ${plan.effectiveEnd.format(TIME_FORMATTER)} statt ${plan.regularEnd.format(TIME_FORMATTER)}"
        plan.gainedUnits > 0 ->
            "${plan.gainedUnits} ${if (plan.gainedUnits == 1) "Stunde" else "Stunden"} frei"
        else -> null
    }

    // 1. in a lesson -> JETZT
    val running = effectivePlaced.firstOrNull {
        it.view.lesson.start <= currentTime && currentTime < it.view.lesson.end
    }
    if (running != null) {
        val room = if (running.view.isFoyer) "Foyer" else running.view.roomText
        return WidgetState.Lesson(
            label = "JETZT",
            title = running.view.title,
            room = room,
            timeText = "bis ${running.view.lesson.end.format(TIME_FORMATTER)}",
            note = note,
        )
    }

    // 2. in a CANCELLED free slot -> Free("Frei bis 11:45", "SP-LK2 entfällt")
    val runningCancelledSlot = plan.freeSlots.firstOrNull {
        it.reason == FreeReason.CANCELLED && it.start <= currentTime && currentTime < it.end
    }
    if (runningCancelledSlot != null) {
        val nextEffective = effectivePlaced.firstOrNull { it.view.lesson.start > currentTime }
        val untilTime = nextEffective?.view?.lesson?.start ?: runningCancelledSlot.end
        val cancelledViews = plan.placed.map { it.view }.filter {
            it.kind == LessonKind.CANCELLED &&
                it.lesson.start < runningCancelledSlot.end && it.lesson.end > runningCancelledSlot.start
        }
        val cancelledNames = cancelledViews.map { it.shortTitle }.distinct()
        val subtitle = when {
            cancelledNames.size == 1 -> "${cancelledNames.first()} entfällt"
            cancelledNames.size > 1 -> "${cancelledNames.joinToString(", ")} entfallen"
            else -> "Unterricht entfällt"
        }
        return WidgetState.Free(
            title = "Frei bis ${untilTime.format(TIME_FORMATTER)}",
            subtitle = subtitle,
            isHoliday = false,
        )
    }

    // 3. after last effective lesson -> Done
    val effectiveEnd = plan.effectiveEnd
    if (effectiveEnd != null && currentTime >= effectiveEnd) {
        var nextDate = todayDate.plusDays(1)
        while (calendar.freeDay(nextDate) != null) {
            nextDate = nextDate.plusDays(1)
        }
        val nextDay = days.firstOrNull { it.date == nextDate } ?: Day(nextDate, DayStatus.NO_DATA, emptyList())
        val nextPlan = DayAnalysis.analyze(nextDay, grid, foyerPrefix)
        val nextStartStr = nextPlan.effectiveStart?.format(TIME_FORMATTER) ?: "08:00"
        val nextDayLabel = if (nextDate == todayDate.plusDays(1)) "Morgen" else formatGermanDay(nextDate)
        return WidgetState.Done(nextText = "$nextDayLabel ab $nextStartStr")
    }

    // 4. before / in break -> ALS NÄCHSTES (next effective lesson)
    val nextEffective = effectivePlaced.firstOrNull { it.view.lesson.start > currentTime }
    if (nextEffective != null) {
        val room = if (nextEffective.view.isFoyer) "Foyer" else nextEffective.view.roomText
        return WidgetState.Lesson(
            label = "ALS NÄCHSTES",
            title = nextEffective.view.title,
            room = room,
            timeText = "um ${nextEffective.view.lesson.start.format(TIME_FORMATTER)}",
            note = note,
        )
    }

    return WidgetState.NoData
}
