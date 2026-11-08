package io.github.omeg4dev.greatuntis.watch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class WatchAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val serviceIntent = Intent(context, WatchService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
