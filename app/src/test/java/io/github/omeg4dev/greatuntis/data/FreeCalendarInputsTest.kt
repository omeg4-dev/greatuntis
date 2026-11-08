package io.github.omeg4dev.greatuntis.data

import io.github.omeg4dev.greatuntis.Fixtures
import io.github.omeg4dev.greatuntis.api.TimetableResponseDto
import io.github.omeg4dev.greatuntis.api.UntisJson
import io.github.omeg4dev.greatuntis.api.UserDataResultDto
import io.github.omeg4dev.greatuntis.api.toDays
import io.github.omeg4dev.greatuntis.api.toMasterData
import io.github.omeg4dev.greatuntis.calendar.FreeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FreeCalendarInputsTest {

    @Test
    fun freeCalendarInputs_pedagogischerTagAndSchoolDays() {
        val timetableJson = Fixtures.load("timetable_2026.json")
        val timetableDto = UntisJson.decodeFromString<TimetableResponseDto>(timetableJson)
        val days = toDays(timetableDto)

        val userdataJson = Fixtures.load("userdata.json")
        val userdataDto = UntisJson.decodeFromString<UserDataResultDto>(userdataJson)
        val masterData = toMasterData(userdataDto)

        val inputs = FreeCalendarBuilder.prepareInputs(
            days = days,
            grid = masterData.timeGrid,
            foyerPrefix = "Foyer",
        )

        val schoolDay = LocalDate.of(2026, 8, 31)
        val paedTestDay = LocalDate.of(2026, 10, 8)

        // 2026-08-31 lands in schoolDays
        assertTrue("2026-08-31 must be in schoolDays", schoolDay in inputs.schoolDays)
        assertFalse("2026-08-31 must not be in timetableFree", inputs.timetableFree.containsKey(schoolDay))

        // 2026-10-08 lands in timetableFree
        assertTrue("2026-10-08 must be in timetableFree", inputs.timetableFree.containsKey(paedTestDay))
        assertFalse("2026-10-08 must not be in schoolDays", paedTestDay in inputs.schoolDays)
        val reason = inputs.timetableFree[paedTestDay]
        assertNotNull("Reason must not be null", reason)
        assertTrue("Reason should contain Päd. TAg", reason!!.contains("Päd", ignoreCase = true))

        // Also verify the built FreeCalendar reflects this
        val calendar = FreeCalendarBuilder.build(
            masterData = masterData,
            days = days,
            apiHolidays = emptyList(),
            foyerPrefix = "Foyer",
            today = LocalDate.of(2026, 9, 1),
        )

        assertFalse("2026-08-31 must be a school day (not free)", calendar.isFree(schoolDay))
        assertTrue("2026-10-08 must be free", calendar.isFree(paedTestDay))
        val freeDay = calendar.freeDay(paedTestDay)
        assertNotNull(freeDay)
        assertEquals(FreeKind.SCHULFREI, freeDay!!.kind)
        assertTrue(freeDay.name.contains("Päd", ignoreCase = true))
    }
}
