package io.github.omeg4dev.greatuntis.ui

import io.github.omeg4dev.greatuntis.data.UntisRepository
import io.github.omeg4dev.greatuntis.model.Absence
import io.github.omeg4dev.greatuntis.model.AbsenceStatsResult
import io.github.omeg4dev.greatuntis.model.Account
import io.github.omeg4dev.greatuntis.model.Day
import io.github.omeg4dev.greatuntis.model.TimeUnit
import java.time.LocalDate
import java.time.LocalDateTime

class AppDeps(
    val repo: UntisRepository,
    val parseAccount: (String) -> Account?,
    val computeStats: (days: List<Day>, absences: List<Absence>, grid: List<TimeUnit>, foyerPrefix: String) -> AbsenceStatsResult,
    val predict: (history: List<Day>, dates: List<LocalDate>, grid: List<TimeUnit>, isFree: (LocalDate) -> Boolean) -> List<Day>,
    val clock: () -> LocalDateTime = { LocalDateTime.now() },
    val onRequestBatteryExemption: () -> Unit = {},
    val appVersion: String = "",
    /** Null in fake/preview mode. */
    val updateController: io.github.omeg4dev.greatuntis.update.UpdateController? = null,
)
