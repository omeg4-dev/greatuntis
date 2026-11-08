package io.github.omeg4dev.greatuntis.calendar

import io.github.omeg4dev.greatuntis.Fixtures
import io.github.omeg4dev.greatuntis.api.ApiHoliday
import io.github.omeg4dev.greatuntis.api.HolidayClient
import io.github.omeg4dev.greatuntis.api.UntisJson
import io.github.omeg4dev.greatuntis.api.UserDataResultDto
import io.github.omeg4dev.greatuntis.api.toMasterData
import io.github.omeg4dev.greatuntis.model.Holiday
import io.github.omeg4dev.greatuntis.model.SchoolYear
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class FreeCalendarTest {
    private lateinit var server: MockWebServer
    private lateinit var client: HolidayClient
    private lateinit var webUntis: List<Holiday>
    private lateinit var apiHolidays: List<ApiHoliday>
    private lateinit var schoolYears: List<SchoolYear>
    private val range: ClosedRange<LocalDate> = LocalDate.of(2025, 8, 1)..LocalDate.of(2027, 7, 31)

    private fun resp(code: Int = 200, body: String = ""): MockResponse =
        MockResponse.Builder().code(code).body(body).build()

    @Before
    fun setUp() = runBlocking {
        server = MockWebServer()
        server.start()
        client = HolidayClient(
            http = OkHttpClient(),
            baseUrl = server.url("/"),
        )

        val schoolHolidaysJson = Fixtures.load("openholidays_SchoolHolidays_NW.json")
        val publicHolidaysJson = Fixtures.load("openholidays_PublicHolidays_NW.json")
        server.enqueue(resp(200, schoolHolidaysJson))
        server.enqueue(resp(200, publicHolidaysJson))

        apiHolidays = client.fetch("NW", range.start, range.endInclusive)

        val userdataJson = Fixtures.load("userdata.json")
        val userDto = UntisJson.decodeFromString<UserDataResultDto>(userdataJson)
        val masterData = toMasterData(userDto)
        webUntis = masterData.holidays
        schoolYears = masterData.schoolYears
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun buildCalendar(
        schoolDays: Set<LocalDate> = emptySet(),
        timetableFree: Map<LocalDate, String> = emptyMap(),
        noDataWeekdays: Set<LocalDate> = emptySet(),
    ): FreeCalendar {
        return FreeCalendar.build(
            webUntis = webUntis,
            api = apiHolidays,
            schoolDays = schoolDays,
            timetableFree = timetableFree,
            noDataWeekdays = noDataWeekdays,
            range = range,
        )
    }

    @Test
    fun testWeihnachtsferienMergedAndPublicHolidayNames() {
        val calendar = buildCalendar()

        val stretch = calendar.stretchAt(LocalDate.of(2025, 12, 25))
        assertNotNull("Stretch for Weihnachtsferien 2025/26 must not be null", stretch)
        assertEquals(LocalDate.of(2025, 12, 20), stretch?.start)
        assertEquals(LocalDate.of(2026, 1, 6), stretch?.end)
        assertEquals("Weihnachtsferien", stretch?.title)

        val day25 = calendar.freeDay(LocalDate.of(2025, 12, 25))
        assertNotNull(day25)
        assertEquals(FreeKind.FERIEN, day25?.kind)
        assertEquals(FreeSource.WEBUNTIS, day25?.source)
        assertEquals(listOf("Weihnachtsferien", "1. Weihnachtsfeiertag"), day25?.names)
    }

    @Test
    fun testRosenmontagAndVeilchendienstagCarnival() {
        val calendar = buildCalendar()

        val day16 = calendar.freeDay(LocalDate.of(2026, 2, 16))
        assertNotNull(day16)
        assertEquals("Rosenmontag", day16?.name)
        assertEquals(FreeKind.BRUECKENTAG, day16?.kind)

        val day17 = calendar.freeDay(LocalDate.of(2026, 2, 17))
        assertNotNull(day17)
        assertEquals("Veilchendienstag", day17?.name)
        assertEquals(FreeKind.BRUECKENTAG, day17?.kind)

        val stretch = calendar.stretchAt(LocalDate.of(2026, 2, 16))
        assertNotNull(stretch)
        assertEquals(LocalDate.of(2026, 2, 14), stretch?.start)
        assertEquals(LocalDate.of(2026, 2, 17), stretch?.end)
        assertTrue(stretch?.isLongWeekend == true)
        assertEquals("Rosenmontag + Veilchendienstag", stretch?.title)
    }

    @Test
    fun testChristiHimmelfahrtAndBrueckentag() {
        val calendar = buildCalendar()

        val day14 = calendar.freeDay(LocalDate.of(2026, 5, 14))
        assertNotNull(day14)
        assertEquals("Christi Himmelfahrt", day14?.name)
        assertEquals(FreeKind.FEIERTAG, day14?.kind)

        val day15 = calendar.freeDay(LocalDate.of(2026, 5, 15))
        assertNotNull(day15)
        assertEquals("Brückentag", day15?.name)
        assertEquals(FreeKind.BRUECKENTAG, day15?.kind)

        val stretch = calendar.stretchAt(LocalDate.of(2026, 5, 14))
        assertNotNull(stretch)
        assertEquals(LocalDate.of(2026, 5, 14), stretch?.start)
        assertEquals(LocalDate.of(2026, 5, 17), stretch?.end)
        assertEquals(4, stretch?.length)
        assertEquals("Do 14.05. – So 17.05. · 4 Tage frei", stretch?.label)
    }

    @Test
    fun testPfingstenStretch() {
        val calendar = buildCalendar()

        val stretch = calendar.stretchAt(LocalDate.of(2026, 5, 26))
        assertNotNull(stretch)
        assertEquals(LocalDate.of(2026, 5, 23), stretch?.start)
        assertEquals(LocalDate.of(2026, 5, 26), stretch?.end)
        assertEquals(FreeKind.FERIEN, stretch?.kind)
        assertEquals("Pfingstferien", stretch?.title)
    }

    @Test
    fun testApiFillsWebUntisGapFor2026AutumnAndNextFerien() {
        val calendar = buildCalendar()

        // 2026-10-20 has no WebUntis entry -> FERIEN "Herbstferien" from API
        val dayOct20 = calendar.freeDay(LocalDate.of(2026, 10, 20))
        assertNotNull(dayOct20)
        assertEquals(FreeKind.FERIEN, dayOct20?.kind)
        assertEquals("Herbstferien", dayOct20?.name)
        assertEquals(FreeSource.API, dayOct20?.source)

        // nextFerien(2026-11-08) = stretch 2026-12-23 … 2027-01-06, title "Weihnachtsferien"
        val nextFer = calendar.nextFerien(LocalDate.of(2026, 11, 8))
        assertNotNull("Next Ferien after 2026-11-08 must exist", nextFer)
        assertEquals(LocalDate.of(2026, 12, 23), nextFer?.start)
        assertEquals(LocalDate.of(2027, 1, 6), nextFer?.end)
        assertEquals("Weihnachtsferien", nextFer?.title)
    }

    @Test
    fun testSchoolDaysOverrideHolidays() {
        val calendar = buildCalendar(
            schoolDays = setOf(LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 1))
        )

        assertNull(calendar.freeDay(LocalDate.of(2026, 8, 31)))
        assertNull(calendar.freeDay(LocalDate.of(2026, 9, 1)))
        assertFalse(calendar.isFree(LocalDate.of(2026, 8, 31)))
        assertFalse(calendar.isFree(LocalDate.of(2026, 9, 1)))
    }

    @Test
    fun testTimetableFreeAndNoDataWeekdays() {
        val timetableDate = LocalDate.of(2026, 10, 8)
        val noDataDate = LocalDate.of(2026, 10, 9)

        val calendar = buildCalendar(
            timetableFree = mapOf(timetableDate to "Pädagogischer Tag"),
            noDataWeekdays = setOf(noDataDate),
        )

        val ttDay = calendar.freeDay(timetableDate)
        assertNotNull(ttDay)
        assertEquals(FreeKind.SCHULFREI, ttDay?.kind)
        assertEquals("Pädagogischer Tag", ttDay?.name)

        val noDataDay = calendar.freeDay(noDataDate)
        assertNotNull(noDataDay)
        assertEquals(FreeKind.SCHULFREI, noDataDay?.kind)
        assertEquals("Unterrichtsfrei", noDataDay?.name)
    }

    @Test
    fun testPlainWeekendIsNotAStretch() {
        val calendar = buildCalendar()

        val nov7 = LocalDate.of(2026, 11, 7) // Saturday
        val day = calendar.freeDay(nov7)
        assertNotNull(day)
        assertEquals(FreeKind.WOCHENENDE, day?.kind)
        assertEquals("Wochenende", day?.name)
        assertNull(calendar.stretchAt(nov7))
    }

    @Test
    fun testSchoolDaysBetweenAndSchoolYearProgress() {
        val calendar = buildCalendar()

        val count = calendar.schoolDaysBetween(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 16))
        assertEquals(5, count)

        val year2526 = schoolYears.first { it.name == "2025/2026" }

        val p1 = calendar.schoolYearProgress(year2526.start.minusDays(5), year2526)
        assertEquals(0f, p1, 0.0001f)

        val p2 = calendar.schoolYearProgress(LocalDate.of(2025, 11, 1), year2526)
        assertTrue("Progress should be in 0..1, got $p2", p2 in 0f..1f)

        val p3 = calendar.schoolYearProgress(LocalDate.of(2026, 2, 1), year2526)
        assertTrue("Progress should be monotonic: p2=$p2 <= p3=$p3", p2 <= p3)

        val p4 = calendar.schoolYearProgress(LocalDate.of(2026, 6, 1), year2526)
        assertTrue("Progress should be monotonic: p3=$p3 <= p4=$p4", p3 <= p4)

        val pEnd = calendar.schoolYearProgress(year2526.end.plusDays(5), year2526)
        assertEquals(1f, pEnd, 0.0001f)
    }

    @Test
    fun testStretchesOverlappingAndNextStretch() {
        val calendar = buildCalendar()

        val octFrom = LocalDate.of(2026, 10, 1)
        val octTo = LocalDate.of(2026, 10, 31)
        val octStretches = calendar.stretches(octFrom, octTo)
        assertFalse(octStretches.isEmpty())

        val nextAfterNov = calendar.nextStretch(LocalDate.of(2026, 11, 8))
        assertNotNull(nextAfterNov)
        assertTrue(nextAfterNov!!.start > LocalDate.of(2026, 11, 8))
    }

    @Test
    fun testEmptyCalendar() {
        val emptyCal = FreeCalendar.EMPTY
        val weekday = LocalDate.of(2026, 11, 10) // Tuesday
        val weekend = LocalDate.of(2026, 11, 14) // Saturday

        assertNull(emptyCal.freeDay(weekday))
        assertFalse(emptyCal.isFree(weekday))

        val freeWeekend = emptyCal.freeDay(weekend)
        assertNotNull(freeWeekend)
        assertEquals(FreeKind.WOCHENENDE, freeWeekend?.kind)
        assertTrue(emptyCal.isFree(weekend))

        assertNull(emptyCal.stretchAt(weekday))
        assertNull(emptyCal.stretchAt(weekend))
        assertNull(emptyCal.nextStretch(weekday))
        assertNull(emptyCal.nextFerien(weekday))
        assertTrue(emptyCal.stretches(weekday, weekend).isEmpty())
    }

    @Test
    fun testFreeStretchSingleDayLabel() {
        val singleDay = FreeDay(
            date = LocalDate.of(2026, 5, 1), // Friday (Tag der Arbeit)
            kind = FreeKind.FEIERTAG,
            names = listOf("Tag der Arbeit"),
            source = FreeSource.API,
        )
        val stretch = FreeStretch(
            start = singleDay.date,
            end = singleDay.date,
            title = "Tag der Arbeit",
            kind = FreeKind.FEIERTAG,
            days = listOf(singleDay),
        )
        assertEquals(1, stretch.length)
        assertEquals("Fr 01.05. · 1 Tag frei", stretch.label)
    }

    @Test
    fun testFakeScenarioCountdown() {
        val cal = io.github.omeg4dev.greatuntis.ui.fake.FakeData.createFakeFreeCalendar()
        val today = LocalDate.of(2026, 11, 3)
        val nextFerien = cal.nextFerien(today)
        assertNotNull(nextFerien)
        val days = cal.schoolDaysBetween(today, nextFerien!!.start)
        println("TEST_OUTPUT: nextFerien: $nextFerien, days: $days, 05.11 free: ${cal.freeDay(LocalDate.of(2026, 11, 5))}")
        assertEquals(35, days)
    }
}
