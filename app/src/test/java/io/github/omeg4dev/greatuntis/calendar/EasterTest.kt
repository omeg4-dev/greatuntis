package io.github.omeg4dev.greatuntis.calendar

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class EasterTest {
    @Test
    fun testEasterSundayCalculation() {
        assertEquals(LocalDate.of(2025, 4, 20), Easter.easterSunday(2025))
        assertEquals(LocalDate.of(2026, 4, 5), Easter.easterSunday(2026))
        assertEquals(LocalDate.of(2027, 3, 28), Easter.easterSunday(2027))
        assertEquals(LocalDate.of(2028, 4, 16), Easter.easterSunday(2028))
        assertEquals(LocalDate.of(2029, 4, 1), Easter.easterSunday(2029))
        assertEquals(LocalDate.of(2030, 4, 21), Easter.easterSunday(2030))
    }

    @Test
    fun testCarnivalDates2026() {
        assertEquals(LocalDate.of(2026, 2, 12), Easter.weiberfastnacht(2026))
        assertEquals(LocalDate.of(2026, 2, 16), Easter.rosenmontag(2026))
        assertEquals(LocalDate.of(2026, 2, 17), Easter.veilchendienstag(2026))
    }
}
