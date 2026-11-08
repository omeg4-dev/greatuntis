package io.github.omeg4dev.greatuntis.watch

import io.github.omeg4dev.greatuntis.Fixtures
import io.github.omeg4dev.greatuntis.api.TimetableResponseDto
import io.github.omeg4dev.greatuntis.api.UntisJson
import io.github.omeg4dev.greatuntis.api.UserDataResultDto
import io.github.omeg4dev.greatuntis.api.toDays
import io.github.omeg4dev.greatuntis.api.toMasterData
import io.github.omeg4dev.greatuntis.model.ChangeKind
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.DayStatus
import io.github.omeg4dev.greatuntis.model.Element
import io.github.omeg4dev.greatuntis.model.ElementStatus
import io.github.omeg4dev.greatuntis.model.Lesson
import io.github.omeg4dev.greatuntis.model.LessonStatus
import io.github.omeg4dev.greatuntis.model.Settings
import io.github.omeg4dev.greatuntis.model.Slot
import io.github.omeg4dev.greatuntis.model.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ChangeDetectorTest {

    private lateinit var timeGrid: List<TimeUnit>
    private lateinit var days: List<Day>

    @Before
    fun setUp() {
        val userJson = Fixtures.load("userdata.json")
        val masterData = toMasterData(UntisJson.decodeFromString<UserDataResultDto>(userJson))
        timeGrid = masterData.timeGrid

        val ttJson = Fixtures.load("timetable_day_foyer.json")
        days = toDays(UntisJson.decodeFromString<TimetableResponseDto>(ttJson))
    }

    @Test
    fun test1_initialDetectionWithFoyerCancelledRoomUrgentFirst() {
        val detector = ChangeDetector(Settings(), timeGrid)
        val now = LocalDateTime.of(2026, 11, 3, 6, 30)
        val result = detector.detect(days, now, emptyMap())

        val toNotify = result.toNotify
        assertTrue(toNotify.isNotEmpty())

        // Check urgent ones first: all urgent items appear before any non-urgent
        val firstNonUrgentIndex = toNotify.indexOfFirst { !it.urgent }
        if (firstNonUrgentIndex != -1) {
            val hasUrgentAfter = toNotify.drop(firstNonUrgentIndex).any { it.urgent }
            assertTrue("Urgent items must come first", !hasUrgentAfter)
        }

        // FOYER for SP-LK2
        val foyer = toNotify.firstOrNull { it.kind == ChangeKind.FOYER && it.subject == "SP-LK2" }
        assertNotNull("FOYER for SP-LK2 must be present", foyer)
        assertEquals("Foyer: SP-LK2 (5.–6. Std.)", foyer!!.title)
        assertTrue(foyer.text.contains("Di 03.11. 11:45"))
        assertTrue(foyer.text.contains("Raum R106 → Foyer"))
        assertTrue(foyer.text.contains("ohne Lehrkraft"))
        assertTrue(foyer.text.contains("eigenverantwortliches Arbeiten"))

        // CANCELLED for KU-LK1
        val cancelled = toNotify.firstOrNull { it.kind == ChangeKind.CANCELLED && it.subject == "KU-LK1" }
        assertNotNull("CANCELLED for KU-LK1 must be present", cancelled)
        assertEquals("Entfall: KU-LK1 (3.–4. Std.)", cancelled!!.title)
        assertEquals("Di 03.11. 09:55 · eigenverantwortliches Arbeiten", cancelled.text)

        // ROOM for KR-GK4 R115 → R114
        val room = toNotify.firstOrNull { it.kind == ChangeKind.ROOM && it.subject == "KR-GK4" }
        assertNotNull("ROOM for KR-GK4 must be present", room)
        assertEquals("Raumänderung: KR-GK4 (1.–2. Std.)", room!!.title)
        assertEquals("Di 03.11. 08:00 · Raum R115 → R114", room.text)
    }

    @Test
    fun test2_runAgainWithPreviousActiveGivesEmptyToNotify() {
        val detector = ChangeDetector(Settings(), timeGrid)
        val now = LocalDateTime.of(2026, 11, 3, 6, 30)
        val result1 = detector.detect(days, now, emptyMap())
        val result2 = detector.detect(days, now, result1.active)

        assertTrue(result2.toNotify.isEmpty())
    }

    @Test
    fun test3_restoredLessonEmitsRestored() {
        val detector = ChangeDetector(Settings(), timeGrid)
        val now = LocalDateTime.of(2026, 11, 3, 6, 30)
        val result1 = detector.detect(days, now, emptyMap())

        // Modify SP-LK2 to be regular in the Day
        val modifiedDay = days[0].copy(
            lessons = days[0].lessons.map { lesson ->
                if (lesson.subject == "SP-LK2") {
                    lesson.copy(
                        status = LessonStatus.REGULAR,
                        rooms = listOf(
                            Slot(
                                current = Element("R106", "Sek II", ElementStatus.REGULAR),
                                removed = null
                            )
                        ),
                        teachers = listOf(
                            Slot(
                                current = Element("T07", "Lehrkraft T07", ElementStatus.REGULAR),
                                removed = null
                            )
                        ),
                        substitutionText = null
                    )
                } else lesson
            }
        )

        val result2 = detector.detect(listOf(modifiedDay), now, result1.active)
        val restored = result2.toNotify.filter { it.kind == ChangeKind.RESTORED }
        assertEquals(1, restored.size)
        assertEquals("Wieder regulär: SP-LK2 (5.–6. Std.)", restored[0].title)
        assertEquals("Di 03.11. 11:45 · Raum R106", restored[0].text)
    }

    @Test
    fun test4_lessonsEndingBeforeNowNotReported() {
        val detector = ChangeDetector(Settings(), timeGrid)
        val now = LocalDateTime.of(2026, 11, 3, 12, 0)
        val result = detector.detect(days, now, emptyMap())

        // KR-GK4 08:00–09:35 must not be reported
        assertTrue(result.toNotify.none { it.subject == "KR-GK4" })
        assertTrue(result.toNotify.none { it.subject == "KU-LK1" }) // 09:55-11:30
        assertTrue(result.toNotify.any { it.subject == "SP-LK2" }) // 11:45-13:20
    }

    @Test
    fun test5_settingsFilterOnlyFoyerInToNotify() {
        val detector = ChangeDetector(Settings(notifyKinds = setOf(ChangeKind.FOYER)), timeGrid)
        val now = LocalDateTime.of(2026, 11, 3, 6, 30)
        val result = detector.detect(days, now, emptyMap())

        assertTrue(result.toNotify.all { it.kind == ChangeKind.FOYER })
        assertTrue(result.active.size > result.toNotify.size)
    }

    @Test
    fun test6_syntheticDayWithCancelledFirstLesson() {
        val date = LocalDate.of(2026, 11, 3)
        val syntheticDay = Day(
            date = date,
            status = DayStatus.REGULAR,
            lessons = listOf(
                Lesson(
                    date = date,
                    start = LocalTime.of(8, 0),
                    end = LocalTime.of(9, 35),
                    periodIds = listOf(1L, 2L),
                    type = "NORMAL_TEACHING_PERIOD",
                    status = LessonStatus.CANCELLED,
                    subjects = listOf(Slot(Element("KR-GK4", "Deutsch", ElementStatus.REGULAR), null)),
                    teachers = emptyList(),
                    rooms = emptyList(),
                ),
                Lesson(
                    date = date,
                    start = LocalTime.of(9, 55),
                    end = LocalTime.of(11, 30),
                    periodIds = listOf(3L, 4L),
                    type = "NORMAL_TEACHING_PERIOD",
                    status = LessonStatus.REGULAR,
                    subjects = listOf(Slot(Element("KU-LK1", "Informatik", ElementStatus.REGULAR), null)),
                    teachers = emptyList(),
                    rooms = emptyList(),
                )
            )
        )

        val detector = ChangeDetector(Settings(), timeGrid)
        val now = LocalDateTime.of(2026, 11, 3, 6, 30)
        val result = detector.detect(listOf(syntheticDay), now, emptyMap())

        val firstLessonChange = result.toNotify.firstOrNull { it.kind == ChangeKind.FIRST_LESSON }
        assertNotNull("FIRST_LESSON change must be present", firstLessonChange)
        assertEquals("Di: Unterricht erst ab 09:55", firstLessonChange!!.title)
        assertEquals("Die 1.–2. Std. entfällt.", firstLessonChange.text)
    }

    @Test
    fun test7_foyerPrefixMatchCaseInsensitive() {
        val date = LocalDate.of(2026, 11, 3)
        fun makeDayWithRoom(roomName: String): Day {
            return Day(
                date = date,
                status = DayStatus.REGULAR,
                lessons = listOf(
                    Lesson(
                        date = date,
                        start = LocalTime.of(14, 15),
                        end = LocalTime.of(15, 0),
                        periodIds = listOf(8L),
                        type = "NORMAL_TEACHING_PERIOD",
                        status = LessonStatus.REGULAR,
                        subjects = listOf(Slot(Element("EK", "Erdkunde", ElementStatus.REGULAR), null)),
                        teachers = emptyList(),
                        rooms = listOf(
                            Slot(
                                current = Element(roomName, "Room", ElementStatus.ADDED),
                                removed = null
                            )
                        )
                    )
                )
            )
        }

        val detector = ChangeDetector(Settings(), timeGrid)
        val now = LocalDateTime.of(2026, 11, 3, 6, 30)

        val res1 = detector.detect(listOf(makeDayWithRoom("Foyer a")), now, emptyMap())
        assertTrue("Foyer a should match FOYER", res1.toNotify.any { it.kind == ChangeKind.FOYER })

        val res2 = detector.detect(listOf(makeDayWithRoom("foyer")), now, emptyMap())
        assertTrue("foyer lowercase should match FOYER", res2.toNotify.any { it.kind == ChangeKind.FOYER })
    }
}
