package io.github.omeg4dev.greatuntis.watch

import io.github.omeg4dev.greatuntis.model.Change
import io.github.omeg4dev.greatuntis.model.ChangeKind
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.DayStatus
import io.github.omeg4dev.greatuntis.model.ElementStatus
import io.github.omeg4dev.greatuntis.model.Lesson
import io.github.omeg4dev.greatuntis.model.LessonStatus
import io.github.omeg4dev.greatuntis.model.Settings
import io.github.omeg4dev.greatuntis.model.TimeUnit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

class ChangeDetector(private val settings: Settings, private val timeGrid: List<TimeUnit>) {

    data class Result(val toNotify: List<Change>, val active: Map<String, Change>)

    /** [days] = the freshly fetched window (today + next school day). [previous] = Result.active of the last run. */
    fun detect(days: List<Day>, now: LocalDateTime, previous: Map<String, Change>): Result {
        val currentChanges = mutableListOf<Pair<Change, LocalTime>>()

        for (day in days) {
            if (day.status != DayStatus.REGULAR) continue

            // 1. FIRST_LESSON per day
            val regularLessons = day.lessons.filter { it.status != LessonStatus.ADDITIONAL }
            val effectiveLessons = day.lessons.filter { it.status != LessonStatus.CANCELLED }

            if (regularLessons.isNotEmpty()) {
                val regularStart = regularLessons.minOf { it.start }
                val regularStartDt = LocalDateTime.of(day.date, regularStart)

                if (now < regularStartDt) {
                    val effectiveStart = effectiveLessons.minOfOrNull { it.start }
                    if (effectiveStart == null || effectiveStart > regularStart) {
                        val change = buildFirstLessonChange(day.date, regularStart, effectiveStart)
                        currentChanges.add(change to regularStart)
                    }
                }
            }

            // 2. Per lesson changes
            for (lesson in day.lessons) {
                val lessonEndDt = LocalDateTime.of(lesson.date, lesson.end)
                if (lessonEndDt <= now) continue

                val isCancelled = lesson.status == LessonStatus.CANCELLED
                val foyerRoom = lesson.rooms.firstOrNull { slot ->
                    val cur = slot.current
                    cur != null && cur.shortName.startsWith(settings.foyerPrefix, ignoreCase = true) &&
                            (cur.status == ElementStatus.ADDED || slot.removed != null)
                }

                if (isCancelled) {
                    currentChanges.add(buildCancelledChange(lesson) to lesson.start)
                } else if (foyerRoom != null) {
                    currentChanges.add(buildFoyerChange(lesson, foyerRoom) to lesson.start)
                } else {
                    // FOYER suppresses ROOM and TEACHER
                    val roomChange = lesson.rooms.firstOrNull { it.removed != null && it.current != null }
                    if (roomChange != null) {
                        currentChanges.add(buildRoomChange(lesson, roomChange) to lesson.start)
                    }

                    val teacherChange = lesson.teachers.firstOrNull { it.removed != null }
                    if (teacherChange != null) {
                        currentChanges.add(buildTeacherChange(lesson, teacherChange) to lesson.start)
                    }

                    if (lesson.type == "EXAM") {
                        currentChanges.add(buildExamChange(lesson) to lesson.start)
                    }

                    if (lesson.status == LessonStatus.ADDITIONAL) {
                        currentChanges.add(buildAddedChange(lesson) to lesson.start)
                    }
                }
            }
        }

        // 3. RESTORED changes
        val restoredChanges = mutableListOf<Pair<Change, LocalTime>>()
        for ((_, prevChange) in previous) {
            if (prevChange.kind != ChangeKind.FOYER && prevChange.kind != ChangeKind.CANCELLED) continue
            val lessonKey = prevChange.lessonKey ?: continue

            val matchingDay = days.firstOrNull { it.status == DayStatus.REGULAR && it.date == prevChange.date }
                ?: continue
            val currentLesson = matchingDay.lessons.firstOrNull { it.key == lessonKey } ?: continue

            val lessonEndDt = LocalDateTime.of(currentLesson.date, currentLesson.end)
            if (lessonEndDt <= now) continue

            val isCancelled = currentLesson.status == LessonStatus.CANCELLED
            val isFoyer = currentLesson.rooms.any { slot ->
                val cur = slot.current
                cur != null && cur.shortName.startsWith(settings.foyerPrefix, ignoreCase = true) &&
                        (cur.status == ElementStatus.ADDED || slot.removed != null)
            }

            if (!isCancelled && !isFoyer) {
                restoredChanges.add(buildRestoredChange(currentLesson) to currentLesson.start)
            }
        }

        val allChangesWithTime = currentChanges + restoredChanges
        val activeMap = allChangesWithTime.associate { it.first.fingerprint to it.first }

        val toNotify = allChangesWithTime
            .filter { (change, _) ->
                change.fingerprint !in previous && change.kind in settings.notifyKinds
            }
            .sortedWith(
                compareByDescending<Pair<Change, LocalTime>> { it.first.urgent }
                    .thenBy { it.first.date }
                    .thenBy { it.second }
            )
            .map { it.first }

        return Result(toNotify = toNotify, active = activeMap)
    }

