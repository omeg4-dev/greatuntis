package io.github.omeg4dev.greatuntis.stats

import io.github.omeg4dev.greatuntis.Fixtures
import io.github.omeg4dev.greatuntis.api.AbsenceResponseDto
import io.github.omeg4dev.greatuntis.api.TimetableResponseDto
import io.github.omeg4dev.greatuntis.api.UntisJson
import io.github.omeg4dev.greatuntis.api.UserDataResultDto
import io.github.omeg4dev.greatuntis.api.toAbsences
import io.github.omeg4dev.greatuntis.api.toDays
import io.github.omeg4dev.greatuntis.api.toMasterData
import io.github.omeg4dev.greatuntis.model.AbsenceCategory
import io.github.omeg4dev.greatuntis.model.AbsenceStatsResult
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Test

@Serializable
data class StatsExpected(
    val heldUnits: Int,
    val missedUnits: Int,
    val missedDays: Int,
    val missedByCategory: Map<String, Int>,
    val missedBySubject: Map<String, Int>,
    val heldBySubject: Map<String, Int>,
    val missedByWeekday: Map<String, Int>,
    val missedByPeriod: Map<String, Int>,
    val missedByWeek: Map<String, Int>,
    val absenceCount: Int,
    val openAbsences: Int,
    val lateCount: Int,
    val lateMinutes: Int,
    val lateByWeekday: Map<String, Int>,
    val cancelledBySubject: Map<String, Int>,
    val foyerBySubject: Map<String, Int>,
)

class AbsenceStatsTest {

    private fun categoryToString(category: AbsenceCategory): String = when (category) {
        AbsenceCategory.EXCUSED -> "excused"
        AbsenceCategory.UNEXCUSED -> "unexcused"
        AbsenceCategory.NOT_COUNTED -> "not_counted"
        AbsenceCategory.OPEN -> "open"
    }

    private fun assertStatsMatch(year: String, result: AbsenceStatsResult) {
        val expectedJson = Fixtures.load("stats_expected_$year.json")
        val expected = UntisJson.decodeFromString<StatsExpected>(expectedJson)

        assertEquals("heldUnits ($year)", expected.heldUnits, result.heldUnits)
        assertEquals("missedUnits ($year)", expected.missedUnits, result.missedUnits)
        assertEquals("missedDays ($year)", expected.missedDays, result.missedDays)

        val resultMissedByCat = result.missedByCategory.mapKeys { categoryToString(it.key) }
        assertEquals("missedByCategory ($year)", expected.missedByCategory, resultMissedByCat)

        assertEquals("missedBySubject ($year)", expected.missedBySubject, result.missedBySubject)
        assertEquals("heldBySubject ($year)", expected.heldBySubject, result.heldBySubject)

        val resultMissedByWeekday = result.missedByWeekday.mapKeys { it.key.toString() }
        assertEquals("missedByWeekday ($year)", expected.missedByWeekday, resultMissedByWeekday)

        val resultMissedByPeriod = result.missedByPeriod.mapKeys { it.key.toString() }
        assertEquals("missedByPeriod ($year)", expected.missedByPeriod, resultMissedByPeriod)

        val resultMissedByWeek = result.missedByWeek.mapKeys { it.key.toString() }
        assertEquals("missedByWeek ($year)", expected.missedByWeek, resultMissedByWeek)

        assertEquals("absenceCount ($year)", expected.absenceCount, result.absenceCount)
        assertEquals("openAbsences ($year)", expected.openAbsences, result.openAbsences)
        assertEquals("lateCount ($year)", expected.lateCount, result.lateCount)
        assertEquals("lateMinutes ($year)", expected.lateMinutes, result.lateMinutes)

        val resultLateByWeekday = result.lateByWeekday.mapKeys { it.key.toString() }
        assertEquals("lateByWeekday ($year)", expected.lateByWeekday, resultLateByWeekday)

        assertEquals("cancelledBySubject ($year)", expected.cancelledBySubject, result.cancelledBySubject)
        assertEquals("foyerBySubject ($year)", expected.foyerBySubject, result.foyerBySubject)
    }

    @Test
    fun testAbsenceStats2025() {
        val userJson = Fixtures.load("userdata.json")
        val masterData = toMasterData(UntisJson.decodeFromString<UserDataResultDto>(userJson))

        val ttJson = Fixtures.load("timetable_2025.json")
        val days = toDays(UntisJson.decodeFromString<TimetableResponseDto>(ttJson))

        val absJson = Fixtures.load("absences_2025.json")
        val absences = toAbsences(UntisJson.decodeFromString<AbsenceResponseDto>(absJson))

        val result = AbsenceStats.compute(days, absences, masterData.timeGrid)
        assertStatsMatch("2025", result)
    }

    @Test
    fun testAbsenceStats2026() {
        val userJson = Fixtures.load("userdata.json")
        val masterData = toMasterData(UntisJson.decodeFromString<UserDataResultDto>(userJson))

        val ttJson = Fixtures.load("timetable_2026.json")
        val days = toDays(UntisJson.decodeFromString<TimetableResponseDto>(ttJson))

        val absJson = Fixtures.load("absences_2026.json")
        val absences = toAbsences(UntisJson.decodeFromString<AbsenceResponseDto>(absJson))

        val result = AbsenceStats.compute(days, absences, masterData.timeGrid)
        assertStatsMatch("2026", result)
    }
}
