package io.github.omeg4dev.greatuntis.ui.fake

import io.github.omeg4dev.greatuntis.data.UntisRepository
import io.github.omeg4dev.greatuntis.model.Absence
import io.github.omeg4dev.greatuntis.model.Account
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.MasterData
import io.github.omeg4dev.greatuntis.model.SchoolInfo
import io.github.omeg4dev.greatuntis.model.Settings
import io.github.omeg4dev.greatuntis.model.SyncState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDate
import io.github.omeg4dev.greatuntis.calendar.FreeCalendar

class FakeUntisRepository(
    initialLoggedIn: Boolean = true,
) : UntisRepository {

    private val _account = MutableStateFlow<Account?>(
        if (initialLoggedIn) FakeData.sampleAccount else null
    )
    override val account: StateFlow<Account?> = _account.asStateFlow()

    private val _masterData = MutableStateFlow<MasterData?>(
        if (initialLoggedIn) FakeData.masterData else null
    )
    override val masterData: StateFlow<MasterData?> = _masterData.asStateFlow()

    private val _days = MutableStateFlow(FakeData.createDays())
    override val days: StateFlow<List<Day>> = _days.asStateFlow()

    private val _absences = MutableStateFlow(FakeData.absences)
    override val absences: StateFlow<List<Absence>> = _absences.asStateFlow()

    private val _settings = MutableStateFlow(Settings())
    override val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // 07:42 CET = 06:42 UTC
    private val _lastSync = MutableStateFlow<Instant?>(
        if (initialLoggedIn) Instant.parse("2026-11-03T06:42:00Z") else null
    )
    override val lastSync: StateFlow<Instant?> = _lastSync.asStateFlow()

    override suspend fun login(account: Account): Result<MasterData> {
        _syncState.value = SyncState.SYNCING
        delay(800)
        return if (account.user.equals("fail", ignoreCase = true)) {
            _syncState.value = SyncState.AUTH_ERROR
            Result.failure(IllegalArgumentException("Benutzer oder Schlüssel falsch."))
        } else {
            _account.value = account
            _masterData.value = FakeData.masterData
            _lastSync.value = Instant.now()
            _syncState.value = SyncState.IDLE
            Result.success(FakeData.masterData)
        }
    }

    override suspend fun logout() {
        _account.value = null
        _masterData.value = null
        _lastSync.value = null
        _syncState.value = SyncState.IDLE
    }

    override suspend fun refresh(): Result<Unit> {
        _syncState.value = SyncState.SYNCING
        delay(600)
        _lastSync.value = Instant.now()
        _syncState.value = SyncState.IDLE
        return Result.success(Unit)
    }

    override suspend fun searchSchools(query: String): Result<List<SchoolInfo>> {
        delay(300)
        val q = query.trim().lowercase()
        val results = if (q.isEmpty()) {
            emptyList()
        } else if (q.contains("muster") || q.contains("demo")) {
            FakeData.sampleSchools
        } else {
            FakeData.sampleSchools.filter {
                it.displayName.lowercase().contains(q) ||
                    it.address.lowercase().contains(q) ||
                    it.loginName.lowercase().contains(q)
            }
        }
        return Result.success(results)
    }

    override suspend fun updateSettings(transform: (Settings) -> Settings) {
        _settings.value = transform(_settings.value)
    }

    
    private val _freeCalendar = MutableStateFlow(FakeData.createFakeFreeCalendar())
    override val freeCalendar: StateFlow<FreeCalendar> = _freeCalendar.asStateFlow()
    override val holidaysUpdated: StateFlow<Instant?> = MutableStateFlow<Instant?>(null).asStateFlow()
    override suspend fun ensureWeek(monday: LocalDate): Result<Unit> = Result.success(Unit) 
}
