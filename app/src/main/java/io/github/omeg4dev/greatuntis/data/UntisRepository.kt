package io.github.omeg4dev.greatuntis.data

import io.github.omeg4dev.greatuntis.model.Absence
import io.github.omeg4dev.greatuntis.model.Account
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.MasterData
import io.github.omeg4dev.greatuntis.model.SchoolInfo
import io.github.omeg4dev.greatuntis.model.Settings
import io.github.omeg4dev.greatuntis.model.SyncState
import io.github.omeg4dev.greatuntis.calendar.FreeCalendar
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.Instant

/** The single data source the UI talks to. Implementations: real (data/) and fake (ui previews/tests). */
interface UntisRepository {
    /** Null = not logged in → show onboarding. */
    val account: StateFlow<Account?>
    val masterData: StateFlow<MasterData?>
    /** All cached days of the current school year plus the visible window, sorted by date. */
    val days: StateFlow<List<Day>>
    /** Absences of the current school year. */
    val absences: StateFlow<List<Absence>>
    val settings: StateFlow<Settings>
    val syncState: StateFlow<SyncState>
    val lastSync: StateFlow<Instant?>
    /** Ferien/Feiertage/schulfreie Tage (WebUntis + OpenHolidays + timetable), rebuilt on every data change. */
    val freeCalendar: StateFlow<FreeCalendar>
    /** When the OpenHolidays list was last fetched successfully; null = never. */
    val holidaysUpdated: StateFlow<Instant?>

    /** Validates by logging in, then stores the account and starts a full sync. */
    suspend fun login(account: Account): Result<MasterData>
    suspend fun logout()
    /** Fetches today + next school day (+ absences, history if stale). */
    suspend fun refresh(): Result<Unit>
    /** Untis school search (no login needed). Empty list on no match. */
    suspend fun searchSchools(query: String): Result<List<SchoolInfo>>
    suspend fun updateSettings(transform: (Settings) -> Settings)
    /** Makes sure the Mon–Fri week starting [monday] is fetched and cached; cached data stays on failure. */
    suspend fun ensureWeek(monday: LocalDate): Result<Unit>
}
