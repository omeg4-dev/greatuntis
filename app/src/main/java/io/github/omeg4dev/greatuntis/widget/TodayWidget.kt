package io.github.omeg4dev.greatuntis.widget

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.omeg4dev.greatuntis.BuildConfig
import io.github.omeg4dev.greatuntis.GreatUntisApp
import io.github.omeg4dev.greatuntis.MainActivity
import io.github.omeg4dev.greatuntis.R
import io.github.omeg4dev.greatuntis.ui.fake.FakeData
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDateTime

class TodayWidget : GlanceAppWidget() {

    companion object {
        val SMALL_SIZE = DpSize(140.dp, 140.dp)
        val WIDE_SIZE = DpSize(280.dp, 140.dp)
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SMALL_SIZE, WIDE_SIZE)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as? GreatUntisApp
        val repo = app?.repo

        // Wait at most 2 s for masterData to be non-null (cache load)
        if (repo != null && repo.account.value != null && repo.masterData.value == null) {
            withTimeoutOrNull(2000L) {
                repo.masterData.filterNotNull().first()
            }
        }

        val isDebug = BuildConfig.DEBUG
        val debugPrefs = if (isDebug) context.getSharedPreferences("widget_debug", Context.MODE_PRIVATE) else null
        val debugNowStr = debugPrefs?.getString("debug_now", null)
        val debugFake = debugPrefs?.getBoolean("debug_fake", false) ?: false

        val fixedClock: LocalDateTime? = if (isDebug && debugNowStr != null) {
            runCatching { LocalDateTime.parse(debugNowStr) }.getOrNull()
        } else null

        val now = fixedClock ?: LocalDateTime.now()

        val isLoggedOut = (repo == null || repo.account.value == null) && (!isDebug || !debugFake)
        val state: WidgetState
        val ferien: String?

        if (isDebug && debugFake) {
            val fakeCal = FakeData.createFakeFreeCalendar()
            state = widgetState(
                now = now,
                days = FakeData.createDays(),
                grid = FakeData.timeGrid,
                foyerPrefix = "Foyer",
                calendar = fakeCal,
            )
            ferien = ferienCountdown(now.toLocalDate(), fakeCal)
        } else if (isLoggedOut) {
            state = WidgetState.NoData
            ferien = null
        } else if (repo != null && repo.masterData.value != null) {
            val master = repo.masterData.value!!
            val cal = repo.freeCalendar.value
            state = widgetState(
                now = now,
                days = repo.days.value,
                grid = master.timeGrid,
                foyerPrefix = repo.settings.value.foyerPrefix,
                calendar = cal,
            )
            ferien = ferienCountdown(now.toLocalDate(), cal)
        } else {
            state = WidgetState.NoData
            ferien = null
        }

        provideContent {
            val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                GlanceTheme.colors
            } else {
                WidgetFallbackColorProviders
            }

            GlanceTheme(colors = colors) {
                TodayWidgetContent(
                    state = state,
                    ferien = ferien,
                    isLoggedOut = isLoggedOut,
                )
            }
        }
    }
}

@Composable
private fun TodayWidgetContent(
    state: WidgetState,
    ferien: String?,
    isLoggedOut: Boolean,
) {
    val (containerColor, contentColor) = when {
        isLoggedOut -> GlanceTheme.colors.primaryContainer to GlanceTheme.colors.onPrimaryContainer
        state is WidgetState.Free && state.isHoliday -> HolidayContainerColorProvider to OnHolidayContainerColorProvider
        state is WidgetState.Free && !state.isHoliday -> FreeContainerColorProvider to OnFreeContainerColorProvider
        else -> GlanceTheme.colors.primaryContainer to GlanceTheme.colors.onPrimaryContainer
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(containerColor)
            .cornerRadius(24.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(16.dp),
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
        ) {
            if (isLoggedOut) {
                Text(
                    text = "HEUTE",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "Anmelden in GreatUntis",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 2,
                )
            } else {
                when (state) {
                    is WidgetState.Lesson -> {
                        Text(
                            text = state.label.uppercase(),
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = state.title,
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 2,
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        val roomTimeText = if (state.room.isNotBlank()) "${state.room} · ${state.timeText}" else state.timeText
                        Text(
                            text = roomTimeText,
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 14.sp,
                            ),
                            maxLines = 1,
                        )
                        if (state.note != null) {
                            Spacer(modifier = GlanceModifier.height(6.dp))
                            Row(
                                modifier = GlanceModifier
                                    .cornerRadius(12.dp)
                                    .background(GlanceTheme.colors.secondaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = state.note,
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSecondaryContainer,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    maxLines = 2,
                                )
                            }
                        }
                    }

                    is WidgetState.Free -> {
                        val iconRes = if (state.isHoliday) R.drawable.ic_widget_holiday else R.drawable.ic_widget_free
                        val labelText = if (state.isHoliday) "FERIEN" else "FREI"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                provider = ImageProvider(iconRes),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(contentColor),
                                modifier = GlanceModifier.size(14.dp),
                            )
                            Spacer(modifier = GlanceModifier.width(4.dp))
                            Text(
                                text = labelText,
                                style = TextStyle(
                                    color = contentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = state.title,
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 2,
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = state.subtitle,
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 14.sp,
                            ),
                            maxLines = 2,
                        )
                    }

                    is WidgetState.Done -> {
                        Text(
                            text = "GESCHAFFT",
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = "Schulschluss",
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 2,
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = state.nextText,
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 14.sp,
                            ),
                            maxLines = 2,
                        )
                    }

                    WidgetState.NoData -> {
                        Text(
                            text = "HEUTE",
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = "Kein Unterricht",
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 2,
                        )
                    }
                }
            }

            // Pin the Ferien line to the BOTTOM in ALL sizes (Column with a weight spacer)
            Spacer(modifier = GlanceModifier.defaultWeight())
            if (!ferien.isNullOrBlank()) {
                Text(
                    text = ferien,
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 12.sp,
                    ),
                    maxLines = 2,
                )
            }
        }
    }
}
