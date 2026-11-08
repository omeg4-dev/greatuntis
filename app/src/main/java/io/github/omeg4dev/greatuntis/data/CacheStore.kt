package io.github.omeg4dev.greatuntis.data

import android.content.Context
import io.github.omeg4dev.greatuntis.model.Absence
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.MasterData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.UUID

class CacheStore(private val cacheDir: File) {
    constructor(context: Context) : this(File(context.filesDir, "cache"))

    private val masterFile = File(cacheDir, "master.json")
    private val yearFile = File(cacheDir, "year.json")
    private val windowFile = File(cacheDir, "window.json")
    private val weeksFile = File(cacheDir, "weeks.json")
    private val absencesFile = File(cacheDir, "absences.json")
    private val metaFile = File(cacheDir, "meta.json")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun writeAtomically(file: File, content: String) {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val tempFile = File(cacheDir, "${file.name}.${UUID.randomUUID()}.tmp")
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

    fun loadMaster(): MasterData? {
        if (!masterFile.exists()) return null
        return runCatching {
            json.decodeFromString<MasterDataCache>(masterFile.readText()).toModel()
        }.getOrNull()
    }

    fun saveMaster(master: MasterData) {
        val dto = MasterDataCache.fromModel(master)
        writeAtomically(masterFile, json.encodeToString(dto))
    }

    fun loadYear(): List<Day> {
        if (!yearFile.exists()) return emptyList()
        return runCatching {
            json.decodeFromString<List<DayCache>>(yearFile.readText()).map { it.toModel() }
        }.getOrDefault(emptyList())
    }

    fun saveYear(days: List<Day>) {
        val dtos = days.map { DayCache.fromModel(it) }
        writeAtomically(yearFile, json.encodeToString(dtos))
    }

    fun loadWindow(): List<Day> {
        if (!windowFile.exists()) return emptyList()
        return runCatching {
            json.decodeFromString<List<DayCache>>(windowFile.readText()).map { it.toModel() }
        }.getOrDefault(emptyList())
    }

    fun saveWindow(days: List<Day>) {
        val dtos = days.map { DayCache.fromModel(it) }
        writeAtomically(windowFile, json.encodeToString(dtos))
    }

    fun loadWeeks(): Map<LocalDate, Day> {
        if (!weeksFile.exists()) return emptyMap()
        return runCatching {
            json.decodeFromString<Map<String, DayCache>>(weeksFile.readText())
                .map { (dateStr, dayCache) -> LocalDate.parse(dateStr) to dayCache.toModel() }
                .toMap()
        }.getOrDefault(emptyMap())
    }

    fun saveWeeks(weeks: Map<LocalDate, Day>) {
        val dtos = weeks.mapKeys { it.key.toString() }.mapValues { DayCache.fromModel(it.value) }
        writeAtomically(weeksFile, json.encodeToString(dtos))
    }

    fun loadAbsences(): List<Absence> {
        if (!absencesFile.exists()) return emptyList()
        return runCatching {
            json.decodeFromString<List<AbsenceCache>>(absencesFile.readText()).map { it.toModel() }
        }.getOrDefault(emptyList())
    }

    fun saveAbsences(absences: List<Absence>) {
        val dtos = absences.map { AbsenceCache.fromModel(it) }
        writeAtomically(absencesFile, json.encodeToString(dtos))
    }

    fun loadMeta(): CacheMeta {
        if (!metaFile.exists()) return CacheMeta()
        return runCatching {
            json.decodeFromString<CacheMeta>(metaFile.readText())
        }.getOrDefault(CacheMeta())
    }

    fun saveMeta(meta: CacheMeta) {
        writeAtomically(metaFile, json.encodeToString(meta))
    }

    fun clear() {
        cacheDir.deleteRecursively()
    }
}
