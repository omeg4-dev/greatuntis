package io.github.omeg4dev.greatuntis.ui.today

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.omeg4dev.greatuntis.calendar.FreeCalendar
import io.github.omeg4dev.greatuntis.calendar.FreeDay
import io.github.omeg4dev.greatuntis.calendar.FreeKind
import io.github.omeg4dev.greatuntis.calendar.FreeSource
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.DayStatus
import io.github.omeg4dev.greatuntis.model.SyncState
import io.github.omeg4dev.greatuntis.model.TimeUnit
import io.github.omeg4dev.greatuntis.timetable.DayAnalysis
import io.github.omeg4dev.greatuntis.timetable.DayPlan
import io.github.omeg4dev.greatuntis.timetable.FreeReason
import io.github.omeg4dev.greatuntis.timetable.FreeSlot
import io.github.omeg4dev.greatuntis.timetable.LessonKind
import io.github.omeg4dev.greatuntis.timetable.LessonView
import io.github.omeg4dev.greatuntis.timetable.PlacedLesson
import io.github.omeg4dev.greatuntis.ui.LocalNow
import io.github.omeg4dev.greatuntis.ui.components.FerienCountdownCard
import io.github.omeg4dev.greatuntis.ui.components.FreeDayHero
import io.github.omeg4dev.greatuntis.ui.components.LessonCard
import io.github.omeg4dev.greatuntis.ui.components.LessonDetailSheet
import io.github.omeg4dev.greatuntis.ui.components.SyncAction
import io.github.omeg4dev.greatuntis.ui.theme.statusColors
import io.github.omeg4dev.greatuntis.ui.theme.tabularNumbers
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TodayScreen(
    days: List<Day>,
    timeGrid: List<TimeUnit>,
    freeCalendar: FreeCalendar,
    syncState: SyncState,
    lastSync: Instant?,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateToFree: () -> Unit,
    settings: io.github.omeg4dev.greatuntis.model.Settings,
    predict: (history: List<Day>, dates: List<LocalDate>, grid: List<TimeUnit>, isFree: (LocalDate) -> Boolean) -> List<Day>,
    modifier: Modifier = Modifier,
) {
    val now = LocalNow.current
    val todayDate = now.toLocalDate()
    val currentTime = now.toLocalTime()
    val statusColors = MaterialTheme.statusColors

    val context = LocalContext.current
    var hasRequestedPermission by rememberSaveable { mutableStateOf(false) }
    var cardDismissed by rememberSaveable { mutableStateOf(false) }
    var notificationsEnabled by rememberSaveable {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationsEnabled = isGranted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasRequestedPermission) {
            hasRequestedPermission = true
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            notificationsEnabled = granted
            if (!granted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Selectable dates: today + next 5 school days
    val selectableDates = remember(todayDate, freeCalendar) {
        val list = mutableListOf(todayDate)
        var testDate = todayDate.plusDays(1)
        while (list.size < 6) {
            if (freeCalendar.freeDay(testDate) == null) {
                list.add(testDate)
            }
            testDate = testDate.plusDays(1)
        }
        list
    }

    var selectedDate by rememberSaveable { mutableStateOf(todayDate) }

    // Pre-calculate DayPlans for selectable dates
    val dayMap = remember(days) { days.associateBy { it.date } }
    val plans = remember(selectableDates, dayMap, timeGrid, settings.foyerPrefix) {
        selectableDates.associateWith { date ->
            val d = dayMap[date] ?: Day(date, DayStatus.NO_DATA, emptyList())
            DayAnalysis.analyze(d, timeGrid, settings.foyerPrefix)
        }
    }

    val currentDay = dayMap[selectedDate] ?: Day(selectedDate, DayStatus.NO_DATA, emptyList())
    val currentPlan = plans[selectedDate] ?: DayAnalysis.analyze(currentDay, timeGrid, settings.foyerPrefix)

    // Subtitle German: "Dienstag, 3. November"
    val fullWeekday = selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.GERMAN)
    val dayOfMonth = selectedDate.dayOfMonth
    val fullMonth = selectedDate.month.getDisplayName(TextStyle.FULL, Locale.GERMAN)
    val subtitleText = "$fullWeekday, $dayOfMonth. $fullMonth"

    val nextFerien = freeCalendar.nextFerien(todayDate)
    val schoolDaysUntilFerien = if (nextFerien != null) freeCalendar.schoolDaysBetween(todayDate, nextFerien.start) else -1
    val showFerienAtTop = schoolDaysUntilFerien in 1..10

    var selectedLessonView by remember { mutableStateOf<LessonView?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val timelineRows = remember(currentPlan, timeGrid) {
        buildHeuteTimeline(currentPlan, timeGrid)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Heute",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    SyncAction(
                        syncState = syncState,
                        lastSync = lastSync,
                        onRefresh = onRefresh,
                    )
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Einstellungen",
                        )
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = syncState == SyncState.SYNCING,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 1. Day selector: horizontally scrolling FilterChips
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        selectableDates.forEach { date ->
                            val isSelected = date == selectedDate
                            val label = when {
                                date == todayDate -> "Heute"
                                date == todayDate.plusDays(1) -> "Morgen"
                                else -> formatGermanDay(date)
                            }
                            val plan = plans[date]
                            val hasFreeHighlight = plan != null && (
                                plan.gainedUnits > 0 || plan.startsLater || plan.endsEarlier || plan.wholeDayFree
                            )

                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedDate = date },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(label)
                                        if (hasFreeHighlight) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(statusColors.free.accent, CircleShape),
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
                }

                // Notification-permission card
                if (!notificationsEnabled && !cardDismissed) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Benachrichtigungen sind aus – du verpasst Foyer-Meldungen.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(onClick = { cardDismissed = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Schließen",
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                        context.startActivity(intent)
                                    },
                                ) {
                                    Text("Einschalten")
                                }
                            }
                        }
                    }
                }

                // Ferien countdown card if <= 10 school days remain (at the top below selector)
                if (showFerienAtTop) {
                    item {
                        FerienCountdownCard(
                            calendar = freeCalendar,
                            today = todayDate,
                            onNavigateToFree = onNavigateToFree,
                        )
                    }
                }

                // Check whole day free or no lessons
                val isFreeByCalendar = freeCalendar.freeDay(selectedDate) != null
                val isWholeDayFree = currentPlan.wholeDayFree || isFreeByCalendar || (!currentPlan.hasSchool && currentDay.status != DayStatus.NOT_ALLOWED)

                if (isWholeDayFree) {
                    item {
                        val freeDay = freeCalendar.freeDay(selectedDate) ?: FreeDay(
                            date = selectedDate,
                            kind = FreeKind.SCHULFREI,
                            names = listOf(currentPlan.wholeDayFreeReason ?: "Kein Unterricht"),
                            source = FreeSource.TIMETABLE,
                        )
                        val stretch = freeCalendar.stretchAt(selectedDate)
                        val cancelledLessons = currentPlan.placed.filter { !it.view.isFreeEvent }
                        val cancelledSummary = if (cancelledLessons.isNotEmpty()) {
                            val names = cancelledLessons.map { it.view.lesson.subject }.distinct().joinToString(", ")
                            "$names entfallen"
                        } else null

                        FreeDayHero(
                            freeDay = freeDay,
                            stretch = stretch,
                            calendar = freeCalendar,
                            today = todayDate,
                            cancelledSummary = cancelledSummary,
                        )
                    }
                } else if (!currentPlan.hasSchool && currentDay.status == DayStatus.NOT_ALLOWED) {
                    // Unknown future day without data
                    val predictedDays = predict(days, listOf(selectedDate), timeGrid) { freeCalendar.isFree(it) }
                    val predDay = predictedDays.firstOrNull()
                    if (predDay != null && predDay.lessons.isNotEmpty()) {
                        item {
                            Text(
                                text = "Noch kein Plan freigegeben",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                        val predPlan = DayAnalysis.analyze(predDay, timeGrid, settings.foyerPrefix)
                        items(predPlan.placed) { placed ->
                            LessonCard(
                                view = placed.view,
                                firstUnit = placed.firstUnit,
                                lastUnit = placed.lastUnit,
                                onClick = { selectedLessonView = placed.view },
                            )
                        }
                    } else {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Noch kein Plan freigegeben",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    // School day with lessons
                    // 2. Hero card (only selected == today and the day has school)
                    if (selectedDate == todayDate && currentPlan.hasSchool) {
                        item {
                            HeuteHeroCard(
                                plan = currentPlan,
                                currentTime = currentTime,
                                freeCalendar = freeCalendar,
                                todayDate = todayDate,
                                days = days,
                                timeGrid = timeGrid,
                                settings = settings,
                            )
                        }
                    }

                    // 3. Free summary card when startsLater || endsEarlier || gainedUnits > 0 (not wholeDayFree)
                    if ((currentPlan.startsLater || currentPlan.endsEarlier || currentPlan.gainedUnits > 0) && !currentPlan.wholeDayFree) {
                        item {
                            FreeSummaryCard(plan = currentPlan)
                        }
                    }

                    // 4. Timeline from DayPlan
                    items(timelineRows) { row ->
                        when (row) {
                            is TimelineRow.LessonEntry -> {
                                val isCurrent = selectedDate == todayDate &&
                                    row.placed.view.lesson.start <= currentTime &&
                                    currentTime < row.placed.view.lesson.end
                                val isPast = selectedDate == todayDate && currentTime >= row.placed.view.lesson.end
                                val elapsed = if (isCurrent) {
                                    val totalSec = ChronoUnit.SECONDS.between(row.placed.view.lesson.start, row.placed.view.lesson.end).toFloat()
                                    if (totalSec > 0f) ChronoUnit.SECONDS.between(row.placed.view.lesson.start, currentTime).toFloat() / totalSec else 0f
                                } else 0f

                                LessonCard(
                                    view = row.placed.view,
                                    firstUnit = row.placed.firstUnit,
                                    lastUnit = row.placed.lastUnit,
                                    isCurrent = isCurrent,
                                    isPast = isPast,
                                    elapsedShare = elapsed,
                                    onClick = { selectedLessonView = row.placed.view },
                                )
                            }
                            is TimelineRow.BreakEntry -> {
                                BreakRow(minutes = row.minutes)
                            }
                            is TimelineRow.FreeSlotEntry -> {
                                FreeSlotCard(slot = row.slot, plan = currentPlan, day = currentDay)
                            }
                        }
                    }
                }

                // 5. FerienCountdownCard at end when > 10 school days remain
                if (!showFerienAtTop && nextFerien != null && schoolDaysUntilFerien > 10) {
                    item {
                        FerienCountdownCard(
                            calendar = freeCalendar,
                            today = todayDate,
                            onNavigateToFree = onNavigateToFree,
                        )
                    }
                }
            }
        }
    }

    if (selectedLessonView != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedLessonView = null },
            sheetState = sheetState,
        ) {
            LessonDetailSheet(
                view = selectedLessonView!!,
                plan = currentPlan,
            )
        }
    }
}

