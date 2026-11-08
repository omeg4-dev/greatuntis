package io.github.omeg4dev.greatuntis.calendar

import io.github.omeg4dev.greatuntis.api.ApiHoliday
import io.github.omeg4dev.greatuntis.api.ApiHolidayType
import io.github.omeg4dev.greatuntis.model.Holiday
import io.github.omeg4dev.greatuntis.model.SchoolYear
import java.time.DayOfWeek
import java.time.LocalDate

enum class FreeKind { FERIEN, FEIERTAG, BRUECKENTAG, SCHULFREI, WOCHENENDE }

enum class FreeSource { WEBUNTIS, API, TIMETABLE, WEEKEND }

data class FreeDay(
    val date: LocalDate,
    val kind: FreeKind,
    /** All names for this day, most important first; first is shown. Never empty. */
    val names: List<String>,
    val source: FreeSource,
) {
    val name: String get() = names.first()
}

data class FreeStretch(
    val start: LocalDate,
    val end: LocalDate,
    val title: String,
    /** Dominant kind: FERIEN if any Ferien day, else FEIERTAG, else BRUECKENTAG, else SCHULFREI. */
    val kind: FreeKind,
    val days: List<FreeDay>,
) {
    val length: Int = (end.toEpochDay() - start.toEpochDay() + 1).toInt() // calendar days, inclusive

    /** 3–4 days that include a weekend and no Ferien day. */
    val isLongWeekend: Boolean = length in 3..4 &&
        days.any { it.kind == FreeKind.WOCHENENDE || it.date.dayOfWeek in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) } &&
        days.none { it.kind == FreeKind.FERIEN }

    /** German label: "Do 14.05. – So 17.05. · 4 Tage frei"; single day: "Mo 01.05. · 1 Tag frei". */
    val label: String = formatLabel()

    private fun formatLabel(): String {
        fun fmt(d: LocalDate): String {
            val dow = when (d.dayOfWeek) {
                DayOfWeek.MONDAY -> "Mo"
                DayOfWeek.TUESDAY -> "Di"
                DayOfWeek.WEDNESDAY -> "Mi"
                DayOfWeek.THURSDAY -> "Do"
                DayOfWeek.FRIDAY -> "Fr"
                DayOfWeek.SATURDAY -> "Sa"
                DayOfWeek.SUNDAY -> "So"
            }
            val dm = "%02d.%02d.".format(d.dayOfMonth, d.monthValue)
            return "$dow $dm"
        }
        return if (length == 1) {
            "${fmt(start)} · 1 Tag frei"
        } else {
            "${fmt(start)} – ${fmt(end)} · $length Tage frei"
        }
    }
}

