package io.github.omeg4dev.greatuntis.timetable

import io.github.omeg4dev.greatuntis.Fixtures
import io.github.omeg4dev.greatuntis.api.TimetableResponseDto
import io.github.omeg4dev.greatuntis.api.UntisJson
import io.github.omeg4dev.greatuntis.api.UserDataResultDto
import io.github.omeg4dev.greatuntis.api.toDays
import io.github.omeg4dev.greatuntis.api.toMasterData
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.DayStatus
import io.github.omeg4dev.greatuntis.model.MasterData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class PredictionIsFreeTest {

    private lateinit var masterData: MasterData
    private lateinit var days2026: List<Day>

    @Before
    fun setUp() {
        val userJson = Fixtures.load("userdata.json")
        masterData = toMasterData(UntisJson.decodeFromString<UserDataResultDto>(userJson))

        val tt2026Json = Fixtures.load("timetable_2026.json")
        days2026 = toDays(UntisJson.decodeFromString<TimetableResponseDto>(tt2026Json))
    }

    /** A date where isFree returns true → Day(date, NO_DATA, []) */
    @Test
    fun testIsFreePredicateReturnsNoData() {
        val freeDate = LocalDate.of(2026, 11, 2) // Monday, not in history → would normally be predicted
        val result = Prediction.fill(
            history = days2026,
            dates = listOf(freeDate),
            timeGrid = masterData.timeGrid,
            isFree = { it == freeDate },
        )

        assertEquals(1, result.size)
        val day = result[0]
        assertEquals(freeDate, day.date)
        assertEquals(DayStatus.NO_DATA, day.status)
        assertTrue("lessons should be empty when isFree", day.lessons.isEmpty())
    }
}
