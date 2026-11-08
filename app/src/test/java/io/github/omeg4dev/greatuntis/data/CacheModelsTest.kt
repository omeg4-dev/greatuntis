package io.github.omeg4dev.greatuntis.data

import io.github.omeg4dev.greatuntis.Fixtures
import io.github.omeg4dev.greatuntis.api.AbsenceResponseDto
import io.github.omeg4dev.greatuntis.api.TimetableResponseDto
import io.github.omeg4dev.greatuntis.api.UntisJson
import io.github.omeg4dev.greatuntis.api.UserDataResultDto
import io.github.omeg4dev.greatuntis.api.toAbsences
import io.github.omeg4dev.greatuntis.api.toDays
import io.github.omeg4dev.greatuntis.api.toMasterData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class CacheModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun roundTripTimetable2026() {
        val fixtureJson = Fixtures.load("timetable_2026.json")
        val dto = UntisJson.decodeFromString<TimetableResponseDto>(fixtureJson)
        val originalDays = toDays(dto)

        val cachedDtos = originalDays.map { DayCache.fromModel(it) }
        val serialized = json.encodeToString(cachedDtos)
        val deserialized = json.decodeFromString<List<DayCache>>(serialized)
        val restoredDays = deserialized.map { it.toModel() }

        assertEquals(originalDays, restoredDays)
    }

    @Test
    fun roundTripAbsences2026() {
        val fixtureJson = Fixtures.load("absences_2026.json")
        val dto = UntisJson.decodeFromString<AbsenceResponseDto>(fixtureJson)
        val originalAbsences = toAbsences(dto)

        val cachedDtos = originalAbsences.map { AbsenceCache.fromModel(it) }
        val serialized = json.encodeToString(cachedDtos)
        val deserialized = json.decodeFromString<List<AbsenceCache>>(serialized)
        val restoredAbsences = deserialized.map { it.toModel() }

        assertEquals(originalAbsences, restoredAbsences)
    }

    @Test
    fun roundTripMasterData() {
        val fixtureJson = Fixtures.load("userdata.json")
        val dto = UntisJson.decodeFromString<UserDataResultDto>(fixtureJson)
        val originalMasterData = toMasterData(dto)

        val cachedDto = MasterDataCache.fromModel(originalMasterData)
        val serialized = json.encodeToString(cachedDto)
        val deserialized = json.decodeFromString<MasterDataCache>(serialized)
        val restoredMasterData = deserialized.toModel()

        assertEquals(originalMasterData, restoredMasterData)
    }

    /** Old cache JSON without "kind" field should decode with kind = "" (backward compatibility). */
    @Test
    fun elementCacheBackwardCompatibility_noKindField() {
        // JSON written by an old version of the app that did not have the "kind" field
        val oldJson = """{"shortName":"SP-LK2","longName":"Sport LK","status":"REGULAR"}"""
        val decoded = json.decodeFromString<ElementCache>(oldJson)
        assertEquals("", decoded.kind)
        val model = decoded.toModel()
        assertEquals("", model.kind)
    }

    @Test
    fun cacheMetaBackwardCompatibility_noNewFields() {
        val oldJson = """{"lastSync":123456,"yearFetchedAt":234567,"absencesFetchedAt":345678}"""
        val decoded = json.decodeFromString<CacheMeta>(oldJson)
        assertEquals(123456L, decoded.lastSync)
        assertEquals(234567L, decoded.yearFetchedAt)
        assertEquals(345678L, decoded.absencesFetchedAt)
        assertEquals(null, decoded.masterFetchedAt)
        assertEquals(emptyMap<String, Long>(), decoded.weeksFetchedAt)
    }
}
