package io.github.omeg4dev.greatuntis.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/*
 * Domain model shared by api/, watch/, stats/, data/ and ui/.
 * All times are local school time (Europe/Berlin); WebUntis sends them without a usable zone.
 * Changing a type here is a contract change: ask before doing it.
 */

/** Login data, exactly the fields of the Untis QR code. */
data class Account(
    /** Host only, e.g. "demo.webuntis.com". */
    val host: String,
    /** Login name of the school, e.g. "demo-schule". */
    val school: String,
    val user: String,
    /** Base32 TOTP secret ("Schlüssel"). */
    val key: String,
    val schoolNumber: String? = null,
)

enum class LessonStatus { REGULAR, CHANGED, CANCELLED, ADDITIONAL, OTHER }

enum class ElementStatus { REGULAR, ADDED, REMOVED, OTHER }

/** A subject, teacher or room as WebUntis shows it. */
data class Element(val shortName: String, val longName: String, val status: ElementStatus, val kind: String = "")

/** One position slot: what is there now and what it replaced. Either side may be null. */
data class Slot(val current: Element?, val removed: Element?) {
    val changed: Boolean get() = removed != null
    /** What the plan originally had (removed if replaced, else current). */
    val original: Element? get() = removed ?: current
}

data class Lesson(
    val date: LocalDate,
    val start: LocalTime,
    val end: LocalTime,
    /** WebUntis period ids, one per 45-min unit (double lesson = 2 ids). */
    val periodIds: List<Long>,
    /** WebUntis entry type: NORMAL_TEACHING_PERIOD, EXAM, EVENT, ... */
    val type: String,
    val status: LessonStatus,
    val subjects: List<Slot>,
    val teachers: List<Slot>,
    val rooms: List<Slot>,
    val substitutionText: String? = null,
    val lessonInfo: String? = null,
    val lessonText: String? = null,
    /** True when this lesson was not fetched but predicted from earlier weeks. */
    val predicted: Boolean = false,
) {
    /** Stable identity of a lesson within one fetch window. */
    val key: String get() = "$date|${periodIds.sorted().joinToString(",")}"

    /** Subject short name for display/statistics: current, else removed, else the entry type. */
    val subject: String
        get() = subjects.firstOrNull()?.let { (it.current ?: it.removed)?.shortName } ?: type

    val isExam: Boolean get() = type == "EXAM"
}

enum class DayStatus { REGULAR, NO_DATA, NOT_ALLOWED, OTHER }

data class Day(val date: LocalDate, val status: DayStatus, val lessons: List<Lesson>)

/** One unit of the school's time grid (1-based index = "Stunde"). */
data class TimeUnit(val index: Int, val start: LocalTime, val end: LocalTime)

data class Holiday(val name: String, val start: LocalDate, val end: LocalDate)

data class SchoolYear(val id: Long, val name: String, val start: LocalDate, val end: LocalDate)

data class MasterData(
    val personId: Long,
    val displayName: String,
    val schoolName: String,
    val timeGrid: List<TimeUnit>,
    val holidays: List<Holiday>,
    val schoolYears: List<SchoolYear>,
)

enum class AbsenceCategory { EXCUSED, UNEXCUSED, NOT_COUNTED, OPEN }

data class Absence(
    val id: Long,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val reason: String,
    /** Raw WebUntis excuse status text, null = no decision yet. */
    val excuseStatus: String?,
    val text: String = "",
) {
    val isLateness: Boolean get() = reason == "Verspätung"

    val category: AbsenceCategory
        get() = when (excuseStatus) {
            null -> AbsenceCategory.OPEN
            "entschuldigt" -> AbsenceCategory.EXCUSED
            "zählt nicht" -> AbsenceCategory.NOT_COUNTED
            else -> AbsenceCategory.UNEXCUSED
        }
}

enum class ChangeKind(val urgent: Boolean) {
    FOYER(true),
    CANCELLED(true),
    FIRST_LESSON(true),
    ROOM(false),
    TEACHER(false),
    EXAM(false),
    ADDED(false),
    /** A previously notified urgent change is gone again (lesson back to normal). */
    RESTORED(false),
}

/** Something the user should be told about. [fingerprint] identifies it for de-duplication. */
data class Change(
    val kind: ChangeKind,
    val date: LocalDate,
    val lessonKey: String?,
    val subject: String?,
    /** German notification title, e.g. "Foyer: SP-LK2 (5.–6. Std.)". */
    val title: String,
    /** German notification text, e.g. "Mo 03.11. 11:45 · Raum R106 → Foyer · ohne Lehrkraft". */
    val text: String,
    val fingerprint: String,
) {
    val urgent: Boolean get() = kind.urgent
}

/** Result of [io.github.omeg4dev.greatuntis.stats.AbsenceStats.compute]. Weekdays: 1 = Monday … 7 = Sunday. */
data class AbsenceStatsResult(
    val heldUnits: Int,
    val missedUnits: Int,
    val missedDays: Int,
    val missedByCategory: Map<AbsenceCategory, Int>,
    val missedBySubject: Map<String, Int>,
    val heldBySubject: Map<String, Int>,
    val missedByWeekday: Map<Int, Int>,
    /** Key = 1-based time-grid unit ("Stunde"). */
    val missedByPeriod: Map<Int, Int>,
    /** Key = Monday of the ISO week. */
    val missedByWeek: Map<LocalDate, Int>,
    val absenceCount: Int,
    val openAbsences: Int,
    val lateCount: Int,
    val lateMinutes: Int,
    val lateByWeekday: Map<Int, Int>,
    val cancelledBySubject: Map<String, Int>,
    val foyerBySubject: Map<String, Int>,
)

/** User settings. Defaults are the product defaults. */
data class Settings(
    val notifyKinds: Set<ChangeKind> = ChangeKind.entries.toSet(),
    /** Room short names starting with this (case-insensitive) count as Foyer. */
    val foyerPrefix: String = "Foyer",
    /** Fast polling on school days from [schoolStart] until the end of the last lesson. */
    val schoolStart: LocalTime = LocalTime.of(6, 0),
    val pollSeconds: Int = 60,
    /** Slow polling (every 15 min) in the evening to catch next-day changes. */
    val eveningStart: LocalTime = LocalTime.of(18, 0),
    val eveningEnd: LocalTime = LocalTime.of(22, 0),
    val watchEnabled: Boolean = true,
    /** German state code for OpenHolidays subdivision suffix (e.g. "NW" for Nordrhein-Westfalen). */
    val federalState: String = "NW",
)

enum class SyncState { IDLE, SYNCING, OFFLINE, AUTH_ERROR, ERROR }

/** A school found via the Untis school search (onboarding). */
data class SchoolInfo(
    val displayName: String,
    val address: String,
    /** Login name, e.g. "demo-schule" → Account.school. */
    val loginName: String,
    /** e.g. "demo.webuntis.com" → Account.host. */
    val host: String,
    val schoolNumber: String,
)
