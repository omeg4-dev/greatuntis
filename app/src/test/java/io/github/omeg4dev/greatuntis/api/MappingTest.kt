package io.github.omeg4dev.greatuntis.api

import io.github.omeg4dev.greatuntis.Fixtures
import io.github.omeg4dev.greatuntis.model.DayStatus
import io.github.omeg4dev.greatuntis.model.ElementStatus
import io.github.omeg4dev.greatuntis.model.LessonStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class MappingTest {

    @Test
    fun testUserDataMapping() {
        val json = Fixtures.load("userdata.json")
        val dto = UntisJson.decodeFromString<UserDataResultDto>(json)
        val masterData = toMasterData(dto)

        assertEquals(9, masterData.timeGrid.size)
        assertEquals(1, masterData.timeGrid[0].index)
        assertEquals(LocalTime.of(8, 0), masterData.timeGrid[0].start)
        assertEquals(LocalTime.of(8, 45), masterData.timeGrid[0].end)

        val unit9 = masterData.timeGrid[8]
        assertEquals(9, unit9.index)
        assertEquals(LocalTime.of(15, 0), unit9.start)
        assertEquals(LocalTime.of(15, 45), unit9.end)

        assertTrue(masterData.schoolYears.size >= 5)
        assertEquals(1L, masterData.personId)
        assertEquals("Student", masterData.displayName)
    }

    @Test
    fun testTimetableDayFoyerMapping() {
        val json = Fixtures.load("timetable_day_foyer.json")
        val dto = UntisJson.decodeFromString<TimetableResponseDto>(json)
        val days = toDays(dto)

        assertEquals(1, days.size)
        val day = days[0]
        assertEquals(LocalDate.of(2026, 11, 3), day.date)
        assertEquals(DayStatus.REGULAR, day.status)

        // Find the SP-LK2 lesson at 11:45–13:20
        val lesson = day.lessons.firstOrNull { it.subject == "SP-LK2" && it.start == LocalTime.of(11, 45) }
        assertNotNull("SP-LK2 lesson at 11:45 must exist", lesson)
        lesson!!

        assertEquals(LocalTime.of(13, 20), lesson.end)
        assertEquals(LessonStatus.CHANGED, lesson.status)
        assertEquals("eigenverantwortliches Arbeiten", lesson.substitutionText)

        // Teacher slot: current == null, removed.shortName non-null
        assertEquals(1, lesson.teachers.size)
        val teacherSlot = lesson.teachers[0]
        assertNull(teacherSlot.current)
        assertNotNull(teacherSlot.removed)
        assertNotNull(teacherSlot.removed?.shortName)

        // Room slot: current.shortName == "Foyer" with status ADDED and removed.shortName == "R106"
        assertEquals(1, lesson.rooms.size)
        val roomSlot = lesson.rooms[0]
        assertNotNull(roomSlot.current)
        assertEquals("Foyer", roomSlot.current?.shortName)
        assertEquals(ElementStatus.ADDED, roomSlot.current?.status)
        assertNotNull(roomSlot.removed)
        assertEquals("R106", roomSlot.removed?.shortName)
    }

    @Test
    fun testTimetableWindowMapping() {
        val json = Fixtures.load("timetable_window.json")
        val dto = UntisJson.decodeFromString<TimetableResponseDto>(json)
        val days = toDays(dto)

        val dayNov10 = days.firstOrNull { it.date == LocalDate.of(2026, 11, 10) }
        assertNotNull("Day 2026-11-10 must exist", dayNov10)
        assertEquals(DayStatus.NOT_ALLOWED, dayNov10!!.status)
    }

    @Test
    fun testAbsences2026Mapping() {
        val json = Fixtures.load("absences_2026.json")
        val dto = UntisJson.decodeFromString<AbsenceResponseDto>(json)
        val absences = toAbsences(dto)

        assertEquals(14, absences.size)
        val first = absences[0]
        assertEquals(LocalDateTime.of(2026, 9, 8, 8, 0), first.start)
        assertEquals(LocalDateTime.of(2026, 9, 8, 9, 35), first.end)
        assertEquals("Schultermin", first.reason)
        assertEquals("zählt nicht", first.excuseStatus)
    }

    @Test
    fun testSchoolSearchMapping() {
        val json = Fixtures.load("schoolsearch.json")
        val dto = UntisJson.decodeFromString<SchoolSearchResponseDto>(json)
        val schools = toSchools(dto)

        assertEquals(3, schools.size)
        val school = schools[0]
        assertEquals("demo.webuntis.com", school.host)
        assertEquals("1234567", school.schoolNumber)
        assertEquals("demo-schule", school.loginName)
    }

    @Test
    fun testElementKindMapped() {
        // 2026-10-06 has an EXAM with position1 containing type=INFO elements
        val json = Fixtures.load("timetable_2026.json")
        val dto = UntisJson.decodeFromString<TimetableResponseDto>(json)
        val days = toDays(dto)

        val day = days.first { it.date == LocalDate.of(2026, 10, 6) }
        val exam = day.lessons.first { it.type == "EXAM" && it.start == LocalTime.of(8, 0) }

        // position1 elements should have kind="INFO" after mapping
        val infoSlot = exam.subjects.first()
        val infoElement = infoSlot.current
        assertNotNull("INFO element should be non-null", infoElement)
        assertEquals("INFO", infoElement!!.kind)

        // position2 elements (teacher) should have kind="TEACHER"
        val teacherSlot = exam.teachers.first()
        val teacherElement = teacherSlot.current
        assertNotNull("TEACHER element should be non-null", teacherElement)
        assertEquals("TEACHER", teacherElement!!.kind)
    }
}
