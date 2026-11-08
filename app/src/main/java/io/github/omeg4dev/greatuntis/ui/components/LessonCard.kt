package io.github.omeg4dev.greatuntis.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.omeg4dev.greatuntis.model.Lesson
import io.github.omeg4dev.greatuntis.model.TimeUnit
import io.github.omeg4dev.greatuntis.timetable.LessonKind
import io.github.omeg4dev.greatuntis.timetable.LessonView
import io.github.omeg4dev.greatuntis.timetable.LessonViews
import io.github.omeg4dev.greatuntis.ui.theme.statusColors
import io.github.omeg4dev.greatuntis.ui.theme.subjectColor
import io.github.omeg4dev.greatuntis.ui.theme.tabularNumbers
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LessonCard(
    view: LessonView,
    firstUnit: Int,
    lastUnit: Int,
    isCurrent: Boolean = false,
    isPast: Boolean = false,
    elapsedShare: Float = 0f,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val darkTheme = isSystemInDarkTheme()
    val statusColors = MaterialTheme.statusColors
    val colorScheme = MaterialTheme.colorScheme

    val isCancelled = view.kind == LessonKind.CANCELLED || LessonKind.CANCELLED in view.badges
    val isPredicted = view.kind == LessonKind.PREDICTED || LessonKind.PREDICTED in view.badges

    val containerColor = when (view.kind) {
        LessonKind.CANCELLED -> statusColors.free.container
        LessonKind.FOYER -> statusColors.foyer.container
        LessonKind.EXAM -> statusColors.exam.container
        LessonKind.EVENT -> colorScheme.surfaceContainerHighest
        LessonKind.PREDICTED -> Color.Transparent
        else -> subjectColor(view.shortTitle, darkTheme, primary = colorScheme.primary)
    }

    val contentColor = when (view.kind) {
        LessonKind.CANCELLED -> statusColors.free.onContainer
        LessonKind.FOYER -> statusColors.foyer.onContainer
        LessonKind.EXAM -> statusColors.exam.onContainer
        else -> colorScheme.onSurface
    }

    val showChangedAccentBar = view.kind == LessonKind.ROOM_CHANGED ||
        view.kind == LessonKind.TEACHER_CHANGED ||
        view.kind == LessonKind.SUBSTITUTION

    val border = when {
        isCurrent -> BorderStroke(2.dp, colorScheme.primary)
        else -> null
    }

    val dashedOutlineModifier = if (isPredicted) {
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

    val cardAlpha = when {
        isPast -> 0.55f
        isPredicted -> 0.7f
        else -> 1.0f
    }

    val unitText = when {
        firstUnit <= 0 -> ""
        firstUnit == lastUnit -> "$firstUnit. Std"
        else -> "$firstUnit.–$lastUnit. Std"
    }

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large,
        border = border,
        modifier = modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .then(dashedOutlineModifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Left 4dp accent bar for changes
                if (showChangedAccentBar) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(84.dp)
                            .background(statusColors.changed.accent),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    // Left column: times
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.width(64.dp),
                    ) {
                        Text(
                            text = view.lesson.start.format(timeFormatter),
                            style = MaterialTheme.typography.titleMedium.tabularNumbers(),
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = view.lesson.end.format(timeFormatter),
                            style = MaterialTheme.typography.bodySmall.tabularNumbers(),
                            color = contentColor.copy(alpha = 0.7f),
                        )
                        if (unitText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = unitText,
                                style = MaterialTheme.typography.labelSmall.tabularNumbers(),
                                color = contentColor.copy(alpha = 0.7f),
                                maxLines = 1,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Right column: title, room · teacher, notes
                    Column(modifier = Modifier.weight(1f)) {
                        FlowRow(
                            verticalArrangement = Arrangement.Center,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = view.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None,
                            )

                            if (isCancelled) {
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
                            if (!isCancelled && (LessonKind.TEACHER_CHANGED in view.badges || LessonKind.SUBSTITUTION in view.badges)) {
                                StatusPill("Vertretung", statusColors.changed)
                            }
                            if (LessonKind.EVENT in view.badges) {
                                Icon(
                                    imageVector = Icons.Outlined.Event,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            if (isPredicted) {
                                StatusPill("Prognose", containerColor = colorScheme.surfaceVariant, contentColor = colorScheme.onSurfaceVariant)
                            }
                        }

                        // Line 2: room · teacher(s) with strike-through changes
                        val roomSlot = view.rooms.firstOrNull()
                        val teacherSlot = view.teachers.firstOrNull()

                        val detailsText = buildAnnotatedString {
                            // Room
                            when {
                                view.isFoyer && roomSlot?.removed != null -> {
                                    append("Raum ")
                                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                                        append(roomSlot.removed.shortName)
                                    }
                                    append(" → Foyer")
                                }
                                view.isFoyer -> {
                                    append("Foyer")
                                }
                                roomSlot?.removed != null && roomSlot.current != null -> {
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

                            // Separator
                            val hasRoom = length > 0
                            val teacherAnnotated = buildAnnotatedString {
                                when {
                                    view.isFoyer && teacherSlot?.current == null -> {
                                        append("ohne Lehrkraft")
                                    }
                                    teacherSlot?.removed != null && teacherSlot.current != null -> {
                                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                                            append(teacherSlot.removed.shortName)
                                        }
                                        append(" → ")
                                        append(teacherSlot.current.shortName)
                                    }
                                    teacherSlot?.removed != null && teacherSlot.current == null -> {
                                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                                            append(teacherSlot.removed.shortName)
                                        }
                                    }
                                    else -> {
                                        append(teacherSlot?.current?.shortName ?: "")
                                    }
                                }
                            }

                            if (hasRoom && teacherAnnotated.isNotEmpty()) {
                                append(" · ")
                            }
                            append(teacherAnnotated)
                        }

                        if (detailsText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = detailsText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = contentColor.copy(alpha = 0.9f),
                            )
                        }

                        // Line 3: substitutionText or infos as bodySmall
                        val notes = listOfNotNull(
                            view.infos.firstOrNull() ?: view.lesson.lessonInfo,
                            view.lesson.substitutionText,
                        ).filter { it.isNotBlank() }.joinToString(" · ")

                        if (notes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = notes,
                                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                color = contentColor.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }

            // Current lesson progress indicator at the bottom
            if (isCurrent) {
                LinearWavyProgressIndicator(
                    progress = { elapsedShare.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = colorScheme.primary,
                    trackColor = colorScheme.primary.copy(alpha = 0.2f),
                )
            }
        }
    }
}

/** Legacy / Preview overload for direct Lesson passing. */
@Composable
fun LessonCard(
    lesson: Lesson,
    timeGrid: List<TimeUnit>,
    currentTime: LocalTime? = null,
    modifier: Modifier = Modifier,
) {
    val view = LessonViews.of(lesson, "Foyer")
    val matchingUnits = timeGrid.filter { it.start < lesson.end && it.end > lesson.start }
    val firstUnit = matchingUnits.firstOrNull()?.index ?: 0
    val lastUnit = matchingUnits.lastOrNull()?.index ?: 0

    val isCurrent = currentTime != null && lesson.start <= currentTime && currentTime < lesson.end
    val isPast = currentTime != null && currentTime >= lesson.end
    val elapsedShare = if (isCurrent && currentTime != null) {
        val total = ChronoUnit.SECONDS.between(lesson.start, lesson.end).toFloat()
        if (total > 0f) ChronoUnit.SECONDS.between(lesson.start, currentTime).toFloat() / total else 0f
    } else 0f

    LessonCard(
        view = view,
        firstUnit = firstUnit,
        lastUnit = lastUnit,
        isCurrent = isCurrent,
        isPast = isPast,
        elapsedShare = elapsedShare,
        modifier = modifier,
    )
}
