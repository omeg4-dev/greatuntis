package io.github.omeg4dev.greatuntis.timetable

import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.DayStatus
import io.github.omeg4dev.greatuntis.model.Element
import io.github.omeg4dev.greatuntis.model.Holiday
import io.github.omeg4dev.greatuntis.model.Lesson
import io.github.omeg4dev.greatuntis.model.LessonStatus
import io.github.omeg4dev.greatuntis.model.Slot
import io.github.omeg4dev.greatuntis.model.TimeUnit
import java.time.DayOfWeek
import java.time.LocalDate

object Prediction {
    /**
     * Returns a Day for every date in [dates]: fetched days with status REGULAR/NO_DATA are returned
     * unchanged; NOT_ALLOWED or missing days are predicted from [history] (lessons marked predicted = true).
     * Dates where [isFree] returns true → Day(date, NO_DATA, []).
     */
    fun fill(
        history: List<Day>,
        dates: List<LocalDate>,
        timeGrid: List<TimeUnit>,
        isFree: (LocalDate) -> Boolean,
    ): List<Day> {
        val historyByDate = history.associateBy { it.date }
        val sortedTimeGrid = timeGrid.sortedBy { it.index }

        return dates.map { date ->
            // isFree predicate (holidays, weekends, custom free days) → Day(date, NO_DATA, [])
            if (isFree(date)) {
                return@map Day(date, DayStatus.NO_DATA, emptyList())
            }

            // Fetched day in history with status REGULAR or NO_DATA → returned unchanged
            val historyDay = historyByDate[date]
            if (historyDay != null && (historyDay.status == DayStatus.REGULAR || historyDay.status == DayStatus.NO_DATA)) {
                return@map historyDay
            }

            // Otherwise predicted
            predictDay(date, history, sortedTimeGrid)
        }
    }

    /**
     * Returns a Day for every date in [dates]: fetched days with status REGULAR/NO_DATA are returned
     * unchanged; NOT_ALLOWED or missing days are predicted from [history] (lessons marked predicted = true).
     * Dates inside a [holidays] range or on weekends are returned as Day(date, NO_DATA, []).
     */
    fun fill(
        history: List<Day>,
        dates: List<LocalDate>,
        timeGrid: List<TimeUnit>,
        holidays: List<Holiday> = emptyList(),
    ): List<Day> = fill(
        history = history,
        dates = dates,
        timeGrid = timeGrid,
        isFree = { date ->
            holidays.any { date >= it.start && date <= it.end } ||
                date.dayOfWeek == DayOfWeek.SATURDAY ||
                date.dayOfWeek == DayOfWeek.SUNDAY
        },
    )

    private data class ElementTuple(
        val subject: Element?,
        val teacher: Element?,
        val room: Element?,
    )

    private fun predictDay(
        targetDate: LocalDate,
        history: List<Day>,
        timeGrid: List<TimeUnit>,
    ): Day {
        val referenceDays = history
            .filter { it.date < targetDate && it.date.dayOfWeek == targetDate.dayOfWeek && it.status == DayStatus.REGULAR && it.lessons.isNotEmpty() }
            .sortedByDescending { it.date }
            .take(4)

        val n = referenceDays.size
        if (n == 0) {
            return Day(targetDate, DayStatus.NOT_ALLOWED, emptyList())
        }

        val threshold = (n + 1) / 2 // ceil(n / 2)

        val predictedUnits = mutableListOf<Pair<TimeUnit, ElementTuple>>()

        for (unit in timeGrid) {
            data class Candidate(
                val tuple: ElementTuple,
                var count: Int,
                val firstSeenIndex: Int,
            )

            val candidates = mutableMapOf<Triple<String?, String?, String?>, Candidate>()

            for ((dayIdx, day) in referenceDays.withIndex()) {
                val coveringLessons = day.lessons.filter { lesson ->
                    lesson.type != "EXAM" && lesson.type != "EVENT" &&
                            lesson.status != LessonStatus.ADDITIONAL &&
                            lesson.start <= unit.start && lesson.end >= unit.end
                }

                val lesson = coveringLessons.firstOrNull() ?: continue
                val origSubject = lesson.subjects.firstOrNull()?.original
                val origTeacher = lesson.teachers.firstOrNull()?.original
                val origRoom = lesson.rooms.firstOrNull()?.original

                val key = Triple(origSubject?.shortName, origTeacher?.shortName, origRoom?.shortName)
                val candidate = candidates.getOrPut(key) {
                    Candidate(
                        tuple = ElementTuple(origSubject, origTeacher, origRoom),
                        count = 0,
                        firstSeenIndex = dayIdx,
                    )
                }
                candidate.count++
            }

            val validCandidates = candidates.values.filter { it.count >= threshold }
            if (validCandidates.isNotEmpty()) {
                val chosen = validCandidates.sortedWith(
                    compareByDescending<Candidate> { it.count }
                        .thenBy { it.firstSeenIndex }
                ).first()
                predictedUnits.add(unit to chosen.tuple)
            }
        }

        val mergedLessons = mutableListOf<Lesson>()
        var currentGroup = mutableListOf<Pair<TimeUnit, ElementTuple>>()

        fun flushGroup() {
            if (currentGroup.isEmpty()) return
            val firstUnit = currentGroup.first().first
            val lastUnit = currentGroup.last().first
            val tuple = currentGroup.first().second

            val lesson = Lesson(
                date = targetDate,
                start = firstUnit.start,
                end = lastUnit.end,
                periodIds = emptyList(),
                type = "NORMAL_TEACHING_PERIOD",
                status = LessonStatus.REGULAR,
                subjects = if (tuple.subject != null) listOf(Slot(current = tuple.subject, removed = null)) else emptyList(),
                teachers = if (tuple.teacher != null) listOf(Slot(current = tuple.teacher, removed = null)) else emptyList(),
                rooms = if (tuple.room != null) listOf(Slot(current = tuple.room, removed = null)) else emptyList(),
                substitutionText = null,
                lessonInfo = null,
                lessonText = null,
                predicted = true,
            )
            mergedLessons.add(lesson)
            currentGroup.clear()
        }

        for (i in predictedUnits.indices) {
            val (unit, tuple) = predictedUnits[i]
            if (currentGroup.isEmpty()) {
                currentGroup.add(unit to tuple)
            } else {
                val (prevUnit, prevTuple) = currentGroup.last()
                if (unit.index == prevUnit.index + 1 &&
                    tuple.subject?.shortName == prevTuple.subject?.shortName &&
                    tuple.teacher?.shortName == prevTuple.teacher?.shortName &&
                    tuple.room?.shortName == prevTuple.room?.shortName
                ) {
                    currentGroup.add(unit to tuple)
                } else {
                    flushGroup()
                    currentGroup.add(unit to tuple)
                }
            }
        }
        flushGroup()

        return Day(
            date = targetDate,
            status = DayStatus.NOT_ALLOWED,
            lessons = mergedLessons,
        )
    }
}
