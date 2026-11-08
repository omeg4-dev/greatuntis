package io.github.omeg4dev.greatuntis.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HolidayNamesTest {
    @Test
    fun testIsUnnamed() {
        assertTrue(HolidayNames.isUnnamed(null))
        assertTrue(HolidayNames.isUnnamed(""))
        assertTrue(HolidayNames.isUnnamed("   "))
        assertTrue(HolidayNames.isUnnamed("unnamed"))
        assertTrue(HolidayNames.isUnnamed("Unnamed"))
        assertTrue(HolidayNames.isUnnamed("31.10."))
        assertTrue(HolidayNames.isUnnamed("31.10.2022"))
        assertTrue(HolidayNames.isUnnamed("16.2.2026-16.2.2026"))
        assertTrue(HolidayNames.isUnnamed("15.5.2026-15.5.2026"))
        assertTrue(HolidayNames.isUnnamed("12.2._1"))
        assertTrue(HolidayNames.isUnnamed("Ferien"))
        assertTrue(HolidayNames.isUnnamed("Ferien1"))
        assertTrue(HolidayNames.isUnnamed("ferien2"))
        assertTrue(HolidayNames.isUnnamed("Ferien 3"))

        assertFalse(HolidayNames.isUnnamed("Weihnachtsferien"))
        assertFalse(HolidayNames.isUnnamed("Weihnachtsferien_1"))
        assertFalse(HolidayNames.isUnnamed("1. Weihnachtsfeiertag"))
        assertFalse(HolidayNames.isUnnamed("2. Weihnachtsfeiertag"))
        assertFalse(HolidayNames.isUnnamed("Christi Himmelfahrt"))
        assertFalse(HolidayNames.isUnnamed("Sommferferien"))
        assertFalse(HolidayNames.isUnnamed("Tag der Deutschen Ei"))
    }

    @Test
    fun testCleanNamed() {
        assertEquals("Weihnachtsferien", HolidayNames.cleanNamed("Weihnachtsferien_1"))
        assertEquals("Weihnachtsferien", HolidayNames.cleanNamed("Weihnachtsferien_2"))
        assertEquals("Sommerferien", HolidayNames.cleanNamed("Sommferferien"))
        assertEquals("Christi Himmelfahrt", HolidayNames.cleanNamed("Chr. Himmelfahrt"))
        assertEquals("Tag der Deutschen Einheit", HolidayNames.cleanNamed("Tag der Deutschen Ei"))
        assertEquals("Herbstferien", HolidayNames.cleanNamed("Herbst"))
    }

    @Test
    fun testDeriveUnnamedCarnival() {
        // In 2026, Easter Sunday is 2026-04-05
        // Rosenmontag = 2026-02-16, Veilchendienstag = 2026-02-17, Weiberfastnacht = 2026-02-12
        val dateWeiberfastnacht = LocalDate.of(2026, 2, 12)
        val dateRosenmontag = LocalDate.of(2026, 2, 16)
        val dateVeilchendienstag = LocalDate.of(2026, 2, 17)

        val isFeiertag = { _: LocalDate -> false }
        val isFree = { _: LocalDate -> false }

        assertEquals(
            "Weiberfastnacht",
            HolidayNames.deriveUnnamed(dateWeiberfastnacht, isFeiertag, isFree),
        )
        assertEquals(
            "Rosenmontag",
            HolidayNames.deriveUnnamed(dateRosenmontag, isFeiertag, isFree),
        )
        assertEquals(
            "Veilchendienstag",
            HolidayNames.deriveUnnamed(dateVeilchendienstag, isFeiertag, isFree),
        )
    }

    @Test
    fun testDeriveUnnamedBrueckentagAndBeweglicherFerientag() {
        // Christi Himmelfahrt: 2026-05-14 (Thursday) is a Feiertag.
        // 2026-05-15 (Friday) is directly after Feiertag, and Saturday (2026-05-16) is a weekend.
        val friday = LocalDate.of(2026, 5, 15)
        val isFeiertag = { d: LocalDate -> d == LocalDate.of(2026, 5, 14) }
        val isFree = { d: LocalDate -> d.dayOfWeek.value in 6..7 }

        assertEquals(
            "Brückentag",
            HolidayNames.deriveUnnamed(friday, isFeiertag, isFree),
        )

        // An isolated date without adjacent Feiertag -> Beweglicher Ferientag
        val otherDate = LocalDate.of(2026, 3, 20)
        assertEquals(
            "Beweglicher Ferientag",
            HolidayNames.deriveUnnamed(otherDate, { false }, { false }),
        )
    }

    @Test
    fun testCleanWebUntisResolution() {
        val dummyFeiertag = { _: LocalDate -> false }
        val dummyFree = { _: LocalDate -> false }

        // longName is "Sommferferien" -> cleanNamed -> "Sommerferien"
        assertEquals(
            "Sommerferien",
            HolidayNames.cleanWebUntis("Sommferferien", "25.8.", LocalDate.of(2025, 8, 25), dummyFeiertag, dummyFree),
        )

        // longName unnamed ("16.2.2026-16.2.2026"), name unnamed ("Ferien1") -> derives to Rosenmontag
        assertEquals(
            "Rosenmontag",
            HolidayNames.cleanWebUntis("16.2.2026-16.2.2026", "Ferien1", LocalDate.of(2026, 2, 16), dummyFeiertag, dummyFree),
        )
    }
}
