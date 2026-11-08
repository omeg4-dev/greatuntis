package io.github.omeg4dev.greatuntis.timetable

import io.github.omeg4dev.greatuntis.model.ElementStatus
import io.github.omeg4dev.greatuntis.model.Lesson
import io.github.omeg4dev.greatuntis.model.LessonStatus
import io.github.omeg4dev.greatuntis.model.Slot

enum class LessonKind {
    REGULAR, CANCELLED, FOYER, ROOM_CHANGED, TEACHER_CHANGED, SUBSTITUTION,
    EXAM, EVENT, ADDITIONAL, PREDICTED
}

data class LessonView(
    val lesson: Lesson,
    /** Primary kind, by priority (see [LessonViews.of]). */
    val kind: LessonKind,
    /** All kinds that apply (always contains [kind]). */
    val badges: Set<LessonKind>,
    /** Display title: subject long name if present, else subject short name, else INFO text, etc. */
    val title: String,
    /** Short label for tight grid cells, e.g. "SP-LK2", "Klausur", "Päd. Tag". */
    val shortTitle: String,
    val subjects: List<Slot>,
    val teachers: List<Slot>,
    val rooms: List<Slot>,
    /** INFO element texts (exam/event titles). */
    val infos: List<String>,
    /** Room label to display. */
    val roomText: String,
    val isFoyer: Boolean,
    /** Free EVENT: no TEACHER and no ROOM element (e.g. "Päd. TAg"). */
    val isFreeEvent: Boolean,
)

private val PAD_TAG_REGEX = Regex("(?i)^päd\\.?\\s*tag$")

/** Normalise "Päd. TAg" / "PÄD.TAG" etc. to the canonical display form. */
private fun normalisePadTag(text: String): String =
    if (PAD_TAG_REGEX.matches(text)) "Pädagogischer Tag" else text

