package io.github.omeg4dev.greatuntis.update

import android.content.Intent
import kotlinx.coroutines.flow.StateFlow

interface UpdateController {
    val state: StateFlow<UpdateState>
    /** Pending release version if any, persists across later/skip. null if up to date or installed. */
    val availableVersion: StateFlow<String?>
    val enabled: Boolean
    val needsInstallPermission: StateFlow<Boolean>

    fun checkOnStartup()
    fun checkNow(overrideMobileData: Boolean = false)
    fun install(overrideMobileData: Boolean = false)
    fun retry()
    fun later()
    fun skip()
    fun dismissMessage()
    fun onInstallerResult(status: Int, message: String?, extraIntent: Intent? = null)
    fun confirmInstallPermission()
    fun cancelInstallPermission()
}
