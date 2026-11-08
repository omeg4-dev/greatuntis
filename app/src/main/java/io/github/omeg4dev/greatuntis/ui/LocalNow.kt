package io.github.omeg4dev.greatuntis.ui

import androidx.compose.runtime.compositionLocalOf
import java.time.LocalDateTime

val LocalNow = compositionLocalOf<LocalDateTime> {
    error("No LocalNow provided")
}