class FreeCalendar private constructor(
    private val dayMap: Map<LocalDate, FreeDay>,
    private val stretchList: List<FreeStretch>,
) {
    /** null = school day; weekends → WOCHENENDE unless more specific */
    fun freeDay(date: LocalDate): FreeDay? {
        val recorded = dayMap[date]
        if (recorded != null) return recorded
        if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
            return FreeDay(date, FreeKind.WOCHENENDE, listOf("Wochenende"), FreeSource.WEEKEND)
        }
        return null
    }

    fun isFree(date: LocalDate): Boolean = freeDay(date) != null

    /** Stretch containing date, or null (plain weekends are not stretches). */
    fun stretchAt(date: LocalDate): FreeStretch? =
        stretchList.firstOrNull { date in it.start..it.end }

    /** First stretch with start > date. */
    fun nextStretch(after: LocalDate): FreeStretch? =
        stretchList.firstOrNull { it.start > after }

    /** First stretch with kind FERIEN and start > date. */
    fun nextFerien(after: LocalDate): FreeStretch? =
        stretchList.firstOrNull { it.kind == FreeKind.FERIEN && it.start > after }

    /** Mon–Fri days in [from, to) that are not free. */
    fun schoolDaysBetween(from: LocalDate, to: LocalDate): Int {
        if (from >= to) return 0
        var count = 0
        var curr = from
        while (curr < to) {
            val dow = curr.dayOfWeek
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                if (!isFree(curr)) {
                    count++
                }
            }
            curr = curr.plusDays(1)
        }
        return count
    }

    /** 0..1 share of school days of [year] already passed at [today] (school days before today / all). */
    fun schoolYearProgress(today: LocalDate, year: SchoolYear): Float {
        val total = schoolDaysBetween(year.start, year.end.plusDays(1))
        if (total <= 0) return 0f
        val passed = when {
            today <= year.start -> 0
            today > year.end -> total
            else -> schoolDaysBetween(year.start, today)
        }
        return (passed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    /** All stretches overlapping [from, to], sorted. */
    fun stretches(from: LocalDate, to: LocalDate): List<FreeStretch> =
        stretchList.filter { it.start <= to && it.end >= from }

    companion object {
        val EMPTY: FreeCalendar = FreeCalendar(emptyMap(), emptyList())

        fun build(
            webUntis: List<Holiday>,
            api: List<ApiHoliday>,
            /** Dates the fetched timetable proves to be school days (real, non-cancelled lessons). */
            schoolDays: Set<LocalDate>,
            /** Dates the timetable proves free, with a reason, e.g. "Pädagogischer Tag", "Alles entfällt". */
            timetableFree: Map<LocalDate, String>,
            /** Weekdays WebUntis returned as NO_DATA without entries. */
            noDataWeekdays: Set<LocalDate>,
            range: ClosedRange<LocalDate>,
        ): FreeCalendar {
            fun isFeiertag(date: LocalDate): Boolean {
                if (api.any { it.type == ApiHolidayType.PUBLIC && date in it.start..it.end }) {
                    return true
                }
                val untisMatch = webUntis.filter { date in it.start..it.end }
                return untisMatch.any { h ->
                    val cleaned = HolidayNames.cleanNamed(h.name)
                    HolidayNames.GERMAN_PUBLIC_HOLIDAYS.any { it.equals(cleaned, ignoreCase = true) } ||
                        cleaned.equals("Chr. Himmelfahrt", ignoreCase = true)
                }
            }

            fun isWeekendOrFree(date: LocalDate): Boolean {
                if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) return true
                if (timetableFree.containsKey(date)) return true
                if (date in noDataWeekdays) return true
                if (webUntis.any { date in it.start..it.end }) return true
                if (api.any { date in it.start..it.end }) return true
                return false
            }

            fun untisKindFor(cleanedName: String, date: LocalDate, apiPublicNames: List<String>): FreeKind {
                val lower = cleanedName.lowercase().trim()
                return when {
                    lower in HolidayNames.BRUECKENTAG_NAMES -> FreeKind.BRUECKENTAG
                    apiPublicNames.any { it.equals(cleanedName, ignoreCase = true) } ||
                        HolidayNames.GERMAN_PUBLIC_HOLIDAYS.any { it.equals(cleanedName, ignoreCase = true) } ||
                        lower == "chr. himmelfahrt" -> FreeKind.FEIERTAG
                    lower.contains("ferien") -> FreeKind.FERIEN
                    else -> FreeKind.SCHULFREI
                }
            }

            val dayMap = mutableMapOf<LocalDate, FreeDay>()

            var currDate = range.start
            while (currDate <= range.endInclusive) {
                val date = currDate
                currDate = currDate.plusDays(1)

                // 1. Date in schoolDays → school day (null)
                if (date in schoolDays) {
                    continue
                }

                // 2. Else WebUntis holiday entries covering the date (cleaned) → source WEBUNTIS
                val untisEntries = webUntis.filter { date in it.start..it.end }
                if (untisEntries.isNotEmpty()) {
                    val apiPublicMatches = api.filter { it.type == ApiHolidayType.PUBLIC && date in it.start..it.end }
                    val apiPublicNames = apiPublicMatches.map { it.name }

                    val cleanedEntries = untisEntries.map { h ->
                        val cleaned = HolidayNames.cleanWebUntis(h.name, date, ::isFeiertag, ::isWeekendOrFree)
                        val kind = untisKindFor(cleaned, date, apiPublicNames)
                        cleaned to kind
                    }

                    // Prefer FERIEN, then BRUECKENTAG, then FEIERTAG, then SCHULFREI
                    val (chosenName, untisKind) = cleanedEntries.firstOrNull { it.second == FreeKind.FERIEN }
                        ?: cleanedEntries.firstOrNull { it.second == FreeKind.BRUECKENTAG }
                        ?: cleanedEntries.firstOrNull { it.second == FreeKind.FEIERTAG }
                        ?: cleanedEntries.first()

                    // If both WEBUNTIS/API-school and an API-public apply: kind FERIEN, names = [ferien name, public name]
                    val isFerien = untisKind == FreeKind.FERIEN || chosenName.contains("ferien", ignoreCase = true)
                    if (isFerien && apiPublicMatches.isNotEmpty()) {
                        val names = mutableListOf(chosenName)
                        for (pub in apiPublicNames) {
                            if (!names.contains(pub)) names.add(pub)
                        }
                        dayMap[date] = FreeDay(date, FreeKind.FERIEN, names, FreeSource.WEBUNTIS)
                    } else {
                        dayMap[date] = FreeDay(date, untisKind, listOf(chosenName), FreeSource.WEBUNTIS)
                    }
                    continue
                }

                // 3. Else API entries covering it → source API. SCHOOL type → FERIEN; PUBLIC → FEIERTAG.
                val apiMatches = api.filter { date in it.start..it.end }
                if (apiMatches.isNotEmpty()) {
                    val apiSchool = apiMatches.filter { it.type == ApiHolidayType.SCHOOL }
                    val apiPublic = apiMatches.filter { it.type == ApiHolidayType.PUBLIC }

                    if (apiSchool.isNotEmpty() && apiPublic.isNotEmpty()) {
                        val names = mutableListOf(apiSchool.first().name)
                        for (pub in apiPublic.map { it.name }) {
                            if (!names.contains(pub)) names.add(pub)
                        }
                        dayMap[date] = FreeDay(date, FreeKind.FERIEN, names, FreeSource.API)
                    } else if (apiSchool.isNotEmpty()) {
                        dayMap[date] = FreeDay(date, FreeKind.FERIEN, apiSchool.map { it.name }, FreeSource.API)
                    } else {
                        dayMap[date] = FreeDay(date, FreeKind.FEIERTAG, apiPublic.map { it.name }, FreeSource.API)
                    }
                    continue
                }

                // 4. Else timetableFree[date] → SCHULFREI, name = reason, source TIMETABLE
                val timetableReason = timetableFree[date]
                if (timetableReason != null) {
                    dayMap[date] = FreeDay(date, FreeKind.SCHULFREI, listOf(timetableReason), FreeSource.TIMETABLE)
                    continue
                }

                // 5. Else date in noDataWeekdays → SCHULFREI, name "Unterrichtsfrei", source TIMETABLE
                if (date in noDataWeekdays) {
                    dayMap[date] = FreeDay(date, FreeKind.SCHULFREI, listOf("Unterrichtsfrei"), FreeSource.TIMETABLE)
                    continue
                }

                // 6. Else Saturday/Sunday → WOCHENENDE, name "Wochenende", source WEEKEND
                if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
                    dayMap[date] = FreeDay(date, FreeKind.WOCHENENDE, listOf("Wochenende"), FreeSource.WEEKEND)
                    continue
                }

                // 7. Else school day
            }

            // Stretches: maximal runs of consecutive free dates;
            // a run that consists only of WOCHENENDE days is not a stretch
            // (but weekend days adjacent to other free days are absorbed into the stretch).
            val runs = mutableListOf<List<FreeDay>>()
            var currentRun = mutableListOf<FreeDay>()

            var d = range.start
            while (d <= range.endInclusive) {
                val day = dayMap[d]
                if (day != null) {
                    currentRun.add(day)
                } else {
                    if (currentRun.isNotEmpty()) {
                        runs.add(currentRun)
                        currentRun = mutableListOf()
                    }
                }
                d = d.plusDays(1)
            }
            if (currentRun.isNotEmpty()) {
                runs.add(currentRun)
            }

            val stretches = runs.filter { run ->
                run.any { it.kind != FreeKind.WOCHENENDE }
            }.map { run ->
                val start = run.first().date
                val end = run.last().date

                val kind = when {
                    run.any { it.kind == FreeKind.FERIEN } -> FreeKind.FERIEN
                    run.any { it.kind == FreeKind.FEIERTAG } -> FreeKind.FEIERTAG
                    run.any { it.kind == FreeKind.BRUECKENTAG } -> FreeKind.BRUECKENTAG
                    else -> FreeKind.SCHULFREI
                }

                val title = if (run.any { it.kind == FreeKind.FERIEN }) {
                    val ferienDays = run.filter { it.kind == FreeKind.FERIEN }
                    val counts = ferienDays.groupingBy { it.name }.eachCount()
                    counts.maxByOrNull { it.value }?.key ?: ferienDays.first().name
                } else {
                    val nonWeekendNames = run
                        .filter { it.kind != FreeKind.WOCHENENDE }
                        .map { it.name }
                        .distinct()
                    if (nonWeekendNames.size <= 2) {
                        nonWeekendNames.joinToString(" + ")
                    } else {
                        nonWeekendNames.take(2).joinToString(" + ") + " + …"
                    }
                }

                FreeStretch(
                    start = start,
                    end = end,
                    title = title,
                    kind = kind,
                    days = run,
                )
            }.sortedBy { it.start }

            return FreeCalendar(dayMap, stretches)
        }
    }
}
