package io.github.omeg4dev.greatuntis.watch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DebugReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_DEBUG) return
        val cmd = intent.getStringExtra("cmd")
        when (cmd) {
            "poll" -> {
                scope.launch {
                    Poller.pollOnce(context)
                }
            }
            "start" -> {
                val serviceIntent = Intent(context, WatchService::class.java).apply {
                    putExtra("force", true)
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            }
            "stop" -> {
                context.stopService(Intent(context, WatchService::class.java))
            }
        }
    }

    companion object {
        const val ACTION_DEBUG = "io.github.omeg4dev.greatuntis.DEBUG"
    }
}
