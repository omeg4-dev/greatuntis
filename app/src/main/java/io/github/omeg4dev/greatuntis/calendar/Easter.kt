package io.github.omeg4dev.greatuntis.calendar

import java.time.LocalDate

/**
 * Calculates Easter Sunday and related carnival days using the Anonymous Gregorian algorithm.
 */
object Easter {
    /**
     * Calculates Easter Sunday for [year].
     */
    fun easterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }

    /** Weiberfastnacht = Easter - 52 days */
    fun weiberfastnacht(year: Int): LocalDate = easterSunday(year).minusDays(52)

    /** Rosenmontag = Easter - 48 days */
    fun rosenmontag(year: Int): LocalDate = easterSunday(year).minusDays(48)

    /** Veilchendienstag = Easter - 47 days */
    fun veilchendienstag(year: Int): LocalDate = easterSunday(year).minusDays(47)
}
