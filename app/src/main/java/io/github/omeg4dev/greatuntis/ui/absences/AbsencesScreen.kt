package io.github.omeg4dev.greatuntis.ui.absences

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.omeg4dev.greatuntis.R
import io.github.omeg4dev.greatuntis.model.Absence
import io.github.omeg4dev.greatuntis.model.AbsenceCategory
import io.github.omeg4dev.greatuntis.model.AbsenceStatsResult
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.Settings
import io.github.omeg4dev.greatuntis.model.TimeUnit
import io.github.omeg4dev.greatuntis.ui.fake.FakeData
import io.github.omeg4dev.greatuntis.ui.theme.GreatUntisTheme
import io.github.omeg4dev.greatuntis.ui.theme.statusColors
import io.github.omeg4dev.greatuntis.ui.theme.tabularNumbers
import io.github.omeg4dev.greatuntis.ui.today.formatGermanDay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AbsencesScreen(
    days: List<Day>,
    absences: List<Absence>,
    timeGrid: List<TimeUnit>,
    settings: Settings,
    computeStats: (days: List<Day>, absences: List<Absence>, grid: List<TimeUnit>, foyerPrefix: String) -> AbsenceStatsResult,
    clock: () -> LocalDateTime,
    modifier: Modifier = Modifier,
) {
    val stats = remember(days, absences, timeGrid, settings.foyerPrefix) {
        computeStats(days, absences, timeGrid, settings.foyerPrefix)
    }

    val today = clock().toLocalDate()
    val statusColors = MaterialTheme.statusColors

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Warning card (only if openAbsences > 0, error container)
        if (stats.openAbsences > 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = pluralStringResource(
                                    R.plurals.absences_open_count,
                                    stats.openAbsences,
                                    stats.openAbsences,
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        val openList = absences.filter { it.category == AbsenceCategory.OPEN }
                        openList.forEach { openItem ->
                            Spacer(modifier = Modifier.height(8.dp))
                            val dateStr = formatGermanDay(openItem.start.toLocalDate())
                            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                            val timeStr = "${openItem.start.format(timeFormatter)}–${openItem.end.format(timeFormatter)}"
                            val daysOpen = ChronoUnit.DAYS.between(openItem.start.toLocalDate(), today) + 1
                            val daysOpenStr = pluralStringResource(
                                R.plurals.absence_open_days,
                                daysOpen.toInt(),
                                daysOpen.toInt(),
                            )

                            Text(
                                text = "$dateStr · $timeStr · ${openItem.reason} · $daysOpenStr",
                                style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                            )
                        }
                    }
                }
            }
        }

        // 2. KPI row (2x2 grid of tiles)
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    KpiTile(
                        label = stringResource(R.string.kpi_missed_units),
                        value = "${stats.missedUnits}",
                        modifier = Modifier.weight(1f),
                    )
                    KpiTile(
                        label = stringResource(R.string.kpi_missed_days),
                        value = "${stats.missedDays}",
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val quote = if (stats.heldUnits > 0) {
                        String.format(Locale.GERMAN, "%.1f %%", (stats.missedUnits.toFloat() / stats.heldUnits) * 100)
                    } else {
                        "0,0 %"
                    }
                    KpiTile(
                        label = stringResource(R.string.kpi_quote),
                        value = quote,
                        modifier = Modifier.weight(1f),
                    )
                    KpiTile(
                        label = stringResource(R.string.kpi_lateness),
                        value = "${stats.lateCount}",
                        subLabel = stringResource(R.string.kpi_lateness_total, stats.lateMinutes),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // 3. Entschuldigung: horizontal stacked bar (12 dp high, rounded)
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.section_excuses),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val catExcused = stats.missedByCategory[AbsenceCategory.EXCUSED] ?: 0
                    val catUnexcused = stats.missedByCategory[AbsenceCategory.UNEXCUSED] ?: 0
                    val catNotCounted = stats.missedByCategory[AbsenceCategory.NOT_COUNTED] ?: 0
                    val catOpen = stats.missedByCategory[AbsenceCategory.OPEN] ?: 0
                    val totalCat = catExcused + catUnexcused + catNotCounted + catOpen

                    val colExcused = MaterialTheme.colorScheme.primary
                    val colUnexcused = MaterialTheme.colorScheme.error
                    val colNotCounted = MaterialTheme.colorScheme.outline
                    val colOpen = statusColors.foyer.accent

                    val cdExcuses = "Entschuldigt: $catExcused, Unentschuldigt: $catUnexcused, Zählt nicht: $catNotCounted, Offen: $catOpen"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .semantics { contentDescription = cdExcuses },
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (totalCat > 0) {
                                var curX = 0f
                                val widthTotal = size.width

                                val wExcused = (catExcused.toFloat() / totalCat) * widthTotal
                                val wUnexcused = (catUnexcused.toFloat() / totalCat) * widthTotal
                                val wNotCounted = (catNotCounted.toFloat() / totalCat) * widthTotal
                                val wOpen = (catOpen.toFloat() / totalCat) * widthTotal

                                if (wExcused > 0) {
                                    drawRect(colExcused, topLeft = Offset(curX, 0f), size = Size(wExcused, size.height))
                                    curX += wExcused
                                }
                                if (wUnexcused > 0) {
                                    drawRect(colUnexcused, topLeft = Offset(curX, 0f), size = Size(wUnexcused, size.height))
                                    curX += wUnexcused
                                }
                                if (wNotCounted > 0) {
                                    drawRect(colNotCounted, topLeft = Offset(curX, 0f), size = Size(wNotCounted, size.height))
                                    curX += wNotCounted
                                }
                                if (wOpen > 0) {
                                    drawRect(colOpen, topLeft = Offset(curX, 0f), size = Size(wOpen, size.height))
                                    curX += wOpen
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Legend chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        CategoryLegend(stringResource(R.string.category_excused), catExcused, colExcused)
                        CategoryLegend(stringResource(R.string.category_unexcused), catUnexcused, colUnexcused)
                        CategoryLegend(stringResource(R.string.category_not_counted), catNotCounted, colNotCounted)
                        CategoryLegend(stringResource(R.string.category_open), catOpen, colOpen)
                    }
                }
            }
        }

        // 4. Nach Fach: sorted by missed/held descending
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.section_by_subject),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val activeSubjects = stats.heldBySubject.keys
                        .filter { (stats.heldBySubject[it] ?: 0) > 0 }
                        .sortedByDescending {
                            (stats.missedBySubject[it] ?: 0).toFloat() / (stats.heldBySubject[it] ?: 1)
                        }

                    activeSubjects.forEach { subject ->
                        val held = stats.heldBySubject[subject] ?: 0
                        val missed = stats.missedBySubject[subject] ?: 0
                        val pct = if (held > 0) (missed.toFloat() / held) * 100 else 0f

                        SubjectRow(
                            subject = subject,
                            missed = missed,
                            held = held,
                            percentage = pct,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // 5. Verlauf: weekly bar chart (Canvas) of missedByWeek
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.section_trend),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    WeeklyTrendChart(
                        missedByWeek = stats.missedByWeek,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                    )
                }
            }
        }

        // 6. Wochentage & Stunden (side by side on wide, stacked on narrow)
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 500.dp
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        WeekdayChartCard(
                            missedByWeekday = stats.missedByWeekday,
                            modifier = Modifier.weight(1f),
                        )
                        PeriodChartCard(
                            missedByPeriod = stats.missedByPeriod,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        WeekdayChartCard(
                            missedByWeekday = stats.missedByWeekday,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        PeriodChartCard(
                            missedByPeriod = stats.missedByPeriod,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        // 7. Entfall & Foyer
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.section_cancelled_foyer),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val sumCancelled = stats.cancelledBySubject.values.sum()
                    val sumFoyer = stats.foyerBySubject.values.sum()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        KpiTile(
                            label = stringResource(R.string.kpi_cancelled_units),
                            value = "$sumCancelled",
                            modifier = Modifier.weight(1f),
                        )
                        KpiTile(
                            label = stringResource(R.string.kpi_foyer_units),
                            value = "$sumFoyer",
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Cancelled top 5
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.badge_cancelled),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = statusColors.free.accent,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val topCancelled = stats.cancelledBySubject.entries
                                .sortedByDescending { it.value }
                                .take(5)
                            topCancelled.forEach { entry ->
                                Text(
                                    text = "${entry.key} · " + stringResource(R.string.units_count, entry.value),
                                    style = MaterialTheme.typography.bodySmall.tabularNumbers(),
                                    modifier = Modifier.padding(vertical = 2.dp),
                                )
                            }
                        }

                        // Foyer top 5
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.badge_foyer),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = statusColors.foyer.accent,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val topFoyer = stats.foyerBySubject.entries
                                .sortedByDescending { it.value }
                                .take(5)
                            topFoyer.forEach { entry ->
                                Text(
                                    text = "${entry.key} · " + stringResource(R.string.units_count, entry.value),
                                    style = MaterialTheme.typography.bodySmall.tabularNumbers(),
                                    modifier = Modifier.padding(vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // 8. Alle Fehlzeiten
        item {
            Text(
                text = stringResource(R.string.section_all_absences),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        val sortedAbsences = absences.sortedByDescending { it.start }
        items(sortedAbsences) { item ->
            AbsenceItemCard(absence = item)
        }
    }
}

@Composable
fun KpiTile(
    label: String,
    value: String,
    subLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall.tabularNumbers(),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subLabel != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.bodySmall.tabularNumbers(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun CategoryLegend(
    label: String,
    count: Int,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label ($count)",
            style = MaterialTheme.typography.labelSmall.tabularNumbers(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SubjectRow(
    subject: String,
    missed: Int,
    held: Int,
    percentage: Float,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = subject,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "$missed / $held · " + String.format(Locale.GERMAN, "%.1f %%", percentage),
                style = MaterialTheme.typography.bodySmall.tabularNumbers(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        val progress = if (held > 0) (missed.toFloat() / held).coerceIn(0f, 1f) else 0f
        val primaryColor = MaterialTheme.colorScheme.primary
        val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
        ) {
            drawRect(trackColor, size = size)
            drawRect(primaryColor, size = Size(size.width * progress, size.height))
        }
    }
}

@Composable
fun WeeklyTrendChart(
    missedByWeek: Map<LocalDate, Int>,
    modifier: Modifier = Modifier,
) {
    val sortedWeeks = missedByWeek.keys.sorted()
    val maxVal = maxOf(4, missedByWeek.values.maxOrNull() ?: 1)
    val barColor = MaterialTheme.colorScheme.primary
    val tickColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)

    val talkBackDesc = "Verlauf der Fehlzeiten über " + sortedWeeks.size + " Wochen"

    Canvas(
        modifier = modifier.semantics { contentDescription = talkBackDesc },
    ) {
        if (sortedWeeks.isEmpty()) return@Canvas

        val w = size.width
        val textHeight = 16.dp.toPx()
        val h = size.height - textHeight
        val step = w / sortedWeeks.size
        val barWidth = (step * 0.5f).coerceIn(4f, 20f)

        sortedWeeks.forEachIndexed { index, monday ->
            val count = missedByWeek[monday] ?: 0
            val x = index * step + step / 2f

            if (count > 0) {
                val barH = (count.toFloat() / maxVal) * (h - 8.dp.toPx())
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x - barWidth / 2f, h - barH),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = Fill,
                )
            } else {
                // Baseline tick
                drawLine(
                    color = tickColor,
                    start = Offset(x - barWidth / 2f, h - 2.dp.toPx()),
                    end = Offset(x + barWidth / 2f, h - 2.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                )
            }

            // Label every 4th week as "KW 38"
            if (index % 4 == 0) {
                val kw = monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                val labelLayout = textMeasurer.measure(
                    text = "KW $kw",
                    style = labelStyle.copy(color = labelColor),
                )
                val labelX = (x - labelLayout.size.width / 2f).coerceIn(0f, w - labelLayout.size.width)
                drawText(
                    textLayoutResult = labelLayout,
                    topLeft = Offset(labelX, h + 2.dp.toPx()),
                )
            }
        }
    }
}

@Composable
fun WeekdayChartCard(
    missedByWeekday: Map<Int, Int>,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.large,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.section_weekdays),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))

            val days = listOf(1, 2, 3, 4, 5)
            val dayNames = listOf("Mo", "Di", "Mi", "Do", "Fr")
            val counts = days.map { missedByWeekday[it] ?: 0 }
            val maxCount = maxOf(1, counts.maxOrNull() ?: 1)
            val barColor = MaterialTheme.colorScheme.primary

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                days.forEachIndexed { index, d ->
                    val c = missedByWeekday[d] ?: 0
                    val fraction = c.toFloat() / maxCount
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (c > 0) {
                            Text(
                                text = "$c",
                                style = MaterialTheme.typography.labelSmall.tabularNumbers().copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                            )
                        } else {
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(maxOf(4.dp, (44 * fraction).dp))
                                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                .background(barColor),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dayNames[index],
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PeriodChartCard(
    missedByPeriod: Map<Int, Int>,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.large,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.section_periods),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))

            val periods = (1..9).toList()
            val counts = periods.map { missedByPeriod[it] ?: 0 }
            val maxCount = maxOf(1, counts.maxOrNull() ?: 1)
            val barColor = MaterialTheme.colorScheme.tertiary

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                periods.forEach { p ->
                    val c = missedByPeriod[p] ?: 0
                    val fraction = c.toFloat() / maxCount
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (c > 0) {
                            Text(
                                text = "$c",
                                style = MaterialTheme.typography.labelSmall.tabularNumbers().copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                            )
                        } else {
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(10.dp)
                                .height(maxOf(4.dp, (44 * fraction).dp))
                                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                .background(barColor),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$p",
                            style = MaterialTheme.typography.labelSmall.tabularNumbers(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AbsenceItemCard(
    absence: Absence,
    modifier: Modifier = Modifier,
) {
    val statusColors = MaterialTheme.statusColors
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val isSingleDay = absence.start.toLocalDate() == absence.end.toLocalDate()
    val dateDisplay = if (isSingleDay) {
        val dateStr = formatGermanDay(absence.start.toLocalDate())
        val timeStr = "${absence.start.format(timeFormatter)}–${absence.end.format(timeFormatter)}"
        "$dateStr · $timeStr"
    } else {
        val s = absence.start.format(DateTimeFormatter.ofPattern("dd."))
        val e = absence.end.format(DateTimeFormatter.ofPattern("dd.MM."))
        "$s–$e"
    }

    val (catLabel, catBg, catFg) = when (absence.category) {
        AbsenceCategory.EXCUSED -> Triple(
            stringResource(R.string.category_excused),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        AbsenceCategory.UNEXCUSED -> Triple(
            stringResource(R.string.category_unexcused),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
        AbsenceCategory.NOT_COUNTED -> Triple(
            stringResource(R.string.category_not_counted),
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AbsenceCategory.OPEN -> Triple(
            stringResource(R.string.category_open),
            statusColors.foyer.container,
            statusColors.foyer.onContainer,
        )
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateDisplay,
                    style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                val reasonText = if (absence.text.isNotBlank()) {
                    "${absence.reason} · ${absence.text}"
                } else {
                    absence.reason
                }
                Text(
                    text = reasonText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Surface(
                color = catBg,
                contentColor = catFg,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = catLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AbsencesScreenPreview() {
    GreatUntisTheme {
        AbsencesScreen(
            days = FakeData.createDays(),
            absences = FakeData.absences,
            timeGrid = FakeData.timeGrid,
            settings = Settings(),
            computeStats = { _, _, _, _ -> FakeData.fakeAbsenceStatsResult },
            clock = { LocalDateTime.of(2026, 11, 3, 10, 50) },
        )
    }
}
