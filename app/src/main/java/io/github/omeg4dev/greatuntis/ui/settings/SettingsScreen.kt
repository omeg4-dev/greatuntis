package io.github.omeg4dev.greatuntis.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.omeg4dev.greatuntis.R
import io.github.omeg4dev.greatuntis.model.Account
import io.github.omeg4dev.greatuntis.model.ChangeKind
import io.github.omeg4dev.greatuntis.model.Settings
import io.github.omeg4dev.greatuntis.ui.fake.FakeData
import io.github.omeg4dev.greatuntis.ui.theme.GreatUntisTheme
import io.github.omeg4dev.greatuntis.ui.theme.tabularNumbers
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val GERMAN_STATES = listOf(
    "BW" to "Baden-Württemberg",
    "BY" to "Bayern",
    "BE" to "Berlin",
    "BB" to "Brandenburg",
    "HB" to "Bremen",
    "HH" to "Hamburg",
    "HE" to "Hessen",
    "MV" to "Mecklenburg-Vorpommern",
    "NI" to "Niedersachsen",
    "NW" to "Nordrhein-Westfalen",
    "RP" to "Rheinland-Pfalz",
    "SL" to "Saarland",
    "SN" to "Sachsen",
    "ST" to "Sachsen-Anhalt",
    "SH" to "Schleswig-Holstein",
    "TH" to "Thüringen",
)

private fun federalStateName(code: String): String =
    GERMAN_STATES.firstOrNull { it.first.equals(code, ignoreCase = true) }?.second ?: code

