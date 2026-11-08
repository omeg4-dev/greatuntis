package io.github.omeg4dev.greatuntis.data

import io.github.omeg4dev.greatuntis.api.ApiHoliday
import io.github.omeg4dev.greatuntis.calendar.FreeCalendar
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.DayStatus
import io.github.omeg4dev.greatuntis.model.MasterData
import io.github.omeg4dev.greatuntis.model.TimeUnit
import io.github.omeg4dev.greatuntis.timetable.DayAnalysis
import java.time.DayOfWeek
import java.time.LocalDate

data class FreeCalendarInputs(
    val schoolDays: Set<LocalDate>,
    val timetableFree: Map<LocalDate, String>,
    val noDataWeekdays: Set<LocalDate>,
)

object FreeCalendarBuilder {

    fun prepareInputs(
        days: List<Day>,
        grid: List<TimeUnit>,
        foyerPrefix: String,
    ): FreeCalendarInputs {
        val schoolDays = mutableSetOf<LocalDate>()
        val timetableFree = mutableMapOf<LocalDate, String>()
        val noDataWeekdays = mutableSetOf<LocalDate>()

        for (day in days) {
            val plan = DayAnalysis.analyze(day, grid, foyerPrefix)
            if (plan.hasSchool && !plan.predicted) {
                schoolDays.add(day.date)
            }
            if (plan.wholeDayFree && plan.wholeDayFreeReason != null) {
                timetableFree[day.date] = plan.wholeDayFreeReason
            }
            val isWeekday = day.date.dayOfWeek in listOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            )
            if (isWeekday && day.status == DayStatus.NO_DATA && day.lessons.isEmpty()) {
                noDataWeekdays.add(day.date)
            }
        }

        return FreeCalendarInputs(schoolDays, timetableFree, noDataWeekdays)
    }

    fun build(
        masterData: MasterData?,
        days: List<Day>,
        apiHolidays: List<ApiHoliday>,
        foyerPrefix: String,
        today: LocalDate = LocalDate.now(),
    ): FreeCalendar {
        val grid = masterData?.timeGrid ?: emptyList()
        val inputs = prepareInputs(days, grid, foyerPrefix)

        val schoolYears = masterData?.schoolYears ?: emptyList()
        val range: ClosedRange<LocalDate> = if (schoolYears.isNotEmpty()) {
            val minStart = schoolYears.minOf { it.start }
            val maxEnd = schoolYears.maxOf { it.end }
            minStart.minusDays(31)..maxEnd.plusDays(400)
        } else {
            today.minusYears(1)..today.plusYears(1)
        }

        return FreeCalendar.build(
            webUntis = masterData?.holidays ?: emptyList(),
            api = apiHolidays,
            schoolDays = inputs.schoolDays,
            timetableFree = inputs.timetableFree,
            noDataWeekdays = inputs.noDataWeekdays,
            range = range,
        )
    }
}
