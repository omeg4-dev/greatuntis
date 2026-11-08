package io.github.omeg4dev.greatuntis.widget

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import androidx.glance.color.ColorProviders
import androidx.glance.material3.ColorProviders
import io.github.omeg4dev.greatuntis.ui.theme.BackgroundDark
import io.github.omeg4dev.greatuntis.ui.theme.BackgroundLight
import io.github.omeg4dev.greatuntis.ui.theme.ErrorContainerDark
import io.github.omeg4dev.greatuntis.ui.theme.ErrorContainerLight
import io.github.omeg4dev.greatuntis.ui.theme.ErrorDark
import io.github.omeg4dev.greatuntis.ui.theme.ErrorLight
import io.github.omeg4dev.greatuntis.ui.theme.InverseOnSurfaceDark
import io.github.omeg4dev.greatuntis.ui.theme.InverseOnSurfaceLight
import io.github.omeg4dev.greatuntis.ui.theme.InversePrimaryDark
import io.github.omeg4dev.greatuntis.ui.theme.InversePrimaryLight
import io.github.omeg4dev.greatuntis.ui.theme.InverseSurfaceDark
import io.github.omeg4dev.greatuntis.ui.theme.InverseSurfaceLight
import io.github.omeg4dev.greatuntis.ui.theme.OnBackgroundDark
import io.github.omeg4dev.greatuntis.ui.theme.OnBackgroundLight
import io.github.omeg4dev.greatuntis.ui.theme.OnErrorContainerDark
import io.github.omeg4dev.greatuntis.ui.theme.OnErrorContainerLight
import io.github.omeg4dev.greatuntis.ui.theme.OnErrorDark
import io.github.omeg4dev.greatuntis.ui.theme.OnErrorLight
import io.github.omeg4dev.greatuntis.ui.theme.OnPrimaryContainerDark
import io.github.omeg4dev.greatuntis.ui.theme.OnPrimaryContainerLight
import io.github.omeg4dev.greatuntis.ui.theme.OnPrimaryDark
import io.github.omeg4dev.greatuntis.ui.theme.OnPrimaryLight
import io.github.omeg4dev.greatuntis.ui.theme.OnSecondaryContainerDark
import io.github.omeg4dev.greatuntis.ui.theme.OnSecondaryContainerLight
import io.github.omeg4dev.greatuntis.ui.theme.OnSecondaryDark
import io.github.omeg4dev.greatuntis.ui.theme.OnSecondaryLight
import io.github.omeg4dev.greatuntis.ui.theme.OnSurfaceDark
import io.github.omeg4dev.greatuntis.ui.theme.OnSurfaceLight
import io.github.omeg4dev.greatuntis.ui.theme.OnSurfaceVariantDark
import io.github.omeg4dev.greatuntis.ui.theme.OnSurfaceVariantLight
import io.github.omeg4dev.greatuntis.ui.theme.OnTertiaryContainerDark
import io.github.omeg4dev.greatuntis.ui.theme.OnTertiaryContainerLight
import io.github.omeg4dev.greatuntis.ui.theme.OnTertiaryDark
import io.github.omeg4dev.greatuntis.ui.theme.OnTertiaryLight
import io.github.omeg4dev.greatuntis.ui.theme.OutlineDark
import io.github.omeg4dev.greatuntis.ui.theme.OutlineLight
import io.github.omeg4dev.greatuntis.ui.theme.OutlineVariantDark
import io.github.omeg4dev.greatuntis.ui.theme.OutlineVariantLight
import io.github.omeg4dev.greatuntis.ui.theme.PrimaryContainerDark
import io.github.omeg4dev.greatuntis.ui.theme.PrimaryContainerLight
import io.github.omeg4dev.greatuntis.ui.theme.PrimaryDark
import io.github.omeg4dev.greatuntis.ui.theme.PrimaryLight
import io.github.omeg4dev.greatuntis.ui.theme.SecondaryContainerDark
import io.github.omeg4dev.greatuntis.ui.theme.SecondaryContainerLight
import io.github.omeg4dev.greatuntis.ui.theme.SecondaryDark
import io.github.omeg4dev.greatuntis.ui.theme.SecondaryLight
import io.github.omeg4dev.greatuntis.ui.theme.SurfaceBrightDark
import io.github.omeg4dev.greatuntis.ui.theme.SurfaceBrightLight
import io.github.omeg4dev.greatuntis.ui.theme.SurfaceContainerDark
import io.github.omeg4dev.greatuntis.ui.theme.SurfaceContainerHighDark
import io.github.omeg4dev.greatuntis.ui.theme.SurfaceContainerHighLight
import io.github.omeg4dev.greatuntis.ui.theme.SurfaceContainerHighestDark
import io.github.omeg4dev.greatuntis.ui.theme.SurfaceContainerHighestLight
import io.github.omeg4dev.greatuntis.ui.theme.SurfaceContainerLight
import io.github.omeg4dev.greatuntis.ui.theme.SurfaceContainerLowDark
import io.github.omeg4dev.greatuntis.ui.theme.SurfaceContainerLowLight
import io.github.omeg4dev.greatuntis.ui.theme.SurfaceContainerLowestDark
import io.github.omeg4dev.greatuntis.ui.theme.SurfaceContainerLowestLight
import io.github.omeg4dev.greatuntis.ui.theme.SurfaceDark
import io.github.omeg4dev.greatuntis.ui.theme.SurfaceDimDark
import io.github.omeg4dev.greatuntis.ui.theme.SurfaceDimLight
import io.github.omeg4dev.greatuntis.ui.theme.SurfaceLight
import io.github.omeg4dev.greatuntis.ui.theme.SurfaceTintDark
import io.github.omeg4dev.greatuntis.ui.theme.SurfaceTintLight
import io.github.omeg4dev.greatuntis.ui.theme.SurfaceVariantDark
import io.github.omeg4dev.greatuntis.ui.theme.SurfaceVariantLight
import io.github.omeg4dev.greatuntis.ui.theme.TertiaryContainerDark
import io.github.omeg4dev.greatuntis.ui.theme.TertiaryContainerLight
import io.github.omeg4dev.greatuntis.ui.theme.TertiaryDark
import io.github.omeg4dev.greatuntis.ui.theme.TertiaryLight

val WidgetLightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    inversePrimary = InversePrimaryLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceTint = SurfaceTintLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceDim = SurfaceDimLight,
    surfaceBright = SurfaceBrightLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
)

val WidgetDarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    inversePrimary = InversePrimaryDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceTint = SurfaceTintDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceDim = SurfaceDimDark,
    surfaceBright = SurfaceBrightDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
)

val WidgetFallbackColorProviders: ColorProviders = ColorProviders(
    light = WidgetLightColorScheme,
    dark = WidgetDarkColorScheme,
)

// Free state colours (from ui/theme/Color.kt statusColors.free)
val FreeContainerColorProvider = ColorProvider(
    day = Color(0xFFBAE1CA),
    night = Color(0xFF1F5D3B),
)
val OnFreeContainerColorProvider = ColorProvider(
    day = Color(0xFF00210D),
    night = Color(0xFFA6F4B9),
)

// Holiday / Ferien state colours (from ui/theme/Color.kt statusColors.holiday)
val HolidayContainerColorProvider = ColorProvider(
    day = Color(0xFFA7E0E4),
    night = Color(0xFF115B5F),
)
val OnHolidayContainerColorProvider = ColorProvider(
    day = Color(0xFF002020),
    night = Color(0xFFB2EFEE),
)
