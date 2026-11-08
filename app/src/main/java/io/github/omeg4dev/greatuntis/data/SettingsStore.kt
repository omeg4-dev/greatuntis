package io.github.omeg4dev.greatuntis.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.omeg4dev.greatuntis.model.ChangeKind
import io.github.omeg4dev.greatuntis.model.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import java.time.format.DateTimeFormatter

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val settingsFlow: Flow<Settings> = context.settingsDataStore.data
        .map { prefs -> prefs.toSettings() }

    suspend fun getSettings(): Settings = settingsFlow.first()

    suspend fun update(transform: (Settings) -> Settings): Settings {
        var updatedSettings = Settings()
        context.settingsDataStore.edit { prefs ->
            val current = prefs.toSettings()
            val updated = transform(current)
            updatedSettings = updated

            prefs[NOTIFY_KINDS] = updated.notifyKinds.joinToString(",") { it.name }
            prefs[FOYER_PREFIX] = updated.foyerPrefix
            prefs[SCHOOL_START] = updated.schoolStart.format(timeFormatter)
            prefs[POLL_SECONDS] = updated.pollSeconds
            prefs[EVENING_START] = updated.eveningStart.format(timeFormatter)
            prefs[EVENING_END] = updated.eveningEnd.format(timeFormatter)
            prefs[WATCH_ENABLED] = updated.watchEnabled
            prefs[FEDERAL_STATE] = updated.federalState
        }
        return updatedSettings
    }

    private fun Preferences.toSettings(): Settings {
        val defaultSettings = Settings()

        val notifyKinds = if (contains(NOTIFY_KINDS)) {
            val raw = this[NOTIFY_KINDS] ?: ""
            if (raw.isBlank()) {
                emptySet()
            } else {
                raw.split(",").mapNotNull { name ->
                    runCatching { ChangeKind.valueOf(name.trim()) }.getOrNull()
                }.toSet()
            }
        } else {
            defaultSettings.notifyKinds
        }

        val foyerPrefix = this[FOYER_PREFIX] ?: defaultSettings.foyerPrefix

        val schoolStart = this[SCHOOL_START]?.let {
            runCatching { LocalTime.parse(it, timeFormatter) }.getOrNull()
        } ?: defaultSettings.schoolStart

        val pollSeconds = this[POLL_SECONDS] ?: defaultSettings.pollSeconds

        val eveningStart = this[EVENING_START]?.let {
            runCatching { LocalTime.parse(it, timeFormatter) }.getOrNull()
        } ?: defaultSettings.eveningStart

        val eveningEnd = this[EVENING_END]?.let {
            runCatching { LocalTime.parse(it, timeFormatter) }.getOrNull()
        } ?: defaultSettings.eveningEnd

        val watchEnabled = this[WATCH_ENABLED] ?: defaultSettings.watchEnabled

        val federalState = this[FEDERAL_STATE] ?: defaultSettings.federalState

        return Settings(
            notifyKinds = notifyKinds,
            foyerPrefix = foyerPrefix,
            schoolStart = schoolStart,
            pollSeconds = pollSeconds,
            eveningStart = eveningStart,
            eveningEnd = eveningEnd,
            watchEnabled = watchEnabled,
            federalState = federalState,
        )
    }

    companion object {
        val NOTIFY_KINDS = stringPreferencesKey("notify_kinds")
        val FOYER_PREFIX = stringPreferencesKey("foyer_prefix")
        val SCHOOL_START = stringPreferencesKey("school_start")
        val POLL_SECONDS = intPreferencesKey("poll_seconds")
        val EVENING_START = stringPreferencesKey("evening_start")
        val EVENING_END = stringPreferencesKey("evening_end")
        val WATCH_ENABLED = booleanPreferencesKey("watch_enabled")
        val FEDERAL_STATE = stringPreferencesKey("federal_state")
    }
}