object LessonViews {
    /**
     * Build a [LessonView] for [lesson] using [foyerPrefix] to identify Foyer rooms.
     * Element routing: if any element in the lesson has a non-empty `kind`, route each slot by
     * the kind of its effective element (current ?: removed).  Otherwise use positional lists.
     */
    fun of(lesson: Lesson, foyerPrefix: String): LessonView {
        // Gather all slots from positions 1–3 in positional order
        val allSlots: List<Slot> = lesson.subjects + lesson.teachers + lesson.rooms

        // Determine whether we have kind information
        val hasKind = allSlots.any { slot ->
            (slot.current?.kind?.isNotEmpty() == true) || (slot.removed?.kind?.isNotEmpty() == true)
        }

        val routedSubjects: MutableList<Slot>
        val routedTeachers: MutableList<Slot>
        val routedRooms: MutableList<Slot>
        val infos: MutableList<String>

        if (hasKind) {
            routedSubjects = mutableListOf()
            routedTeachers = mutableListOf()
            routedRooms = mutableListOf()
            infos = mutableListOf()
            for (slot in allSlots) {
                val effectiveElement = slot.current ?: slot.removed ?: continue
                when (effectiveElement.kind) {
                    "SUBJECT" -> routedSubjects.add(slot)
                    "TEACHER" -> routedTeachers.add(slot)
                    "ROOM" -> routedRooms.add(slot)
                    "INFO" -> {
                        val text = effectiveElement.longName.ifBlank { effectiveElement.shortName }
                        if (text.isNotEmpty()) infos.add(text)
                    }
                    else -> { /* ignore CLASS, etc. */ }
                }
            }
        } else {
            // Old cache / predicted lesson: use positional lists unchanged
            routedSubjects = lesson.subjects.toMutableList()
            routedTeachers = lesson.teachers.toMutableList()
            routedRooms = lesson.rooms.toMutableList()
            infos = mutableListOf()
        }

        // isFoyer: some room slot whose current shortName starts with foyerPrefix AND
        // (current.status == ADDED OR removed != null)
        val isFoyer = routedRooms.any { slot ->
            val cur = slot.current
            cur != null &&
                cur.shortName.startsWith(foyerPrefix, ignoreCase = true) &&
                (cur.status == ElementStatus.ADDED || slot.removed != null)
        }

        // isFreeEvent: type == EVENT and no routed teachers and no routed rooms
        val isFreeEvent = lesson.type == "EVENT" && routedTeachers.isEmpty() && routedRooms.isEmpty()

        // --- Title ---
        val rawTitle: String = run {
            val subjectLong = routedSubjects.firstOrNull()?.let { (it.current ?: it.removed) }
                ?.longName?.takeIf { it.isNotBlank() }
            if (subjectLong != null) return@run subjectLong

            val subjectShort = routedSubjects.firstOrNull()?.let { (it.current ?: it.removed) }
                ?.shortName?.takeIf { it.isNotBlank() }
            if (subjectShort != null) return@run subjectShort

            val infoText = infos.firstOrNull()?.takeIf { it.isNotBlank() }
            if (infoText != null) return@run infoText

            val lessonInfo = lesson.lessonInfo?.takeIf { it.isNotBlank() }
            if (lessonInfo != null) return@run lessonInfo

            // German fallback by type
            when (lesson.type) {
                "EXAM" -> "Klausur"
                "EVENT" -> "Veranstaltung"
                else -> lesson.type
            }
        }
        val title = normalisePadTag(rawTitle)

        // --- Short title ---
        val rawShortTitle: String = run {
            val subjectShort = routedSubjects.firstOrNull()?.let { (it.current ?: it.removed) }
                ?.shortName?.takeIf { it.isNotBlank() }
            if (subjectShort != null) return@run subjectShort

            when (lesson.type) {
                "EXAM" -> "Klausur"
                "EVENT" -> normalisePadTag(rawTitle).let { normalised ->
                    if (normalised == "Pädagogischer Tag") "Päd. Tag" else normalised
                }
                else -> title
            }
        }
        val shortTitle = rawShortTitle.ifBlank { title }

        // --- roomText ---
        val roomText: String = when {
            isFoyer -> {
                val foyerRoom = routedRooms.firstOrNull { slot ->
                    val cur = slot.current
                    cur != null && cur.shortName.startsWith(foyerPrefix, ignoreCase = true) &&
                        (cur.status == ElementStatus.ADDED || slot.removed != null)
                }
                foyerRoom?.current?.shortName ?: "Foyer"
            }
            routedRooms.any { it.current != null } -> {
                routedRooms.mapNotNull { it.current?.shortName?.takeIf { n -> n.isNotBlank() } }
                    .joinToString(", ")
            }
            routedRooms.any { it.removed != null } -> {
                routedRooms.mapNotNull { it.removed?.shortName?.takeIf { n -> n.isNotBlank() } }
                    .joinToString(", ")
            }
            else -> ""
        }

        // --- Badges ---
        val badges = mutableSetOf<LessonKind>()
        if (lesson.predicted) badges.add(LessonKind.PREDICTED)
        if (lesson.status == LessonStatus.CANCELLED) badges.add(LessonKind.CANCELLED)
        if (lesson.type == "EXAM") badges.add(LessonKind.EXAM)
        if (lesson.type == "EVENT") badges.add(LessonKind.EVENT)
        if (isFoyer) badges.add(LessonKind.FOYER)
        if (!isFoyer && routedRooms.any { it.removed != null && it.current != null }) {
            badges.add(LessonKind.ROOM_CHANGED)
        }
        if (routedTeachers.any { it.removed != null }) badges.add(LessonKind.TEACHER_CHANGED)
        if (lesson.substitutionText != null || lesson.status == LessonStatus.CHANGED) {
            badges.add(LessonKind.SUBSTITUTION)
        }
        if (lesson.status == LessonStatus.ADDITIONAL) badges.add(LessonKind.ADDITIONAL)
        if (badges.isEmpty()) badges.add(LessonKind.REGULAR)

        // --- Primary kind by priority ---
        val kindPriority = listOf(
            LessonKind.PREDICTED,
            LessonKind.CANCELLED,
            LessonKind.EXAM,
            LessonKind.EVENT,
            LessonKind.FOYER,
            LessonKind.ROOM_CHANGED,
            LessonKind.TEACHER_CHANGED,
            LessonKind.ADDITIONAL,
            LessonKind.SUBSTITUTION,
            LessonKind.REGULAR,
        )
        val kind = kindPriority.first { it in badges }

        return LessonView(
            lesson = lesson,
            kind = kind,
            badges = badges,
            title = title,
            shortTitle = shortTitle,
            subjects = routedSubjects,
            teachers = routedTeachers,
            rooms = routedRooms,
            infos = infos,
            roomText = roomText,
            isFoyer = isFoyer,
            isFreeEvent = isFreeEvent,
        )
    }
}
