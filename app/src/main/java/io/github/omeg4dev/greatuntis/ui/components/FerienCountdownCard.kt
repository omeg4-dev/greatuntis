package io.github.omeg4dev.greatuntis.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.omeg4dev.greatuntis.calendar.FreeCalendar
import io.github.omeg4dev.greatuntis.ui.theme.statusColors
import io.github.omeg4dev.greatuntis.ui.theme.tabularNumbers
import java.time.LocalDate

@Composable
fun FerienCountdownCard(
    calendar: FreeCalendar,
    today: LocalDate,
    onNavigateToFree: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nextFerien = calendar.nextFerien(today) ?: return
    val schoolDays = calendar.schoolDaysBetween(today, nextFerien.start)
    val statusColors = MaterialTheme.statusColors

    Surface(
        color = statusColors.holiday.container,
        contentColor = statusColors.holiday.onContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateToFree),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Start,
            ) {
                Text(
                    text = "Noch ",
                    style = MaterialTheme.typography.titleLarge,
                    color = statusColors.holiday.onContainer,
                )
                Text(
                    text = "$schoolDays",
                    style = MaterialTheme.typography.displaySmall.tabularNumbers(),
                    fontWeight = FontWeight.Bold,
                    color = statusColors.holiday.accent,
                )
                val unitText = if (schoolDays == 1) " Schultag" else " Schultage"
                Text(
                    text = unitText,
                    style = MaterialTheme.typography.titleLarge,
                    color = statusColors.holiday.onContainer,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "bis ${nextFerien.title}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = statusColors.holiday.onContainer,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = nextFerien.label,
                style = MaterialTheme.typography.bodyMedium,
                color = statusColors.holiday.onContainer.copy(alpha = 0.8f),
            )
        }
    }
}