@Composable
fun SettingsScreen(
    settings: Settings,
    account: Account?,
    appVersion: String,
    onUpdateSettings: (transform: (Settings) -> Settings) -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onLogout: () -> Unit,
    updateSection: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    var showIntervalDialog by remember { mutableStateOf(false) }
    var showSchoolStartDialog by remember { mutableStateOf(false) }
    var showFoyerPrefixDialog by remember { mutableStateOf(false) }
    var showFederalStateDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val transparentItemColors = ListItemDefaults.colors(containerColor = Color.Transparent)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Section: Benachrichtigungen
        item {
            SettingsSection(title = "Benachrichtigungen") {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_watch_enabled)) },
                    trailingContent = {
                        Switch(
                            checked = settings.watchEnabled,
                            onCheckedChange = { checked ->
                                onUpdateSettings { it.copy(watchEnabled = checked) }
                            },
                        )
                    },
                    colors = transparentItemColors,
                )

                val kindItems = listOf(
                    ChangeKind.FOYER to R.string.kind_foyer,
                    ChangeKind.CANCELLED to R.string.kind_cancelled,
                    ChangeKind.FIRST_LESSON to R.string.kind_first_lesson,
                    ChangeKind.ROOM to R.string.kind_room,
                    ChangeKind.TEACHER to R.string.kind_teacher,
                    ChangeKind.EXAM to R.string.kind_exam,
                    ChangeKind.ADDED to R.string.kind_added,
                    ChangeKind.RESTORED to R.string.kind_restored,
                )

                kindItems.forEach { (kind, labelRes) ->
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    )

                    val isChecked = kind in settings.notifyKinds
                    val soundText = if (kind.urgent) {
                        stringResource(R.string.sound_loud)
                    } else {
                        stringResource(R.string.sound_silent)
                    }

                    ListItem(
                        headlineContent = { Text(stringResource(labelRes)) },
                        supportingContent = { Text(soundText) },
                        trailingContent = {
                            Switch(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    onUpdateSettings { s ->
                                        val newKinds = if (checked) {
                                            s.notifyKinds + kind
                                        } else {
                                            s.notifyKinds - kind
                                        }
                                        s.copy(notifyKinds = newKinds)
                                    }
                                },
                            )
                        },
                        colors = transparentItemColors,
                    )
                }
            }
        }

        // 2. Section: Überwachung
        item {
            SettingsSection(title = "Überwachung") {
                // Prüfintervall
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_poll_interval)) },
                    supportingContent = {
                        Text(
                            stringResource(R.string.settings_poll_seconds, settings.pollSeconds),
                            style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                        )
                    },
                    modifier = Modifier.clickable { showIntervalDialog = true },
                    colors = transparentItemColors,
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                )

                // Beginn morgens
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_school_start)) },
                    supportingContent = {
                        Text(
                            settings.schoolStart.format(timeFormatter),
                            style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                        )
                    },
                    modifier = Modifier.clickable { showSchoolStartDialog = true },
                    colors = transparentItemColors,
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                )

                // Abends prüfen
                val eveningRangeStr = "${settings.eveningStart.format(timeFormatter)}–${settings.eveningEnd.format(timeFormatter)}"
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_evening_range)) },
                    supportingContent = {
                        Text(
                            eveningRangeStr,
                            style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                        )
                    },
                    colors = transparentItemColors,
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                )

                // Akku-Optimierung deaktivieren
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val isIgnoring = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
                val statusText = if (isIgnoring) "Aktiv" else "Nicht aktiv"

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_battery_exemption)) },
                    supportingContent = { Text(statusText) },
                    modifier = Modifier.clickable { onRequestBatteryExemption() },
                    colors = transparentItemColors,
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                )

                // Explainer text
                Text(
                    text = stringResource(R.string.settings_battery_explainer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        // 3. Section: Stundenplan
        item {
            SettingsSection(title = "Stundenplan") {
                // Foyer-Raumname
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_foyer_prefix)) },
                    supportingContent = { Text(settings.foyerPrefix) },
                    modifier = Modifier.clickable { showFoyerPrefixDialog = true },
                    colors = transparentItemColors,
                )
            }
        }

        // 4. Section: Ferien & Feiertage
        item {
            SettingsSection(title = "Ferien & Feiertage") {
                ListItem(
                    headlineContent = { Text("Bundesland") },
                    supportingContent = { Text(federalStateName(settings.federalState)) },
                    modifier = Modifier.clickable { showFederalStateDialog = true },
                    colors = transparentItemColors,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                )
                Text(
                    text = "Quelle: WebUntis + openholidaysapi.org",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        // 5. Section: Konto
        if (account != null) {
            item {
                SettingsSection(title = "Konto") {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.school_name_label)) },
                        supportingContent = { Text(account.school) },
                        colors = transparentItemColors,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.username_label)) },
                        supportingContent = { Text(account.user) },
                        colors = transparentItemColors,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    )
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.settings_logout),
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        modifier = Modifier.clickable { showLogoutDialog = true },
                        colors = transparentItemColors,
                    )
                }
            }
        }

        // 6. Section: Über
        item {
            SettingsSection(title = "Über") {
                // Clearly marked slot for the updater added later
                updateSection()

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_version)) },
                    supportingContent = { Text(appVersion.ifEmpty { "0.1.0" }) },
                    colors = transparentItemColors,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_github)) },
                    supportingContent = { Text(stringResource(R.string.github_url)) },
                    modifier = Modifier.clickable {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/omeg4dev/greatuntis"),
                        )
                        context.startActivity(intent)
                    },
                    colors = transparentItemColors,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_license_label)) },
                    supportingContent = { Text(stringResource(R.string.settings_license)) },
                    colors = transparentItemColors,
                )
            }
        }
    }

    // Interval Dialog
    if (showIntervalDialog) {
        val intervals = listOf(30, 60, 120)
        var selectedInterval by remember { mutableIntStateOf(settings.pollSeconds) }

        AlertDialog(
            onDismissRequest = { showIntervalDialog = false },
            title = { Text(stringResource(R.string.settings_poll_interval)) },
            text = {
                Column {
                    intervals.forEach { sec ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedInterval = sec }
                                .padding(vertical = 6.dp),
                        ) {
                            RadioButton(
                                selected = selectedInterval == sec,
                                onClick = { selectedInterval = sec },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.settings_poll_seconds, sec),
                                style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateSettings { it.copy(pollSeconds = selectedInterval) }
                        showIntervalDialog = false
                    },
                ) {
                    Text(stringResource(R.string.btn_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showIntervalDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        )
    }

    // School start time dialog (06:00 / 07:00 / 07:30 / 08:00)
    if (showSchoolStartDialog) {
        val options = listOf(
            LocalTime.of(6, 0),
            LocalTime.of(7, 0),
            LocalTime.of(7, 30),
            LocalTime.of(8, 0),
        )
        var selectedTime by remember { mutableStateOf(settings.schoolStart) }

        AlertDialog(
            onDismissRequest = { showSchoolStartDialog = false },
            title = { Text(stringResource(R.string.settings_school_start)) },
            text = {
                Column {
                    options.forEach { t ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTime = t }
                                .padding(vertical = 6.dp),
                        ) {
                            RadioButton(
                                selected = selectedTime == t,
                                onClick = { selectedTime = t },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = t.format(timeFormatter),
                                style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateSettings { it.copy(schoolStart = selectedTime) }
                        showSchoolStartDialog = false
                    },
                ) {
                    Text(stringResource(R.string.btn_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSchoolStartDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        )
    }

    // Foyer prefix dialog
    if (showFoyerPrefixDialog) {
        var prefixText by remember { mutableStateOf(settings.foyerPrefix) }

        AlertDialog(
            onDismissRequest = { showFoyerPrefixDialog = false },
            title = { Text(stringResource(R.string.settings_foyer_prefix)) },
            text = {
                OutlinedTextField(
                    value = prefixText,
                    onValueChange = { prefixText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (prefixText.isNotBlank()) {
                            onUpdateSettings { it.copy(foyerPrefix = prefixText.trim()) }
                        }
                        showFoyerPrefixDialog = false
                    },
                ) {
                    Text(stringResource(R.string.btn_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFoyerPrefixDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        )
    }

    // Bundesland dialog (16 states)
    if (showFederalStateDialog) {
        AlertDialog(
            onDismissRequest = { showFederalStateDialog = false },
            title = { Text("Bundesland") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    GERMAN_STATES.forEach { (code, name) ->
                        val isSelected = settings.federalState.equals(code, ignoreCase = true)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateSettings { it.copy(federalState = code) }
                                    showFederalStateDialog = false
                                }
                                .padding(vertical = 6.dp),
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onUpdateSettings { it.copy(federalState = code) }
                                    showFederalStateDialog = false
                                },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showFederalStateDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        )
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.settings_logout_confirm_title)) },
            text = { Text(stringResource(R.string.settings_logout_confirm_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.settings_logout),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    GreatUntisTheme {
        SettingsScreen(
            settings = Settings(),
            account = FakeData.sampleAccount,
            appVersion = "0.1.0",
            onUpdateSettings = {},
            onRequestBatteryExemption = {},
            onLogout = {},
        )
    }
}