    private fun unitLabel(lesson: Lesson): String {
        val matching = timeGrid.filter { it.start >= lesson.start && it.end <= lesson.end }
            .ifEmpty {
                timeGrid.filter { it.start < lesson.end && it.end > lesson.start }
            }
        if (matching.isEmpty()) return ""
        val minIdx = matching.minOf { it.index }
        val maxIdx = matching.maxOf { it.index }
        return if (minIdx == maxIdx) "$minIdx. Std." else "$minIdx.–$maxIdx. Std."
    }

    private fun formatWeekday(date: LocalDate): String = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "Mo"
        DayOfWeek.TUESDAY -> "Di"
        DayOfWeek.WEDNESDAY -> "Mi"
        DayOfWeek.THURSDAY -> "Do"
        DayOfWeek.FRIDAY -> "Fr"
        DayOfWeek.SATURDAY -> "Sa"
        DayOfWeek.SUNDAY -> "So"
    }

    private fun formatDateTime(date: LocalDate, time: LocalTime): String {
        val weekday = formatWeekday(date)
        return String.format(Locale.ROOT, "%s %02d.%02d. %02d:%02d", weekday, date.dayOfMonth, date.monthValue, time.hour, time.minute)
    }

    private fun subjectWithUnit(subject: String, unitStr: String): String {
        return if (unitStr.isNotEmpty()) "$subject ($unitStr)" else subject
    }

    private fun buildCancelledChange(lesson: Lesson): Change {
        val unitStr = unitLabel(lesson)
        val title = "Entfall: ${subjectWithUnit(lesson.subject, unitStr)}"
        val timeStr = formatDateTime(lesson.date, lesson.start)
        val subText = lesson.substitutionText ?: "fällt aus"
        val text = "$timeStr · $subText"
        val detail = lesson.substitutionText ?: "cancelled"
        val fingerprint = "CANCELLED|${lesson.key}|$detail"

        return Change(
            kind = ChangeKind.CANCELLED,
            date = lesson.date,
            lessonKey = lesson.key,
            subject = lesson.subject,
            title = title,
            text = text,
            fingerprint = fingerprint,
        )
    }

    private fun buildFoyerChange(lesson: Lesson, foyerSlot: io.github.omeg4dev.greatuntis.model.Slot): Change {
        val unitStr = unitLabel(lesson)
        val title = "Foyer: ${subjectWithUnit(lesson.subject, unitStr)}"
        val timeStr = formatDateTime(lesson.date, lesson.start)

        val removedRoom = foyerSlot.removed
        val currentRoom = foyerSlot.current!!
        val roomPart = if (removedRoom != null) {
            "Raum ${removedRoom.shortName} → ${currentRoom.shortName}"
        } else {
            "Raum ${currentRoom.shortName}"
        }

        val teacherSlot = lesson.teachers.firstOrNull()
        val teacherPart = if (teacherSlot?.removed != null) {
            if (teacherSlot.current == null) "ohne Lehrkraft" else "Vertretung ${teacherSlot.current.shortName}"
        } else null

        val textParts = mutableListOf(timeStr, roomPart)
        if (teacherPart != null) textParts.add(teacherPart)
        if (!lesson.substitutionText.isNullOrBlank()) textParts.add(lesson.substitutionText)

        val text = textParts.joinToString(" · ")
        val detail = "${removedRoom?.shortName ?: "-"}>${currentRoom.shortName}"
        val fingerprint = "FOYER|${lesson.key}|$detail"

        return Change(
            kind = ChangeKind.FOYER,
            date = lesson.date,
            lessonKey = lesson.key,
            subject = lesson.subject,
            title = title,
            text = text,
            fingerprint = fingerprint,
        )
    }

    private fun buildRoomChange(lesson: Lesson, roomSlot: io.github.omeg4dev.greatuntis.model.Slot): Change {
        val unitStr = unitLabel(lesson)
        val title = "Raumänderung: ${subjectWithUnit(lesson.subject, unitStr)}"
        val timeStr = formatDateTime(lesson.date, lesson.start)
        val removed = roomSlot.removed!!
        val current = roomSlot.current!!
        val text = "$timeStr · Raum ${removed.shortName} → ${current.shortName}"
        val detail = "${removed.shortName}>${current.shortName}"
        val fingerprint = "ROOM|${lesson.key}|$detail"

        return Change(
            kind = ChangeKind.ROOM,
            date = lesson.date,
            lessonKey = lesson.key,
            subject = lesson.subject,
            title = title,
            text = text,
            fingerprint = fingerprint,
        )
    }

    private fun buildTeacherChange(lesson: Lesson, teacherSlot: io.github.omeg4dev.greatuntis.model.Slot): Change {
        val unitStr = unitLabel(lesson)
        val title = "Vertretung: ${subjectWithUnit(lesson.subject, unitStr)}"
        val timeStr = formatDateTime(lesson.date, lesson.start)
        val removed = teacherSlot.removed!!
        val current = teacherSlot.current
        val teacherDest = current?.shortName ?: "ohne Lehrkraft"
        val text = "$timeStr · ${removed.shortName} → $teacherDest"
        val detail = "${removed.shortName}>${current?.shortName ?: "-"}"
        val fingerprint = "TEACHER|${lesson.key}|$detail"

        return Change(
            kind = ChangeKind.TEACHER,
            date = lesson.date,
            lessonKey = lesson.key,
            subject = lesson.subject,
            title = title,
            text = text,
            fingerprint = fingerprint,
        )
    }

    private fun buildExamChange(lesson: Lesson): Change {
        val unitStr = unitLabel(lesson)
        val title = "Klausur: ${subjectWithUnit(lesson.subject, unitStr)}"
        val timeStr = formatDateTime(lesson.date, lesson.start)
        val text = "$timeStr · ${lesson.lessonInfo ?: "Klausur"}"
        val detail = lesson.lessonInfo ?: "exam"
        val fingerprint = "EXAM|${lesson.key}|$detail"

        return Change(
            kind = ChangeKind.EXAM,
            date = lesson.date,
            lessonKey = lesson.key,
            subject = lesson.subject,
            title = title,
            text = text,
            fingerprint = fingerprint,
        )
    }

    private fun buildAddedChange(lesson: Lesson): Change {
        val unitStr = unitLabel(lesson)
        val title = "Zusatzstunde: ${subjectWithUnit(lesson.subject, unitStr)}"
        val timeStr = formatDateTime(lesson.date, lesson.start)
        val roomName = lesson.rooms.firstOrNull()?.current?.shortName ?: ""
        val roomPart = if (roomName.isNotEmpty()) " · Raum $roomName" else ""
        val text = "$timeStr$roomPart"
        val detail = roomName.ifEmpty { "added" }
        val fingerprint = "ADDED|${lesson.key}|$detail"

        return Change(
            kind = ChangeKind.ADDED,
            date = lesson.date,
            lessonKey = lesson.key,
            subject = lesson.subject,
            title = title,
            text = text,
            fingerprint = fingerprint,
        )
    }

    private fun buildRestoredChange(lesson: Lesson): Change {
        val unitStr = unitLabel(lesson)
        val title = "Wieder regulär: ${subjectWithUnit(lesson.subject, unitStr)}"
        val timeStr = formatDateTime(lesson.date, lesson.start)
        val roomName = lesson.rooms.firstOrNull()?.current?.shortName ?: ""
        val roomPart = if (roomName.isNotEmpty()) " · Raum $roomName" else ""
        val text = "$timeStr$roomPart"
        val detail = roomName.ifEmpty { "regular" }
        val fingerprint = "RESTORED|${lesson.key}|$detail"

        return Change(
            kind = ChangeKind.RESTORED,
            date = lesson.date,
            lessonKey = lesson.key,
            subject = lesson.subject,
            title = title,
            text = text,
            fingerprint = fingerprint,
        )
    }

    private fun buildFirstLessonChange(date: LocalDate, regularStart: LocalTime, effectiveStart: LocalTime?): Change {
        val weekday = formatWeekday(date)
        if (effectiveStart != null) {
            val title = String.format(Locale.ROOT, "%s: Unterricht erst ab %02d:%02d", weekday, effectiveStart.hour, effectiveStart.minute)
            val cancelledUnits = timeGrid.filter { it.start >= regularStart && it.end <= effectiveStart }
            val unitStr = if (cancelledUnits.isNotEmpty()) {
                val minIdx = cancelledUnits.minOf { it.index }
                val maxIdx = cancelledUnits.maxOf { it.index }
                if (minIdx == maxIdx) "$minIdx. Std." else "$minIdx.–$maxIdx. Std."
            } else ""

            val text = if (unitStr.isNotEmpty()) "Die $unitStr entfällt." else "Der Unterricht beginnt später."
            val detail = "$date|$effectiveStart"
            val fingerprint = "FIRST_LESSON|null|$detail"

            return Change(
                kind = ChangeKind.FIRST_LESSON,
                date = date,
                lessonKey = null,
                subject = null,
                title = title,
                text = text,
                fingerprint = fingerprint,
            )
        } else {
            val title = "$weekday: Kein Unterricht"
            val text = "Alle Stunden entfallen."
            val detail = "$date|none"
            val fingerprint = "FIRST_LESSON|null|$detail"

            return Change(
                kind = ChangeKind.FIRST_LESSON,
                date = date,
                lessonKey = null,
                subject = null,
                title = title,
                text = text,
                fingerprint = fingerprint,
            )
        }
    }
}
