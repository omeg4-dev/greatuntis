package io.github.omeg4dev.greatuntis.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.omeg4dev.greatuntis.model.SyncState
import io.github.omeg4dev.greatuntis.ui.LocalNow
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SyncAction(
    syncState: SyncState,
    lastSync: Instant?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = LocalNow.current
    val tooltipText = when (syncState) {
        SyncState.IDLE -> {
            if (lastSync != null) {
                val nowInstant = now.atZone(ZoneId.systemDefault()).toInstant()
                val minutes = Duration.between(lastSync, nowInstant).toMinutes().coerceAtLeast(0)
                "Aktualisiert vor $minutes min"
            } else {
                "Aktualisiert"
            }
        }
        SyncState.SYNCING -> "Aktualisiere…"
        SyncState.OFFLINE -> "Offline – zeige gespeicherten Stand"
        SyncState.AUTH_ERROR -> "Anmeldung abgelaufen"
        SyncState.ERROR -> "Fehler beim Laden"
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(tooltipText)
            }
        },
        state = rememberTooltipState(),
        modifier = modifier,
    ) {
        IconButton(
            onClick = onRefresh,
            enabled = syncState != SyncState.SYNCING,
        ) {
            when (syncState) {
                SyncState.SYNCING -> {
                    Box(contentAlignment = Alignment.Center) {
                        CircularWavyProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                SyncState.IDLE -> {
                    Icon(
                        imageVector = Icons.Outlined.CloudDone,
                        contentDescription = tooltipText,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                SyncState.OFFLINE -> {
                    Icon(
                        imageVector = Icons.Outlined.CloudOff,
                        contentDescription = tooltipText,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SyncState.AUTH_ERROR, SyncState.ERROR -> {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = tooltipText,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
