package io.github.omeg4dev.greatuntis.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val UntisJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}

@Serializable
data class JsonRpcResponse<T>(
    val result: T? = null,
    val error: JsonRpcError? = null,
    val id: String? = null,
    val jsonrpc: String? = null,
)

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String = "",
)

@Serializable
data class UserDataResultDto(
    val userData: UserDataDto,
    val masterData: MasterDataDto,
)

@Serializable
data class UserDataDto(
    val elemType: String? = null,
    val elemId: Long,
    val displayName: String = "",
    val schoolName: String = "",
)

@Serializable
data class MasterDataDto(
    val timeGrid: TimeGridDto? = null,
    val holidays: List<HolidayDto> = emptyList(),
    val schoolyears: List<SchoolYearDto> = emptyList(),
)

@Serializable
data class TimeGridDto(
    val days: List<TimeGridDayDto> = emptyList(),
)

@Serializable
data class TimeGridDayDto(
    val units: List<TimeGridUnitDto> = emptyList(),
)

@Serializable
data class TimeGridUnitDto(
    val startTime: String,
    val endTime: String,
    val label: String? = null,
)

@Serializable
data class HolidayDto(
    val longName: String = "",
    val startDate: String,
    val endDate: String,
    val name: String? = null,
    val id: Long? = null,
)

@Serializable
data class SchoolYearDto(
    val id: Long,
    val name: String,
    val startDate: String,
    val endDate: String,
)

@Serializable
data class TimetableResponseDto(
    val days: List<TimetableDayDto> = emptyList(),
)

@Serializable
data class TimetableDayDto(
    val date: String,
    val status: String? = null,
    val gridEntries: List<GridEntryDto> = emptyList(),
)

@Serializable
data class GridEntryDto(
    val ids: List<Long> = emptyList(),
    val duration: DurationDto,
    val type: String? = null,
    val status: String? = null,
    val position1: List<SlotDto>? = null,
    val position2: List<SlotDto>? = null,
    val position3: List<SlotDto>? = null,
    val substitutionText: String? = null,
    val lessonInfo: String? = null,
    val lessonText: String? = null,
)

@Serializable
data class DurationDto(
    val start: String,
    val end: String,
)

@Serializable
data class SlotDto(
    val current: ElementDto? = null,
    val removed: ElementDto? = null,
)

@Serializable
data class ElementDto(
    val type: String? = null,
    val status: String? = null,
    val shortName: String? = null,
    val longName: String? = null,
)

@Serializable
data class AbsenceResponseDto(
    val data: AbsenceDataDto? = null,
)

@Serializable
data class AbsenceDataDto(
    val absences: List<AbsenceDto> = emptyList(),
)

@Serializable
data class AbsenceDto(
    val id: Long,
    val startDate: Int,
    val endDate: Int,
    val startTime: Int,
    val endTime: Int,
    val reason: String = "",
    val excuseStatus: String? = null,
    val text: String = "",
)

@Serializable
data class SchoolSearchResponseDto(
    val result: SchoolSearchResultDto? = null,
)

@Serializable
data class SchoolSearchResultDto(
    val schools: List<SchoolDto> = emptyList(),
)

@Serializable
data class SchoolDto(
    val displayName: String = "",
    val address: String = "",
    val loginName: String = "",
    val server: String = "",
    val schoolId: Long,
)
