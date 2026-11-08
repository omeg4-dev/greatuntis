package io.github.omeg4dev.greatuntis.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.omeg4dev.greatuntis.R
import io.github.omeg4dev.greatuntis.model.SyncState
import io.github.omeg4dev.greatuntis.ui.theme.tabularNumbers
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SyncChip(
    syncState: SyncState,
    lastSync: Instant?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val timeStr = lastSync?.atZone(ZoneId.of("Europe/Berlin"))?.format(formatter) ?: ""

    val isError = syncState == SyncState.AUTH_ERROR || syncState == SyncState.ERROR
    val isSyncing = syncState == SyncState.SYNCING

    val containerColor by animateColorAsState(
        targetValue = when {
            isError -> MaterialTheme.colorScheme.errorContainer
            isSyncing -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "syncChipContainer",
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isError -> MaterialTheme.colorScheme.onErrorContainer
            isSyncing -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "syncChipContent",
    )

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.clickable(enabled = !isSyncing, onClick = onRefresh),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            when {
                isSyncing -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = contentColor,
                    )
                }
                isError -> {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
                syncState == SyncState.OFFLINE -> {
                    Icon(
                        imageVector = Icons.Outlined.WifiOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            val label = when (syncState) {
                SyncState.SYNCING -> stringResource(R.string.sync_syncing)
                SyncState.AUTH_ERROR -> stringResource(R.string.sync_auth_error)
                SyncState.ERROR -> stringResource(R.string.sync_error)
                SyncState.OFFLINE -> stringResource(R.string.sync_offline, timeStr)
                SyncState.IDLE -> stringResource(R.string.sync_updated, timeStr)
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.tabularNumbers(),
            )
        }
    }
}
