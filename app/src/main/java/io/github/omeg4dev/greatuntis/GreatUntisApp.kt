package io.github.omeg4dev.greatuntis

import android.app.Application
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import io.github.omeg4dev.greatuntis.data.RealUntisRepository
import io.github.omeg4dev.greatuntis.update.UpdateController
import io.github.omeg4dev.greatuntis.update.UpdateControllerImpl
import io.github.omeg4dev.greatuntis.watch.DebugReceiver
import io.github.omeg4dev.greatuntis.watch.Notifier
import io.github.omeg4dev.greatuntis.watch.WatchScheduler
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class GreatUntisApp : Application() {

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val repo by lazy { RealUntisRepository(this, okHttpClient) }

    val updateController: UpdateController by lazy {
        UpdateControllerImpl(
            context = this,
            okHttpClient = okHttpClient,
        )
    }

    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
        if (BuildConfig.DEBUG) {
            ContextCompat.registerReceiver(
                this,
                DebugReceiver(),
                IntentFilter(DebugReceiver.ACTION_DEBUG),
                ContextCompat.RECEIVER_EXPORTED,
            )
        }
        WatchScheduler.reschedule(this)
    }
}
