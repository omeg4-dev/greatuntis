package io.github.omeg4dev.greatuntis.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import io.github.omeg4dev.greatuntis.GreatUntisApp

class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val extraIntent: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_INTENT)
        }

        val app = context.applicationContext as? GreatUntisApp
        app?.updateController?.onInstallerResult(status, message, extraIntent)
    }

    companion object {
        const val ACTION_INSTALL_RESULT = "io.github.omeg4dev.greatuntis.update.INSTALL_RESULT"
    }
}
