package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val CyberIndigoScheme = darkColorScheme(
    primary = CyberPrimary,
    onPrimary = CyberOnPrimary,
    primaryContainer = CyberPrimaryContainer,
    onPrimaryContainer = CyberOnPrimaryContainer,
    secondary = CyberSecondary,
    onSecondary = CyberOnSecondary,
    secondaryContainer = CyberSecondaryContainer,
    onSecondaryContainer = CyberOnSecondaryContainer,
    tertiary = CyberTertiary,
    onTertiary = CyberOnTertiary,
    background = CyberBackground,
    onBackground = CyberTextPrimary,
    surface = CyberSurface,
    onSurface = CyberTextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = CyberTextSecondary
)

private val SaffronObsidianScheme = darkColorScheme(
    primary = SaffronPrimary,
    onPrimary = SaffronOnPrimary,
    primaryContainer = SaffronPrimaryContainer,
    onPrimaryContainer = SaffronOnPrimaryContainer,
    secondary = SaffronSecondary,
    onSecondary = SaffronOnSecondary,
    secondaryContainer = SaffronSecondaryContainer,
    onSecondaryContainer = SaffronOnSecondaryContainer,
    tertiary = SaffronTertiary,
    background = SaffronBackground,
    onBackground = CyberTextPrimary,
    surface = SaffronSurface,
    onSurface = CyberTextPrimary,
    surfaceVariant = SaffronSurfaceVariant,
    onSurfaceVariant = CyberTextSecondary
)

private val DaylightStudioScheme = lightColorScheme(
    primary = DaylightPrimary,
    onPrimary = DaylightOnPrimary,
    primaryContainer = DaylightPrimaryContainer,
    onPrimaryContainer = DaylightOnPrimaryContainer,
    secondary = DaylightSecondary,
    onSecondary = DaylightOnSecondary,
    secondaryContainer = DaylightSecondaryContainer,
    onSecondaryContainer = DaylightOnSecondaryContainer,
    tertiary = DaylightTertiary,
    background = DaylightBackground,
    onBackground = DaylightTextPrimary,
    surface = DaylightSurface,
    onSurface = DaylightTextPrimary,
    surfaceVariant = DaylightSurfaceVariant,
    onSurfaceVariant = DaylightTextSecondary
)

@Composable
fun PersonalAssistantTheme(
    themePreset: String = "CYBER_INDIGO",
    useDynamicColors: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isLightPreset = themePreset == "DAYLIGHT_STUDIO"

    val colorScheme = when {
        useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isLightPreset) dynamicLightColorScheme(context) else dynamicDarkColorScheme(context)
        }
        themePreset == "SAFFRON_OBSIDIAN" -> SaffronObsidianScheme
        themePreset == "DAYLIGHT_STUDIO" -> DaylightStudioScheme
        else -> CyberIndigoScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
