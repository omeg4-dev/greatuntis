package io.github.omeg4dev.greatuntis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.omeg4dev.greatuntis.calendar.FreeCalendar
import io.github.omeg4dev.greatuntis.model.Absence
import io.github.omeg4dev.greatuntis.model.Account
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.MasterData
import io.github.omeg4dev.greatuntis.model.SchoolInfo
import io.github.omeg4dev.greatuntis.model.Settings
import io.github.omeg4dev.greatuntis.model.SyncState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

class AppViewModel(
    val deps: AppDeps,
) : ViewModel() {

    val account: StateFlow<Account?> = deps.repo.account
    val masterData: StateFlow<MasterData?> = deps.repo.masterData
    val days: StateFlow<List<Day>> = deps.repo.days
    val absences: StateFlow<List<Absence>> = deps.repo.absences
    val settings: StateFlow<Settings> = deps.repo.settings
    val syncState: StateFlow<SyncState> = deps.repo.syncState
    val lastSync: StateFlow<Instant?> = deps.repo.lastSync
    val freeCalendar: StateFlow<FreeCalendar> = deps.repo.freeCalendar

    fun refresh() {
        viewModelScope.launch {
            deps.repo.refresh()
        }
    }

    suspend fun ensureWeek(monday: LocalDate): Result<Unit> {
        return deps.repo.ensureWeek(monday)
    }

    fun logout() {
        viewModelScope.launch {
            deps.repo.logout()
        }
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        viewModelScope.launch {
            deps.repo.updateSettings(transform)
        }
    }

    suspend fun login(account: Account): Result<MasterData> {
        return deps.repo.login(account)
    }

    suspend fun searchSchools(query: String): Result<List<SchoolInfo>> {
        return deps.repo.searchSchools(query)
    }

    companion object {
        fun provideFactory(deps: AppDeps): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AppViewModel(deps) as T
                }
            }
    }
}
