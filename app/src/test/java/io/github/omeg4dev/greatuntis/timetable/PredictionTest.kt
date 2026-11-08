package io.github.omeg4dev.greatuntis.timetable

import io.github.omeg4dev.greatuntis.Fixtures
import io.github.omeg4dev.greatuntis.api.TimetableResponseDto
import io.github.omeg4dev.greatuntis.api.UntisJson
import io.github.omeg4dev.greatuntis.api.UserDataResultDto
import io.github.omeg4dev.greatuntis.api.toDays
import io.github.omeg4dev.greatuntis.api.toMasterData
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.DayStatus
import io.github.omeg4dev.greatuntis.model.Holiday
import io.github.omeg4dev.greatuntis.model.MasterData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class PredictionTest {

    private lateinit var masterData: MasterData
    private lateinit var days2026: List<Day>
    private lateinit var daysWindow: List<Day>

    @Before
    fun setUp() {
        val userJson = Fixtures.load("userdata.json")
        masterData = toMasterData(UntisJson.decodeFromString<UserDataResultDto>(userJson))

        val tt2026Json = Fixtures.load("timetable_2026.json")
        days2026 = toDays(UntisJson.decodeFromString<TimetableResponseDto>(tt2026Json))

        val ttWindowJson = Fixtures.load("timetable_window.json")
        daysWindow = toDays(UntisJson.decodeFromString<TimetableResponseDto>(ttWindowJson))
    }

    @Test
    fun testA_predict2026_11_05UsingHistoryBeforeIt() {
        val targetDate = LocalDate.of(2026, 11, 5)
        val historyBefore = days2026.filter { it.date < targetDate }

        val result = Prediction.fill(
            history = historyBefore,
            dates = listOf(targetDate),
            timeGrid = masterData.timeGrid,
            holidays = masterData.holidays,
        )

        assertEquals(1, result.size)
        val day = result[0]
        assertEquals(targetDate, day.date)
        assertEquals(DayStatus.NOT_ALLOWED, day.status)

        // Actual lessons in fixture: 08:00–09:35 KU-LK1, 09:55–11:30 SP-LK2, 11:45–12:30 PKI-GK2
        assertEquals(3, day.lessons.size)

        val l1 = day.lessons[0]
        assertEquals(LocalTime.of(8, 0), l1.start)
        assertEquals(LocalTime.of(9, 35), l1.end)
        assertEquals("KU-LK1", l1.subject)
        assertTrue(l1.predicted)

        val l2 = day.lessons[1]
        assertEquals(LocalTime.of(9, 55), l2.start)
        assertEquals(LocalTime.of(11, 30), l2.end)
        assertEquals("SP-LK2", l2.subject)
        assertTrue(l2.predicted)

        val l3 = day.lessons[2]
        assertEquals(LocalTime.of(11, 45), l3.start)
        assertEquals(LocalTime.of(12, 30), l3.end)
        assertEquals("PKI-GK2", l3.subject)
        assertTrue(l3.predicted)
    }

    @Test
    fun testB_predict2026_11_10FromTimetableWindowAndHistory() {
        val targetDate = LocalDate.of(2026, 11, 10)
        val history = days2026.filter { it.date < targetDate }

        val result = Prediction.fill(
            history = history,
            dates = listOf(targetDate),
            timeGrid = masterData.timeGrid,
            holidays = masterData.holidays,
        )

        assertEquals(1, result.size)
        val day = result[0]
        assertEquals(targetDate, day.date)
        assertEquals(DayStatus.NOT_ALLOWED, day.status)
        assertTrue("Predicted day should have non-empty lessons", day.lessons.isNotEmpty())
        assertTrue("All lessons should be marked predicted", day.lessons.all { it.predicted })
    }

    @Test
    fun testC_holidayReturnsNoDataEmpty() {
        val holidayDate = LocalDate.of(2026, 10, 20)
        val holidays = masterData.holidays + listOf(
            Holiday("Herbstferien", LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 31))
        )

        val result = Prediction.fill(
            history = days2026,
            dates = listOf(holidayDate),
            timeGrid = masterData.timeGrid,
            holidays = holidays,
        )

        assertEquals(1, result.size)
        val day = result[0]
        assertEquals(holidayDate, day.date)
        assertEquals(DayStatus.NO_DATA, day.status)
        assertTrue(day.lessons.isEmpty())
    }

    @Test
    fun testD_regularFetchedDayReturnedUnchanged() {
        val targetDate = LocalDate.of(2026, 11, 5)
        val expectedDay = days2026.first { it.date == targetDate }
        assertEquals(DayStatus.REGULAR, expectedDay.status)

        val result = Prediction.fill(
            history = days2026,
            dates = listOf(targetDate),
            timeGrid = masterData.timeGrid,
            holidays = masterData.holidays,
        )

        assertEquals(1, result.size)
        val day = result[0]
        assertEquals(expectedDay, day)
        assertFalse(day.lessons.any { it.predicted })
    }
}
