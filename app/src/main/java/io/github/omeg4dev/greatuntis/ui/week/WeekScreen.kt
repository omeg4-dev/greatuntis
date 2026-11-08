package io.github.omeg4dev.greatuntis.ui.week

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.omeg4dev.greatuntis.calendar.FreeCalendar
import io.github.omeg4dev.greatuntis.calendar.FreeDay
import io.github.omeg4dev.greatuntis.calendar.FreeKind
import io.github.omeg4dev.greatuntis.calendar.FreeStretch
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.DayStatus
import io.github.omeg4dev.greatuntis.model.SyncState
import io.github.omeg4dev.greatuntis.model.TimeUnit
import io.github.omeg4dev.greatuntis.timetable.DayAnalysis
import io.github.omeg4dev.greatuntis.timetable.DayPlan
import io.github.omeg4dev.greatuntis.timetable.GridBreak
import io.github.omeg4dev.greatuntis.timetable.LessonKind
import io.github.omeg4dev.greatuntis.timetable.LessonView
import io.github.omeg4dev.greatuntis.timetable.PlacedLesson
import io.github.omeg4dev.greatuntis.ui.LocalNow
import io.github.omeg4dev.greatuntis.ui.components.LessonDetailSheet
import io.github.omeg4dev.greatuntis.ui.components.StatusPill
import io.github.omeg4dev.greatuntis.ui.components.SyncAction
import io.github.omeg4dev.greatuntis.ui.theme.statusColors
import io.github.omeg4dev.greatuntis.ui.theme.subjectColor
import io.github.omeg4dev.greatuntis.ui.theme.tabularNumbers
import io.github.omeg4dev.greatuntis.ui.today.formatGermanDay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekScreen(
    days: List<Day>,
    timeGrid: List<TimeUnit>,
    freeCalendar: FreeCalendar,
    syncState: SyncState,
    lastSync: Instant?,
    predict: (history: List<Day>, dates: List<LocalDate>, grid: List<TimeUnit>, isFree: (LocalDate) -> Boolean) -> List<Day>,
    ensureWeek: suspend (LocalDate) -> Result<Unit>,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    settings: io.github.omeg4dev.greatuntis.model.Settings,
    modifier: Modifier = Modifier,
) {
    val now = LocalNow.current
    val todayDate = now.toLocalDate()
    val currentTime = now.toLocalTime()

    val initialMonday = remember(todayDate) {
        if (todayDate.dayOfWeek == DayOfWeek.SATURDAY || todayDate.dayOfWeek == DayOfWeek.SUNDAY) {
            todayDate.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        } else {
            todayDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        }
    }

    val currentMonday = remember(todayDate) {
        todayDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    val pagerState = rememberPagerState(
        initialPage = 260,
        pageCount = { 521 },
    )
    val coroutineScope = rememberCoroutineScope()

    // monday for a given page
    fun mondayForPage(page: Int): LocalDate = initialMonday.plusWeeks((page - 260).toLong())

    val selectedMonday = mondayForPage(pagerState.currentPage)

    LaunchedEffect(pagerState.settledPage) {
        val monday = mondayForPage(pagerState.settledPage)
        ensureWeek(monday)
    }

    val weekNumber = selectedMonday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
    val friday = selectedMonday.plusDays(4)

    val subtitle = remember(selectedMonday, friday) {
        when {
            selectedMonday.year != friday.year -> {
                val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                "${selectedMonday.format(fmt)} – ${friday.format(fmt)}"
            }
            selectedMonday.month != friday.month -> {
                val m1 = selectedMonday.month.getDisplayName(TextStyle.FULL, Locale.GERMAN)
                val m2 = friday.month.getDisplayName(TextStyle.FULL, Locale.GERMAN)
                "%02d. %s – %02d. %s %d".format(selectedMonday.dayOfMonth, m1, friday.dayOfMonth, m2, selectedMonday.year)
            }
            else -> {
                val m = selectedMonday.month.getDisplayName(TextStyle.FULL, Locale.GERMAN)
                "%02d.–%02d. %s %d".format(selectedMonday.dayOfMonth, friday.dayOfMonth, m, selectedMonday.year)
            }
        }
    }

    var selectedLessonView by remember { mutableStateOf<LessonView?>(null) }
    var selectedClusterLessons by remember { mutableStateOf<List<LessonView>?>(null) }
    var selectedLessonPlan by remember { mutableStateOf<DayPlan?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "KW $weekNumber",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    if (pagerState.currentPage != 260) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(260)
                                }
                            },
                        ) {
                            Text("Heute")
                        }
                    }
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
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val monday = mondayForPage(page)
                WeekPageContent(
                    monday = monday,
                    currentMonday = currentMonday,
                    days = days,
                    timeGrid = timeGrid,
                    freeCalendar = freeCalendar,
                    todayDate = todayDate,
                    currentTime = currentTime,
                    predict = predict,
                    settings = settings,
                    onLessonClick = { view, plan ->
                        selectedLessonView = view
                        selectedClusterLessons = null
                        selectedLessonPlan = plan
                    },
                    onClusterClick = { views, plan ->
                        selectedClusterLessons = views
                        selectedLessonView = null
                        selectedLessonPlan = plan
                    },
                )
            }
        }
    }

    val activeLessons = selectedClusterLessons ?: listOfNotNull(selectedLessonView)
    if (activeLessons.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = {
                selectedLessonView = null
                selectedClusterLessons = null
                selectedLessonPlan = null
            },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
            ) {
                activeLessons.forEachIndexed { index, view ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                    LessonDetailSheet(
                        view = view,
                        plan = selectedLessonPlan,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekPageContent(
    monday: LocalDate,
    currentMonday: LocalDate,
    days: List<Day>,
    timeGrid: List<TimeUnit>,
    freeCalendar: FreeCalendar,
    todayDate: LocalDate,
    currentTime: LocalTime,
    predict: (history: List<Day>, dates: List<LocalDate>, grid: List<TimeUnit>, isFree: (LocalDate) -> Boolean) -> List<Day>,
    settings: io.github.omeg4dev.greatuntis.model.Settings,
    onLessonClick: (LessonView, DayPlan) -> Unit,
    onClusterClick: (List<LessonView>, DayPlan) -> Unit,
) {
    val statusColors = MaterialTheme.statusColors
    val weekDates = remember(monday) { (0..4).map { monday.plusDays(it.toLong()) } }

    val dayMap = remember(days) { days.associateBy { it.date } }

    // Identify days needing prediction
    val predictedDays = remember(days, weekDates, timeGrid, freeCalendar) {
        val missingOrUnallowed = weekDates.any {
            val d = dayMap[it]
            d == null || d.status == DayStatus.NOT_ALLOWED
        }
        if (missingOrUnallowed) {
            predict(days, weekDates, timeGrid) { freeCalendar.isFree(it) }.associateBy { it.date }
        } else {
            emptyMap()
        }
    }

    val plans = remember(weekDates, dayMap, predictedDays, timeGrid, settings.foyerPrefix) {
        weekDates.map { date ->
            val day = dayMap[date] ?: predictedDays[date] ?: Day(date, DayStatus.NO_DATA, emptyList())
            DayAnalysis.analyze(day, timeGrid, settings.foyerPrefix)
        }
    }

    // Week summary line
    val ferienStretch = freeCalendar.stretchAt(monday)
    val isFerienWeek = ferienStretch != null && ferienStretch.kind == FreeKind.FERIEN &&
        weekDates.all { freeCalendar.freeDay(it)?.kind == FreeKind.FERIEN }

    val (summaryText, summaryIsFree) = remember(plans, isFerienWeek, ferienStretch) {
        if (isFerienWeek && ferienStretch != null) {
            ferienStretch.title to true
        } else {
            val freeHours = plans.sumOf { it.gainedUnits }
            val exams = plans.flatMap { it.placed }.filter {
                it.view.kind == LessonKind.EXAM || LessonKind.EXAM in it.view.badges
            }.distinctBy { it.view.lesson.date to it.view.title }.size
            val changes = plans.flatMap { it.placed }.filter {
                LessonKind.ROOM_CHANGED in it.view.badges ||
                    LessonKind.TEACHER_CHANGED in it.view.badges ||
                    LessonKind.SUBSTITUTION in it.view.badges ||
                    LessonKind.FOYER in it.view.badges
            }.distinctBy { it.view.lesson.date to it.view.title }.size

            val parts = mutableListOf<String>()
            if (freeHours > 0) parts.add(if (freeHours == 1) "1 Stunde frei" else "$freeHours Stunden frei")
            if (exams > 0) parts.add(if (exams == 1) "1 Klausur" else "$exams Klausuren")
            if (changes > 0) parts.add(if (changes == 1) "1 Änderung" else "$changes Änderungen")

            if (parts.isNotEmpty()) {
                "Diese Woche: " + parts.joinToString(" · ") to (freeHours > 0)
            } else {
                "" to false
            }
        }
    }

    // Precompute Grid Geometry
    val sortedGrid = remember(timeGrid) { timeGrid.sortedBy { it.index } }
    val breaks = remember(sortedGrid) { DayAnalysis.breaks(sortedGrid) }

    data class GridGeometry(
        val unitY: Map<Int, Dp>,
        val breakY: Map<Int, Dp>,
        val breakHeight: Map<Int, Dp>,
        val totalHeight: Dp,
    )

    val geometry = remember(sortedGrid, breaks) {
        val uY = mutableMapOf<Int, Dp>()
        val bY = mutableMapOf<Int, Dp>()
        val bH = mutableMapOf<Int, Dp>()
        var curY = 0.dp
        for (unit in sortedGrid) {
            uY[unit.index] = curY
            curY += 56.dp
            val brk = breaks.firstOrNull { it.afterUnit == unit.index }
            if (brk != null) {
                val h = if (brk.minutes >= 15) 12.dp else 3.dp
                bY[unit.index] = curY
                bH[unit.index] = h
                curY += h
            }
        }
        GridGeometry(uY, bY, bH, curY)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Week summary line
        if (summaryText.isNotEmpty()) {
            Text(
                text = summaryText,
                style = MaterialTheme.typography.labelLarge,
                color = if (summaryIsFree) statusColors.free.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        // Day header row: 36dp rail spacer + 5 columns
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            Spacer(modifier = Modifier.width(36.dp))

            weekDates.forEachIndexed { i, date ->
                val plan = plans[i]
                val isToday = date == todayDate
                val isFree = freeCalendar.freeDay(date) != null || plan.wholeDayFree
                val isPredicted = plan.predicted || (dayMap[date]?.status == DayStatus.NOT_ALLOWED)

                val dayNameShort = when (date.dayOfWeek) {
                    DayOfWeek.MONDAY -> "Mo"
                    DayOfWeek.TUESDAY -> "Di"
                    DayOfWeek.WEDNESDAY -> "Mi"
                    DayOfWeek.THURSDAY -> "Do"
                    DayOfWeek.FRIDAY -> "Fr"
                    else -> ""
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = dayNameShort,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    val circleColor = when {
                        isToday -> MaterialTheme.colorScheme.primary
                        else -> Color.Transparent
                    }
                    val textColor = when {
                        isToday -> MaterialTheme.colorScheme.onPrimary
                        isFree -> statusColors.holiday.accent
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .background(circleColor, CircleShape),
                    ) {
                        Text(
                            text = "${date.dayOfMonth}",
                            style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                            fontWeight = if (isToday || isFree) FontWeight.Bold else FontWeight.Medium,
                            color = textColor,
                        )
                    }

                    if (isPredicted) {
                        Text(
                            text = "Prognose",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                        )
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Grid + Legend (vertically scrollable)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(geometry.totalHeight),
            ) {
                // Rail: 36dp (unit number labelMedium bold + start time 9sp tabular)
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(geometry.totalHeight),
                ) {
                    sortedGrid.forEach { unit ->
                        val y = geometry.unitY[unit.index] ?: 0.dp
                        val timeStr = unit.start.format(DateTimeFormatter.ofPattern("HH:mm"))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .offset(y = y)
                                .size(width = 36.dp, height = 56.dp),
                        ) {
                            Text(
                                text = "${unit.index}",
                                style = MaterialTheme.typography.labelMedium.tabularNumbers(),
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.labelSmall.tabularNumbers().copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                    }
                }

                // Main 5-day grid
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .height(geometry.totalHeight),
                ) {
                    val totalWidth = maxWidth
                    val columnWidth = totalWidth / 5

                    // Draw unit horizontal lines and break bands
                    sortedGrid.forEach { unit ->
                        val brk = breaks.firstOrNull { it.afterUnit == unit.index }
                        if (brk != null) {
                            val by = geometry.breakY[unit.index] ?: 0.dp
                            val bh = geometry.breakHeight[unit.index] ?: 0.dp
                            if (brk.minutes >= 15) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(y = by)
                                        .height(bh)
                                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                                )
                            }
                        }
                        // unit divider line
                        val nextUnitY = (geometry.unitY[unit.index] ?: 0.dp) + 56.dp
                        HorizontalDivider(
                            modifier = Modifier.offset(y = nextUnitY),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        )
                    }

                    // Identify Free Columns and merge adjacent free columns sharing the same stretch/reason
                    data class FreeRange(
                        val startIdx: Int,
                        val endIdx: Int,
                        val kind: FreeKind,
                        val title: String,
                        val endDate: LocalDate,
                        val isMultiDay: Boolean,
                    )

                    val freeRanges = mutableListOf<FreeRange>()
                    var idx = 0
                    while (idx < 5) {
                        val d = weekDates[idx]
                        val plan = plans[idx]
                        val fDay = freeCalendar.freeDay(d)
                        val isFreeCol = (fDay != null && fDay.kind != FreeKind.WOCHENENDE) || plan.wholeDayFree

                        if (isFreeCol) {
                            val stretch = freeCalendar.stretchAt(d)
                            val title = stretch?.title ?: plan.wholeDayFreeReason ?: fDay?.name ?: "Frei"
                            val kind = stretch?.kind ?: (if (plan.wholeDayFree) FreeKind.SCHULFREI else fDay?.kind ?: FreeKind.SCHULFREI)

                            var endIdx = idx
                            while (endIdx + 1 < 5) {
                                val nextD = weekDates[endIdx + 1]
                                val nextPlan = plans[endIdx + 1]
                                val nextFDay = freeCalendar.freeDay(nextD)
                                val nextStretch = freeCalendar.stretchAt(nextD)
                                val isNextFree = (nextFDay != null && nextFDay.kind != FreeKind.WOCHENENDE) || nextPlan.wholeDayFree
                                val nextTitle = nextStretch?.title ?: nextPlan.wholeDayFreeReason ?: nextFDay?.name ?: "Frei"

                                if (isNextFree && (stretch != null && nextStretch == stretch || title == nextTitle)) {
                                    endIdx++
                                } else {
                                    break
                                }
                            }
                            val lastFreeDate = stretch?.end ?: weekDates[endIdx]
                            val isMulti = endIdx > idx || (stretch != null && stretch.length > 1)
                            freeRanges.add(FreeRange(idx, endIdx, kind, title, lastFreeDate, isMulti))
                            idx = endIdx + 1
                        } else {
                            idx++
                        }
                    }

                    // Render Merged Free Tiles
                    freeRanges.forEach { fr ->
                        val tileX = columnWidth * fr.startIdx
                        val tileW = columnWidth * (fr.endIdx - fr.startIdx + 1)
                        val isFreeOrSchoolFree = fr.kind == FreeKind.SCHULFREI
                        val containerColor = if (isFreeOrSchoolFree) statusColors.free.container else statusColors.holiday.container
                        val accentColor = if (isFreeOrSchoolFree) statusColors.free.accent else statusColors.holiday.accent

                        val icon = when (fr.kind) {
                            FreeKind.FERIEN -> Icons.Outlined.BeachAccess
                            FreeKind.FEIERTAG -> Icons.Outlined.Celebration
                            FreeKind.BRUECKENTAG -> Icons.Outlined.Link
                            FreeKind.SCHULFREI -> Icons.Outlined.EventBusy
                            FreeKind.WOCHENENDE -> Icons.Outlined.Weekend
                        }

                        val displayTitle = if (tileW < 120.dp && fr.kind != FreeKind.FERIEN) {
                            if (fr.title.contains("Pädagogischer Tag", ignoreCase = true)) {
                                fr.title.replace("Pädagogischer Tag", "Päd. Tag", ignoreCase = true)
                            } else {
                                fr.title
                            }
                        } else {
                            fr.title
                        }

                        Surface(
                            color = containerColor,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .offset(x = tileX, y = 0.dp)
                                .size(width = tileW, height = geometry.totalHeight)
                                .padding(2.dp),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(32.dp),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = displayTitle,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        hyphens = Hyphens.Auto,
                                        localeList = LocaleList("de"),
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFreeOrSchoolFree) statusColors.free.onContainer else statusColors.holiday.onContainer,
                                    textAlign = TextAlign.Center,
                                    softWrap = true,
                                )
                                if (fr.isMultiDay) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "bis ${formatGermanDay(fr.endDate)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isFreeOrSchoolFree) statusColors.free.onContainer.copy(alpha = 0.8f) else statusColors.holiday.onContainer.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }

                    // Render Cells for non-free columns
                    weekDates.forEachIndexed { colIdx, date ->
                        val isColFree = freeRanges.any { colIdx in it.startIdx..it.endIdx }
                        if (!isColFree) {
                            val plan = plans[colIdx]
                            val colLeft = columnWidth * colIdx
                            val placedList = plan.placed

                            if (placedList.isNotEmpty()) {
                                val n = placedList.size
                                val parent = IntArray(n) { it }
                                fun find(i: Int): Int {
                                    var r = i
                                    while (parent[r] != r) r = parent[r]
                                    var c = i
                                    while (parent[c] != r) { val next = parent[c]; parent[c] = r; c = next }
                                    return r
                                }
                                fun union(i: Int, j: Int) { parent[find(i)] = find(j) }
                                for (i in 0 until n) {
                                    for (j in i + 1 until n) {
                                        val a = placedList[i]
                                        val b = placedList[j]
                                        if (a.firstUnit <= b.lastUnit && b.firstUnit <= a.lastUnit) {
                                            union(i, j)
                                        }
                                    }
                                }

                                val clusters = placedList.groupBy { find(placedList.indexOf(it)) }.values

                                clusters.forEach { cluster ->
                                    val cancelledEntries = cluster.filter { isLessonCancelled(it) }
                                    val effectiveEntries = cluster.filter { !isLessonCancelled(it) }

                                    if (effectiveEntries.isNotEmpty() && cancelledEntries.isNotEmpty()) {
                                        // 1) Draw cancelled entries as 6dp hatched strips at right edge of column
                                        val stripW = 6.dp
                                        val stripX = colLeft + columnWidth - stripW
                                        cancelledEntries.forEach { cancelled ->
                                            val topY = geometry.unitY[cancelled.firstUnit] ?: 0.dp
                                            val bottomUnitY = (geometry.unitY[cancelled.lastUnit] ?: 0.dp) + 56.dp
                                            val cellH = (bottomUnitY - topY - 2.dp).coerceAtLeast(20.dp)
                                            CancelledStrip(
                                                placed = cancelled,
                                                onClick = { onLessonClick(cancelled.view, plan) },
                                                modifier = Modifier
                                                    .offset(x = stripX, y = topY)
                                                    .size(width = stripW, height = cellH),
                                            )
                                        }

                                        // 2) Lanes ONLY among effective entries
                                        val availableW = columnWidth - stripW
                                        val maxEffectiveLane = effectiveEntries.maxOfOrNull { it.lane } ?: 0
                                        val effectiveLaneCount = maxEffectiveLane + 1

                                        if (effectiveLaneCount > 2) {
                                            val lane0Entries = effectiveEntries.filter { it.lane == 0 }
                                            val hiddenCount = cluster.size - lane0Entries.size
                                            lane0Entries.forEach { entry ->
                                                val topY = geometry.unitY[entry.firstUnit] ?: 0.dp
                                                val bottomUnitY = (geometry.unitY[entry.lastUnit] ?: 0.dp) + 56.dp
                                                val cellH = (bottomUnitY - topY - 2.dp).coerceAtLeast(20.dp)
                                                WeekLessonCell(
                                                    placed = entry,
                                                    onClick = { onClusterClick(cluster.map { it.view }, plan) },
                                                    badgeCount = hiddenCount,
                                                    modifier = Modifier
                                                        .offset(x = colLeft, y = topY)
                                                        .size(width = availableW, height = cellH),
                                                )
                                            }
                                        } else {
                                            val laneW = availableW / effectiveLaneCount
                                            effectiveEntries.forEach { entry ->
                                                val topY = geometry.unitY[entry.firstUnit] ?: 0.dp
                                                val bottomUnitY = (geometry.unitY[entry.lastUnit] ?: 0.dp) + 56.dp
                                                val cellH = (bottomUnitY - topY - 2.dp).coerceAtLeast(20.dp)
                                                val laneX = colLeft + (laneW * entry.lane)
                                                WeekLessonCell(
                                                    placed = entry,
                                                    onClick = { onLessonClick(entry.view, plan) },
                                                    modifier = Modifier
                                                        .offset(x = laneX, y = topY)
                                                        .size(width = laneW, height = cellH),
                                                )
                                            }
                                        }
                                    } else {
                                        // Either only effective entries OR only cancelled entries
                                        val entries = effectiveEntries.ifEmpty { cancelledEntries }
                                        val maxLane = entries.maxOfOrNull { it.lane } ?: 0
                                        val laneCount = maxLane + 1

                                        if (laneCount > 2) {
                                            val lane0Entries = entries.filter { it.lane == 0 }
                                            val hiddenCount = entries.size - lane0Entries.size
                                            lane0Entries.forEach { entry ->
                                                val topY = geometry.unitY[entry.firstUnit] ?: 0.dp
                                                val bottomUnitY = (geometry.unitY[entry.lastUnit] ?: 0.dp) + 56.dp
                                                val cellH = (bottomUnitY - topY - 2.dp).coerceAtLeast(20.dp)
                                                WeekLessonCell(
                                                    placed = entry,
                                                    onClick = { onClusterClick(entries.map { it.view }, plan) },
                                                    badgeCount = hiddenCount,
                                                    modifier = Modifier
                                                        .offset(x = colLeft, y = topY)
                                                        .size(width = columnWidth, height = cellH),
                                                )
                                            }
                                        } else {
                                            val laneW = columnWidth / laneCount
                                            entries.forEach { entry ->
                                                val topY = geometry.unitY[entry.firstUnit] ?: 0.dp
                                                val bottomUnitY = (geometry.unitY[entry.lastUnit] ?: 0.dp) + 56.dp
                                                val cellH = (bottomUnitY - topY - 2.dp).coerceAtLeast(20.dp)
                                                val laneX = colLeft + (laneW * entry.lane)
                                                WeekLessonCell(
                                                    placed = entry,
                                                    onClick = { onLessonClick(entry.view, plan) },
                                                    modifier = Modifier
                                                        .offset(x = laneX, y = topY)
                                                        .size(width = laneW, height = cellH),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Now-line: on current week, today's column + rail
                    val isCurrentWeek = monday == currentMonday
                    val todayColIdx = weekDates.indexOf(todayDate)

                    if (isCurrentWeek && todayColIdx in 0..4) {
                        val firstUnit = sortedGrid.firstOrNull()
                        val lastUnit = sortedGrid.lastOrNull()

                        if (firstUnit != null && lastUnit != null && currentTime in firstUnit.start..lastUnit.end) {
                            val nowY = calculateNowLineY(currentTime, sortedGrid, breaks, geometry.unitY, geometry.breakY, geometry.breakHeight)
                            if (nowY != null) {
                                val isColFree = freeRanges.any { todayColIdx in it.startIdx..it.endIdx }

                                // Dot at rail edge
                                Box(
                                    modifier = Modifier
                                        .offset(x = (-4).dp, y = nowY - 4.dp)
                                        .size(8.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                )

                                if (!isColFree) {
                                    val todayColLeft = columnWidth * todayColIdx

                                    // Line across rail
                                    Box(
                                        modifier = Modifier
                                            .offset(x = (-36).dp, y = nowY - 1.dp)
                                            .size(width = 36.dp, height = 2.dp)
                                            .background(MaterialTheme.colorScheme.primary),
                                    )

                                    // Line across today's column
                                    Box(
                                        modifier = Modifier
                                            .offset(x = todayColLeft, y = nowY - 1.dp)
                                            .size(width = columnWidth, height = 2.dp)
                                            .background(MaterialTheme.colorScheme.primary),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Legend row under the grid (scrolls with it)
            Spacer(modifier = Modifier.height(16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                StatusPill("Frei", statusColors.free)
                StatusPill("Foyer", statusColors.foyer)
                StatusPill("Klausur", statusColors.exam)
                StatusPill("Änderung", statusColors.changed)
                StatusPill("Prognose", containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun calculateNowLineY(
    now: LocalTime,
    grid: List<TimeUnit>,
    breaks: List<GridBreak>,
    unitY: Map<Int, Dp>,
    breakY: Map<Int, Dp>,
    breakHeight: Map<Int, Dp>,
): Dp? {
    val unit = grid.firstOrNull { it.start <= now && now <= it.end }
    if (unit != null) {
        val y = unitY[unit.index] ?: return null
        val totalSec = ChronoUnit.SECONDS.between(unit.start, unit.end).toFloat()
        val elapsedSec = ChronoUnit.SECONDS.between(unit.start, now).toFloat()
        val frac = if (totalSec > 0f) (elapsedSec / totalSec).coerceIn(0f, 1f) else 0f
        return y + 56.dp * frac
    }

    val brk = breaks.firstOrNull { it.start <= now && now <= it.end }
    if (brk != null) {
        val by = breakY[brk.afterUnit] ?: return null
        val bh = breakHeight[brk.afterUnit] ?: return null
        return by + bh / 2
    }

    return null
}

private fun isLessonCancelled(placed: PlacedLesson): Boolean {
    val view = placed.view
    return view.kind == LessonKind.CANCELLED ||
        LessonKind.CANCELLED in view.badges ||
        view.lesson.status == io.github.omeg4dev.greatuntis.model.LessonStatus.CANCELLED
}

@Composable
fun CancelledStrip(
    placed: PlacedLesson,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusColors = MaterialTheme.statusColors
    val hatchColor = statusColors.free.accent

    Surface(
        color = statusColors.free.container,
        shape = RoundedCornerShape(2.dp),
        modifier = modifier
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(2.dp))
            .drawBehind {
                clipRect {
                    val step = 4.dp.toPx()
                    val strokeWidth = 1.5.dp.toPx()
                    val maxDim = size.width + size.height
                    var offset = -size.height
                    while (offset < maxDim) {
                        drawLine(
                            color = hatchColor,
                            start = Offset(offset, 0f),
                            end = Offset(offset + size.height, size.height),
                            strokeWidth = strokeWidth,
                        )
                        offset += step
                    }
                }
            }
            .clickable(onClick = onClick),
    ) {
    }
}

@Composable
fun WeekLessonCell(
    placed: PlacedLesson,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
) {
    val darkTheme = isSystemInDarkTheme()
    val statusColors = MaterialTheme.statusColors
    val colorScheme = MaterialTheme.colorScheme
    val view = placed.view

    val isCancelled = view.kind == LessonKind.CANCELLED || LessonKind.CANCELLED in view.badges
    val isFoyer = view.kind == LessonKind.FOYER || LessonKind.FOYER in view.badges
    val isExam = view.kind == LessonKind.EXAM || LessonKind.EXAM in view.badges
    val isChanged = LessonKind.ROOM_CHANGED in view.badges ||
        LessonKind.TEACHER_CHANGED in view.badges ||
        LessonKind.SUBSTITUTION in view.badges
    val isPredicted = view.kind == LessonKind.PREDICTED || LessonKind.PREDICTED in view.badges

    val baseColor = when {
        isCancelled -> statusColors.free.container
        isFoyer -> statusColors.foyer.container
        isExam -> statusColors.exam.container
        isPredicted -> Color.Transparent
        else -> subjectColor(view.shortTitle, darkTheme, primary = colorScheme.primary)
    }

    val textColor = when {
        isCancelled -> statusColors.free.onContainer
        isFoyer -> statusColors.foyer.onContainer
        isExam -> statusColors.exam.onContainer
        isPredicted -> colorScheme.onSurface.copy(alpha = 0.7f)
        else -> colorScheme.onSurface
    }

    val dashedBorderModifier = if (isPredicted) {
        val outlineColor = colorScheme.outline
        Modifier.drawBehind {
            drawRoundRect(
                color = outlineColor,
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                ),
            )
        }
    } else {
        Modifier
    }

    val hatchModifier = if (isCancelled) {
        val hatchColor = statusColors.free.accent.copy(alpha = 0.25f)
        Modifier.drawBehind {
            clipRect {
                val step = 6.dp.toPx()
                val strokeWidth = 1.dp.toPx()
                val maxDim = size.width + size.height
                var offset = -size.height
                while (offset < maxDim) {
                    drawLine(
                        color = hatchColor,
                        start = Offset(offset, 0f),
                        end = Offset(offset + size.height, size.height),
                        strokeWidth = strokeWidth,
                    )
                    offset += step
                }
            }
        }
    } else {
        Modifier
    }

    Surface(
        color = baseColor,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
            .padding(1.dp)
            .clip(MaterialTheme.shapes.small)
            .alpha(if (isPredicted) 0.7f else 1f)
            .then(dashedBorderModifier)
            .then(hatchModifier)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
        ) {
            // Top-right 6dp dot for changes
            if (isChanged && !isCancelled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(6.dp)
                        .background(statusColors.changed.accent, CircleShape),
                )
            }

            if (badgeCount > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(1.dp),
                ) {
                    Text(
                        text = "+$badgeCount",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = view.shortTitle,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None,
                )
                val secondaryText = when {
                    isCancelled -> "frei"
                    isFoyer -> "Foyer"
                    isExam -> "Klausur"
                    view.roomText.isNotEmpty() -> view.roomText
                    else -> ""
                }
                if (secondaryText.isNotEmpty()) {
                    Text(
                        text = secondaryText,
                        style = MaterialTheme.typography.labelSmall.tabularNumbers().copy(fontSize = 10.sp),
                        color = textColor.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
