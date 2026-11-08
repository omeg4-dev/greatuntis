package io.github.omeg4dev.greatuntis.data

import android.content.Context
import io.github.omeg4dev.greatuntis.api.ApiHoliday
import io.github.omeg4dev.greatuntis.api.ApiHolidayType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Serializable
data class ApiHolidayCache(
    val name: String,
    val start: String,
    val end: String,
    val type: String,
) {
    fun toModel(): ApiHoliday = ApiHoliday(
        name = name,
        start = LocalDate.parse(start),
        end = LocalDate.parse(end),
        type = ApiHolidayType.valueOf(type),
    )

    companion object {
        fun fromModel(h: ApiHoliday): ApiHolidayCache = ApiHolidayCache(
            name = h.name,
            start = h.start.toString(),
            end = h.end.toString(),
            type = h.type.name,
        )
    }
}

@Serializable
data class HolidayStoreCache(
    val state: String = "",
    val fetchedAt: Long? = null,
    val holidays: List<ApiHolidayCache> = emptyList(),
)

data class HolidayData(
    val holidays: List<ApiHoliday>,
    val state: String,
    val fetchedAt: Instant?,
)

class HolidayStore(private val filesDir: File) {
    constructor(context: Context) : this(context.filesDir)

    private val holidayFile = File(filesDir, "holidays.json")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun writeAtomically(file: File, content: String) {
        if (!filesDir.exists()) {
            filesDir.mkdirs()
        }
        val tempFile = File(filesDir, "${file.name}.${UUID.randomUUID()}.tmp")
        try {
            FileOutputStream(tempFile).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
                fos.fd.sync()
            }
            if (!tempFile.renameTo(file)) {
                file.delete()
                if (!tempFile.renameTo(file)) {
                    tempFile.delete()
                }
            }
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }

    fun load(): HolidayData {
        if (!holidayFile.exists()) return HolidayData(emptyList(), "", null)
        return runCatching {
            val cache = json.decodeFromString<HolidayStoreCache>(holidayFile.readText())
            HolidayData(
                holidays = cache.holidays.map { it.toModel() },
                state = cache.state,
                fetchedAt = cache.fetchedAt?.let { Instant.ofEpochMilli(it) },
            )
        }.getOrDefault(HolidayData(emptyList(), "", null))
    }

    fun save(holidays: List<ApiHoliday>, state: String, fetchedAt: Instant) {
        val cache = HolidayStoreCache(
            state = state,
            fetchedAt = fetchedAt.toEpochMilli(),
            holidays = holidays.map { ApiHolidayCache.fromModel(it) },
        )
        writeAtomically(holidayFile, json.encodeToString(cache))
    }

    fun clear() {
        holidayFile.delete()
    }

    companion object {
        fun shouldFetch(
            cachedState: String,
            cachedFetchedAt: Instant?,
            currentState: String,
            now: Instant = Instant.now(),
        ): Boolean {
            if (cachedFetchedAt == null) return true
            if (cachedState != currentState) return true
            val ageMs = java.time.Duration.between(cachedFetchedAt, now).toMillis()
            return ageMs > 7 * 24 * 3600 * 1000L
        }
    }
}