private sealed class TimelineRow {
    data class LessonEntry(val placed: PlacedLesson) : TimelineRow()
    data class BreakEntry(val minutes: Int) : TimelineRow()
    data class FreeSlotEntry(val slot: FreeSlot) : TimelineRow()
}

private fun buildHeuteTimeline(plan: DayPlan, timeGrid: List<TimeUnit>): List<TimelineRow> {
    val result = mutableListOf<TimelineRow>()
    val placedSorted = plan.placed.sortedWith(
        compareBy<PlacedLesson> { it.view.lesson.start }
            .thenBy { it.lane }
    )

    // Merge lessons and free slots sorted by start time
    data class TimedItem(val start: LocalTime, val end: LocalTime, val lesson: PlacedLesson?, val freeSlot: FreeSlot?)
    val allItems = mutableListOf<TimedItem>()
    val cancelledFreeSlots = plan.freeSlots.filter { it.reason == FreeReason.CANCELLED }
    for (p in placedSorted) {
        val isCancelled = p.view.kind == LessonKind.CANCELLED || LessonKind.CANCELLED in p.view.badges
        val insideCancelledSlot = isCancelled && cancelledFreeSlots.any { fs ->
            p.view.lesson.start >= fs.start && p.view.lesson.end <= fs.end
        }
        if (!insideCancelledSlot) {
            allItems.add(TimedItem(p.view.lesson.start, p.view.lesson.end, p, null))
        }
    }
    for (fs in plan.freeSlots) {
        allItems.add(TimedItem(fs.start, fs.end, null, fs))
    }
    val sortedItems = allItems.sortedWith(compareBy({ it.start }, { if (it.lesson != null) 0 else 1 }))

    val breaks = DayAnalysis.breaks(timeGrid).filter { it.minutes >= 10 }

    var lastEnd: LocalTime? = null
    for (item in sortedItems) {
        if (lastEnd != null && item.start > lastEnd) {
            val matchingBreak = breaks.firstOrNull { it.start == lastEnd && it.end == item.start }
            val gapMinutes = if (matchingBreak != null) matchingBreak.minutes else ChronoUnit.MINUTES.between(lastEnd, item.start).toInt()
            if (gapMinutes >= 10) {
                result.add(TimelineRow.BreakEntry(gapMinutes))
            }
        }
        if (item.lesson != null) {
            result.add(TimelineRow.LessonEntry(item.lesson))
        } else if (item.freeSlot != null) {
            result.add(TimelineRow.FreeSlotEntry(item.freeSlot))
        }
        if (lastEnd == null || item.end > lastEnd) {
            lastEnd = item.end
        }
    }

    return result
}

