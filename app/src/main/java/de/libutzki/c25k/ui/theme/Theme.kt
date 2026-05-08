package de.libutzki.c25k.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = CoachGreen,
    onPrimary = CoachCream,
    secondary = CoachBlue,
    onSecondary = CoachCream,
    tertiary = CoachMint,
    onTertiary = CoachSlate,
    error = CoachCoral,
    onError = CoachCream,
    background = CoachCream,
    onBackground = CoachSlate,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = CoachSlate,
    surfaceVariant = CoachSand,
    onSurfaceVariant = CoachFog,
    outline = CoachFog,
    outlineVariant = CoachSand
)

private val DarkColors = darkColorScheme(
    primary = CoachMint,
    onPrimary = CoachNight,
    secondary = CoachBlue,
    onSecondary = CoachCream,
    tertiary = CoachGreen,
    onTertiary = CoachCream,
    error = CoachCoral,
    onError = CoachNight,
    background = CoachNight,
    onBackground = CoachCream,
    surface = CoachNightSurface,
    onSurface = CoachCream,
    surfaceVariant = CoachGreenDark,
    onSurfaceVariant = CoachSand,
    outline = CoachMint,
    outlineVariant = CoachGreenDark
)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36.dp)
)

private val AppTypography = Typography()

@Composable
fun C25kTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
