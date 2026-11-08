package io.github.omeg4dev.greatuntis.data

import android.content.Context
import android.util.Log
import io.github.omeg4dev.greatuntis.BuildConfig
import io.github.omeg4dev.greatuntis.api.ApiHoliday
import io.github.omeg4dev.greatuntis.api.HolidayClient
import io.github.omeg4dev.greatuntis.api.UntisClient
import io.github.omeg4dev.greatuntis.api.UntisException
import io.github.omeg4dev.greatuntis.calendar.FreeCalendar
import io.github.omeg4dev.greatuntis.model.Absence
import io.github.omeg4dev.greatuntis.model.Account
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.MasterData
import io.github.omeg4dev.greatuntis.model.SchoolInfo
import io.github.omeg4dev.greatuntis.model.Settings
import io.github.omeg4dev.greatuntis.model.SyncState
import io.github.omeg4dev.greatuntis.watch.WatchScheduler
import io.github.omeg4dev.greatuntis.watch.WatchState
import io.github.omeg4dev.greatuntis.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.time.Instant
import java.time.LocalDate

class RealUntisRepository(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : UntisRepository {

    private val accountStore = AccountStore(context)
    private val settingsStore = SettingsStore(context)
    private val cacheStore = CacheStore(context)
    private val holidayStore = HolidayStore(context)
    private val holidayClient = HolidayClient(okHttpClient)

    private val _account = MutableStateFlow<Account?>(null)
    override val account: StateFlow<Account?> = _account.asStateFlow()

    private val _masterData = MutableStateFlow<MasterData?>(null)
    override val masterData: StateFlow<MasterData?> = _masterData.asStateFlow()

    private val _yearDays = MutableStateFlow<List<Day>>(emptyList())
    private val _windowDays = MutableStateFlow<List<Day>>(emptyList())
    private val _weeksDays = MutableStateFlow<Map<LocalDate, Day>>(emptyMap())

    private val _days = MutableStateFlow<List<Day>>(emptyList())
    override val days: StateFlow<List<Day>> = _days.asStateFlow()

    private val _absences = MutableStateFlow<List<Absence>>(emptyList())
    override val absences: StateFlow<List<Absence>> = _absences.asStateFlow()

    private val _settings = MutableStateFlow(Settings())
    override val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSync = MutableStateFlow<Instant?>(null)
    override val lastSync: StateFlow<Instant?> = _lastSync.asStateFlow()

    private val _apiHolidays = MutableStateFlow<List<ApiHoliday>>(emptyList())

    private val _holidaysUpdated = MutableStateFlow<Instant?>(null)
    override val holidaysUpdated: StateFlow<Instant?> = _holidaysUpdated.asStateFlow()

    private val _freeCalendar = MutableStateFlow(FreeCalendar.EMPTY)
    override val freeCalendar: StateFlow<FreeCalendar> = _freeCalendar.asStateFlow()

    @Volatile
    private var untisClient: UntisClient? = null

    private val weekManager = WeekManager(
        cacheStore = cacheStore,
        fetchTimetable = { start, end ->
            val acc = _account.value ?: throw IllegalStateException("Not logged in")
            val client = untisClient ?: UntisClient(acc, okHttpClient, baseUrl = getBaseUrl(acc)).also { untisClient = it }
            client.timetable(start, end)
        },
        onDaysUpdated = { weeks ->
            _weeksDays.value = weeks
            _days.value = WeekUtils.mergeDays(_yearDays.value, _weeksDays.value, _windowDays.value)
        },
        onAuthError = {
            _syncState.value = SyncState.AUTH_ERROR
        },
    )

    init {
        scope.launch {
            settingsStore.settingsFlow.collect { s ->
                _settings.value = s
            }
        }
        scope.launch {
            combine(_days, _masterData, _apiHolidays, _settings) { days, masterData, apiHolidays, settings ->
                withContext(Dispatchers.Default) {
                    FreeCalendarBuilder.build(
                        masterData = masterData,
                        days = days,
                        apiHolidays = apiHolidays,
                        foyerPrefix = settings.foyerPrefix,
                    )
                }
            }.collect { cal ->
                _freeCalendar.value = cal
            }
        }
        scope.launch {
            val loadedAccount = accountStore.load()
            _account.value = loadedAccount
            if (loadedAccount != null) {
                untisClient = UntisClient(loadedAccount, okHttpClient, baseUrl = getBaseUrl(loadedAccount))
            }

            val loadedMaster = cacheStore.loadMaster()
            _masterData.value = loadedMaster

            val loadedYear = cacheStore.loadYear()
            _yearDays.value = loadedYear

            val loadedWindow = cacheStore.loadWindow()
            _windowDays.value = loadedWindow

            val loadedWeeks = cacheStore.loadWeeks()
            _weeksDays.value = loadedWeeks

            _days.value = WeekUtils.mergeDays(loadedYear, loadedWeeks, loadedWindow)

            val loadedAbsences = cacheStore.loadAbsences()
            _absences.value = loadedAbsences

            val meta = cacheStore.loadMeta()
            _lastSync.value = meta.lastSync?.let { Instant.ofEpochMilli(it) }

            val loadedHolidays = holidayStore.load()
            _apiHolidays.value = loadedHolidays.holidays
            _holidaysUpdated.value = loadedHolidays.fetchedAt

            if (loadedAccount != null) {
                refresh()
                scope.launch {
                    checkAndFetchHolidays()
                }
            }
        }
    }

    private fun getBaseUrl(account: Account): HttpUrl {
        return if (BuildConfig.DEBUG && (account.host.startsWith("http://") || account.host.startsWith("https://"))) {
            account.host.toHttpUrl()
        } else {
            "https://${account.host}".toHttpUrl()
        }
    }

    override suspend fun login(account: Account): Result<MasterData> = withContext(Dispatchers.IO) {
        _syncState.value = SyncState.SYNCING
        try {
            val client = UntisClient(account, okHttpClient, baseUrl = getBaseUrl(account))
            val master = client.login()
            untisClient = client

            _account.value = account
            accountStore.save(account)

            _masterData.value = master
            cacheStore.saveMaster(master)

            val nowMs = System.currentTimeMillis()
            val meta = cacheStore.loadMeta().copy(masterFetchedAt = nowMs)
            cacheStore.saveMeta(meta)

            scope.launch {
                checkAndFetchHolidays(force = true)
            }

            scope.launch {
                refresh(full = true)
                // Immediate first check: urgent changes (Foyer/Entfall) notify now, the rest becomes the baseline.
                io.github.omeg4dev.greatuntis.watch.Poller.pollOnce(context)
            }
            WatchScheduler.reschedule(context)
            _syncState.value = SyncState.IDLE
            Result.success(master)
        } catch (e: Exception) {
            val state = when (e) {
                is UntisException.BadCredentials -> SyncState.AUTH_ERROR
                is UntisException.Network -> SyncState.OFFLINE
                else -> SyncState.ERROR
            }
            _syncState.value = state
            Result.failure(e)
        }
    }

    suspend fun refresh(full: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val acc = _account.value ?: return@withContext Result.failure(IllegalStateException("Not logged in"))
        val client = untisClient ?: UntisClient(acc, okHttpClient, baseUrl = getBaseUrl(acc)).also { untisClient = it }

        _syncState.value = SyncState.SYNCING
        try {
            var meta = cacheStore.loadMeta()
            val nowMs = System.currentTimeMillis()

            val masterStale = full || meta.masterFetchedAt == null || (nowMs - meta.masterFetchedAt) > 24 * 3600 * 1000L
            if (masterStale) {
                try {
                    val freshMaster = client.login()
                    _masterData.value = freshMaster
                    cacheStore.saveMaster(freshMaster)
                    meta = meta.copy(masterFetchedAt = nowMs)
                    cacheStore.saveMeta(meta)
                } catch (e: Exception) {
                    if (e is UntisException.BadCredentials) {
                        _syncState.value = SyncState.AUTH_ERROR
                        return@withContext Result.failure(e)
                    }
                    Log.w("RealUntisRepo", "Master data refresh failed; keeping existing copy", e)
                }
            }

            val today = LocalDate.now()
            val windowDays = client.timetable(today, today.plusDays(4))
            _windowDays.value = windowDays
            cacheStore.saveWindow(windowDays)

            val master = _masterData.value
            val schoolYear = master?.schoolYears?.firstOrNull { today in it.start..it.end }
                ?: master?.schoolYears?.maxByOrNull { it.start }

            val yearStale = full || meta.yearFetchedAt == null || (nowMs - meta.yearFetchedAt) > 6 * 3600 * 1000L
            if (yearStale && schoolYear != null) {
                val yearStart = schoolYear.start
                val yesterday = today.minusDays(1)
                if (!yesterday.isBefore(yearStart)) {
                    val yearDays = client.timetable(yearStart, yesterday)
                    _yearDays.value = yearDays
                    cacheStore.saveYear(yearDays)
                    meta = meta.copy(yearFetchedAt = nowMs)
                }
            }

            val absencesStale = full || meta.absencesFetchedAt == null || (nowMs - meta.absencesFetchedAt) > 30 * 60 * 1000L
            if (absencesStale && schoolYear != null) {
                val absStart = schoolYear.start
                val absEnd = schoolYear.end
                val abs = client.absences(absStart, absEnd)
                _absences.value = abs
                cacheStore.saveAbsences(abs)
                meta = meta.copy(absencesFetchedAt = nowMs)
            }

            _days.value = WeekUtils.mergeDays(_yearDays.value, _weeksDays.value, _windowDays.value)
            val nowInstant = Instant.now()
            _lastSync.value = nowInstant
            meta = meta.copy(lastSync = nowInstant.toEpochMilli())
            cacheStore.saveMeta(meta)

            scope.launch {
                checkAndFetchHolidays()
            }

            _syncState.value = SyncState.IDLE
            WidgetUpdater.requestUpdate(context)
            Result.success(Unit)
        } catch (e: Exception) {
            val state = when (e) {
                is UntisException.BadCredentials -> SyncState.AUTH_ERROR
                is UntisException.Network -> SyncState.OFFLINE
                else -> SyncState.ERROR
            }
            _syncState.value = state
            Result.failure(e)
        }
    }

    override suspend fun refresh(): Result<Unit> = refresh(full = false)

    suspend fun pollWindow(): Result<List<Day>> = withContext(Dispatchers.IO) {
        val acc = _account.value ?: return@withContext Result.failure(IllegalStateException("Not logged in"))
        val client = untisClient ?: UntisClient(acc, okHttpClient, baseUrl = getBaseUrl(acc)).also { untisClient = it }

        try {
            val today = LocalDate.now()
            val windowDays = client.timetable(today, today.plusDays(4))
            _windowDays.value = windowDays
            cacheStore.saveWindow(windowDays)
            _days.value = WeekUtils.mergeDays(_yearDays.value, _weeksDays.value, windowDays)

            val nowInstant = Instant.now()
            _lastSync.value = nowInstant
            val meta = cacheStore.loadMeta().copy(lastSync = nowInstant.toEpochMilli())
            cacheStore.saveMeta(meta)

            _syncState.value = SyncState.IDLE
            Result.success(windowDays)
        } catch (e: Exception) {
            val state = when (e) {
                is UntisException.BadCredentials -> SyncState.AUTH_ERROR
                is UntisException.Network -> SyncState.OFFLINE
                else -> SyncState.ERROR
            }
            _syncState.value = state
            Result.failure(e)
        }
    }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        WatchScheduler.cancel(context)
        accountStore.clear()
        cacheStore.clear()
        holidayStore.clear()
        WatchState.clear(context)
        untisClient = null

        _account.value = null
        _masterData.value = null
        _yearDays.value = emptyList()
        _windowDays.value = emptyList()
        _weeksDays.value = emptyMap()
        _days.value = emptyList()
        _absences.value = emptyList()
        _syncState.value = SyncState.IDLE
        _lastSync.value = null
        _holidaysUpdated.value = null
        _apiHolidays.value = emptyList()
        _freeCalendar.value = FreeCalendar.EMPTY
    }

    override suspend fun searchSchools(query: String): Result<List<SchoolInfo>> = withContext(Dispatchers.IO) {
        try {
            val results = UntisClient.searchSchools(okHttpClient, query)
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSettings(transform: (Settings) -> Settings) {
        val oldState = _settings.value.federalState
        val updated = settingsStore.update(transform)
        _settings.value = updated
        WatchScheduler.reschedule(context)
        if (updated.federalState != oldState) {
            scope.launch {
                checkAndFetchHolidays(force = true)
            }
        }
    }

    override suspend fun ensureWeek(monday: LocalDate): Result<Unit> {
        return weekManager.ensureWeek(monday, _days.value)
    }

    private suspend fun checkAndFetchHolidays(force: Boolean = false) {
        try {
            val currentState = _settings.value.federalState
            val cached = holidayStore.load()
            val should = force || HolidayStore.shouldFetch(
                cachedState = cached.state,
                cachedFetchedAt = cached.fetchedAt,
                currentState = currentState,
            )
            if (!should) return

            val today = LocalDate.now()
            val master = _masterData.value
            val schoolYear = master?.schoolYears?.firstOrNull { today in it.start..it.end }
                ?: master?.schoolYears?.maxByOrNull { it.start }

            val from: LocalDate
            val to: LocalDate
            if (schoolYear != null) {
                from = schoolYear.start.minusDays(31)
                to = schoolYear.start.plusYears(2)
            } else {
                from = today.minusMonths(1)
                to = today.plusYears(2)
            }

            val holidays = holidayClient.fetch(currentState, from, to)
            val now = Instant.now()
            holidayStore.save(holidays, currentState, now)
            _holidaysUpdated.value = now
            _apiHolidays.value = holidays
        } catch (e: Exception) {
            Log.w("RealUntisRepo", "Failed to fetch holidays: ${e.message}", e)
        }
    }
}
