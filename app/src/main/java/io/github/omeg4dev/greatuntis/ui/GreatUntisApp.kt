package io.github.omeg4dev.greatuntis.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.omeg4dev.greatuntis.R
import io.github.omeg4dev.greatuntis.ui.absences.AbsencesScreen
import io.github.omeg4dev.greatuntis.ui.components.SyncAction
import io.github.omeg4dev.greatuntis.ui.free.FreeScreen
import io.github.omeg4dev.greatuntis.ui.onboarding.OnboardingScreen
import io.github.omeg4dev.greatuntis.ui.settings.SettingsScreen
import io.github.omeg4dev.greatuntis.ui.today.TodayScreen
import io.github.omeg4dev.greatuntis.ui.week.WeekScreen
import kotlinx.coroutines.delay

enum class AppTab(
    val selectedIcon: @Composable () -> Unit,
    val unselectedIcon: @Composable () -> Unit,
    val titleRes: Int,
) {
    HEUTE(
        selectedIcon = { Icon(Icons.Filled.Today, contentDescription = null) },
        unselectedIcon = { Icon(Icons.Outlined.Today, contentDescription = null) },
        titleRes = R.string.nav_today,
    ),
    WOCHE(
        selectedIcon = { Icon(Icons.Filled.CalendarViewWeek, contentDescription = null) },
        unselectedIcon = { Icon(Icons.Outlined.CalendarViewWeek, contentDescription = null) },
        titleRes = R.string.nav_week,
    ),
    FREI(
        selectedIcon = { Icon(Icons.Filled.BeachAccess, contentDescription = null) },
        unselectedIcon = { Icon(Icons.Outlined.BeachAccess, contentDescription = null) },
        titleRes = R.string.nav_free,
    ),
    FEHLZEITEN(
        selectedIcon = { Icon(Icons.Filled.QueryStats, contentDescription = null) },
        unselectedIcon = { Icon(Icons.Outlined.QueryStats, contentDescription = null) },
        titleRes = R.string.nav_absences,
    ),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GreatUntisApp(
    deps: AppDeps,
    initialScreen: String? = null,
    viewModel: AppViewModel = viewModel(factory = AppViewModel.provideFactory(deps)),
) {
    val account by viewModel.account.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val lastSync by viewModel.lastSync.collectAsState()
    val days by viewModel.days.collectAsState()
    val masterData by viewModel.masterData.collectAsState()
    val absences by viewModel.absences.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val freeCalendar by viewModel.freeCalendar.collectAsState()
    val holidaysUpdated by deps.repo.holidaysUpdated.collectAsState()

    val initialIndex = when (initialScreen?.lowercase()) {
        "woche" -> 1
        "frei" -> 2
        "fehlzeiten" -> 3
        else -> 0
    }

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(initialIndex) }
    var showSettings by rememberSaveable { mutableStateOf(initialScreen?.lowercase() == "einstellungen") }

    val now by produceState(initialValue = deps.clock()) {
        val initial = deps.clock()
        value = initial
        while (true) {
            val current = deps.clock()
            value = current
            val delayMillis = (60_000L - current.second * 1000L - (current.nano / 1_000_000L)).coerceAtLeast(100L)
            delay(delayMillis)
            value = deps.clock()
        }
    }

    CompositionLocalProvider(LocalNow provides now) {
        deps.updateController?.let { updater ->
            if (account != null) {
                androidx.compose.runtime.LaunchedEffect(updater) { updater.checkOnStartup() }
                io.github.omeg4dev.greatuntis.update.ui.UpdateDialog(updater)
                io.github.omeg4dev.greatuntis.update.ui.InstallPermissionDialog(updater)
            }
        }
        if (account == null) {
            OnboardingScreen(
                parseAccount = deps.parseAccount,
                onLogin = { acc -> viewModel.login(acc).map { } },
                searchSchools = { q -> viewModel.searchSchools(q) },
            )
        } else if (masterData == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LoadingIndicator()
                    Text(
                        text = "Stundenplan wird geladen …",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            BackHandler(enabled = showSettings) {
                showSettings = false
            }

            AnimatedContent(
                targetState = showSettings,
                transitionSpec = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) togetherWith
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
                },
                label = "SettingsTransition",
            ) { isSettings ->
                if (isSettings) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Einstellungen") },
                                navigationIcon = {
                                    IconButton(onClick = { showSettings = false }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                            contentDescription = "Zurück",
                                        )
                                    }
                                },
                            )
                        },
                        modifier = Modifier.fillMaxSize(),
                    ) { innerPadding ->
                        SettingsScreen(
                            settings = settings,
                            account = account,
                            appVersion = deps.appVersion,
                            onUpdateSettings = { transform -> viewModel.updateSettings(transform) },
                            onRequestBatteryExemption = deps.onRequestBatteryExemption,
                            onLogout = { viewModel.logout() },
                            updateSection = {
                                deps.updateController?.let { io.github.omeg4dev.greatuntis.update.ui.UpdateSection(it) }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        )
                    }
                } else {
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                AppTab.entries.forEachIndexed { index, tab ->
                                    NavigationBarItem(
                                        selected = selectedTabIndex == index,
                                        onClick = { selectedTabIndex = index },
                                        icon = if (selectedTabIndex == index) tab.selectedIcon else tab.unselectedIcon,
                                        label = { Text(text = stringResource(tab.titleRes)) },
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    ) { innerPadding ->
                        val contentModifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)

                        when (AppTab.entries[selectedTabIndex]) {
                            AppTab.HEUTE -> TodayScreen(
                                days = days,
                                timeGrid = masterData!!.timeGrid,
                                freeCalendar = freeCalendar,
                                syncState = syncState,
                                lastSync = lastSync,
                                onRefresh = { viewModel.refresh() },
                                onOpenSettings = { showSettings = true },
                                onNavigateToFree = { selectedTabIndex = AppTab.FREI.ordinal },
                                settings = settings,
                                predict = deps.predict,
                                modifier = contentModifier,
                            )
                            AppTab.WOCHE -> WeekScreen(
                                days = days,
                                timeGrid = masterData!!.timeGrid,
                                freeCalendar = freeCalendar,
                                syncState = syncState,
                                lastSync = lastSync,
                                predict = deps.predict,
                                ensureWeek = { monday -> viewModel.ensureWeek(monday) },
                                onRefresh = { viewModel.refresh() },
                                onOpenSettings = { showSettings = true },
                                settings = settings,
                                modifier = contentModifier,
                            )
                            AppTab.FREI -> FreeScreen(
                                freeCalendar = freeCalendar,
                                masterData = masterData!!,
                                days = days,
                                settings = settings,
                                holidaysUpdated = holidaysUpdated,
                                syncState = syncState,
                                lastSync = lastSync,
                                onRefresh = { viewModel.refresh() },
                                onOpenSettings = { showSettings = true },
                                modifier = contentModifier,
                            )
                            AppTab.FEHLZEITEN -> Scaffold(
                                topBar = {
                                    TopAppBar(
                                        title = { Text("Fehlzeiten") },
                                        actions = {
                                            SyncAction(
                                                syncState = syncState,
                                                lastSync = lastSync,
                                                onRefresh = { viewModel.refresh() },
                                            )
                                            IconButton(onClick = { showSettings = true }) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Settings,
                                                    contentDescription = "Einstellungen",
                                                )
                                            }
                                        },
                                    )
                                },
                                modifier = contentModifier,
                            ) { innerAbsencesPadding ->
                                AbsencesScreen(
                                    days = days,
                                    absences = absences,
                                    timeGrid = masterData!!.timeGrid,
                                    settings = settings,
                                    computeStats = deps.computeStats,
                                    clock = { now },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerAbsencesPadding),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
