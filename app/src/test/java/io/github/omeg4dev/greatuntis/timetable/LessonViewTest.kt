package io.github.omeg4dev.greatuntis.timetable

import io.github.omeg4dev.greatuntis.Fixtures
import io.github.omeg4dev.greatuntis.api.TimetableResponseDto
import io.github.omeg4dev.greatuntis.api.UntisJson
import io.github.omeg4dev.greatuntis.api.toDays
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.Element
import io.github.omeg4dev.greatuntis.model.ElementStatus
import io.github.omeg4dev.greatuntis.model.Lesson
import io.github.omeg4dev.greatuntis.model.LessonStatus
import io.github.omeg4dev.greatuntis.model.Slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class LessonViewTest {

    private lateinit var days2026: List<Day>

    @Before
    fun setUp() {
        val tt2026Json = Fixtures.load("timetable_2026.json")
        days2026 = toDays(UntisJson.decodeFromString<TimetableResponseDto>(tt2026Json))
    }

    // Helper to get a lesson on a date at a time matching start
    private fun lesson(date: LocalDate, start: LocalTime, type: String? = null): Lesson {
        val day = days2026.first { it.date == date }
        return day.lessons.first { l ->
            l.start == start && (type == null || l.type == type)
        }
    }

    /** 2026-09-14 09:55 EVENT: position1=TEACHER slots, position2=ROOM, no subjects */
    @Test
    fun testEventRouting_20260914() {
        val l = lesson(LocalDate.of(2026, 9, 14), LocalTime.of(9, 55), "EVENT")
        val view = LessonViews.of(l, "Foyer")

        assertEquals(LessonKind.EVENT, view.kind)
        // After routing by kind: teachers should contain T05, T08
        assertEquals(2, view.teachers.size)
        val teacherShortNames = view.teachers.mapNotNull { it.current?.shortName }
        assertTrue("T05 expected", teacherShortNames.contains("T05"))
        assertTrue("T08 expected", teacherShortNames.contains("T08"))
        // rooms should contain Aula
        assertEquals(1, view.rooms.size)
        assertEquals("Aula", view.rooms.first().current?.shortName)
        // no subjects
        assertTrue("subjects should be empty", view.subjects.isEmpty())
        assertFalse("isFreeEvent should be false", view.isFreeEvent)
    }

    /** 2026-10-08 EVENT "Päd. TAg": title normalised, isFreeEvent true */
    @Test
    fun testFreeEvent_PadTag_20261008() {
        val l = lesson(LocalDate.of(2026, 10, 8), LocalTime.of(8, 0), "EVENT")
        val view = LessonViews.of(l, "Foyer")

        assertEquals("Pädagogischer Tag", view.title)
        assertEquals("Päd. Tag", view.shortTitle)
        assertTrue("isFreeEvent should be true", view.isFreeEvent)
    }

    /** 2026-11-03 08:00 EXAM KR-GK4: kind=EXAM, room current=R114/removed=R115, SUBSTITUTION badge */
    @Test
    fun testExamDGK4_20261103() {
        val l = lesson(LocalDate.of(2026, 11, 3), LocalTime.of(8, 0), "EXAM")
        val view = LessonViews.of(l, "Foyer")

        assertEquals(LessonKind.EXAM, view.kind)
        // Room slot: current=R114 (ADDED), removed=R115
        assertEquals(1, view.rooms.size)
        val roomSlot = view.rooms.first()
        assertEquals("R114", roomSlot.current?.shortName)
        assertEquals("R115", roomSlot.removed?.shortName)
        // SUBSTITUTION badge because substitutionText is non-null
        assertTrue("SUBSTITUTION badge expected", view.badges.contains(LessonKind.SUBSTITUTION))
    }

    /** 2026-10-06 11:45 EXAM: title = "Klausur LK KU/HS+ LK M/RE" (from INFO element) */
    @Test
    fun testExamTitle_20261006() {
        val l = lesson(LocalDate.of(2026, 10, 6), LocalTime.of(11, 45), "EXAM")
        val view = LessonViews.of(l, "Foyer")

        assertEquals("Klausur LK KU/HS+ LK M/RE", view.title)
    }

    /** Synthetic lesson with room "Foyer" ADDED → kind FOYER */
    @Test
    fun testFoyerDetection() {
        val foyerRoom = Element("Foyer", "Foyer", ElementStatus.ADDED, kind = "ROOM")
        val removedRoom = Element("R106", "Sek II", ElementStatus.REMOVED, kind = "ROOM")
        val subject = Element("SP-LK2", "Sport LK", ElementStatus.REGULAR, kind = "SUBJECT")
        val lesson = Lesson(
            date = LocalDate.of(2026, 11, 3),
            start = LocalTime.of(11, 45),
            end = LocalTime.of(13, 20),
            periodIds = listOf(999L),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.CHANGED,
            subjects = listOf(Slot(subject, null)),
            teachers = listOf(Slot(null, Element("T07", "Lehrkraft T07", ElementStatus.REMOVED, kind = "TEACHER"))),
            rooms = listOf(Slot(foyerRoom, removedRoom)),
        )
        val view = LessonViews.of(lesson, "Foyer")
        assertEquals(LessonKind.FOYER, view.kind)
        assertTrue(view.isFoyer)
    }

    /** Same synthetic lesson with foyerPrefix "Aula" → not Foyer */
    @Test
    fun testFoyerNotMatchingPrefix() {
        val foyerRoom = Element("Foyer", "Foyer", ElementStatus.ADDED, kind = "ROOM")
        val removedRoom = Element("R106", "Sek II", ElementStatus.REMOVED, kind = "ROOM")
        val subject = Element("SP-LK2", "Sport LK", ElementStatus.REGULAR, kind = "SUBJECT")
        val lesson = Lesson(
            date = LocalDate.of(2026, 11, 3),
            start = LocalTime.of(11, 45),
            end = LocalTime.of(13, 20),
            periodIds = listOf(999L),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.CHANGED,
            subjects = listOf(Slot(subject, null)),
            teachers = listOf(Slot(null, Element("T07", "Lehrkraft T07", ElementStatus.REMOVED, kind = "TEACHER"))),
            rooms = listOf(Slot(foyerRoom, removedRoom)),
        )
        val view = LessonViews.of(lesson, "Aula")
        assertFalse("Should not be Foyer with prefix 'Aula'", view.isFoyer)
    }

    /** Old-cache lesson with no kinds → positional fallback, subjects from position1 */
    @Test
    fun testOldCacheFallback() {
        // Elements without kind (default "")
        val subject = Element("SP-LK2", "Sport LK", ElementStatus.REGULAR)
        val teacher = Element("T07", "Lehrkraft T07", ElementStatus.REGULAR)
        val room = Element("R106", "Sek II", ElementStatus.REGULAR)
        val lesson = Lesson(
            date = LocalDate.of(2026, 10, 1),
            start = LocalTime.of(9, 55),
            end = LocalTime.of(11, 30),
            periodIds = listOf(100L, 101L),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subject, null)),
            teachers = listOf(Slot(teacher, null)),
            rooms = listOf(Slot(room, null)),
        )
        val view = LessonViews.of(lesson, "Foyer")

        // Positional fallback: subjects from lesson.subjects
        assertEquals(1, view.subjects.size)
        assertEquals("SP-LK2", view.subjects.first().current?.shortName)
        // infos should be empty for old-cache
        assertTrue("infos should be empty for old-cache lesson", view.infos.isEmpty())
        assertEquals(LessonKind.REGULAR, view.kind)
    }
}
