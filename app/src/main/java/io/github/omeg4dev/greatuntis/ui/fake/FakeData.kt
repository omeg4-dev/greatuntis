package io.github.omeg4dev.greatuntis.ui.fake

import io.github.omeg4dev.greatuntis.api.ApiHoliday
import io.github.omeg4dev.greatuntis.api.ApiHolidayType
import io.github.omeg4dev.greatuntis.calendar.FreeCalendar
import io.github.omeg4dev.greatuntis.model.Absence
import io.github.omeg4dev.greatuntis.model.AbsenceCategory
import io.github.omeg4dev.greatuntis.model.AbsenceStatsResult
import io.github.omeg4dev.greatuntis.model.Account
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.DayStatus
import io.github.omeg4dev.greatuntis.model.Element
import io.github.omeg4dev.greatuntis.model.ElementStatus
import io.github.omeg4dev.greatuntis.model.Holiday
import io.github.omeg4dev.greatuntis.model.Lesson
import io.github.omeg4dev.greatuntis.model.LessonStatus
import io.github.omeg4dev.greatuntis.model.MasterData
import io.github.omeg4dev.greatuntis.model.SchoolInfo
import io.github.omeg4dev.greatuntis.model.SchoolYear
import io.github.omeg4dev.greatuntis.model.Slot
import io.github.omeg4dev.greatuntis.model.TimeUnit
import io.github.omeg4dev.greatuntis.ui.AppDeps
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object FakeData {

    val timeGrid = listOf(
        TimeUnit(1, LocalTime.of(8, 0), LocalTime.of(8, 45)),
        TimeUnit(2, LocalTime.of(8, 50), LocalTime.of(9, 35)),
        TimeUnit(3, LocalTime.of(9, 55), LocalTime.of(10, 40)),
        TimeUnit(4, LocalTime.of(10, 45), LocalTime.of(11, 30)),
        TimeUnit(5, LocalTime.of(11, 45), LocalTime.of(12, 30)),
        TimeUnit(6, LocalTime.of(12, 35), LocalTime.of(13, 20)),
        TimeUnit(7, LocalTime.of(13, 30), LocalTime.of(14, 15)),
        TimeUnit(8, LocalTime.of(14, 15), LocalTime.of(15, 0)),
        TimeUnit(9, LocalTime.of(15, 0), LocalTime.of(15, 45)),
    )

    val schoolYear = SchoolYear(
        id = 1L,
        name = "2026/2027",
        start = LocalDate.of(2026, 8, 31),
        end = LocalDate.of(2027, 7, 18),
    )

    val holidays = listOf(
        Holiday("Herbstferien", LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 31)),
        Holiday("Weihnachtsferien", LocalDate.of(2026, 12, 23), LocalDate.of(2027, 1, 6)),
    )

    val masterData = MasterData(
        personId = 1L,
        displayName = "Max Mustermann",
        schoolName = "Musterschule Beispielstadt",
        timeGrid = timeGrid,
        holidays = holidays,
        schoolYears = listOf(schoolYear),
    )

    val sampleAccount = Account(
        host = "demo.webuntis.com",
        school = "demo-schule",
        user = "mmustermann",
        key = "TY6CQ6J5X7Y2Z3A4",
        schoolNumber = "1234567",
    )

    val sampleSchools = listOf(
        SchoolInfo(
            displayName = "Musterschule Beispielstadt",
            address = "Musterstraße 1, 12345 Beispielstadt",
            loginName = "demo-schule",
            host = "demo.webuntis.com",
            schoolNumber = "1234567",
        ),
        SchoolInfo(
            displayName = "Gymnasium Nordstadt",
            address = "Nordstraße 10, 12345 Nordstadt",
            loginName = "gym-nordstadt",
            host = "gym-nordstadt.webuntis.com",
            schoolNumber = "2345678",
        ),
        SchoolInfo(
            displayName = "Gesamtschule Süd",
            address = "Südstraße 20, 12345 Südstadt",
            loginName = "ge-sued",
            host = "ge-sued.webuntis.com",
            schoolNumber = "3456789",
        ),
    )

    // Elements
    val subPH = Element("IF-GK1", "Informatik GK", ElementStatus.REGULAR)
    val subE = Element("PH-GK4", "Physik GK", ElementStatus.REGULAR)
    val subKU = Element("SW-GK1", "Sozialwissenschaften GK", ElementStatus.REGULAR)
    val subD = Element("KR-GK4", "Kath. Religion GK", ElementStatus.REGULAR)
    val subIF = Element("KU-LK1", "Kunst LK", ElementStatus.REGULAR)
    val subM = Element("SP-LK2", "Sport LK", ElementStatus.REGULAR)
    val subSW = Element("EK-ZK1", "Erdkunde ZK", ElementStatus.REGULAR)
    val subKR = Element("GE-GK2", "Geschichte GK", ElementStatus.REGULAR)
    val subSP = Element("D-GK1", "Deutsch GK", ElementStatus.REGULAR)
    val subEK = Element("PKI-GK2", "PKI GK", ElementStatus.REGULAR)

    val t01 = Element("T01", "Lehrkraft T01", ElementStatus.REGULAR)
    val t02 = Element("T02", "Lehrkraft T02", ElementStatus.REGULAR)
    val t03 = Element("T03", "Lehrkraft T03", ElementStatus.REGULAR)
    val t04 = Element("T04", "Lehrkraft T04", ElementStatus.REGULAR)
    val t05 = Element("T05", "Lehrkraft T05", ElementStatus.REGULAR)
    val t06 = Element("T06", "Lehrkraft T06", ElementStatus.REGULAR)
    val t07 = Element("T07", "Lehrkraft T07", ElementStatus.REGULAR)
    val t08 = Element("T08", "Lehrkraft T08", ElementStatus.REGULAR)
    val t09 = Element("T09", "Lehrkraft T09", ElementStatus.REGULAR)
    val t10 = Element("T10", "Lehrkraft T10", ElementStatus.REGULAR)

    val r137 = Element("R109", "Raum R109", ElementStatus.REGULAR)
    val r204 = Element("R116", "Raum R116", ElementStatus.REGULAR)
    val r010 = Element("R101", "Raum R101", ElementStatus.REGULAR)
    val r203 = Element("R115", "Raum R115", ElementStatus.REGULAR)
    val r159 = Element("R114", "Raum R114", ElementStatus.REGULAR)
    val r208 = Element("R118", "Raum R118", ElementStatus.REGULAR)
    val r119 = Element("R106", "Raum R106", ElementStatus.REGULAR)
    val r213 = Element("R121", "Raum R121", ElementStatus.REGULAR)
    val r215 = Element("R123", "Raum R123", ElementStatus.REGULAR)
    val r211 = Element("R120", "Raum R120", ElementStatus.REGULAR)
    val rSPH1 = Element("R125", "Raum R125", ElementStatus.REGULAR)
    val r154 = Element("R113", "Raum R113", ElementStatus.REGULAR)
    val r131 = Element("R107", "Raum R107", ElementStatus.REGULAR)
    val r209 = Element("R119", "Raum R119", ElementStatus.REGULAR)
    val rFoyer = Element("Foyer", "Foyer", ElementStatus.ADDED)

    private fun regularMon(date: LocalDate, baseId: Long): List<Lesson> = listOf(
        Lesson(
            date = date,
            start = LocalTime.of(8, 0),
            end = LocalTime.of(9, 35),
            periodIds = listOf(baseId + 1, baseId + 2),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subPH, null)),
            teachers = listOf(Slot(t01, null)),
            rooms = listOf(Slot(r137, null)),
        ),
        Lesson(
            date = date,
            start = LocalTime.of(9, 55),
            end = LocalTime.of(11, 30),
            periodIds = listOf(baseId + 3, baseId + 4),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subE, null)),
            teachers = listOf(Slot(t02, null)),
            rooms = listOf(Slot(r204, null)),
        ),
        Lesson(
            date = date,
            start = LocalTime.of(11, 45),
            end = LocalTime.of(13, 20),
            periodIds = listOf(baseId + 5, baseId + 6),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subKU, null)),
            teachers = listOf(Slot(t03, null)),
            rooms = listOf(Slot(r010, null)),
        ),
        Lesson(
            date = date,
            start = LocalTime.of(14, 15),
            end = LocalTime.of(15, 45),
            periodIds = listOf(baseId + 7, baseId + 8),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subSP, null)),
            teachers = listOf(Slot(t09, null)),
            rooms = listOf(Slot(rSPH1, null)),
        ),
    )

    private fun regularTue(date: LocalDate, baseId: Long): List<Lesson> = listOf(
        Lesson(
            date = date,
            start = LocalTime.of(8, 0),
            end = LocalTime.of(9, 35),
            periodIds = listOf(baseId + 1, baseId + 2),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subD, null)),
            teachers = listOf(Slot(t04, null)),
            rooms = listOf(Slot(r203, null)),
        ),
        Lesson(
            date = date,
            start = LocalTime.of(9, 55),
            end = LocalTime.of(11, 30),
            periodIds = listOf(baseId + 3, baseId + 4),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subIF, null)),
            teachers = listOf(Slot(t05, null)),
            rooms = listOf(Slot(r208, null)),
        ),
        Lesson(
            date = date,
            start = LocalTime.of(11, 45),
            end = LocalTime.of(13, 20),
            periodIds = listOf(baseId + 5, baseId + 6),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subM, null)),
            teachers = listOf(Slot(t07, null)),
            rooms = listOf(Slot(r119, null)),
        ),
        Lesson(
            date = date,
            start = LocalTime.of(14, 15),
            end = LocalTime.of(15, 45),
            periodIds = listOf(baseId + 7, baseId + 8),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subSW, null)),
            teachers = listOf(Slot(t08, null)),
            rooms = listOf(Slot(r154, null)),
        ),
    )

    private fun regularWed(date: LocalDate, baseId: Long): List<Lesson> = listOf(
        Lesson(
            date = date,
            start = LocalTime.of(8, 0),
            end = LocalTime.of(9, 35),
            periodIds = listOf(baseId + 1, baseId + 2),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subIF, null)),
            teachers = listOf(Slot(t05, null)),
            rooms = listOf(Slot(r208, null)),
        ),
        Lesson(
            date = date,
            start = LocalTime.of(9, 55),
            end = LocalTime.of(11, 30),
            periodIds = listOf(baseId + 3, baseId + 4),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subM, null)),
            teachers = listOf(Slot(t07, null)),
            rooms = listOf(Slot(r119, null)),
        ),
        Lesson(
            date = date,
            start = LocalTime.of(11, 45),
            end = LocalTime.of(13, 20),
            periodIds = listOf(baseId + 5, baseId + 6),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subEK, null)),
            teachers = listOf(Slot(t10, null)),
            rooms = listOf(Slot(r131, null)),
        ),
        Lesson(
            date = date,
            start = LocalTime.of(14, 15),
            end = LocalTime.of(15, 0),
            periodIds = listOf(baseId + 7),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subE, null)),
            teachers = listOf(Slot(t02, null)),
            rooms = listOf(Slot(r137, null)),
        ),
    )

    private fun regularThu(date: LocalDate, baseId: Long): List<Lesson> = listOf(
        Lesson(
            date = date,
            start = LocalTime.of(8, 0),
            end = LocalTime.of(9, 35),
            periodIds = listOf(baseId + 1, baseId + 2),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subD, null)),
            teachers = listOf(Slot(t04, null)),
            rooms = listOf(Slot(r203, null)),
        ),
        Lesson(
            date = date,
            start = LocalTime.of(9, 55),
            end = LocalTime.of(11, 30),
            periodIds = listOf(baseId + 3, baseId + 4),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subPH, null)),
            teachers = listOf(Slot(t01, null)),
            rooms = listOf(Slot(r137, null)),
        ),
        Lesson(
            date = date,
            start = LocalTime.of(11, 45),
            end = LocalTime.of(13, 20),
            periodIds = listOf(baseId + 5, baseId + 6),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subIF, null)),
            teachers = listOf(Slot(t05, null)),
            rooms = listOf(Slot(r208, null)),
        ),
        Lesson(
            date = date,
            start = LocalTime.of(13, 30),
            end = LocalTime.of(15, 0),
            periodIds = listOf(baseId + 7, baseId + 8),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subM, null)),
            teachers = listOf(Slot(t07, null)),
            rooms = listOf(Slot(r119, null)),
        ),
    )

    private fun regularFri(date: LocalDate, baseId: Long): List<Lesson> = listOf(
        Lesson(
            date = date,
            start = LocalTime.of(8, 0),
            end = LocalTime.of(9, 35),
            periodIds = listOf(baseId + 1, baseId + 2),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subKU, null)),
            teachers = listOf(Slot(t03, null)),
            rooms = listOf(Slot(r010, null)),
        ),
        Lesson(
            date = date,
            start = LocalTime.of(9, 55),
            end = LocalTime.of(10, 40),
            periodIds = listOf(baseId + 3),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subKR, null)),
            teachers = listOf(Slot(t06, null)),
            rooms = listOf(Slot(r215, null)),
        ),
        Lesson(
            date = date,
            start = LocalTime.of(10, 45),
            end = LocalTime.of(11, 30),
            periodIds = listOf(baseId + 4),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subKR, null)),
            teachers = listOf(Slot(t06, null)),
            rooms = listOf(Slot(r211, null)),
        ),
        Lesson(
            date = date,
            start = LocalTime.of(11, 45),
            end = LocalTime.of(13, 20),
            periodIds = listOf(baseId + 5, baseId + 6),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subSW, null)),
            teachers = listOf(Slot(t08, null)),
            rooms = listOf(Slot(r154, null)),
        ),
    )

    // Lessons for week of 2026-11-02
    val lessonsTueNov3 = listOf(
        // Tue 03.11 08:00–09:35 EXAM KR-GK4 (room R114 changed from R115)
        Lesson(
            date = LocalDate.of(2026, 11, 3),
            start = LocalTime.of(8, 0),
            end = LocalTime.of(9, 35),
            periodIds = listOf(3637269L, 3637272L),
            type = "EXAM",
            status = LessonStatus.CHANGED,
            subjects = listOf(Slot(subD, null)),
            teachers = listOf(Slot(t05, null)),
            rooms = listOf(
                Slot(
                    current = Element("R114", "Sek II Sprachen", ElementStatus.ADDED),
                    removed = Element("R115", "Sek II", ElementStatus.REMOVED),
                )
            ),
            lessonInfo = "Klausur KR-GK4/610",
            substitutionText = "eigenverantwortliches Arbeiten",
        ),
        // Tue 09:55–10:40 EXAM KR-GK4 in lane 0
        Lesson(
            date = LocalDate.of(2026, 11, 3),
            start = LocalTime.of(9, 55),
            end = LocalTime.of(10, 40),
            periodIds = listOf(3907084L),
            type = "EXAM",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subD, null)),
            teachers = listOf(Slot(t05, null)),
            rooms = listOf(Slot(r159, null)),
            lessonInfo = "Klausur KR-GK4/610",
        ),
        // Tue 09:55–11:30 CANCELLED KU-LK1 in lane 1
        Lesson(
            date = LocalDate.of(2026, 11, 3),
            start = LocalTime.of(9, 55),
            end = LocalTime.of(11, 30),
            periodIds = listOf(3666500L, 3666503L),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.CANCELLED,
            subjects = listOf(Slot(subIF, null)),
            teachers = listOf(Slot(t06, null)),
            rooms = listOf(Slot(r208, null)),
            substitutionText = "eigenverantwortliches Arbeiten",
        ),
        // Tue 10:45–11:30 EXAM KR-GK4 in lane 0
        Lesson(
            date = LocalDate.of(2026, 11, 3),
            start = LocalTime.of(10, 45),
            end = LocalTime.of(11, 30),
            periodIds = listOf(3907087L),
            type = "EXAM",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subD, null)),
            teachers = listOf(Slot(t05, null)),
            rooms = listOf(Slot(r159, null)),
            lessonInfo = "Klausur KR-GK4/610",
        ),
        // Tue 11:45–13:20 SP-LK2 Foyer move
        Lesson(
            date = LocalDate.of(2026, 11, 3),
            start = LocalTime.of(11, 45),
            end = LocalTime.of(13, 20),
            periodIds = listOf(3695844L, 3695847L),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.CHANGED,
            subjects = listOf(Slot(subM, null)),
            teachers = listOf(
                Slot(
                    current = null,
                    removed = Element("T07", "Lehrkraft T07", ElementStatus.REMOVED),
                )
            ),
            rooms = listOf(
                Slot(
                    current = Element("Foyer", "Foyer", ElementStatus.ADDED),
                    removed = Element("R106", "R106", ElementStatus.REMOVED),
                )
            ),
            substitutionText = "eigenverantwortliches Arbeiten",
        ),
        // Tue 14:15–15:45 EK-ZK1
        Lesson(
            date = LocalDate.of(2026, 11, 3),
            start = LocalTime.of(14, 15),
            end = LocalTime.of(15, 45),
            periodIds = listOf(3723925L, 3723928L),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subSW, null)),
            teachers = listOf(Slot(Element("T46", "Lehrkraft T46", ElementStatus.REGULAR), null)),
            rooms = listOf(Slot(r213, null)),
        ),
    )

    val lessonsThuNov5 = listOf(
        Lesson(
            date = LocalDate.of(2026, 11, 5),
            start = LocalTime.of(8, 0),
            end = LocalTime.of(15, 0),
            periodIds = listOf(5099L),
            type = "EVENT",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(Element("Päd. Tag", "Pädagogischer Tag", ElementStatus.REGULAR, "INFO"), null)),
            teachers = emptyList(),
            rooms = emptyList(),
            lessonInfo = "Päd. Tag",
        ),
    ) + regularThu(LocalDate.of(2026, 11, 5), 5100L).map { it.copy(status = LessonStatus.CANCELLED) }

    val lessonsWedNov4 = listOf(
        Lesson(
            date = LocalDate.of(2026, 11, 4),
            start = LocalTime.of(8, 0),
            end = LocalTime.of(9, 35),
            periodIds = listOf(4001L, 4002L),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subIF, null)),
            teachers = listOf(Slot(t05, null)),
            rooms = listOf(Slot(r208, null)),
        ),
        Lesson(
            date = LocalDate.of(2026, 11, 4),
            start = LocalTime.of(9, 55),
            end = LocalTime.of(11, 30),
            periodIds = listOf(4003L, 4004L),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subM, null)),
            teachers = listOf(Slot(t07, null)),
            rooms = listOf(Slot(r119, null)),
        ),
        Lesson(
            date = LocalDate.of(2026, 11, 4),
            start = LocalTime.of(11, 45),
            end = LocalTime.of(13, 20),
            periodIds = listOf(4005L, 4006L),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subEK, null)),
            teachers = listOf(Slot(t10, null)),
            rooms = listOf(Slot(r131, null)),
        ),
        // Wed 14:15–15:00 PH-GK4 moved to Foyer the same way
        Lesson(
            date = LocalDate.of(2026, 11, 4),
            start = LocalTime.of(14, 15),
            end = LocalTime.of(15, 0),
            periodIds = listOf(4007L),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.CHANGED,
            subjects = listOf(Slot(subE, null)),
            teachers = listOf(
                Slot(
                    current = null,
                    removed = Element("T02", "Lehrkraft T02", ElementStatus.REMOVED),
                )
            ),
            rooms = listOf(
                Slot(
                    current = Element("Foyer", "Foyer", ElementStatus.ADDED),
                    removed = Element("R109", "R109", ElementStatus.REMOVED),
                )
            ),
            substitutionText = "eigenverantwortliches Arbeiten",
        ),
    )

    val lessonsFriNov6 = listOf(
        Lesson(
            date = LocalDate.of(2026, 11, 6),
            start = LocalTime.of(8, 0),
            end = LocalTime.of(9, 35),
            periodIds = listOf(6001L, 6002L),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subKU, null)),
            teachers = listOf(Slot(t03, null)),
            rooms = listOf(Slot(r010, null)),
        ),
        // Fri 09:55 GE-GK2 room R123 → R120
        Lesson(
            date = LocalDate.of(2026, 11, 6),
            start = LocalTime.of(9, 55),
            end = LocalTime.of(10, 40),
            periodIds = listOf(6003L),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.CHANGED,
            subjects = listOf(Slot(subKR, null)),
            teachers = listOf(Slot(t06, null)),
            rooms = listOf(
                Slot(
                    current = Element("R120", "R120", ElementStatus.ADDED),
                    removed = Element("R123", "R123", ElementStatus.REMOVED),
                )
            ),
        ),
        Lesson(
            date = LocalDate.of(2026, 11, 6),
            start = LocalTime.of(10, 45),
            end = LocalTime.of(11, 30),
            periodIds = listOf(6004L),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subKR, null)),
            teachers = listOf(Slot(t06, null)),
            rooms = listOf(Slot(r211, null)),
        ),
        Lesson(
            date = LocalDate.of(2026, 11, 6),
            start = LocalTime.of(11, 45),
            end = LocalTime.of(13, 20),
            periodIds = listOf(6005L, 6006L),
            type = "NORMAL_TEACHING_PERIOD",
            status = LessonStatus.REGULAR,
            subjects = listOf(Slot(subSW, null)),
            teachers = listOf(Slot(t08, null)),
            rooms = listOf(Slot(r154, null)),
        ),
    )

    fun createDays(): List<Day> {
        val days = mutableListOf<Day>()

        // Week 2026-10-05
        val w1Mon = LocalDate.of(2026, 10, 5)
        days.add(Day(w1Mon, DayStatus.REGULAR, regularMon(w1Mon, 1000L)))
        days.add(Day(w1Mon.plusDays(1), DayStatus.REGULAR, regularTue(w1Mon.plusDays(1), 1100L)))
        days.add(Day(w1Mon.plusDays(2), DayStatus.REGULAR, regularWed(w1Mon.plusDays(2), 1200L)))
        days.add(Day(w1Mon.plusDays(3), DayStatus.REGULAR, regularThu(w1Mon.plusDays(3), 1300L)))
        days.add(Day(w1Mon.plusDays(4), DayStatus.REGULAR, regularFri(w1Mon.plusDays(4), 1400L)))

        // Week 2026-10-12
        val w2Mon = LocalDate.of(2026, 10, 12)
        days.add(Day(w2Mon, DayStatus.REGULAR, regularMon(w2Mon, 2000L)))
        days.add(Day(w2Mon.plusDays(1), DayStatus.REGULAR, regularTue(w2Mon.plusDays(1), 2100L)))
        days.add(Day(w2Mon.plusDays(2), DayStatus.REGULAR, regularWed(w2Mon.plusDays(2), 2200L)))
        days.add(Day(w2Mon.plusDays(3), DayStatus.REGULAR, regularThu(w2Mon.plusDays(3), 2300L)))
        days.add(Day(w2Mon.plusDays(4), DayStatus.REGULAR, regularFri(w2Mon.plusDays(4), 2400L)))

        // Week 2026-11-02
        val w3Mon = LocalDate.of(2026, 11, 2)
        days.add(Day(w3Mon, DayStatus.REGULAR, regularMon(w3Mon, 3000L)))
        days.add(Day(w3Mon.plusDays(1), DayStatus.REGULAR, lessonsTueNov3))
        days.add(Day(w3Mon.plusDays(2), DayStatus.REGULAR, lessonsWedNov4))
        days.add(Day(w3Mon.plusDays(3), DayStatus.REGULAR, lessonsThuNov5))
        days.add(Day(w3Mon.plusDays(4), DayStatus.REGULAR, lessonsFriNov6))

        // Week 2026-11-09 (Mon only; Tue–Fri of that week NOT_ALLOWED, no lessons)
        val w4Mon = LocalDate.of(2026, 11, 9)
        days.add(Day(w4Mon, DayStatus.REGULAR, regularMon(w4Mon, 4000L)))
        days.add(Day(w4Mon.plusDays(1), DayStatus.NOT_ALLOWED, emptyList()))
        days.add(Day(w4Mon.plusDays(2), DayStatus.NOT_ALLOWED, emptyList()))
        days.add(Day(w4Mon.plusDays(3), DayStatus.NOT_ALLOWED, emptyList()))
        days.add(Day(w4Mon.plusDays(4), DayStatus.NOT_ALLOWED, emptyList()))

        return days.sortedBy { it.date }
    }

    // Synthetic absences (from app/src/test/resources/fixtures/absences_2026.json) — never real ones.
    val absences = listOf(
        Absence(
            id = 900000L,
            start = LocalDateTime.of(2026, 9, 8, 8, 0),
            end = LocalDateTime.of(2026, 9, 8, 9, 35),
            reason = "Schultermin",
            excuseStatus = "zählt nicht",
            text = "",
        ),
        Absence(
            id = 900001L,
            start = LocalDateTime.of(2026, 9, 9, 8, 0),
            end = LocalDateTime.of(2026, 9, 9, 15, 0),
            reason = "Erkrankung",
            excuseStatus = "entschuldigt",
            text = "",
        ),
        Absence(
            id = 900002L,
            start = LocalDateTime.of(2026, 9, 17, 11, 45),
            end = LocalDateTime.of(2026, 9, 17, 12, 30),
            reason = "Arzttermin",
            excuseStatus = "entschuldigt",
            text = "",
        ),
        Absence(
            id = 900003L,
            start = LocalDateTime.of(2026, 9, 23, 8, 0),
            end = LocalDateTime.of(2026, 9, 25, 15, 0),
            reason = "Erkrankung",
            excuseStatus = "entschuldigt",
            text = "",
        ),
        Absence(
            id = 900004L,
            start = LocalDateTime.of(2026, 9, 24, 9, 55),
            end = LocalDateTime.of(2026, 9, 24, 11, 30),
            reason = "Arzttermin",
            excuseStatus = "entschuldigt",
            text = "",
        ),
        Absence(
            id = 900005L,
            start = LocalDateTime.of(2026, 9, 25, 10, 45),
            end = LocalDateTime.of(2026, 9, 25, 11, 30),
            reason = "Abwesend",
            excuseStatus = "nicht akzeptiert",
            text = "",
        ),
        Absence(
            id = 900006L,
            start = LocalDateTime.of(2026, 10, 5, 8, 0),
            end = LocalDateTime.of(2026, 10, 5, 15, 0),
            reason = "Erkrankung",
            excuseStatus = null,
            text = "",
        ),
        Absence(
            id = 900007L,
            start = LocalDateTime.of(2026, 10, 12, 8, 0),
            end = LocalDateTime.of(2026, 10, 12, 15, 0),
            reason = "Erkrankung",
            excuseStatus = "entschuldigt",
            text = "",
        ),
        Absence(
            id = 900008L,
            start = LocalDateTime.of(2026, 10, 14, 8, 0),
            end = LocalDateTime.of(2026, 10, 14, 15, 0),
            reason = "Erkrankung",
            excuseStatus = "nicht entschuldigt",
            text = "",
        ),
        Absence(
            id = 900009L,
            start = LocalDateTime.of(2026, 10, 15, 8, 0),
            end = LocalDateTime.of(2026, 10, 15, 8, 4),
            reason = "Verspätung",
            excuseStatus = "nicht entschuldbar",
            text = "",
        ),
        Absence(
            id = 900010L,
            start = LocalDateTime.of(2026, 11, 2, 9, 55),
            end = LocalDateTime.of(2026, 11, 2, 11, 30),
            reason = "Abwesend",
            excuseStatus = null,
            text = "",
        ),
        Absence(
            id = 900011L,
            start = LocalDateTime.of(2026, 11, 4, 8, 0),
            end = LocalDateTime.of(2026, 11, 4, 15, 0),
            reason = "Schultermin",
            excuseStatus = "zählt nicht",
            text = "",
        ),
        Absence(
            id = 900012L,
            start = LocalDateTime.of(2026, 11, 5, 8, 0),
            end = LocalDateTime.of(2026, 11, 5, 8, 5),
            reason = "Verspätung",
            excuseStatus = "nicht entschuldbar",
            text = "",
        ),
        Absence(
            id = 900013L,
            start = LocalDateTime.of(2026, 11, 9, 8, 0),
            end = LocalDateTime.of(2026, 11, 9, 8, 5),
            reason = "Verspätung",
            excuseStatus = "entschuldigt",
            text = "",
        ),
    )

    // Synthetic numbers (from app/src/test/resources/fixtures/stats_expected_2026.json) — never real absences.
    val fakeAbsenceStatsResult = AbsenceStatsResult(
        heldUnits = 224,
        missedUnits = 50,
        missedDays = 11,
        missedByCategory = mapOf(
            AbsenceCategory.EXCUSED to 31,
            AbsenceCategory.NOT_COUNTED to 8,
            AbsenceCategory.OPEN to 8,
            AbsenceCategory.UNEXCUSED to 3,
        ),
        missedBySubject = mapOf(
            "KR-GK4" to 5,
            "PH-GK4" to 10,
            "PKI-GK2" to 4,
            "KU-LK1" to 3,
            "GE-GK2" to 7,
            "SW-GK1" to 8,
            "SP-LK2" to 2,
            "IF-GK1" to 5,
            "D-GK1" to 6,
        ),
        heldBySubject = mapOf(
            "KR-GK4" to 19,
            "PH-GK4" to 21,
            "PKI-GK2" to 17,
            "KU-LK1" to 29,
            "GE-GK2" to 17,
            "SW-GK1" to 23,
            "Klausur GK PKI/ST+SÖ" to 4,
            "Klausur LK KU/HS+ LK M/RE" to 5,
            "SP-LK2" to 32,
            "IF-GK1" to 22,
            "D-GK1" to 21,
            "EK-ZK1" to 14,
        ),
        missedByWeekday = mapOf(
            1 to 14,
            2 to 2,
            3 to 21,
            4 to 6,
            5 to 7,
        ),
        missedByPeriod = mapOf(
            1 to 5,
            2 to 8,
            3 to 8,
            4 to 8,
            5 to 7,
            6 to 1,
            7 to 6,
            8 to 7,
        ),
        missedByWeek = mapOf(
            LocalDate.of(2026, 9, 7) to 8,
            LocalDate.of(2026, 9, 14) to 1,
            LocalDate.of(2026, 9, 21) to 18,
            LocalDate.of(2026, 10, 5) to 6,
            LocalDate.of(2026, 10, 12) to 9,
            LocalDate.of(2026, 11, 2) to 8,
        ),
        absenceCount = 11,
        openAbsences = 2,
        lateCount = 3,
        lateMinutes = 14,
        lateByWeekday = mapOf(
            1 to 1,
            4 to 2,
        ),
        cancelledBySubject = mapOf(
            "KR-GK4" to 5,
            "PH-GK4" to 5,
            "PKI-GK2" to 7,
            "KU-LK1" to 9,
            "GE-GK2" to 7,
            "SW-GK1" to 3,
            "SP-LK2" to 12,
            "IF-GK1" to 3,
            "D-GK1" to 3,
            "EK-ZK1" to 2,
        ),
        foyerBySubject = mapOf(
            "PH-GK4" to 3,
            "KU-LK1" to 2,
            "SW-GK1" to 2,
            "SP-LK2" to 2,
            "IF-GK1" to 2,
            "EK-ZK1" to 2,
        ),
    )

    fun fakePredict(
        history: List<Day>,
        dates: List<LocalDate>,
        grid: List<TimeUnit>,
        isFree: (LocalDate) -> Boolean,
    ): List<Day> {
        val dayByDate = history.associateBy { it.date }
        return dates.map { date ->
            if (isFree(date)) {
                return@map Day(date, DayStatus.NO_DATA, emptyList())
            }
            val existing = dayByDate[date]
            if (existing != null && existing.status != DayStatus.NOT_ALLOWED) {
                existing
            } else {
                val prevWeekDate = date.minusWeeks(1)
                val prevDay = dayByDate[prevWeekDate]
                if (prevDay != null) {
                    val predictedLessons = prevDay.lessons.map { lesson ->
                        lesson.copy(
                            date = date,
                            predicted = true,
                            status = LessonStatus.REGULAR,
                            periodIds = emptyList(),
                        )
                    }
                    Day(date = date, status = DayStatus.REGULAR, lessons = predictedLessons)
                } else {
                    existing ?: Day(date = date, status = DayStatus.NO_DATA, lessons = emptyList())
                }
            }
        }
    }

    fun fakeParseAccount(text: String): Account? {
        if (!text.contains("key=")) return null
        return sampleAccount
    }

    val fakeApiHolidays = listOf(
        ApiHoliday("Allerheiligen", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1), ApiHolidayType.PUBLIC),
    )

    fun createFakeFreeCalendar(): FreeCalendar {
        val days = createDays()
        val schoolDays = days.filter { it.lessons.any { l -> l.status != LessonStatus.CANCELLED && l.type != "EVENT" } }
            .map { it.date }.toSet()
        val timetableFree = mapOf(
            LocalDate.of(2026, 11, 5) to "Pädagogischer Tag"
        )
        return FreeCalendar.build(
            webUntis = holidays,
            api = fakeApiHolidays,
            schoolDays = schoolDays,
            timetableFree = timetableFree,
            noDataWeekdays = emptySet(),
            range = LocalDate.of(2026, 8, 1)..LocalDate.of(2027, 7, 31),
        )
    }

    fun createFakeDeps(
        fixedClock: LocalDateTime? = null,
        initialLoggedIn: Boolean = true,
    ): AppDeps {
        val repo = FakeUntisRepository(initialLoggedIn = initialLoggedIn)
        return AppDeps(
            repo = repo,
            parseAccount = ::fakeParseAccount,
            computeStats = { _, _, _, _ -> fakeAbsenceStatsResult },
            predict = ::fakePredict,
            clock = if (fixedClock != null) { { fixedClock } } else { { LocalDateTime.now() } },
            onRequestBatteryExemption = {},
            appVersion = "0.1.0",
        )
    }
}
