package io.github.omeg4dev.greatuntis.data

import io.github.omeg4dev.greatuntis.api.UntisException
import io.github.omeg4dev.greatuntis.model.Day
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object WeekUtils {

    fun mergeDays(
        year: List<Day>,
        weeks: Map<LocalDate, Day>,
        window: List<Day>,
    ): List<Day> {
        val windowMap = window.associateBy { it.date }
        val yearMap = year.associateBy { it.date }
        val allDates = (yearMap.keys + weeks.keys + windowMap.keys).toSet()
        return allDates.mapNotNull { date ->
            windowMap[date] ?: weeks[date] ?: yearMap[date]
        }.sortedBy { it.date }
    }

    fun mergeWeeks(
        existing: Map<LocalDate, Day>,
        newDays: List<Day>,
        maxWeeks: Int = 30,
        weekFetchedAt: Map<LocalDate, Long> = emptyMap(),
    ): Map<LocalDate, Day> {
        val combined = existing.toMutableMap()
        for (day in newDays) {
            combined[day.date] = day
        }

        val weeksByMonday = combined.values.groupBy {
            it.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        }

        if (weeksByMonday.size <= maxWeeks) {
            return combined
        }

        val sortedMondays = weeksByMonday.keys.sortedWith(
            compareBy({ weekFetchedAt[it] ?: 0L }, { it })
        )
        val toDrop = sortedMondays.take(weeksByMonday.size - maxWeeks).toSet()

        return combined.filterValues {
            it.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) !in toDrop
        }
    }
}

class WeekManager(
    private val cacheStore: CacheStore,
    private val fetchTimetable: suspend (LocalDate, LocalDate) -> List<Day>,
    private val onDaysUpdated: (weeks: Map<LocalDate, Day>) -> Unit,
    private val onAuthError: () -> Unit = {},
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private val inFlight = mutableMapOf<LocalDate, CompletableDeferred<Result<Unit>>>()

    suspend fun ensureWeek(
        monday: LocalDate,
        cachedDays: List<Day>,
        today: LocalDate = LocalDate.now(),
    ): Result<Unit> {
        val weekDates = (0L..4L).map { monday.plusDays(it) }
        val cachedDates = cachedDays.map { it.date }.toSet()
        val allPresent = weekDates.all { it in cachedDates }

        val meta = cacheStore.loadMeta()
        val lastFetchedMs = meta.weeksFetchedAt[monday.toString()]
        val friday = monday.plusDays(4)
        val yesterday = today.minusDays(1)
        val fullyInPast = friday.isBefore(yesterday)
        val nowMs = clock()

        val fetchedOnce = lastFetchedMs != null || (fullyInPast && meta.yearFetchedAt != null && allPresent)
        val fetchedRecent = lastFetchedMs != null && (nowMs - lastFetchedMs) < 10 * 60 * 1000L

        if (allPresent && (fetchedRecent || (fullyInPast && fetchedOnce))) {
            return Result.success(Unit)
        }

        val myDeferred: CompletableDeferred<Result<Unit>>
        val isLeader: Boolean

        mutex.withLock {
            val existing = inFlight[monday]
            if (existing != null) {
                myDeferred = existing
                isLeader = false
            } else {
                myDeferred = CompletableDeferred()
                inFlight[monday] = myDeferred
                isLeader = true
            }
        }

        if (!isLeader) {
            return myDeferred.await()
        }

        val result: Result<Unit> = try {
            val fetchedDays = fetchTimetable(monday, monday.plusDays(4))
            val currentWeeks = cacheStore.loadWeeks()

            val currentMeta = cacheStore.loadMeta()
            val fetchTime = clock()
            val updatedWeeksFetchedAt = currentMeta.weeksFetchedAt.toMutableMap()
            updatedWeeksFetchedAt[monday.toString()] = fetchTime

            val fetchedAtLocalDateMap = updatedWeeksFetchedAt.mapKeys { LocalDate.parse(it.key) }
            val merged = WeekUtils.mergeWeeks(
                existing = currentWeeks,
                newDays = fetchedDays,
                maxWeeks = 30,
                weekFetchedAt = fetchedAtLocalDateMap,
            )

            cacheStore.saveWeeks(merged)
            cacheStore.saveMeta(currentMeta.copy(weeksFetchedAt = updatedWeeksFetchedAt))
            onDaysUpdated(merged)

            val res = Result.success(Unit)
            myDeferred.complete(res)
            res
        } catch (e: Throwable) {
            if (e is UntisException.BadCredentials) {
                onAuthError()
            }
            val res = Result.failure<Unit>(e)
            myDeferred.complete(res)
            if (e is CancellationException) throw e
            res
        } finally {
            mutex.withLock {
                inFlight.remove(monday)
            }
        }

        return result
    }
}
