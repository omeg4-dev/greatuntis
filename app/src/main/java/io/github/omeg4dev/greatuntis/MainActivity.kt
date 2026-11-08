package io.github.omeg4dev.greatuntis

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.omeg4dev.greatuntis.api.AccountParser
import io.github.omeg4dev.greatuntis.stats.AbsenceStats
import io.github.omeg4dev.greatuntis.timetable.Prediction
import io.github.omeg4dev.greatuntis.ui.AppDeps
import io.github.omeg4dev.greatuntis.ui.GreatUntisApp
import io.github.omeg4dev.greatuntis.ui.fake.FakeData
import io.github.omeg4dev.greatuntis.ui.theme.GreatUntisTheme
import java.time.LocalDateTime
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialScreen = intent?.getStringExtra("screen")
        var fixedClock: LocalDateTime? = null
        var useFakeRepo = false

        if (BuildConfig.DEBUG) {
            intent?.getStringExtra("now")?.let {
                runCatching { LocalDateTime.parse(it) }.getOrNull()?.let { dt ->
                    fixedClock = dt
                }
            }
            useFakeRepo = intent?.getBooleanExtra("fake", false) ?: false
        }

        val app = application as io.github.omeg4dev.greatuntis.GreatUntisApp

        // Debug only: `--es login '<QR text>' [--es host http://10.0.2.2:8777]` logs in without the camera (e2e tests).
        if (BuildConfig.DEBUG && !useFakeRepo) {
            val hostOverride = intent?.getStringExtra("host") // e.g. http://10.0.2.2:8777 for tools/mock_untis.py
            intent?.getStringExtra("login")?.let(AccountParser::parse)?.let { acc ->
                val account = hostOverride?.let { acc.copy(host = it) } ?: acc
                kotlinx.coroutines.MainScope().launch { app.repo.login(account) }
            }
        }

        val onRequestBatteryExemption = {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }

        val deps = if (useFakeRepo) {
            FakeData.createFakeDeps(
                fixedClock = fixedClock,
                initialLoggedIn = initialScreen != "onboarding",
            )
        } else {
            AppDeps(
                repo = app.repo,
                parseAccount = AccountParser::parse,
                computeStats = AbsenceStats::compute,
                predict = Prediction::fill,
                clock = fixedClock?.let { { it } } ?: { LocalDateTime.now() },
                onRequestBatteryExemption = onRequestBatteryExemption,
                appVersion = BuildConfig.VERSION_NAME,
                updateController = app.updateController,
            )
        }

        setContent {
            GreatUntisTheme {
                GreatUntisApp(deps = deps, initialScreen = initialScreen)
            }
        }
    }
}
