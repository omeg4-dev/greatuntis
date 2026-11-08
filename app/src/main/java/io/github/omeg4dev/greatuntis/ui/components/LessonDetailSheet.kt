package io.github.omeg4dev.greatuntis.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.omeg4dev.greatuntis.timetable.DayPlan
import io.github.omeg4dev.greatuntis.timetable.LessonKind
import io.github.omeg4dev.greatuntis.timetable.LessonView
import io.github.omeg4dev.greatuntis.ui.theme.statusColors
import io.github.omeg4dev.greatuntis.ui.theme.tabularNumbers
import io.github.omeg4dev.greatuntis.ui.today.formatGermanDay
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LessonDetailSheet(
    view: LessonView,
    plan: DayPlan?,
    modifier: Modifier = Modifier,
) {
    val statusColors = MaterialTheme.statusColors
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val placed = plan?.placed?.firstOrNull { it.view.lesson.key == view.lesson.key }
    val unitText = if (placed != null) {
        if (placed.firstUnit == placed.lastUnit) "${placed.firstUnit}. Std" else "${placed.firstUnit}.–${placed.lastUnit}. Std"
    } else {
        ""
    }

    val subtitleParts = listOfNotNull(
        formatGermanDay(view.lesson.date),
        unitText.takeIf { it.isNotEmpty() },
        "${view.lesson.start.format(timeFormatter)}–${view.lesson.end.format(timeFormatter)}",
    )
    val subtitle = subtitleParts.joinToString(" · ")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = view.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Kind pills
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (LessonKind.CANCELLED in view.badges) {
                StatusPill("frei", statusColors.free)
            }
            if (LessonKind.FOYER in view.badges) {
                StatusPill("Foyer", statusColors.foyer)
            }
            if (LessonKind.EXAM in view.badges) {
                StatusPill("Klausur", statusColors.exam)
            }
            if (LessonKind.ROOM_CHANGED in view.badges) {
                StatusPill("Raum", statusColors.changed)
            }
            if (LessonKind.CANCELLED !in view.badges && (LessonKind.TEACHER_CHANGED in view.badges || LessonKind.SUBSTITUTION in view.badges)) {
                StatusPill("Vertretung", statusColors.changed)
            }
            if (LessonKind.EVENT in view.badges) {
                StatusPill("Veranstaltung", containerColor = MaterialTheme.colorScheme.surfaceContainerHighest, contentColor = MaterialTheme.colorScheme.onSurface)
            }
            if (LessonKind.PREDICTED in view.badges) {
                StatusPill("Prognose", containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Details rows
        // 1. Raum
        val roomSlot = view.rooms.firstOrNull()
        if (roomSlot != null) {
            val roomAnnotated = buildAnnotatedString {
                when {
                    view.isFoyer && roomSlot.removed != null -> {
                        append("Raum ")
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(roomSlot.removed.shortName)
                        }
                        append(" → Foyer")
                    }
                    view.isFoyer -> {
                        append("Foyer")
                    }
                    roomSlot.removed != null && roomSlot.current != null -> {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(roomSlot.removed.shortName)
                        }
                        append(" → ")
                        append(roomSlot.current.shortName)
                    }
                    else -> {
                        append(view.roomText)
                    }
                }
            }
            if (roomAnnotated.isNotEmpty()) {
                DetailRow(icon = Icons.Outlined.Place, text = roomAnnotated.text)
            }
        }

        // 2. Lehrkraft
        val teacherSlot = view.teachers.firstOrNull()
        if (teacherSlot != null) {
            val teacherAnnotated = buildAnnotatedString {
                when {
                    view.isFoyer && teacherSlot.current == null -> {
                        append("ohne Lehrkraft")
                    }
                    teacherSlot.removed != null && teacherSlot.current != null -> {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(teacherSlot.removed.shortName)
                        }
                        append(" → ")
                        append(teacherSlot.current.shortName)
                    }
                    teacherSlot.removed != null && teacherSlot.current == null -> {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(teacherSlot.removed.shortName)
                        }
                    }
                    else -> {
                        append(teacherSlot.current?.shortName ?: "")
                    }
                }
            }
            if (teacherAnnotated.isNotEmpty()) {
                DetailRow(icon = Icons.Outlined.Person, text = teacherAnnotated.text)
            }
        }

        // 3. Info
        val infoText = view.infos.firstOrNull() ?: view.lesson.lessonInfo
        if (!infoText.isNullOrBlank()) {
            DetailRow(icon = Icons.Outlined.Info, text = infoText)
        }

        // 4. Vertretungstext
        val subText = view.lesson.substitutionText
        if (!subText.isNullOrBlank()) {
            DetailRow(icon = Icons.Outlined.SwapHoriz, text = subText)
        }

        // 5. Predicted note
        if (view.lesson.predicted) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Noch nicht freigegeben – vorhergesagt aus den letzten Wochen",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
