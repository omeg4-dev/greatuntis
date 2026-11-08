package io.github.omeg4dev.greatuntis.stats

import io.github.omeg4dev.greatuntis.model.Absence
import io.github.omeg4dev.greatuntis.model.AbsenceCategory
import io.github.omeg4dev.greatuntis.model.AbsenceStatsResult
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.ElementStatus
import io.github.omeg4dev.greatuntis.model.Lesson
import io.github.omeg4dev.greatuntis.model.LessonStatus
import io.github.omeg4dev.greatuntis.model.TimeUnit
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

object AbsenceStats {

    /** Rules: see tools/untis_probe.py `compute_stats` — results must match it exactly. */
    fun compute(
        days: List<Day>,
        absences: List<Absence>,
        timeGrid: List<TimeUnit>,
        foyerPrefix: String = "Foyer",
    ): AbsenceStatsResult {
        // Collect units
        val units = mutableListOf<LessonUnit>()
        for (day in days) {
            for (lesson in day.lessons) {
                val held = lesson.status != LessonStatus.CANCELLED && lesson.type != "EVENT"
                for ((s, e) in unitsOfEntry(lesson, timeGrid)) {
                    units.add(LessonUnit(s, e, lesson.subject, held))
                }
            }
        }
        units.sort()

        val sortedAbsences = absences.sortedWith(compareBy({ it.start }, { it.end }, { it.id }))
        val late = sortedAbsences.filter { it.isLateness }
        val real = sortedAbsences.filterNot { it.isLateness }

        val heldBySubject = mutableMapOf<String, Int>()
        for (u in units) {
            if (u.held) {
                heldBySubject[u.subject] = (heldBySubject[u.subject] ?: 0) + 1
            }
        }

        val missedBySubject = mutableMapOf<String, Int>()
        val missedByCategory = mutableMapOf<AbsenceCategory, Int>()
        val missedByWeekday = mutableMapOf<Int, Int>()
        val missedByPeriod = mutableMapOf<Int, Int>()
        val missedByWeek = mutableMapOf<LocalDate, Int>()
        val missedDays = mutableSetOf<LocalDate>()

        for (u in units) {
            if (!u.held) continue
            for (a in real) {
                val overlapStart = if (u.start > a.start) u.start else a.start
                val overlapEnd = if (u.end < a.end) u.end else a.end
                val overlapSeconds = Duration.between(overlapStart, overlapEnd).seconds
                val overlapMinutes = overlapSeconds / 60.0
                if (overlapMinutes >= 1.0) {
                    missedBySubject[u.subject] = (missedBySubject[u.subject] ?: 0) + 1
                    missedByCategory[a.category] = (missedByCategory[a.category] ?: 0) + 1
                    val weekday = u.start.dayOfWeek.value
                    missedByWeekday[weekday] = (missedByWeekday[weekday] ?: 0) + 1

                    val gridIdx = timeGrid.indexOfFirst { it.start == u.start.toLocalTime() }
                    val periodIdx = if (gridIdx >= 0) gridIdx + 1 else 0
                    missedByPeriod[periodIdx] = (missedByPeriod[periodIdx] ?: 0) + 1

                    val monday = u.start.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    missedByWeek[monday] = (missedByWeek[monday] ?: 0) + 1

                    missedDays.add(u.start.toLocalDate())
                    break
                }
            }
        }

        var lateMinutes = 0
        val lateByWeekday = mutableMapOf<Int, Int>()
        for (a in late) {
            val durationSeconds = Duration.between(a.start, a.end).seconds
            lateMinutes += (durationSeconds / 60).toInt()
            val weekday = a.start.dayOfWeek.value
            lateByWeekday[weekday] = (lateByWeekday[weekday] ?: 0) + 1
        }

        val cancelledBySubject = mutableMapOf<String, Int>()
        val foyerBySubject = mutableMapOf<String, Int>()
        for (day in days) {
            for (lesson in day.lessons) {
                val n = unitsOfEntry(lesson, timeGrid).size
                if (lesson.status == LessonStatus.CANCELLED) {
                    cancelledBySubject[lesson.subject] = (cancelledBySubject[lesson.subject] ?: 0) + n
                }
                val isFoyer = lesson.rooms.any { slot ->
                    val cur = slot.current
                    cur != null && cur.shortName.startsWith(foyerPrefix, ignoreCase = true) && cur.status == ElementStatus.ADDED
                }
                if (isFoyer) {
                    foyerBySubject[lesson.subject] = (foyerBySubject[lesson.subject] ?: 0) + n
                }
            }
        }

        return AbsenceStatsResult(
            heldUnits = heldBySubject.values.sum(),
            missedUnits = missedBySubject.values.sum(),
            missedDays = missedDays.size,
            missedByCategory = missedByCategory.toSortedMap(),
            missedBySubject = missedBySubject.toSortedMap(),
            heldBySubject = heldBySubject.toSortedMap(),
            missedByWeekday = missedByWeekday.toSortedMap(),
            missedByPeriod = missedByPeriod.toSortedMap(),
            missedByWeek = missedByWeek.toSortedMap(),
            absenceCount = real.size,
            openAbsences = real.count { it.category == AbsenceCategory.OPEN },
            lateCount = late.size,
            lateMinutes = lateMinutes,
            lateByWeekday = lateByWeekday.toSortedMap(),
            cancelledBySubject = cancelledBySubject.toSortedMap(),
            foyerBySubject = foyerBySubject.toSortedMap(),
        )
    }

    private fun unitsOfEntry(
        lesson: Lesson,
        timeGrid: List<TimeUnit>,
    ): List<Pair<LocalDateTime, LocalDateTime>> {
        val a = LocalDateTime.of(lesson.date, lesson.start)
        val b = LocalDateTime.of(lesson.date, lesson.end)
        val out = mutableListOf<Pair<LocalDateTime, LocalDateTime>>()
        for (u in timeGrid) {
            val s = LocalDateTime.of(lesson.date, u.start)
            val e = LocalDateTime.of(lesson.date, u.end)
            if (s >= a && e <= b) {
                out.add(s to e)
            }
        }
        return if (out.isNotEmpty()) out else listOf(a to b)
    }

    private data class LessonUnit(
        val start: LocalDateTime,
        val end: LocalDateTime,
        val subject: String,
        val held: Boolean,
    ) : Comparable<LessonUnit> {
        override fun compareTo(other: LessonUnit): Int {
            val c = start.compareTo(other.start)
            if (c != 0) return c
            val c2 = end.compareTo(other.end)
            if (c2 != 0) return c2
            val c3 = subject.compareTo(other.subject)
            if (c3 != 0) return c3
            return held.compareTo(other.held)
        }
    }
}
