package io.github.omeg4dev.greatuntis.data

import io.github.omeg4dev.greatuntis.api.ApiHoliday
import io.github.omeg4dev.greatuntis.api.ApiHolidayType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant
import java.time.LocalDate

class HolidayStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun roundTripHolidays() {
        val store = HolidayStore(tempFolder.root)
        val holidays = listOf(
            ApiHoliday(
                name = "Herbstferien",
                start = LocalDate.of(2026, 10, 12),
                end = LocalDate.of(2026, 10, 24),
                type = ApiHolidayType.SCHOOL,
            ),
            ApiHoliday(
                name = "Tag der Deutschen Einheit",
                start = LocalDate.of(2026, 10, 3),
                end = LocalDate.of(2026, 10, 3),
                type = ApiHolidayType.PUBLIC,
            ),
        )
        val fetchedAt = Instant.parse("2026-10-01T12:00:00Z")
        store.save(holidays, "NW", fetchedAt)

        val loaded = store.load()
        assertEquals("NW", loaded.state)
        assertEquals(fetchedAt, loaded.fetchedAt)
        assertEquals(holidays, loaded.holidays)
    }

    @Test
    fun corruptFile_returnsEmpty() {
        val store = HolidayStore(tempFolder.root)
        val file = File(tempFolder.root, "holidays.json")
        file.writeText("{ this is definitely not valid json }")

        val loaded = store.load()
        assertEquals(emptyList<ApiHoliday>(), loaded.holidays)
        assertEquals("", loaded.state)
        assertNull(loaded.fetchedAt)
    }

    @Test
    fun nonExistentFile_returnsEmpty() {
        val store = HolidayStore(tempFolder.root)
        val loaded = store.load()
        assertEquals(emptyList<ApiHoliday>(), loaded.holidays)
        assertEquals("", loaded.state)
        assertNull(loaded.fetchedAt)
    }

    @Test
    fun clear_deletesFile() {
        val store = HolidayStore(tempFolder.root)
        store.save(emptyList(), "NW", Instant.now())
        assertTrue(File(tempFolder.root, "holidays.json").exists())

        store.clear()
        assertFalse(File(tempFolder.root, "holidays.json").exists())
    }

    @Test
    fun refetchRules() {
        val now = Instant.parse("2026-11-08T12:00:00Z")

        // 1. Never fetched (fetchedAt == null) -> should fetch
        assertTrue(HolidayStore.shouldFetch(cachedState = "NW", cachedFetchedAt = null, currentState = "NW", now = now))

        // 2. State changed ("NW" -> "BY") -> should fetch even if fetched 1 hour ago
        val oneHourAgo = now.minusSeconds(3600)
        assertTrue(HolidayStore.shouldFetch(cachedState = "NW", cachedFetchedAt = oneHourAgo, currentState = "BY", now = now))

        // 3. Older than 7 days -> should fetch
        val eightDaysAgo = now.minusSeconds(8 * 24 * 3600)
        assertTrue(HolidayStore.shouldFetch(cachedState = "NW", cachedFetchedAt = eightDaysAgo, currentState = "NW", now = now))

        // 4. Fresh fetch (3 days ago, same state) -> should NOT fetch
        val threeDaysAgo = now.minusSeconds(3 * 24 * 3600)
        assertFalse(HolidayStore.shouldFetch(cachedState = "NW", cachedFetchedAt = threeDaysAgo, currentState = "NW", now = now))

        // 5. Exactly 7 days boundary (7 days + 1 second -> fetch, 7 days - 1 second -> no fetch)
        val sevenDaysMinus1s = now.minusSeconds(7 * 24 * 3600 - 1)
        assertFalse(HolidayStore.shouldFetch(cachedState = "NW", cachedFetchedAt = sevenDaysMinus1s, currentState = "NW", now = now))

        val sevenDaysPlus1s = now.minusSeconds(7 * 24 * 3600 + 1)
        assertTrue(HolidayStore.shouldFetch(cachedState = "NW", cachedFetchedAt = sevenDaysPlus1s, currentState = "NW", now = now))
    }
}
