package io.github.omeg4dev.greatuntis.update

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal val Context.updateDataStore: DataStore<Preferences> by preferencesDataStore(name = "update_settings")

data class UpdatePrefs(
    val autoCheck: Boolean = true,
    val lastCheckMs: Long = 0L,
    val laterUntilMs: Long = 0L,
    val skippedVersion: String? = null,
    val installExplained: Boolean = false,
    val knownAvailable: String? = null,
)

class UpdateStore(private val context: Context) {

    private val keyAutoCheck = booleanPreferencesKey("update_auto_check")
    private val keyLastCheckMs = longPreferencesKey("update_last_check_ms")
    private val keyLaterUntilMs = longPreferencesKey("update_later_until_ms")
    private val keySkippedVersion = stringPreferencesKey("update_skipped_version")
    private val keyInstallExplained = booleanPreferencesKey("update_install_explained")
    private val keyKnownAvailable = stringPreferencesKey("update_known_available")

    val updatePrefs: Flow<UpdatePrefs> = context.updateDataStore.data.map { prefs ->
        UpdatePrefs(
            autoCheck = prefs[keyAutoCheck] ?: true,
            lastCheckMs = prefs[keyLastCheckMs] ?: 0L,
            laterUntilMs = prefs[keyLaterUntilMs] ?: 0L,
            skippedVersion = prefs[keySkippedVersion],
            installExplained = prefs[keyInstallExplained] ?: false,
            knownAvailable = prefs[keyKnownAvailable],
        )
    }

    suspend fun getPrefs(): UpdatePrefs = updatePrefs.first()

    suspend fun update(transform: (UpdatePrefs) -> UpdatePrefs): UpdatePrefs {
        var updated = UpdatePrefs()
        context.updateDataStore.edit { prefs ->
            val current = UpdatePrefs(
                autoCheck = prefs[keyAutoCheck] ?: true,
                lastCheckMs = prefs[keyLastCheckMs] ?: 0L,
                laterUntilMs = prefs[keyLaterUntilMs] ?: 0L,
                skippedVersion = prefs[keySkippedVersion],
                installExplained = prefs[keyInstallExplained] ?: false,
                knownAvailable = prefs[keyKnownAvailable],
            )
            val newPrefs = transform(current)
            updated = newPrefs
            prefs[keyAutoCheck] = newPrefs.autoCheck
            prefs[keyLastCheckMs] = newPrefs.lastCheckMs
            prefs[keyLaterUntilMs] = newPrefs.laterUntilMs
            if (newPrefs.skippedVersion != null) {
                prefs[keySkippedVersion] = newPrefs.skippedVersion
            } else {
                prefs.remove(keySkippedVersion)
            }
            prefs[keyInstallExplained] = newPrefs.installExplained
            if (newPrefs.knownAvailable != null) {
                prefs[keyKnownAvailable] = newPrefs.knownAvailable
            } else {
                prefs.remove(keyKnownAvailable)
            }
        }
        return updated
    }
}
