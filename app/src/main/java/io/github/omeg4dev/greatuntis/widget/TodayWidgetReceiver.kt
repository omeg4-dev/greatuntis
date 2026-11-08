package io.github.omeg4dev.greatuntis.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import io.github.omeg4dev.greatuntis.BuildConfig
import io.github.omeg4dev.greatuntis.data.AccountStore
import io.github.omeg4dev.greatuntis.data.CacheStore
import io.github.omeg4dev.greatuntis.ui.fake.FakeData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (BuildConfig.DEBUG && intent.action == ACTION_PIN) {
            val pendingResult = goAsync()
            val debugPrefs = context.getSharedPreferences("widget_debug", Context.MODE_PRIVATE)
            val nowExtra = intent.getStringExtra("now")
            val fakeExtra = intent.getBooleanExtra("fake", false)

            debugPrefs.edit().apply {
                if (nowExtra != null) putString("debug_now", nowExtra)
                if (intent.hasExtra("fake")) putBoolean("debug_fake", fakeExtra)
            }.commit()

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val myProvider = ComponentName(context, TodayWidgetReceiver::class.java)
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                try {
                    appWidgetManager.requestPinAppWidget(myProvider, null, null)
                } catch (e: Exception) {
                    // Ignored if calling application does not have a foreground activity
                }
            }

            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch {
                try {
                    if (fakeExtra) {
                        val accountStore = AccountStore(context)
                        accountStore.save(FakeData.sampleAccount)
                        val cacheStore = CacheStore(context)
                        cacheStore.saveMaster(FakeData.masterData)
                        cacheStore.saveYear(FakeData.createDays())
                        cacheStore.saveWindow(FakeData.createDays())
                    }
                    TodayWidget().updateAll(context)
                } catch (e: Exception) {
                    android.util.Log.e("TodayWidget", "Failed to update widget", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_PIN = "io.github.omeg4dev.greatuntis.PIN_WIDGET"
    }
}
