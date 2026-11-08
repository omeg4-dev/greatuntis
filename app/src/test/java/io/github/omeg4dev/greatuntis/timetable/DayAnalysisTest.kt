package io.github.omeg4dev.greatuntis.timetable

import io.github.omeg4dev.greatuntis.Fixtures
import io.github.omeg4dev.greatuntis.api.TimetableResponseDto
import io.github.omeg4dev.greatuntis.api.UntisJson
import io.github.omeg4dev.greatuntis.api.UserDataResultDto
import io.github.omeg4dev.greatuntis.api.toDays
import io.github.omeg4dev.greatuntis.api.toMasterData
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.DayStatus
import io.github.omeg4dev.greatuntis.model.Element
import io.github.omeg4dev.greatuntis.model.ElementStatus
import io.github.omeg4dev.greatuntis.model.Lesson
import io.github.omeg4dev.greatuntis.model.LessonStatus
import io.github.omeg4dev.greatuntis.model.MasterData
import io.github.omeg4dev.greatuntis.model.Slot
import io.github.omeg4dev.greatuntis.model.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DayAnalysisTest {

    private lateinit var masterData: MasterData
    private lateinit var days2026: List<Day>

    @Before
    fun setUp() {
        val userJson = Fixtures.load("userdata.json")
        masterData = toMasterData(UntisJson.decodeFromString<UserDataResultDto>(userJson))

        val tt2026Json = Fixtures.load("timetable_2026.json")
        days2026 = toDays(UntisJson.decodeFromString<TimetableResponseDto>(tt2026Json))
    }

    private fun analyzeDay(date: LocalDate): DayPlan {
        val day = days2026.first { it.date == date }
        return DayAnalysis.analyze(day, masterData.timeGrid, "Foyer")
    }

    /** 2026-10-08: Päd. Tag EVENT → wholeDayFree, reason "Pädagogischer Tag", hasSchool false */
    @Test
    fun testWholeDayFree_PadTag_20261008() {
        val plan = analyzeDay(LocalDate.of(2026, 10, 8))

        assertTrue("wholeDayFree expected", plan.wholeDayFree)
        assertEquals("Pädagogischer Tag", plan.wholeDayFreeReason)
        assertFalse("hasSchool should be false", plan.hasSchool)
    }

    /** 2026-10-06: not wholeDayFree; EXAM 11:45-12:30 and SP-LK2 cancelled 11:45-13:20 in different lanes */
    @Test
    fun testLanes_20261006() {
        val plan = analyzeDay(LocalDate.of(2026, 10, 6))

        assertFalse("Should not be wholeDayFree", plan.wholeDayFree)

        // Find placed lessons for EXAM at 11:45 and cancelled SP-LK2 at 11:45
        val examAt1145 = plan.placed.firstOrNull { p ->
            p.view.lesson.type == "EXAM" && p.view.lesson.start == LocalTime.of(11, 45)
        }
        assertNotNull("EXAM at 11:45 should exist", examAt1145)
        examAt1145!!

        val spLk2 = plan.placed.firstOrNull { p ->
            p.view.lesson.status == LessonStatus.CANCELLED &&
                p.view.lesson.start == LocalTime.of(11, 45)
        }
        assertNotNull("Cancelled SP-LK2 at 11:45 should exist", spLk2)
        spLk2!!

        // They should be in different lanes
        assertTrue("Exam and SP-LK2 should be in different lanes", examAt1145.lane != spLk2.lane)
        // Both should have laneCount = 2 (they form a 2-lesson cluster)
        assertEquals(2, examAt1145.laneCount)
        assertEquals(2, spLk2.laneCount)
    }

    /** 2026-10-06: free slot at 12:35-13:20 (unit 6), none at 11:45 */
    @Test
    fun testFreeSlot_20261006() {
        val plan = analyzeDay(LocalDate.of(2026, 10, 6))

        // Unit 5 covers 11:45-12:30: EXAM (effective) + cancelled SP-LK2 → not free
        val slotAt1145 = plan.freeSlots.firstOrNull { it.start == LocalTime.of(11, 45) }
        assertNull("No free slot at 11:45", slotAt1145)

        // Unit 6 covers 12:35-13:20: only cancelled SP-LK2 → CANCELLED free slot
        val slotAt1235 = plan.freeSlots.firstOrNull { it.start == LocalTime.of(12, 35) }
        assertNotNull("CANCELLED free slot at 12:35 expected", slotAt1235)
        slotAt1235!!
        assertEquals(LocalTime.of(13, 20), slotAt1235.end)
        assertEquals(6, slotAt1235.firstUnit)
        assertEquals(6, slotAt1235.lastUnit)
        assertEquals(FreeReason.CANCELLED, slotAt1235.reason)
    }

    /** 2026-09-28: wholeDayFree reason "Alles entfällt" (all cancelled, no free event) */
    @Test
    fun testWholeDayFree_AllesEntfaellt_20260928() {
        val plan = analyzeDay(LocalDate.of(2026, 9, 28))

        assertTrue("wholeDayFree expected", plan.wholeDayFree)
        assertEquals("Alles entfällt", plan.wholeDayFreeReason)
    }

    /**
     * 2026-09-02: cancelled first double lesson (units 2+3) → startsLater true, gainedUnits 2.
     * This day has first two lessons cancelled (08:50-09:35, 09:55-10:40) and regular lessons after.
     */
    @Test
    fun testStartsLater_gainedUnits_20260902() {
        val plan = analyzeDay(LocalDate.of(2026, 9, 2))

        assertTrue("startsLater should be true", plan.startsLater)
        assertEquals("gainedUnits should be 2", 2, plan.gainedUnits)
    }

    /** breaks() should return correct GridBreaks for the fixture time grid */
    @Test
    fun testBreaks() {
        val breaks = DayAnalysis.breaks(masterData.timeGrid)

        // Grid units: 1(08:00-08:45), 2(08:50-09:35), 3(09:55-10:40), 4(10:45-11:30),
        //             5(11:45-12:30), 6(12:35-13:20), 7(13:30-14:15), 8(14:15-15:00), 9(15:00-15:45)
        // Breaks: after 1(5min), after 2(20min), after 3(5min), after 4(15min), after 5(5min), after 6(10min)
        // No breaks after 7 (8 starts right at 14:15), after 8 (9 starts right at 15:00)
        assertEquals(6, breaks.size)

        val afterUnit2 = breaks.first { it.afterUnit == 2 }
        assertEquals(LocalTime.of(9, 35), afterUnit2.start)
        assertEquals(LocalTime.of(9, 55), afterUnit2.end)
        assertEquals(20, afterUnit2.minutes)

        val afterUnit4 = breaks.first { it.afterUnit == 4 }
        assertEquals(LocalTime.of(11, 30), afterUnit4.start)
        assertEquals(LocalTime.of(11, 45), afterUnit4.end)
        assertEquals(15, afterUnit4.minutes)

        val afterUnit1 = breaks.first { it.afterUnit == 1 }
        assertEquals(5, afterUnit1.minutes)

        val afterUnit5 = breaks.first { it.afterUnit == 5 }
        assertEquals(5, afterUnit5.minutes)

        val afterUnit6 = breaks.first { it.afterUnit == 6 }
        assertEquals(10, afterUnit6.minutes)

        // No breaks after unit 7 or unit 8
        assertNull("No break after unit 7", breaks.firstOrNull { it.afterUnit == 7 })
        assertNull("No break after unit 8", breaks.firstOrNull { it.afterUnit == 8 })
    }

    /** A lesson at 08:20-09:10 overlaps units 1 and 2 (not off-grid) */
    @Test
    fun testOffGridAligned_OverlapsUnits1And2() {
        val subject = Element("TEST", "Test", ElementStatus.REGULAR)
        val lesson = Lesson(
            date = LocalDate.of(2026, 9, 1),
            start = LocalTime.of(8, 20),
            end = LocalTime.of(9, 10),
            periodIds = listOf(1L),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subject, null)),
            teachers = emptyList(),
            rooms = emptyList(),
        )
        val day = Day(LocalDate.of(2026, 9, 1), DayStatus.REGULAR, listOf(lesson))
        val plan = DayAnalysis.analyze(day, masterData.timeGrid, "Foyer")

        assertEquals(1, plan.placed.size)
        val placed = plan.placed.first()
        assertEquals(1, placed.firstUnit)
        assertEquals(2, placed.lastUnit)
        assertFalse("offGrid should be false since lesson overlaps units 1 and 2", placed.offGrid)
    }

    /**
     * 2026-11-03 in timetable_day_foyer.json:
     * EXAM KR-GK4 09:55–10:40 and EXAM KR-GK4 10:45–11:30 are both in lane 0 (they do not overlap each other).
     * CANCELLED KU-LK1 09:55–11:30 is in lane 1.
     * All three have laneCount = 2 (not 3).
     */
    @Test
    fun testLanesForExamsBesideCancellations_20261103() {
        val foyerJson = Fixtures.load("timetable_day_foyer.json")
        val daysFoyer = toDays(UntisJson.decodeFromString<TimetableResponseDto>(foyerJson))
        val day = daysFoyer.first { it.date == LocalDate.of(2026, 11, 3) }
        val plan = DayAnalysis.analyze(day, masterData.timeGrid, "Foyer")

        val exam1 = plan.placed.first { it.view.lesson.type == "EXAM" && it.view.lesson.start == LocalTime.of(9, 55) }
        val exam2 = plan.placed.first { it.view.lesson.type == "EXAM" && it.view.lesson.start == LocalTime.of(10, 45) }
        val cancelled = plan.placed.first { it.view.lesson.status == LessonStatus.CANCELLED && it.view.lesson.start == LocalTime.of(9, 55) }

        assertEquals("First exam should be in lane 0", 0, exam1.lane)
        assertEquals("Second exam should be in lane 0", 0, exam2.lane)
        assertEquals("Cancelled lesson should be in lane 1", 1, cancelled.lane)
        assertEquals("exam1 laneCount should be 2", 2, exam1.laneCount)
        assertEquals("exam2 laneCount should be 2", 2, exam2.laneCount)
        assertEquals("cancelled laneCount should be 2", 2, cancelled.laneCount)
    }

    /**
     * Week summary rule: counts exams per (date, title) once (not per unit/period).
     */
    @Test
    fun testWeekSummaryCountingDistinctByDateAndTitle() {
        val foyerJson = Fixtures.load("timetable_day_foyer.json")
        val daysFoyer = toDays(UntisJson.decodeFromString<TimetableResponseDto>(foyerJson))
        val day = daysFoyer.first { it.date == LocalDate.of(2026, 11, 3) }
        val plan = DayAnalysis.analyze(day, masterData.timeGrid, "Foyer")

        val examPeriods = plan.placed.filter {
            it.view.kind == LessonKind.EXAM || LessonKind.EXAM in it.view.badges
        }
        assertEquals(3, examPeriods.size)

        val distinctExams = examPeriods.distinctBy { it.view.lesson.date to it.view.title }
        assertEquals(1, distinctExams.size)
    }
}
