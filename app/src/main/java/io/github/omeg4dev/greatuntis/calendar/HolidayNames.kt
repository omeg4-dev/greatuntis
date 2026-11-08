package io.github.omeg4dev.greatuntis.calendar

import java.time.LocalDate

object HolidayNames {
    val GERMAN_PUBLIC_HOLIDAYS = listOf(
        "Neujahr",
        "Heilige Drei Könige",
        "Karfreitag",
        "Ostersonntag",
        "Ostermontag",
        "Tag der Arbeit",
        "Christi Himmelfahrt",
        "Pfingstsonntag",
        "Pfingstmontag",
        "Fronleichnam",
        "Mariä Himmelfahrt",
        "Tag der Deutschen Einheit",
        "Reformationstag",
        "Allerheiligen",
        "Buß- und Bettag",
        "1. Weihnachtsfeiertag",
        "2. Weihnachtsfeiertag",
        "1. Weihnachtstag",
        "2. Weihnachtstag",
        "Erster Weihnachtstag",
        "Zweiter Weihnachtstag",
        "Heiligabend",
        "Silvester",
    )

    val KNOWN_SCHOOL_HOLIDAYS = listOf(
        "Herbstferien",
        "Weihnachtsferien",
        "Osterferien",
        "Pfingstferien",
        "Sommerferien",
        "Winterferien",
    )

    val ALL_KNOWN_HOLIDAYS = GERMAN_PUBLIC_HOLIDAYS + KNOWN_SCHOOL_HOLIDAYS

    val BRUECKENTAG_NAMES = setOf(
        "brückentag",
        "beweglicher ferientag",
        "rosenmontag",
        "veilchendienstag",
        "weiberfastnacht",
    )

    private val DATE_PATTERN = Regex("""^\d{1,2}\.\d{1,2}\b.*""")
    private val FERIEN_PATTERN = Regex("""^ferien\s*\d*$""", RegexOption.IGNORE_CASE)

    /**
     * Checks if a name is unnamed according to the specification:
     * - blank or "unnamed"
     * - matches a date pattern (`^\d{1,2}\.\d{1,2}\.(\d{2,4})?` possibly with a `-` range)
     * - matches `(?i)^ferien\s*\d*$`
     */
    fun isUnnamed(text: String?): Boolean {
        if (text.isNullOrBlank()) return true
        val trimmed = text.trim()
        if (trimmed.equals("unnamed", ignoreCase = true)) return true
        if (DATE_PATTERN.matches(trimmed)) return true
        if (FERIEN_PATTERN.matches(trimmed)) return true
        return false
    }

    /**
     * Cleans a known/named holiday text:
     * - Strip suffix `_\d+` ("Weihnachtsferien_1" → "Weihnachtsferien")
     * - Typo map: "Sommferferien" → "Sommerferien"; "Chr. Himmelfahrt" → "Christi Himmelfahrt"
     * - Prefix matching if length <= 20
     */
    fun cleanNamed(raw: String): String {
        var cleaned = raw.trim().replace(Regex("""_\d+$"""), "").trim()

        cleaned = when {
            cleaned.equals("Sommferferien", ignoreCase = true) -> "Sommerferien"
            cleaned.equals("Chr. Himmelfahrt", ignoreCase = true) -> "Christi Himmelfahrt"
            else -> cleaned
        }

        if (cleaned.length <= 20) {
            val matched = ALL_KNOWN_HOLIDAYS.firstOrNull {
                it.startsWith(cleaned, ignoreCase = true)
            }
            if (matched != null) {
                cleaned = matched
            }
        }

        return cleaned
    }

    /**
     * Derives a name for an unnamed holiday on a specific date:
     * - Easter−52 "Weiberfastnacht", Easter−48 "Rosenmontag", Easter−47 "Veilchendienstag"
     * - Day directly before or after a FEIERTAG whose other side is a weekend or free day → "Brückentag"
     * - Otherwise → "Beweglicher Ferientag"
     */
    fun deriveUnnamed(
        date: LocalDate,
        isFeiertag: (LocalDate) -> Boolean,
        isWeekendOrFree: (LocalDate) -> Boolean,
    ): String {
        val easter = Easter.easterSunday(date.year)
        when (date) {
            easter.minusDays(52) -> return "Weiberfastnacht"
            easter.minusDays(48) -> return "Rosenmontag"
            easter.minusDays(47) -> return "Veilchendienstag"
        }

        val beforeFeiertag = isFeiertag(date.plusDays(1)) && isWeekendOrFree(date.minusDays(1))
        val afterFeiertag = isFeiertag(date.minusDays(1)) && isWeekendOrFree(date.plusDays(1))

        if (beforeFeiertag || afterFeiertag) {
            return "Brückentag"
        }

        return "Beweglicher Ferientag"
    }

    /**
     * Resolves and cleans a WebUntis holiday entry:
     * - Use [longName] unless it is blank or "unnamed", then [name].
     * - If still unnamed, derives per [date].
     */
    fun cleanWebUntis(
        longName: String?,
        name: String?,
        date: LocalDate,
        isFeiertag: (LocalDate) -> Boolean,
        isWeekendOrFree: (LocalDate) -> Boolean,
    ): String {
        val chosen = when {
            !longName.isNullOrBlank() && !isUnnamed(longName) -> longName
            !name.isNullOrBlank() && !isUnnamed(name) -> name
            !longName.isNullOrBlank() -> longName
            else -> name.orEmpty()
        }

        return if (isUnnamed(chosen)) {
            deriveUnnamed(date, isFeiertag, isWeekendOrFree)
        } else {
            cleanNamed(chosen)
        }
    }

    /**
     * Overload for when only a single name is present (e.g. from Model Holiday.name).
     */
    fun cleanWebUntis(
        name: String,
        date: LocalDate,
        isFeiertag: (LocalDate) -> Boolean,
        isWeekendOrFree: (LocalDate) -> Boolean,
    ): String = cleanWebUntis(null, name, date, isFeiertag, isWeekendOrFree)
}
