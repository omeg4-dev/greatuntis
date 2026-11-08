package io.github.omeg4dev.greatuntis.api

import io.github.omeg4dev.greatuntis.model.Absence
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.DayStatus
import io.github.omeg4dev.greatuntis.model.Element
import io.github.omeg4dev.greatuntis.model.ElementStatus
import io.github.omeg4dev.greatuntis.model.Holiday
import io.github.omeg4dev.greatuntis.model.Lesson
import io.github.omeg4dev.greatuntis.model.LessonStatus
import io.github.omeg4dev.greatuntis.model.MasterData
import io.github.omeg4dev.greatuntis.model.SchoolInfo
import io.github.omeg4dev.greatuntis.model.SchoolYear
import io.github.omeg4dev.greatuntis.model.Slot
import io.github.omeg4dev.greatuntis.model.TimeUnit
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

fun toMasterData(userData: UserDataResultDto): MasterData {
    val units = userData.masterData.timeGrid?.days?.firstOrNull()?.units?.mapIndexed { index, unit ->
        TimeUnit(
            index = index + 1,
            start = LocalTime.parse(unit.startTime.removePrefix("T")),
            end = LocalTime.parse(unit.endTime.removePrefix("T")),
        )
    } ?: emptyList()

    val holidays = userData.masterData.holidays.map {
        Holiday(
            name = it.longName,
            start = LocalDate.parse(it.startDate),
            end = LocalDate.parse(it.endDate),
        )
    }

    val schoolYears = userData.masterData.schoolyears.map {
        SchoolYear(
            id = it.id,
            name = it.name,
            start = LocalDate.parse(it.startDate),
            end = LocalDate.parse(it.endDate),
        )
    }

    return MasterData(
        personId = userData.userData.elemId,
        displayName = userData.userData.displayName,
        schoolName = userData.userData.schoolName,
        timeGrid = units,
        holidays = holidays,
        schoolYears = schoolYears,
    )
}

fun toDays(timetableResponse: TimetableResponseDto): List<Day> {
    return timetableResponse.days.map { dayDto ->
        val dayStatus = when (dayDto.status) {
            "REGULAR" -> DayStatus.REGULAR
            "NO_DATA" -> DayStatus.NO_DATA
            "NOT_ALLOWED" -> DayStatus.NOT_ALLOWED
            else -> DayStatus.OTHER
        }

        val lessons = dayDto.gridEntries.map { entry ->
            val startDateTime = LocalDateTime.parse(entry.duration.start)
            val endDateTime = LocalDateTime.parse(entry.duration.end)

            val lessonStatus = when (entry.status) {
                "REGULAR" -> LessonStatus.REGULAR
                "CHANGED" -> LessonStatus.CHANGED
                "CANCELLED" -> LessonStatus.CANCELLED
                "ADDITIONAL" -> LessonStatus.ADDITIONAL
                else -> LessonStatus.OTHER
            }

            Lesson(
                date = startDateTime.toLocalDate(),
                start = startDateTime.toLocalTime(),
                end = endDateTime.toLocalTime(),
                periodIds = entry.ids,
                type = entry.type ?: "",
                status = lessonStatus,
                subjects = entry.position1?.map { toSlot(it) } ?: emptyList(),
                teachers = entry.position2?.map { toSlot(it) } ?: emptyList(),
                rooms = entry.position3?.map { toSlot(it) } ?: emptyList(),
                substitutionText = entry.substitutionText?.trim()?.takeIf { it.isNotEmpty() },
                lessonInfo = entry.lessonInfo?.trim()?.takeIf { it.isNotEmpty() },
                lessonText = entry.lessonText?.trim()?.takeIf { it.isNotEmpty() },
                predicted = false,
            )
        }

        Day(
            date = LocalDate.parse(dayDto.date),
            status = dayStatus,
            lessons = lessons,
        )
    }
}

private fun toSlot(dto: SlotDto): Slot {
    return Slot(
        current = dto.current?.let { toElement(it) },
        removed = dto.removed?.let { toElement(it) },
    )
}

private fun toElement(dto: ElementDto): Element {
    val status = when (dto.status) {
        "REGULAR" -> ElementStatus.REGULAR
        "ADDED" -> ElementStatus.ADDED
        "REMOVED" -> ElementStatus.REMOVED
        else -> ElementStatus.OTHER
    }
    return Element(
        shortName = dto.shortName ?: "",
        longName = dto.longName ?: "",
        status = status,
        kind = dto.type ?: "",
    )
}

fun toAbsences(absenceResponse: AbsenceResponseDto): List<Absence> {
    return absenceResponse.data?.absences?.map { toAbsence(it) } ?: emptyList()
}

fun toAbsences(absences: List<AbsenceDto>): List<Absence> {
    return absences.map { toAbsence(it) }
}

private fun toAbsence(dto: AbsenceDto): Absence {
    return Absence(
        id = dto.id,
        start = parseDateTime(dto.startDate, dto.startTime),
        end = parseDateTime(dto.endDate, dto.endTime),
        reason = dto.reason,
        excuseStatus = dto.excuseStatus,
        text = dto.text,
    )
}

private fun parseDateTime(date: Int, time: Int): LocalDateTime {
    val year = date / 10000
    val month = (date % 10000) / 100
    val day = date % 100
    val hour = time / 100
    val minute = time % 100
    return LocalDateTime.of(year, month, day, hour, minute)
}

fun toSchools(searchResponse: SchoolSearchResponseDto): List<SchoolInfo> {
    return searchResponse.result?.schools?.map { toSchool(it) } ?: emptyList()
}

fun toSchools(searchResult: SchoolSearchResultDto): List<SchoolInfo> {
    return searchResult.schools.map { toSchool(it) }
}

private fun toSchool(dto: SchoolDto): SchoolInfo {
    return SchoolInfo(
        displayName = dto.displayName,
        address = dto.address,
        loginName = dto.loginName,
        host = dto.server,
        schoolNumber = dto.schoolId.toString(),
    )
}
