package io.github.omeg4dev.greatuntis.watch

import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.DayStatus
import io.github.omeg4dev.greatuntis.model.Holiday
import io.github.omeg4dev.greatuntis.model.Lesson
import io.github.omeg4dev.greatuntis.model.LessonStatus
import io.github.omeg4dev.greatuntis.model.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class WatchSchedulerTest {

    private val settings = Settings(schoolStart = LocalTime.of(6, 0))

    @Test
    fun schoolDay0800To1545ResultsInWindow0600To1555() {
        val date = LocalDate.of(2026, 11, 2) // Monday
        val lesson1 = Lesson(
            date = date,
            start = LocalTime.of(8, 0),
            end = LocalTime.of(8, 45),
            periodIds = listOf(1L),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = emptyList(),
            teachers = emptyList(),
            rooms = emptyList(),
        )
        val lesson2 = Lesson(
            date = date,
            start = LocalTime.of(15, 0),
            end = LocalTime.of(15, 45),
            periodIds = listOf(9L),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = emptyList(),
            teachers = emptyList(),
            rooms = emptyList(),
        )
        val day = Day(date, DayStatus.REGULAR, listOf(lesson1, lesson2))

        val window = WatchScheduler.computeWindowToday(date, settings, listOf(day), emptyList())
        assertNotNull(window)
        assertEquals(LocalDateTime.of(date, LocalTime.of(6, 0)), window!!.start)
        assertEquals(LocalDateTime.of(date, LocalTime.of(15, 55)), window.endInclusive)

        val nowInside = LocalDateTime.of(date, LocalTime.of(10, 0))
        val schedule = WatchScheduler.computeSchedule(nowInside, settings, listOf(day), emptyList())
        assertTrue(schedule.isInsideWindow)
        assertEquals(window, schedule.windowToday)
    }

    @Test
    fun saturdayToNextMonday0600() {
        val saturday = LocalDateTime.of(2026, 11, 7, 14, 0)
        val schedule = WatchScheduler.computeSchedule(saturday, settings, emptyList(), emptyList())

        assertFalse(schedule.isInsideWindow)
        assertNull(schedule.windowToday)
        assertEquals(LocalDateTime.of(2026, 11, 9, 6, 0), schedule.nextWindowStart)
    }

    @Test
    fun insideHerbstferienJumpsToFirstSchoolDayAfterHolidays() {
        val holidays = listOf(
            Holiday("Herbstferien", LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 31))
        )
        // 2026-10-20 is Tuesday inside Herbstferien. Oct 31 is Sat, Nov 1 is Sun. Next school day is Mon Nov 2.
        val now = LocalDateTime.of(2026, 10, 20, 11, 0)
        val schedule = WatchScheduler.computeSchedule(now, settings, emptyList(), holidays)

        assertFalse(schedule.isInsideWindow)
        assertNull(schedule.windowToday)
        assertEquals(LocalDateTime.of(2026, 11, 2, 6, 0), schedule.nextWindowStart)
    }

    @Test
    fun unknownLessonsDefaultsTo1600() {
        val date = LocalDate.of(2026, 11, 3) // Tuesday (school day)
        val window = WatchScheduler.computeWindowToday(date, settings, emptyList(), emptyList())

        assertNotNull(window)
        assertEquals(LocalDateTime.of(date, LocalTime.of(6, 0)), window!!.start)
        assertEquals(LocalDateTime.of(date, LocalTime.of(16, 0)), window.endInclusive)
    }
}
