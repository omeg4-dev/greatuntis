package io.github.omeg4dev.greatuntis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.omeg4dev.greatuntis.calendar.FreeCalendar
import io.github.omeg4dev.greatuntis.calendar.FreeDay
import io.github.omeg4dev.greatuntis.calendar.FreeKind
import io.github.omeg4dev.greatuntis.calendar.FreeStretch
import io.github.omeg4dev.greatuntis.ui.theme.statusColors
import io.github.omeg4dev.greatuntis.ui.today.formatGermanDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FreeDayHero(
    freeDay: FreeDay,
    stretch: FreeStretch?,
    calendar: FreeCalendar,
    today: LocalDate,
    cancelledSummary: String? = null,
    modifier: Modifier = Modifier,
) {
    val statusColors = MaterialTheme.statusColors

    val isFreeOrWeekend = freeDay.kind == FreeKind.SCHULFREI || freeDay.kind == FreeKind.WOCHENENDE
    val containerColor = if (isFreeOrWeekend) statusColors.free.container else statusColors.holiday.container
    val accentColor = if (isFreeOrWeekend) statusColors.free.accent else statusColors.holiday.accent

    val (icon, shape: Shape) = when (freeDay.kind) {
        FreeKind.FERIEN -> Icons.Outlined.BeachAccess to MaterialShapes.Sunny.toShape()
        FreeKind.FEIERTAG -> Icons.Outlined.Celebration to MaterialShapes.Cookie9Sided.toShape()
        FreeKind.BRUECKENTAG -> Icons.Outlined.Link to MaterialShapes.Clover4Leaf.toShape()
        FreeKind.SCHULFREI -> Icons.Outlined.EventBusy to MaterialShapes.Pill.toShape()
        FreeKind.WOCHENENDE -> Icons.Outlined.Weekend to MaterialShapes.Circle.toShape()
    }

    val subtitle = when {
        freeDay.kind == FreeKind.WOCHENENDE -> {
            var nextSchool = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            while (calendar.freeDay(nextSchool) != null) {
                nextSchool = nextSchool.plusDays(1)
            }
            "Schule wieder ${formatGermanDay(nextSchool)}"
        }
        stretch != null -> {
            val daysRemaining = (ChronoUnit.DAYS.between(today, stretch.end) + 1).coerceAtLeast(1)
            var nextSchool = stretch.end.plusDays(1)
            while (calendar.freeDay(nextSchool) != null) {
                nextSchool = nextSchool.plusDays(1)
            }
            val tagText = if (daysRemaining == 1L) "1 Tag" else "$daysRemaining Tage"
            "noch $tagText frei · Schule wieder ${formatGermanDay(nextSchool)}"
        }
        else -> {
            var nextSchool = today.plusDays(1)
            while (calendar.freeDay(nextSchool) != null) {
                nextSchool = nextSchool.plusDays(1)
            }
            "Schule wieder ${formatGermanDay(nextSchool)}"
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(shape)
                .background(containerColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(56.dp),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = freeDay.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (!cancelledSummary.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = cancelledSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
        }
    }
}
