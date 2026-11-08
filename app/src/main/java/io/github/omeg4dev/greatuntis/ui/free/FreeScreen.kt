package io.github.omeg4dev.greatuntis.ui.free

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.omeg4dev.greatuntis.calendar.FreeCalendar
import io.github.omeg4dev.greatuntis.calendar.FreeKind
import io.github.omeg4dev.greatuntis.calendar.FreeStretch
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.DayStatus
import io.github.omeg4dev.greatuntis.model.MasterData
import io.github.omeg4dev.greatuntis.model.SchoolYear
import io.github.omeg4dev.greatuntis.model.Settings
import io.github.omeg4dev.greatuntis.model.SyncState
import io.github.omeg4dev.greatuntis.timetable.DayAnalysis
import io.github.omeg4dev.greatuntis.timetable.DayPlan
import io.github.omeg4dev.greatuntis.timetable.LessonKind
import io.github.omeg4dev.greatuntis.ui.LocalNow
import io.github.omeg4dev.greatuntis.ui.components.StatusPill
import io.github.omeg4dev.greatuntis.ui.components.SyncAction
import io.github.omeg4dev.greatuntis.ui.theme.statusColors
import io.github.omeg4dev.greatuntis.ui.theme.tabularNumbers
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FreeScreen(
    freeCalendar: FreeCalendar,
    masterData: MasterData,
    days: List<Day>,
    settings: Settings,
    holidaysUpdated: Instant?,
    syncState: SyncState,
    lastSync: Instant?,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = LocalNow.current
    val today = now.toLocalDate()
    val statusColors = MaterialTheme.statusColors

    val currentStretch = freeCalendar.stretchAt(today)
    val nextFerien = freeCalendar.nextFerien(today)

    // School year: schoolYears containing today, else next upcoming
    val currentSchoolYear: SchoolYear? = remember(masterData.schoolYears, today) {
        masterData.schoolYears.firstOrNull { today in it.start..it.end }
            ?: masterData.schoolYears.firstOrNull { it.start > today }
            ?: masterData.schoolYears.lastOrNull()
    }

    val yearStart = currentSchoolYear?.start ?: LocalDate.of(today.year, 8, 1)
    val yearEnd = currentSchoolYear?.end ?: LocalDate.of(today.year + 1, 7, 31)

    // All stretches of current school year
    val allStretches = remember(freeCalendar, yearStart, yearEnd) {
        freeCalendar.stretches(yearStart, yearEnd)
    }

    // Next 14 days upcoming DayPlans (only fetched days, not predicted)
    val dayMap = remember(days) { days.associateBy { it.date } }
    val upcomingPlans = remember(days, today, masterData.timeGrid, settings.foyerPrefix) {
        (0L..14L).map { today.plusDays(it) }.mapNotNull { date ->
            val day = dayMap[date] ?: return@mapNotNull null
            if (day.status == DayStatus.NOT_ALLOWED || day.status == DayStatus.NO_DATA) return@mapNotNull null
            val plan = DayAnalysis.analyze(day, masterData.timeGrid, settings.foyerPrefix)
            if (plan.startsLater || plan.endsEarlier || plan.gainedUnits > 0 || plan.wholeDayFree) {
                plan
            } else {
                null
            }
        }
    }

    var selectedStretch by remember { mutableStateOf<FreeStretch?>(null) }
    var showPastStretches by rememberSaveable { mutableStateOf(false) }

    val pastStretches = remember(allStretches, today) { allStretches.filter { it.end < today } }
    val visibleStretches = remember(allStretches, pastStretches, showPastStretches) {
        if (showPastStretches) allStretches else allStretches.filter { it.end >= today }
    }

    // Group stretches by month
    val groupedStretches = remember(visibleStretches) {
        visibleStretches.groupBy { stretch ->
            "${stretch.start.month.getDisplayName(TextStyle.FULL, Locale.GERMAN)} ${stretch.start.year}"
        }
    }

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Frei") },
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
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // 1. Countdown Hero
            item {
                Surface(
                    color = statusColors.holiday.container,
                    contentColor = statusColors.holiday.onContainer,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (currentStretch != null) {
                                Text(
                                    text = "Du hast frei!",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColors.holiday.accent,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentStretch.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                val daysLeft = (ChronoUnit.DAYS.between(today, currentStretch.end) + 1).coerceAtLeast(1)
                                val daysLeftText = if (daysLeft == 1L) "noch 1 Tag" else "noch $daysLeft Tage"
                                Text(
                                    text = daysLeftText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = statusColors.holiday.onContainer.copy(alpha = 0.85f),
                                )
                            } else if (nextFerien != null) {
                                val schoolDays = freeCalendar.schoolDaysBetween(today, nextFerien.start)
                                val calDays = ChronoUnit.DAYS.between(today, nextFerien.start)

                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "Noch ",
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                    Text(
                                        text = "$schoolDays",
                                        style = MaterialTheme.typography.displayLarge.tabularNumbers(),
                                        fontWeight = FontWeight.Bold,
                                        color = statusColors.holiday.accent,
                                    )
                                    val unitText = if (schoolDays == 1) " Schultag" else " Schultage"
                                    Text(
                                        text = unitText,
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "bis ${nextFerien.title}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = nextFerien.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = statusColors.holiday.onContainer.copy(alpha = 0.8f),
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val calDaysText = if (calDays == 1L) "das ist 1 Tag" else "das sind $calDays Tage"
                                Text(
                                    text = calDaysText,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = statusColors.holiday.onContainer.copy(alpha = 0.7f),
                                )
                            } else {
                                Text(
                                    text = "Keine Ferien bekannt",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (holidaysUpdated == null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Ferienkalender wird geladen",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = statusColors.holiday.onContainer.copy(alpha = 0.8f),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // MaterialShapes.Sunny 72dp decoration with BeachAccess icon
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(MaterialShapes.Sunny.toShape())
                                .background(statusColors.holiday.accent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BeachAccess,
                                contentDescription = null,
                                tint = statusColors.holiday.container,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }
                }
            }

            // 2. Schuljahr card
            if (currentSchoolYear != null) {
                item {
                    val progress = freeCalendar.schoolYearProgress(today, currentSchoolYear)
                    val pct = (progress * 100).roundToInt()
                    val remainingSchoolDays = freeCalendar.schoolDaysBetween(today, currentSchoolYear.end.plusDays(1))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Text(
                                text = "Schuljahr ${currentSchoolYear.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            LinearWavyProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            Text(
                                text = "$pct % geschafft · noch $remainingSchoolDays Schultage",
                                style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // 3. Bald frei (next 14 days)
            item {
                Text(
                    text = "Bald frei",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (upcomingPlans.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Gerade nichts entfällt.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            } else {
                items(upcomingPlans) { plan ->
                    val desc = formatBaldFreiText(plan, timeFormatter)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                        ) {
                            // Leading date block
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.size(width = 46.dp, height = 46.dp),
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    val dow = formatShortWeekday(plan.date.dayOfWeek)
                                    Text(
                                        text = dow,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = "%02d.%02d.".format(plan.date.dayOfMonth, plan.date.monthValue),
                                        style = MaterialTheme.typography.labelSmall.tabularNumbers(),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = statusColors.free.accent,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            // 4. Alle freien Tage
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Text(
                        text = "Alle freien Tage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (pastStretches.isNotEmpty() && !showPastStretches) {
                        TextButton(onClick = { showPastStretches = true }) {
                            Text("${pastStretches.size} vergangene anzeigen")
                        }
                    }
                }
            }

            groupedStretches.forEach { (monthHeader, stretchesInMonth) ->
                item(key = monthHeader) {
                    Text(
                        text = monthHeader,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                items(stretchesInMonth) { stretch ->
                    val isPast = stretch.end < today
                    val isCurrent = today in stretch.start..stretch.end

                    val isFreeOrSchoolFree = stretch.kind == FreeKind.SCHULFREI || stretch.kind == FreeKind.WOCHENENDE
                    val shapeContainer = if (isFreeOrSchoolFree) statusColors.free.container else statusColors.holiday.container
                    val shapeAccent = if (isFreeOrSchoolFree) statusColors.free.accent else statusColors.holiday.accent

                    val (icon, shape) = when (stretch.kind) {
                        FreeKind.FERIEN -> Icons.Outlined.BeachAccess to MaterialShapes.Sunny.toShape()
                        FreeKind.FEIERTAG -> Icons.Outlined.Celebration to MaterialShapes.Cookie9Sided.toShape()
                        FreeKind.BRUECKENTAG -> Icons.Outlined.Link to MaterialShapes.Clover4Leaf.toShape()
                        FreeKind.SCHULFREI -> Icons.Outlined.EventBusy to MaterialShapes.Pill.toShape()
                        FreeKind.WOCHENENDE -> Icons.Outlined.Weekend to MaterialShapes.Circle.toShape()
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (isPast) 0.5f else 1.0f)
                            .clickable { selectedStretch = stretch },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                        ) {
                            // Leading 40dp shape with kind icon
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(shape)
                                    .background(shapeContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = shapeAccent,
                                    modifier = Modifier.size(20.dp),
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stretch.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = stretch.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            when {
                                isCurrent -> StatusPill("jetzt", statusColors.holiday)
                                isPast -> Text(
                                    text = "vorbei",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                )
                                stretch.isLongWeekend -> StatusPill("Langes Wochenende", statusColors.changed)
                            }
                        }
                    }
                }
            }

            // 5. Footer
            item {
                val standDateStr = holidaysUpdated?.let {
                    it.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                } ?: "-"
                val state = settings.federalState.ifBlank { "NW" }
                Text(
                    text = "Ferien & Feiertage: WebUntis deiner Schule und openholidaysapi.org (Bundesland $state). Stand: $standDateStr.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                )
            }
        }
    }

    // Stretch bottom sheet
    if (selectedStretch != null) {
        val stretch = selectedStretch!!
        ModalBottomSheet(
            onDismissRequest = { selectedStretch = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text(
                    text = stretch.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stretch.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                stretch.days.forEach { day ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                    ) {
                        val dow = formatShortWeekday(day.date.dayOfWeek)
                        val dateFormatted = "$dow %02d.%02d.%04d".format(day.date.dayOfMonth, day.date.monthValue, day.date.year)
                        Text(
                            text = dateFormatted,
                            style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = day.names.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun formatShortWeekday(dow: DayOfWeek): String = when (dow) {
    DayOfWeek.MONDAY -> "Mo"
    DayOfWeek.TUESDAY -> "Di"
    DayOfWeek.WEDNESDAY -> "Mi"
    DayOfWeek.THURSDAY -> "Do"
    DayOfWeek.FRIDAY -> "Fr"
    DayOfWeek.SATURDAY -> "Sa"
    DayOfWeek.SUNDAY -> "So"
}

private fun formatBaldFreiText(plan: DayPlan, timeFormatter: DateTimeFormatter): String {
    if (plan.wholeDayFree) {
        val reason = plan.wholeDayFreeReason ?: "Alles entfällt"
        return "Ganzer Tag frei · $reason"
    }
    if (plan.startsLater && plan.effectiveStart != null && plan.regularStart != null) {
        return "Beginn ${plan.effectiveStart.format(timeFormatter)} statt ${plan.regularStart.format(timeFormatter)}"
    }
    if (plan.endsEarlier && plan.effectiveEnd != null && plan.regularEnd != null) {
        return "Schluss ${plan.effectiveEnd.format(timeFormatter)} statt ${plan.regularEnd.format(timeFormatter)}"
    }
    if (plan.gainedUnits > 0) {
        val cancelledSubjects = plan.placed.filter {
            it.view.kind == LessonKind.CANCELLED || LessonKind.CANCELLED in it.view.badges
        }.map { it.view.shortTitle.ifBlank { it.view.title } }.distinct()
        val subjectSuffix = if (cancelledSubjects.isNotEmpty()) " (${cancelledSubjects.joinToString(", ")})" else ""
        val unitLabel = if (plan.gainedUnits == 1) "1 Stunde frei" else "${plan.gainedUnits} Stunden frei"
        return "$unitLabel$subjectSuffix"
    }
    return "Unterrichtsfrei"
}
