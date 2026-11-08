package io.github.omeg4dev.greatuntis.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

// Light (seed #1E3A5F, tonal spot)
val PrimaryLight = Color(0xFF44608A)
val OnPrimaryLight = Color(0xFFF8F8FF)
val PrimaryContainerLight = Color(0xFFA6C2F2)
val OnPrimaryContainerLight = Color(0xFF1F3D65)
val SecondaryLight = Color(0xFF555F71)
val OnSecondaryLight = Color(0xFFF8F8FF)
val SecondaryContainerLight = Color(0xFFD9E3F8)
val OnSecondaryContainerLight = Color(0xFF485264)
val TertiaryLight = Color(0xFF655882)
val OnTertiaryLight = Color(0xFFFDF7FF)
val TertiaryContainerLight = Color(0xFFDACAFB)
val OnTertiaryContainerLight = Color(0xFF4D4169)
val ErrorLight = Color(0xFFA83836)
val OnErrorLight = Color(0xFFFFF7F6)
val ErrorContainerLight = Color(0xFFFA746F)
val OnErrorContainerLight = Color(0xFF6E0A12)
val BackgroundLight = Color(0xFFF9F9FE)
val OnBackgroundLight = Color(0xFF2F333A)
val SurfaceLight = Color(0xFFF9F9FE)
val OnSurfaceLight = Color(0xFF2F333A)
val SurfaceVariantLight = Color(0xFFDFE2EC)
val OnSurfaceVariantLight = Color(0xFF5B5F68)
val OutlineLight = Color(0xFF777B84)
val OutlineVariantLight = Color(0xFFAEB2BB)
val InverseSurfaceLight = Color(0xFF0C0E12)
val InverseOnSurfaceLight = Color(0xFF9C9DA1)
val InversePrimaryLight = Color(0xFFA6C2F2)
val SurfaceTintLight = Color(0xFF44608A)
val SurfaceDimLight = Color(0xFFD7DAE4)
val SurfaceBrightLight = Color(0xFFF9F9FE)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF2F3FA)
val SurfaceContainerLight = Color(0xFFECEDF6)
val SurfaceContainerHighLight = Color(0xFFE6E8F1)
val SurfaceContainerHighestLight = Color(0xFFDFE2EC)

// Dark (seed #1E3A5F, tonal spot)
val PrimaryDark = Color(0xFFB2C8ED)
val OnPrimaryDark = Color(0xFF2C415F)
val PrimaryContainerDark = Color(0xFF3F5373)
val OnPrimaryContainerDark = Color(0xFFD6E4FF)
val SecondaryDark = Color(0xFFBDC7DC)
val OnSecondaryDark = Color(0xFF374151)
val SecondaryContainerDark = Color(0xFF323C4C)
val OnSecondaryContainerDark = Color(0xFFB5C0D4)
val TertiaryDark = Color(0xFFE7DAFF)
val OnTertiaryDark = Color(0xFF554972)
val TertiaryContainerDark = Color(0xFF4D4169)
val OnTertiaryContainerDark = Color(0xFFE7DAFF)
val ErrorDark = Color(0xFFFA746F)
val OnErrorDark = Color(0xFF490006)
val ErrorContainerDark = Color(0xFF871F21)
val OnErrorContainerDark = Color(0xFFFF9993)
val BackgroundDark = Color(0xFF0C0E12)
val OnBackgroundDark = Color(0xFFE2E5EF)
val SurfaceDark = Color(0xFF0C0E12)
val OnSurfaceDark = Color(0xFFE2E5EF)
val SurfaceVariantDark = Color(0xFF22262D)
val OnSurfaceVariantDark = Color(0xFFA8ABB4)
val OutlineDark = Color(0xFF72757E)
val OutlineVariantDark = Color(0xFF444850)
val InverseSurfaceDark = Color(0xFFF9F9FE)
val InverseOnSurfaceDark = Color(0xFF535559)
val InversePrimaryDark = Color(0xFF4B6080)
val SurfaceTintDark = Color(0xFFB2C8ED)
val SurfaceDimDark = Color(0xFF0C0E12)
val SurfaceBrightDark = Color(0xFF282C34)
val SurfaceContainerLowestDark = Color(0xFF000000)
val SurfaceContainerLowDark = Color(0xFF111318)
val SurfaceContainerDark = Color(0xFF171A1F)
val SurfaceContainerHighDark = Color(0xFF1C2026)
val SurfaceContainerHighestDark = Color(0xFF22262D)

data class StatusColor(
    val accent: Color,
    val container: Color,
    val onContainer: Color,
)

