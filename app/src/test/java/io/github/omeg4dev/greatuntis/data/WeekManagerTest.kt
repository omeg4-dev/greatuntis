package io.github.omeg4dev.greatuntis.data

import io.github.omeg4dev.greatuntis.api.UntisException
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.DayStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class WeekManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun makeDay(date: LocalDate, status: DayStatus = DayStatus.REGULAR): Day {
        return Day(date = date, status = status, lessons = emptyList())
    }

    @Test
    fun weekMergePrecedence_windowOverWeeksOverYear() {
        val date1 = LocalDate.of(2026, 11, 2)
        val date2 = LocalDate.of(2026, 11, 3)
        val date3 = LocalDate.of(2026, 11, 4)
        val date4 = LocalDate.of(2026, 11, 5)

        val yearDay1 = makeDay(date1, DayStatus.NO_DATA)
        val yearDay2 = makeDay(date2, DayStatus.NO_DATA)
        val yearDay3 = makeDay(date3, DayStatus.NO_DATA)
        val yearDay4 = makeDay(date4, DayStatus.REGULAR)

        val weeksDay1 = makeDay(date1, DayStatus.NOT_ALLOWED)
        val weeksDay2 = makeDay(date2, DayStatus.REGULAR)
        val weeksDay3 = makeDay(date3, DayStatus.REGULAR)

        val windowDay1 = makeDay(date1, DayStatus.OTHER)
        val windowDay2 = makeDay(date2, DayStatus.OTHER)

        val yearList = listOf(yearDay4, yearDay3, yearDay2, yearDay1)
        val weeksMap = mapOf(date1 to weeksDay1, date2 to weeksDay2, date3 to weeksDay3)
        val windowList = listOf(windowDay2, windowDay1)

        val merged = WeekUtils.mergeDays(
            year = yearList,
            weeks = weeksMap,
            window = windowList,
        )

        assertEquals(4, merged.size)
        // Check sorting
        assertEquals(listOf(date1, date2, date3, date4), merged.map { it.date })

        // date1: present in window, weeks, year -> window wins
        assertEquals(DayStatus.OTHER, merged[0].status)

        // date2: present in window, weeks, year -> window wins
        assertEquals(DayStatus.OTHER, merged[1].status)

        // date3: present in weeks, year -> weeks wins
        assertEquals(DayStatus.REGULAR, merged[2].status)

        // date4: present only in year -> year wins
        assertEquals(DayStatus.REGULAR, merged[3].status)
    }

    @Test
    fun max30WeeksEviction() {
        val baseMonday = LocalDate.of(2026, 1, 5) // Monday
        val weekFetchedAt = mutableMapOf<LocalDate, Long>()

        var weeksMap = emptyMap<LocalDate, Day>()

        // Add 31 weeks sequentially
        for (i in 0..30) {
            val monday = baseMonday.plusWeeks(i.toLong())
            val timestamp = 1000L + i * 100L
            weekFetchedAt[monday] = timestamp

            val weekDays = (0L..4L).map { makeDay(monday.plusDays(it)) }
            weeksMap = WeekUtils.mergeWeeks(
                existing = weeksMap,
                newDays = weekDays,
                maxWeeks = 30,
                weekFetchedAt = weekFetchedAt,
            )
        }

        // Must keep at most 30 weeks
        val distinctMondays = weeksMap.keys.map {
            it.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        }.distinct()

        assertEquals(30, distinctMondays.size)
        // 30 weeks * 5 days = 150 days
        assertEquals(150, weeksMap.size)

        // The oldest week (baseMonday, i=0, timestamp 1000L) must have been evicted
        assertFalse(weeksMap.containsKey(baseMonday))
        assertFalse(weeksMap.containsKey(baseMonday.plusDays(4)))

        // The newest week (i=30) must be present
        val newestMonday = baseMonday.plusWeeks(30)
        assertTrue(weeksMap.containsKey(newestMonday))
        assertTrue(weeksMap.containsKey(newestMonday.plusDays(4)))

        // The second week (i=1) must still be present
        val secondMonday = baseMonday.plusWeeks(1)
        assertTrue(weeksMap.containsKey(secondMonday))
    }

    @Test
    fun ensureWeek_secondCallWithin10MinDoesNoFetch() = runBlocking {
        val cacheStore = CacheStore(tempFolder.root)
        val fetchCount = AtomicInteger(0)
        var currentTime = 1_000_000L

        val monday = LocalDate.of(2026, 11, 16)
        val fakeDays = (0L..4L).map { makeDay(monday.plusDays(it)) }

        var cachedDays = emptyList<Day>()

        val manager = WeekManager(
            cacheStore = cacheStore,
            fetchTimetable = { _, _ ->
                fetchCount.incrementAndGet()
                fakeDays
            },
            onDaysUpdated = { weeks ->
                cachedDays = weeks.values.toList()
            },
            clock = { currentTime },
        )

        // First call -> does network fetch
        val result1 = manager.ensureWeek(monday, cachedDays, today = LocalDate.of(2026, 11, 9))
        assertTrue(result1.isSuccess)
        assertEquals(1, fetchCount.get())
        assertEquals(5, cachedDays.size)

        // Second call 5 minutes later (< 10 min) -> does no network fetch
        currentTime += 5 * 60 * 1000L
        val result2 = manager.ensureWeek(monday, cachedDays, today = LocalDate.of(2026, 11, 9))
        assertTrue(result2.isSuccess)
        assertEquals("Fake fetcher should not be called again within 10 min", 1, fetchCount.get())

        // Third call 11 minutes after the original fetch (6 min after second call) -> triggers fetch
        currentTime += 6 * 60 * 1000L
        val result3 = manager.ensureWeek(monday, cachedDays, today = LocalDate.of(2026, 11, 9))
        assertTrue(result3.isSuccess)
        assertEquals("Fake fetcher should be called when older than 10 min", 2, fetchCount.get())
    }

    @Test
    fun ensureWeek_twoConcurrentCalls_singleFetch() = runBlocking {
        val cacheStore = CacheStore(tempFolder.root)
        val fetchCount = AtomicInteger(0)
        val monday = LocalDate.of(2026, 11, 23)
        val fakeDays = (0L..4L).map { makeDay(monday.plusDays(it)) }

        var cachedDays = emptyList<Day>()

        val manager = WeekManager(
            cacheStore = cacheStore,
            fetchTimetable = { _, _ ->
                fetchCount.incrementAndGet()
                delay(100) // simulate in-flight network request
                fakeDays
            },
            onDaysUpdated = { weeks ->
                cachedDays = weeks.values.toList()
            },
        )

        // Launch two concurrent calls
        val deferred1 = async { manager.ensureWeek(monday, cachedDays, today = LocalDate.of(2026, 11, 9)) }
        val deferred2 = async { manager.ensureWeek(monday, cachedDays, today = LocalDate.of(2026, 11, 9)) }

        val res1 = deferred1.await()
        val res2 = deferred2.await()

        assertTrue(res1.isSuccess)
        assertTrue(res2.isSuccess)
        assertEquals("Only one network fetch should be performed for concurrent calls", 1, fetchCount.get())
        assertEquals(5, cachedDays.size)
    }

    @Test
    fun ensureWeek_pastWeekFullyBeforeYesterday_fetchedOnceDoesNoFetch() = runBlocking {
        val cacheStore = CacheStore(tempFolder.root)
        val fetchCount = AtomicInteger(0)
        var currentTime = 1_000_000L

        val pastMonday = LocalDate.of(2026, 10, 5) // Mon-Fri: Oct 5 to Oct 9
        val today = LocalDate.of(2026, 10, 20) // yesterday = Oct 19; Oct 9 is before Oct 19
        val fakeDays = (0L..4L).map { makeDay(pastMonday.plusDays(it)) }

        var cachedDays = emptyList<Day>()

        val manager = WeekManager(
            cacheStore = cacheStore,
            fetchTimetable = { _, _ ->
                fetchCount.incrementAndGet()
                fakeDays
            },
            onDaysUpdated = { weeks ->
                cachedDays = weeks.values.toList()
            },
            clock = { currentTime },
        )

        // First call -> fetches
        val res1 = manager.ensureWeek(pastMonday, cachedDays, today = today)
        assertTrue(res1.isSuccess)
        assertEquals(1, fetchCount.get())

        // Advance time by 2 days (> 10 min)
        currentTime += 2 * 24 * 3600 * 1000L

        // Second call for past week -> does no network fetch because it lies fully in the past before yesterday and was fetched once
        val res2 = manager.ensureWeek(pastMonday, cachedDays, today = today)
        assertTrue(res2.isSuccess)
        assertEquals("Past week fetched once should not be refetched even after 10 min", 1, fetchCount.get())
    }

    @Test
    fun ensureWeek_networkError_returnsFailureAndKeepsCache() = runBlocking {
        val cacheStore = CacheStore(tempFolder.root)
        val monday = LocalDate.of(2026, 11, 30)
        val authErrorOccurred = AtomicBoolean(false)

        val manager = WeekManager(
            cacheStore = cacheStore,
            fetchTimetable = { _, _ ->
                throw IOException("Network unreachable")
            },
            onDaysUpdated = {},
            onAuthError = { authErrorOccurred.set(true) },
        )

        val result = manager.ensureWeek(monday, emptyList(), today = LocalDate.of(2026, 11, 9))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
        assertFalse(authErrorOccurred.get())
    }

    @Test
    fun ensureWeek_authError_triggersCallback() = runBlocking {
        val cacheStore = CacheStore(tempFolder.root)
        val monday = LocalDate.of(2026, 11, 30)
        val authErrorOccurred = AtomicBoolean(false)

        val manager = WeekManager(
            cacheStore = cacheStore,
            fetchTimetable = { _, _ ->
                throw UntisException.BadCredentials("Invalid token")
            },
            onDaysUpdated = {},
            onAuthError = { authErrorOccurred.set(true) },
        )

        val result = manager.ensureWeek(monday, emptyList(), today = LocalDate.of(2026, 11, 9))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UntisException.BadCredentials)
        assertTrue(authErrorOccurred.get())
    }
}