@Composable
private fun BreakRow(minutes: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )
        Text(
            text = "Pause · $minutes min",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun FreeSlotCard(slot: FreeSlot, plan: DayPlan, day: Day) {
    val statusColors = MaterialTheme.statusColors
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val unitText = if (slot.firstUnit == slot.lastUnit) "${slot.firstUnit}. Std" else "${slot.firstUnit}.–${slot.lastUnit}. Std"

    val isCancelled = slot.reason == FreeReason.CANCELLED
    val title = if (isCancelled) "Frei · $unitText · ${slot.start.format(timeFormatter)}–${slot.end.format(timeFormatter)}"
    else "Freistunde · $unitText · ${slot.start.format(timeFormatter)}–${slot.end.format(timeFormatter)}"

    val cancelledViewsInSlot = if (isCancelled) {
        plan.placed.filter { p ->
            (p.view.kind == LessonKind.CANCELLED || LessonKind.CANCELLED in p.view.badges) &&
                p.view.lesson.start >= slot.start && p.view.lesson.end <= slot.end
        }.map { it.view }.distinctBy { it.lesson.key }
    } else emptyList()

    val outlineColor = statusColors.free.accent

    Surface(
        color = statusColors.free.container.copy(alpha = 0.5f),
        contentColor = statusColors.free.onContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = outlineColor,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                    ),
                )
            },
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.tabularNumbers(),
                fontWeight = FontWeight.SemiBold,
                color = statusColors.free.onContainer,
            )
            if (cancelledViewsInSlot.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                cancelledViewsInSlot.forEach { v ->
                    val text = buildAnnotatedString {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(v.title)
                        }
                        append(" entfällt")
                        val subText = v.lesson.substitutionText
                        if (!subText.isNullOrBlank()) {
                            append(" · ")
                            withStyle(
                                SpanStyle(
                                    color = statusColors.free.onContainer.copy(alpha = 0.7f),
                                    fontStyle = FontStyle.Italic,
                                )
                            ) {
                                append(subText)
                            }
                        }
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColors.free.onContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun FreeSummaryCard(plan: DayPlan) {
    val statusColors = MaterialTheme.statusColors
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val lines = mutableListOf<String>()
    if (plan.startsLater && plan.effectiveStart != null && plan.regularStart != null) {
        lines.add("Beginn ${plan.effectiveStart.format(timeFormatter)} statt ${plan.regularStart.format(timeFormatter)}")
    }
    if (plan.endsEarlier && plan.effectiveEnd != null && plan.regularEnd != null) {
        lines.add("Schluss ${plan.effectiveEnd.format(timeFormatter)} statt ${plan.regularEnd.format(timeFormatter)}")
    }
    if (plan.gainedUnits > 0) {
        val label = if (plan.gainedUnits == 1) "1 Stunde frei" else "${plan.gainedUnits} Stunden frei"
        lines.add(label)
    }

    Surface(
        color = statusColors.free.container,
        contentColor = statusColors.free.onContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Celebration,
                contentDescription = null,
                tint = statusColors.free.accent,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                lines.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = statusColors.free.onContainer,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeuteHeroCard(
    plan: DayPlan,
    currentTime: LocalTime,
    freeCalendar: FreeCalendar,
    todayDate: LocalDate,
    days: List<Day>,
    timeGrid: List<TimeUnit>,
    settings: io.github.omeg4dev.greatuntis.model.Settings,
) {
    val statusColors = MaterialTheme.statusColors
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val effectivePlaced = plan.placed.filter {
        it.view.kind != LessonKind.CANCELLED && !it.view.isFreeEvent
    }.sortedBy { it.view.lesson.start }

    val effectiveStart = plan.effectiveStart
    val effectiveEnd = plan.effectiveEnd

    val running = effectivePlaced.firstOrNull {
        it.view.lesson.start <= currentTime && currentTime < it.view.lesson.end
    }

    val runningFreeSlot = plan.freeSlots.firstOrNull {
        it.start <= currentTime && currentTime < it.end
    }

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
        ) {
            when {
                // State 1: before first lesson
                effectiveStart != null && currentTime < effectiveStart -> {
                    val first = effectivePlaced.firstOrNull()
                    val minutesUntil = ChronoUnit.MINUTES.between(currentTime, effectiveStart)
                    Text(
                        text = "ALS NÄCHSTES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = first?.view?.title ?: "",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val roomPart = if (!first?.view?.roomText.isNullOrBlank()) " in ${first.view.roomText}" else ""
                    Text(
                        text = "um ${effectiveStart.format(timeFormatter)}$roomPart · noch $minutesUntil min",
                        style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                    )
                }

                // State 2: in a lesson
                running != null -> {
                    val minutesRemain = ChronoUnit.MINUTES.between(currentTime, running.view.lesson.end)
                    val totalSec = ChronoUnit.SECONDS.between(running.view.lesson.start, running.view.lesson.end).toFloat()
                    val elapsedSec = ChronoUnit.SECONDS.between(running.view.lesson.start, currentTime).toFloat()
                    val elapsedShare = if (totalSec > 0f) (elapsedSec / totalSec).coerceIn(0f, 1f) else 0f

                    Text(
                        text = "JETZT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = running.view.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (running.view.roomText.isNotBlank()) {
                        Text(
                            text = running.view.roomText,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "noch $minutesRemain min",
                        style = MaterialTheme.typography.displaySmall.tabularNumbers(),
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearWavyProgressIndicator(
                        progress = { elapsedShare },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                    )
                }

                // State 4: in a free slot
                runningFreeSlot != null -> {
                    Surface(
                        color = statusColors.free.container,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "FREI · bis ${runningFreeSlot.end.format(timeFormatter)}",
                                style = MaterialTheme.typography.titleLarge.tabularNumbers(),
                                fontWeight = FontWeight.Bold,
                                color = statusColors.free.accent,
                            )
                        }
                    }
                }

                // State 3: in a break
                effectiveStart != null && effectiveEnd != null && currentTime in effectiveStart..effectiveEnd -> {
                    val next = effectivePlaced.firstOrNull { it.view.lesson.start > currentTime }
                    val minutesRemain = if (next != null) ChronoUnit.MINUTES.between(currentTime, next.view.lesson.start) else 0

                    Text(
                        text = "PAUSE · noch $minutesRemain min",
                        style = MaterialTheme.typography.titleLarge.tabularNumbers(),
                        fontWeight = FontWeight.Bold,
                    )
                    if (next != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "dann ${next.view.shortTitle} in ${next.view.roomText}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                // State 5: after last lesson
                effectiveEnd != null && currentTime >= effectiveEnd -> {
                    // Next school day start
                    var nextDate = todayDate.plusDays(1)
                    while (freeCalendar.freeDay(nextDate) != null) {
                        nextDate = nextDate.plusDays(1)
                    }
                    val nextDay = days.firstOrNull { it.date == nextDate } ?: Day(nextDate, DayStatus.NO_DATA, emptyList())
                    val nextPlan = DayAnalysis.analyze(nextDay, timeGrid, settings.foyerPrefix)
                    val nextStartStr = nextPlan.effectiveStart?.format(timeFormatter) ?: "08:00"
                    val nextDayLabel = if (nextDate == todayDate.plusDays(1)) "Morgen" else formatGermanDay(nextDate)

                    Text(
                        text = "GESCHAFFT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Schulschluss war ${effectiveEnd.format(timeFormatter)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$nextDayLabel ab $nextStartStr",
                        style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                    )
                }
            }
        }
    }
}

fun formatGermanDay(date: LocalDate): String {
    val dow = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "Mo"
        DayOfWeek.TUESDAY -> "Di"
        DayOfWeek.WEDNESDAY -> "Mi"
        DayOfWeek.THURSDAY -> "Do"
        DayOfWeek.FRIDAY -> "Fr"
        DayOfWeek.SATURDAY -> "Sa"
        DayOfWeek.SUNDAY -> "So"
        null -> ""
    }
    val formatter = DateTimeFormatter.ofPattern("dd.MM.")
    return "$dow ${date.format(formatter)}"
}
