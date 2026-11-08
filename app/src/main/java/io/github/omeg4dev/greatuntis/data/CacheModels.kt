package io.github.omeg4dev.greatuntis.data

import io.github.omeg4dev.greatuntis.model.Absence
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.DayStatus
import io.github.omeg4dev.greatuntis.model.Element
import io.github.omeg4dev.greatuntis.model.ElementStatus
import io.github.omeg4dev.greatuntis.model.Holiday
import io.github.omeg4dev.greatuntis.model.Lesson
import io.github.omeg4dev.greatuntis.model.LessonStatus
import io.github.omeg4dev.greatuntis.model.MasterData
import io.github.omeg4dev.greatuntis.model.SchoolYear
import io.github.omeg4dev.greatuntis.model.Slot
import io.github.omeg4dev.greatuntis.model.TimeUnit
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Serializable
data class ElementCache(
    val shortName: String,
    val longName: String,
    val status: String,
    val kind: String = "",
) {
    fun toModel(): Element = Element(
        shortName = shortName,
        longName = longName,
        status = ElementStatus.valueOf(status),
        kind = kind,
    )

    companion object {
        fun fromModel(e: Element): ElementCache = ElementCache(
            shortName = e.shortName,
            longName = e.longName,
            status = e.status.name,
            kind = e.kind,
        )
    }
}

@Serializable
data class SlotCache(
    val current: ElementCache? = null,
    val removed: ElementCache? = null,
) {
    fun toModel(): Slot = Slot(
        current = current?.toModel(),
        removed = removed?.toModel(),
    )

    companion object {
        fun fromModel(s: Slot): SlotCache = SlotCache(
            current = s.current?.let { ElementCache.fromModel(it) },
            removed = s.removed?.let { ElementCache.fromModel(it) },
        )
    }
}

@Serializable
data class LessonCache(
    val date: String,
    val start: String,
    val end: String,
    val periodIds: List<Long>,
    val type: String,
    val status: String,
    val subjects: List<SlotCache>,
    val teachers: List<SlotCache>,
    val rooms: List<SlotCache>,
    val substitutionText: String? = null,
    val lessonInfo: String? = null,
    val lessonText: String? = null,
    val predicted: Boolean = false,
) {
    fun toModel(): Lesson = Lesson(
        date = LocalDate.parse(date),
        start = LocalTime.parse(start),
        end = LocalTime.parse(end),
        periodIds = periodIds,
        type = type,
        status = LessonStatus.valueOf(status),
        subjects = subjects.map { it.toModel() },
        teachers = teachers.map { it.toModel() },
        rooms = rooms.map { it.toModel() },
        substitutionText = substitutionText,
        lessonInfo = lessonInfo,
        lessonText = lessonText,
        predicted = predicted,
    )

    companion object {
        fun fromModel(l: Lesson): LessonCache = LessonCache(
            date = l.date.toString(),
            start = l.start.toString(),
            end = l.end.toString(),
            periodIds = l.periodIds,
            type = l.type,
            status = l.status.name,
            subjects = l.subjects.map { SlotCache.fromModel(it) },
            teachers = l.teachers.map { SlotCache.fromModel(it) },
            rooms = l.rooms.map { SlotCache.fromModel(it) },
            substitutionText = l.substitutionText,
            lessonInfo = l.lessonInfo,
            lessonText = l.lessonText,
            predicted = l.predicted,
        )
    }
}

@Serializable
data class DayCache(
    val date: String,
    val status: String,
    val lessons: List<LessonCache>,
) {
    fun toModel(): Day = Day(
        date = LocalDate.parse(date),
        status = DayStatus.valueOf(status),
        lessons = lessons.map { it.toModel() },
    )

    companion object {
        fun fromModel(d: Day): DayCache = DayCache(
            date = d.date.toString(),
            status = d.status.name,
            lessons = d.lessons.map { LessonCache.fromModel(it) },
        )
    }
}

@Serializable
data class TimeUnitCache(
    val index: Int,
    val start: String,
    val end: String,
) {
    fun toModel(): TimeUnit = TimeUnit(
        index = index,
        start = LocalTime.parse(start),
        end = LocalTime.parse(end),
    )

    companion object {
        fun fromModel(t: TimeUnit): TimeUnitCache = TimeUnitCache(
            index = t.index,
            start = t.start.toString(),
            end = t.end.toString(),
        )
    }
}

@Serializable
data class HolidayCache(
    val name: String,
    val start: String,
    val end: String,
) {
    fun toModel(): Holiday = Holiday(
        name = name,
        start = LocalDate.parse(start),
        end = LocalDate.parse(end),
    )

    companion object {
        fun fromModel(h: Holiday): HolidayCache = HolidayCache(
            name = h.name,
            start = h.start.toString(),
            end = h.end.toString(),
        )
    }
}

@Serializable
data class SchoolYearCache(
    val id: Long,
    val name: String,
    val start: String,
    val end: String,
) {
    fun toModel(): SchoolYear = SchoolYear(
        id = id,
        name = name,
        start = LocalDate.parse(start),
        end = LocalDate.parse(end),
    )

    companion object {
        fun fromModel(s: SchoolYear): SchoolYearCache = SchoolYearCache(
            id = s.id,
            name = s.name,
            start = s.start.toString(),
            end = s.end.toString(),
        )
    }
}

@Serializable
data class MasterDataCache(
    val personId: Long,
    val displayName: String,
    val schoolName: String,
    val timeGrid: List<TimeUnitCache>,
    val holidays: List<HolidayCache>,
    val schoolYears: List<SchoolYearCache>,
) {
    fun toModel(): MasterData = MasterData(
        personId = personId,
        displayName = displayName,
        schoolName = schoolName,
        timeGrid = timeGrid.map { it.toModel() },
        holidays = holidays.map { it.toModel() },
        schoolYears = schoolYears.map { it.toModel() },
    )

    companion object {
        fun fromModel(m: MasterData): MasterDataCache = MasterDataCache(
            personId = m.personId,
            displayName = m.displayName,
            schoolName = m.schoolName,
            timeGrid = m.timeGrid.map { TimeUnitCache.fromModel(it) },
            holidays = m.holidays.map { HolidayCache.fromModel(it) },
            schoolYears = m.schoolYears.map { SchoolYearCache.fromModel(it) },
        )
    }
}

@Serializable
data class AbsenceCache(
    val id: Long,
    val start: String,
    val end: String,
    val reason: String,
    val excuseStatus: String?,
    val text: String = "",
) {
    fun toModel(): Absence = Absence(
        id = id,
        start = LocalDateTime.parse(start),
        end = LocalDateTime.parse(end),
        reason = reason,
        excuseStatus = excuseStatus,
        text = text,
    )

    companion object {
        fun fromModel(a: Absence): AbsenceCache = AbsenceCache(
            id = a.id,
            start = a.start.toString(),
            end = a.end.toString(),
            reason = a.reason,
            excuseStatus = a.excuseStatus,
            text = a.text,
        )
    }
}

@Serializable
data class CacheMeta(
    val lastSync: Long? = null,
    val yearFetchedAt: Long? = null,
    val absencesFetchedAt: Long? = null,
    val masterFetchedAt: Long? = null,
    val weeksFetchedAt: Map<String, Long> = emptyMap(),
)
