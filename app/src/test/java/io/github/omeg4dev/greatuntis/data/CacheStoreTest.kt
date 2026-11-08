package io.github.omeg4dev.greatuntis.data

import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.DayStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

class CacheStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun roundTripWeeks() {
        val store = CacheStore(tempFolder.root)
        val d1 = LocalDate.of(2026, 11, 2)
        val d2 = LocalDate.of(2026, 11, 3)
        val weeks = mapOf(
            d1 to Day(date = d1, status = DayStatus.REGULAR, lessons = emptyList()),
            d2 to Day(date = d2, status = DayStatus.NO_DATA, lessons = emptyList()),
        )

        store.saveWeeks(weeks)
        val loaded = store.loadWeeks()

        assertEquals(weeks, loaded)
    }

    @Test
    fun corruptCacheFiles_treatAsEmptyNeverCrash() {
        val store = CacheStore(tempFolder.root)
        val badJson = "{ invalid json content ... @"

        // Corrupt master.json
        File(tempFolder.root, "master.json").writeText(badJson)
        assertNull("Corrupt master should return null", store.loadMaster())

        // Corrupt year.json
        File(tempFolder.root, "year.json").writeText(badJson)
        assertEquals("Corrupt year should return empty list", emptyList<Day>(), store.loadYear())

        // Corrupt window.json
        File(tempFolder.root, "window.json").writeText(badJson)
        assertEquals("Corrupt window should return empty list", emptyList<Day>(), store.loadWindow())

        // Corrupt weeks.json
        File(tempFolder.root, "weeks.json").writeText(badJson)
        assertEquals("Corrupt weeks should return empty map", emptyMap<LocalDate, Day>(), store.loadWeeks())

        // Corrupt absences.json
        File(tempFolder.root, "absences.json").writeText(badJson)
        assertEquals("Corrupt absences should return empty list", emptyList<Any>(), store.loadAbsences())

        // Corrupt meta.json
        File(tempFolder.root, "meta.json").writeText(badJson)
        assertEquals("Corrupt meta should return default CacheMeta", CacheMeta(), store.loadMeta())
    }

    @Test
    fun clear_removesCacheDir() {
        val store = CacheStore(tempFolder.root)
        store.saveMeta(CacheMeta(lastSync = 12345L))
        assertTrue(tempFolder.root.exists())

        store.clear()
        // clear deletes cacheDir
        val metaFile = File(tempFolder.root, "meta.json")
        assertTrue(!metaFile.exists())
    }
}
