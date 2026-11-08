package io.github.omeg4dev.greatuntis.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object WidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun requestUpdate(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            runCatching {
                TodayWidget().updateAll(appContext)
            }
        }
    }
}
