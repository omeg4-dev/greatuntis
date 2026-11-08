package io.github.omeg4dev.greatuntis.widget

import io.github.omeg4dev.greatuntis.Fixtures
import io.github.omeg4dev.greatuntis.api.ApiHoliday
import io.github.omeg4dev.greatuntis.api.ApiHolidayType
import io.github.omeg4dev.greatuntis.api.TimetableResponseDto
import io.github.omeg4dev.greatuntis.api.UntisJson
import io.github.omeg4dev.greatuntis.api.UserDataResultDto
import io.github.omeg4dev.greatuntis.api.toDays
import io.github.omeg4dev.greatuntis.api.toMasterData
import io.github.omeg4dev.greatuntis.calendar.FreeCalendar
import io.github.omeg4dev.greatuntis.data.FreeCalendarBuilder
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.MasterData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class WidgetModelTest {

    private lateinit var masterData: MasterData
    private lateinit var days2026: List<Day>
    private lateinit var calendar: FreeCalendar

    @Before
    fun setUp() {
        val userJson = Fixtures.load("userdata.json")
        masterData = toMasterData(UntisJson.decodeFromString<UserDataResultDto>(userJson))

        val tt2026Json = Fixtures.load("timetable_2026.json")
        days2026 = toDays(UntisJson.decodeFromString<TimetableResponseDto>(tt2026Json))

        val holidays = listOf(
            ApiHoliday(
                name = "Herbstferien",
                start = LocalDate.of(2026, 10, 17),
                end = LocalDate.of(2026, 10, 31),
                type = ApiHolidayType.SCHOOL,
            ),
            ApiHoliday(
                name = "Weihnachtsferien",
                start = LocalDate.of(2026, 12, 23),
                end = LocalDate.of(2027, 1, 6),
                type = ApiHolidayType.SCHOOL,
            ),
        )

        calendar = FreeCalendarBuilder.build(
            masterData = masterData,
            days = days2026,
            apiHolidays = holidays,
            foyerPrefix = "Foyer",
            today = LocalDate.of(2026, 11, 3),
        )
    }

    /** 2026-11-03 07:30: before first lesson (starts at 08:00) -> ALS NÄCHSTES */
    @Test
    fun testBeforeFirstLesson_20261103_0730() {
        val now = LocalDateTime.of(2026, 11, 3, 7, 30)
        val state = widgetState(now, days2026, masterData.timeGrid, "Foyer", calendar)

        assertTrue("Expected Lesson state, was $state", state is WidgetState.Lesson)
        val lesson = state as WidgetState.Lesson
        assertEquals("ALS NÄCHSTES", lesson.label)
        assertEquals("Kath. Religion GK", lesson.title)
        assertEquals("R114", lesson.room)
        assertEquals("um 08:00", lesson.timeText)
        assertNull(lesson.note)
    }

    /** 2026-11-03 08:30: in running lesson (08:00-09:35) -> JETZT */
    @Test
    fun testInLesson_20261103_0830() {
        val now = LocalDateTime.of(2026, 11, 3, 8, 30)
        val state = widgetState(now, days2026, masterData.timeGrid, "Foyer", calendar)

        assertTrue("Expected Lesson state, was $state", state is WidgetState.Lesson)
        val lesson = state as WidgetState.Lesson
        assertEquals("JETZT", lesson.label)
        assertEquals("Kath. Religion GK", lesson.title)
        assertEquals("R114", lesson.room)
        assertEquals("bis 09:35", lesson.timeText)
        assertNull(lesson.note)
    }

    /** 2026-11-03 09:40: in break before second lesson (09:55-10:40) -> ALS NÄCHSTES */
    @Test
    fun testInBreak_20261103_0940() {
        val now = LocalDateTime.of(2026, 11, 3, 9, 40)
        val state = widgetState(now, days2026, masterData.timeGrid, "Foyer", calendar)

        assertTrue("Expected Lesson state, was $state", state is WidgetState.Lesson)
        val lesson = state as WidgetState.Lesson
        assertEquals("ALS NÄCHSTES", lesson.label)
        assertEquals("Kath. Religion GK", lesson.title)
        assertEquals("R114", lesson.room)
        assertEquals("um 09:55", lesson.timeText)
        assertNull(lesson.note)
    }

    /** 2026-11-03 12:00: in Foyer lesson (11:45-13:20) -> room is "Foyer" */
    @Test
    fun testFoyerRoom_20261103_1200() {
        val now = LocalDateTime.of(2026, 11, 3, 12, 0)
        val state = widgetState(now, days2026, masterData.timeGrid, "Foyer", calendar)

        assertTrue("Expected Lesson state, was $state", state is WidgetState.Lesson)
        val lesson = state as WidgetState.Lesson
        assertEquals("JETZT", lesson.label)
        assertEquals("Sport LK", lesson.title)
        assertEquals("Foyer", lesson.room)
        assertEquals("bis 13:20", lesson.timeText)
    }

    /** 2026-11-03 13:30: before afternoon lesson (14:15) -> ALS NÄCHSTES */
    @Test
    fun testBeforeAfternoon_20261103_1330() {
        val now = LocalDateTime.of(2026, 11, 3, 13, 30)
        val state = widgetState(now, days2026, masterData.timeGrid, "Foyer", calendar)

        assertTrue("Expected Lesson state, was $state", state is WidgetState.Lesson)
        val lesson = state as WidgetState.Lesson
        assertEquals("ALS NÄCHSTES", lesson.label)
        assertEquals("Erdkunde ZK", lesson.title)
        assertEquals("R121", lesson.room)
        assertEquals("um 14:15", lesson.timeText)
        assertNull(lesson.note)
    }

    /** 2026-10-08 09:00: Päd. Tag -> Free "Pädagogischer Tag", subtitle "Schule wieder Fr 09.10.", isHoliday = false */
    @Test
    fun testPadTag_20261008_0900() {
        val now = LocalDateTime.of(2026, 10, 8, 9, 0)
        val state = widgetState(now, days2026, masterData.timeGrid, "Foyer", calendar)

        assertTrue("Expected Free state, was $state", state is WidgetState.Free)
        val free = state as WidgetState.Free
        assertEquals("Pädagogischer Tag", free.title)
        assertEquals("Schule wieder Fr 09.10.", free.subtitle)
        org.junit.Assert.assertFalse("Päd. Tag is not holiday/ferien", free.isHoliday)
    }

    /** 2026-10-20 09:00: Herbstferien -> Free "Herbstferien", subtitle "Schule wieder Mo 02.11.", isHoliday = true */
    @Test
    fun testHerbstferien_20261020_0900() {
        val now = LocalDateTime.of(2026, 10, 20, 9, 0)
        val state = widgetState(now, days2026, masterData.timeGrid, "Foyer", calendar)

        assertTrue("Expected Free state, was $state", state is WidgetState.Free)
        val free = state as WidgetState.Free
        assertEquals("Herbstferien", free.title)
        assertEquals("Schule wieder Mo 02.11.", free.subtitle)
        assertTrue("Herbstferien is a holiday/ferien state", free.isHoliday)
    }

    /** 2026-10-06 12:40: SP-LK2 cancelled (11:45-13:20) while exam ended at 12:30 -> Free("Frei bis 14:15", "SP-LK2 entfällt") */
    @Test
    fun testInCancelledSlot_20261006_1240() {
        val now = LocalDateTime.of(2026, 10, 6, 12, 40)
        val state = widgetState(now, days2026, masterData.timeGrid, "Foyer", calendar)

        assertTrue("Expected Free state, was $state", state is WidgetState.Free)
        val free = state as WidgetState.Free
        assertEquals("Frei bis 14:15", free.title)
        assertEquals("SP-LK2 entfällt", free.subtitle)
    }

    /** 2026-09-02 08:00: startsLater -> note="Beginn 10:45 statt 08:50" */
    @Test
    fun testStartsLaterNote_20260902_0800() {
        val now = LocalDateTime.of(2026, 9, 2, 8, 0)
        val state = widgetState(now, days2026, masterData.timeGrid, "Foyer", calendar)

        assertTrue("Expected Lesson state, was $state", state is WidgetState.Lesson)
        val lesson = state as WidgetState.Lesson
        assertEquals("ALS NÄCHSTES", lesson.label)
        assertEquals("Beginn 10:45 statt 08:50", lesson.note)
    }

    /** 2026-11-03 16:00: after last lesson -> Done("Morgen ab ...") */
    @Test
    fun testAfterSchool_20261103_1600() {
        val now = LocalDateTime.of(2026, 11, 3, 16, 0)
        val state = widgetState(now, days2026, masterData.timeGrid, "Foyer", calendar)

        assertTrue("Expected Done state, was $state", state is WidgetState.Done)
        val done = state as WidgetState.Done
        assertTrue("Done text should start with 'Morgen ab', was ${done.nextText}", done.nextText.startsWith("Morgen ab"))
    }

    /** Ferien countdown */
    @Test
    fun testFerienCountdown() {
        val ferienStr = ferienCountdown(LocalDate.of(2026, 11, 3), calendar)
        assertNotNull(ferienStr)
        assertTrue("Should contain 'bis Weihnachtsferien'", ferienStr!!.contains("bis Weihnachtsferien"))
        assertTrue("Should start with 'Noch '", ferienStr.startsWith("Noch "))
    }
}
