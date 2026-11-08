package io.github.omeg4dev.greatuntis.update.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.omeg4dev.greatuntis.BuildConfig
import io.github.omeg4dev.greatuntis.update.ReleaseInfo
import io.github.omeg4dev.greatuntis.update.UpdateController
import io.github.omeg4dev.greatuntis.update.UpdateState

@Composable
fun UpdateSection(
    controller: UpdateController,
    modifier: Modifier = Modifier,
) {
    if (!controller.enabled) return

    val state by controller.state.collectAsStateWithLifecycle()
    val availableVersion by controller.availableVersion.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val appVersion = BuildConfig.VERSION_NAME

    InstallPermissionDialog(controller = controller)

    Column(modifier = modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text("App-Version") },
            supportingContent = { Text(appVersion) },
        )
        HorizontalDivider()

        val checkSupportingText: String? = when (val s = state) {
            is UpdateState.Checking -> "Suche nach Updates…"
            is UpdateState.UpToDate -> "Aktuell"
            is UpdateState.Available -> "Version ${s.release.version} verfügbar"
            is UpdateState.Downloading -> "Wird heruntergeladen…"
            is UpdateState.ReadyToInstall -> "Bereit zur Installation"
            is UpdateState.CheckFailed -> s.reason
            is UpdateState.DownloadFailed -> s.reason
            is UpdateState.SignatureMismatch -> "Signatur stimmt nicht überein"
            is UpdateState.Idle -> if (availableVersion != null) "Version $availableVersion verfügbar" else null
        }

        ListItem(
            headlineContent = { Text("Nach Updates suchen") },
            supportingContent = checkSupportingText?.let { { Text(it) } },
            trailingContent = {
                if (state is UpdateState.Checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    FilledTonalButton(
                        onClick = { controller.checkNow() },
                        enabled = state !is UpdateState.Downloading,
                    ) {
                        Text("Suchen")
                    }
                }
            },
        )

        when (val s = state) {
            is UpdateState.UpToDate -> {
                if (s.manual) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Aktuell: Die installierte Version ist auf dem neuesten Stand.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = controller::dismissMessage) {
                                Text("OK")
                            }
                        }
                    }
                }
            }

            is UpdateState.Available -> {
                AvailableUpdateCard(
                    release = s.release,
                    onInstall = { controller.install() },
                    onLater = controller::later,
                    onSkip = controller::skip,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            is UpdateState.Downloading -> {
                DownloadingCard(
                    release = s.release,
                    fraction = s.fraction,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            is UpdateState.ReadyToInstall -> {
                ReadyToInstallCard(
                    release = s.release,
                    onInstall = { controller.install() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            is UpdateState.CheckFailed -> {
                ErrorCard(
                    message = s.reason,
                    onDismiss = controller::dismissMessage,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            is UpdateState.DownloadFailed -> {
                DownloadFailedCard(
                    message = s.reason,
                    onRetry = controller::retry,
                    onDismiss = controller::later,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            is UpdateState.SignatureMismatch -> {
                SignatureMismatchCard(
                    release = s.release,
                    onOpenPage = { openUrl(context, s.release.pageUrl) },
                    onDismiss = controller::dismissMessage,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            is UpdateState.Idle -> {}
            is UpdateState.Checking -> {}
        }
    }
}

@Composable
fun UpdateDialog(
    controller: UpdateController,
) {
    if (!controller.enabled) return

    val state by controller.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    InstallPermissionDialog(controller = controller)

    when (val s = state) {
        is UpdateState.Available -> {
            AlertDialog(
                onDismissRequest = controller::later,
                title = { Text("Version ${s.release.version} verfügbar") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            text = "Ein neues Update für GreatUntis ist verfügbar.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (!s.release.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Was ist neu:",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            s.release.notes.lines().forEach { line ->
                                if (line.isNotBlank()) {
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 1.dp),
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { controller.install() }) {
                        Text("Installieren")
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = controller::skip) {
                            Text("Überspringen")
                        }
                        TextButton(onClick = controller::later) {
                            Text("Später")
                        }
                    }
                },
            )
        }

        is UpdateState.Downloading -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Update wird heruntergeladen…") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Version ${s.release.version} wird heruntergeladen.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (s.fraction != null) {
                            LinearProgressIndicator(
                                progress = { s.fraction },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${(s.fraction * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.align(Alignment.End),
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = controller::later) {
                        Text("Abbrechen")
                    }
                },
            )
        }

        is UpdateState.ReadyToInstall -> {
            AlertDialog(
                onDismissRequest = controller::later,
                title = { Text("Update bereit") },
                text = {
                    Text(
                        text = "Version ${s.release.version} wurde heruntergeladen und kann jetzt installiert werden.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    Button(onClick = { controller.install() }) {
                        Text("Installieren")
                    }
                },
                dismissButton = {
                    TextButton(onClick = controller::later) {
                        Text("Später")
                    }
                },
            )
        }

        is UpdateState.SignatureMismatch -> {
            AlertDialog(
                onDismissRequest = controller::dismissMessage,
                title = { Text("Signaturfehler") },
                text = {
                    Text(
                        text = "Die Signatur des Updates stimmt nicht mit der installierten Version überein. Bitte installieren Sie die Version manuell von der Release-Seite.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        openUrl(context, s.release.pageUrl)
                        controller.dismissMessage()
                    }) {
                        Text("Release-Seite")
                    }
                },
                dismissButton = {
                    TextButton(onClick = controller::dismissMessage) {
                        Text("Schließen")
                    }
                },
            )
        }

        else -> {}
    }
}

@Composable
fun InstallPermissionDialog(
    controller: UpdateController,
) {
    val needsPermission by controller.needsInstallPermission.collectAsStateWithLifecycle()
    if (needsPermission) {
        AlertDialog(
            onDismissRequest = controller::cancelInstallPermission,
            title = { Text("Installation erlauben?") },
            text = {
                Text("Um Updates zu installieren, benötigt GreatUntis die Berechtigung, Apps zu installieren. Dies wird ausschließlich für eigene App-Updates verwendet. Sie bestätigen jede Installation noch einmal im Systemdialog.")
            },
            confirmButton = {
                TextButton(onClick = controller::confirmInstallPermission) {
                    Text("Weiter")
                }
            },
            dismissButton = {
                TextButton(onClick = controller::cancelInstallPermission) {
                    Text("Abbrechen")
                }
            },
        )
    }
}

@Composable
private fun AvailableUpdateCard(
    release: ReleaseInfo,
    onInstall: () -> Unit,
    onLater: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Version ${release.version} verfügbar",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (!release.notes.isNullOrBlank()) {
                Text(
                    text = "Was ist neu:",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.height(4.dp))
                release.notes.lines().forEach { line ->
                    if (line.isNotBlank()) {
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 1.dp),
                        )
                    }
                }
            } else {
                Text(
                    text = "Keine Versionshinweise für dieses Release.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onInstall) {
                    Text("Installieren")
                }
                TextButton(onClick = onLater) {
                    Text("Später")
                }
                TextButton(onClick = onSkip) {
                    Text("Überspringen")
                }
            }
        }
    }
}

@Composable
private fun DownloadingCard(
    release: ReleaseInfo,
    fraction: Float?,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Version ${release.version} wird heruntergeladen…",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (fraction != null) {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End),
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ReadyToInstallCard(
    release: ReleaseInfo,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Update heruntergeladen",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Version ${release.version} ist bereit zur Installation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onInstall) {
                Text("Installieren")
            }
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) {
                Text("OK", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
private fun DownloadFailedCard(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRetry) {
                    Text("Wiederholen")
                }
                TextButton(onClick = onDismiss) {
                    Text("Später", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
}

@Composable
private fun SignatureMismatchCard(
    release: ReleaseInfo,
    onOpenPage: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Die Signatur des Updates stimmt nicht mit der installierten Version überein. Bitte installieren Sie Version ${release.version} manuell von der GitHub-Release-Seite.",
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenPage) {
                    Text("Release-Seite öffnen")
                }
                TextButton(onClick = onDismiss) {
                    Text("Schließen", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    if (url.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}