data class StatusColors(
    val free: StatusColor,
    val foyer: StatusColor,
    val exam: StatusColor,
    val changed: StatusColor,
    val holiday: StatusColor,
)

val LocalStatusColors = staticCompositionLocalOf<StatusColors> {
    error("No StatusColors provided")
}

val MaterialTheme.statusColors: StatusColors
    @Composable
    @ReadOnlyComposable
    get() = LocalStatusColors.current

fun statusColors(darkTheme: Boolean, colorScheme: ColorScheme): StatusColors {
    fun blendWithPrimary(containerColor: Color): Color =
        Color(ColorUtils.blendARGB(containerColor.toArgb(), colorScheme.primary.toArgb(), 0.10f))

    val free = if (darkTheme) {
        StatusColor(
            accent = Color(0xFF8BD89E),
            container = blendWithPrimary(Color(0xFF0F5228)),
            onContainer = Color(0xFFA6F4B9),
        )
    } else {
        StatusColor(
            accent = Color(0xFF1B6D3A),
            container = blendWithPrimary(Color(0xFFC8F0D2)),
            onContainer = Color(0xFF00210D),
        )
    }

    val foyer = if (darkTheme) {
        StatusColor(
            accent = Color(0xFFF5BF3C),
            container = blendWithPrimary(Color(0xFF5C4300)),
            onContainer = Color(0xFFFFE08A),
        )
    } else {
        StatusColor(
            accent = Color(0xFF7A5900),
            container = blendWithPrimary(Color(0xFFFFE08A)),
            onContainer = Color(0xFF251A00),
        )
    }

    val exam = StatusColor(
        accent = colorScheme.tertiary,
        container = blendWithPrimary(colorScheme.tertiaryContainer),
        onContainer = colorScheme.onTertiaryContainer,
    )

    val changed = StatusColor(
        accent = colorScheme.secondary,
        container = blendWithPrimary(colorScheme.secondaryContainer),
        onContainer = colorScheme.onSecondaryContainer,
    )

    val holiday = if (darkTheme) {
        StatusColor(
            accent = Color(0xFF80D5D4),
            container = blendWithPrimary(Color(0xFF004F50)),
            onContainer = Color(0xFFB2EFEE),
        )
    } else {
        StatusColor(
            accent = Color(0xFF006A6A),
            container = blendWithPrimary(Color(0xFFB2EFEE)),
            onContainer = Color(0xFF002020),
        )
    }

    return StatusColors(
        free = free,
        foyer = foyer,
        exam = exam,
        changed = changed,
        holiday = holiday,
    )
}

// Compatibility for existing code before migration
data class ChangeColors(
    val foyer: Color,
    val foyerContainer: Color,
    val onFoyerContainer: Color,
    val cancelled: Color,
    val cancelledContainer: Color,
    val onCancelledContainer: Color,
    val changed: Color,
    val changedContainer: Color,
    val onChangedContainer: Color,
    val exam: Color,
    val examContainer: Color,
    val onExamContainer: Color,
    val predicted: Color,
)

val MaterialTheme.changeColors: ChangeColors
    @Composable
    @ReadOnlyComposable
    get() {
        val sc = statusColors
        val cs = colorScheme
        return ChangeColors(
            foyer = sc.foyer.accent,
            foyerContainer = sc.foyer.container,
            onFoyerContainer = sc.foyer.onContainer,
            cancelled = sc.free.accent,
            cancelledContainer = sc.free.container,
            onCancelledContainer = sc.free.onContainer,
            changed = sc.changed.accent,
            changedContainer = sc.changed.container,
            onChangedContainer = sc.changed.onContainer,
            exam = sc.exam.accent,
            examContainer = sc.exam.container,
            onExamContainer = sc.exam.onContainer,
            predicted = cs.onSurface.copy(alpha = 0.38f),
        )
    }

fun subjectColor(
    subject: String,
    dark: Boolean,
    primary: Color = if (dark) PrimaryDark else PrimaryLight,
): Color {
    val prefix = subject.substringBefore('-')
    val hash = (prefix.hashCode() and 0x7FFFFFFF) % 360
    val hue = hash.toFloat()
    val saturation = if (dark) 0.42f else 0.50f
    val lightness = if (dark) 0.30f else 0.87f
    val baseArgb = ColorUtils.HSLToColor(floatArrayOf(hue, saturation, lightness))
    val blendedArgb = ColorUtils.blendARGB(baseArgb, primary.toArgb(), 0.12f)
    return Color(blendedArgb)
}
