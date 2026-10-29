package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.preferences.AccentColorPreset
import com.example.data.preferences.AppThemeMode

@Immutable
data class LiquidGlassPalette(
    val isDark: Boolean,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val ambientGlow: Color,
    val glassSurfaceTop: Color,
    val glassSurfaceBottom: Color,
    val glassSpecularBorderTop: Color,
    val glassSpecularBorderBottom: Color,
    val textPrimary: Color,
    val textSecondary: Color
)

val LocalLiquidGlass = staticCompositionLocalOf {
    LiquidGlassPalette(
        isDark = true,
        primaryAccent = VividCyan,
        secondaryAccent = ElectricBlue,
        ambientGlow = Color(0xFF0284C7),
        glassSurfaceTop = GlassFillTopDark,
        glassSurfaceBottom = GlassFillBottomDark,
        glassSpecularBorderTop = GlassHighlightDark,
        glassSpecularBorderBottom = GlassBorderDark,
        textPrimary = DarkTextPrimary,
        textSecondary = DarkTextSecondary
    )
}

@Composable
fun PrismScanTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    accentPreset: AccentColorPreset = AccentColorPreset.CYAN_PRISM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> systemDark
    }

    val rawPrimaryAccent = if (darkTheme) Color(accentPreset.primaryHex) else Color(accentPreset.glowHex)
    val rawSecondaryAccent = if (darkTheme) Color(accentPreset.secondaryHex) else Color(accentPreset.primaryHex)
    val rawAmbientGlow = Color(accentPreset.glowHex)

    val primaryAccent by animateColorAsState(rawPrimaryAccent, tween(240), label = "primaryAccent")
    val secondaryAccent by animateColorAsState(rawSecondaryAccent, tween(240), label = "secondaryAccent")
    val bgColor by animateColorAsState(
        if (darkTheme) ObsidianAbyss else DaylightBackground,
        tween(240),
        label = "bgColor"
    )
    val surfaceColor by animateColorAsState(
        if (darkTheme) ObsidianSurface else DaylightSurface,
        tween(240),
        label = "surfaceColor"
    )
    val surfaceVariantColor by animateColorAsState(
        if (darkTheme) ObsidianElevated else DaylightElevated,
        tween(240),
        label = "surfaceVariantColor"
    )
    val textPrimaryColor by animateColorAsState(
        if (darkTheme) DarkTextPrimary else LightTextPrimary,
        tween(240),
        label = "textPrimary"
    )
    val textSecondaryColor by animateColorAsState(
        if (darkTheme) DarkTextSecondary else LightTextSecondary,
        tween(240),
        label = "textSecondary"
    )

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            primary = primaryAccent,
            onPrimary = Color(0xFF02060D),
            primaryContainer = rawAmbientGlow.copy(alpha = 0.35f),
            onPrimaryContainer = Color(0xFFE0F2FE),
            secondary = secondaryAccent,
            onSecondary = Color(0xFF02060D),
            secondaryContainer = surfaceVariantColor,
            onSecondaryContainer = Color(0xFFE2E8F0),
            background = bgColor,
            onBackground = textPrimaryColor,
            surface = surfaceColor,
            onSurface = textPrimaryColor,
            surfaceVariant = surfaceVariantColor,
            onSurfaceVariant = textSecondaryColor,
            error = DangerCrimson,
            onError = Color.White
        )
        else -> lightColorScheme(
            primary = primaryAccent,
            onPrimary = Color.White,
            primaryContainer = secondaryAccent.copy(alpha = 0.18f),
            onPrimaryContainer = Color(0xFF082F49),
            secondary = secondaryAccent,
            onSecondary = Color.White,
            secondaryContainer = surfaceVariantColor,
            onSecondaryContainer = Color(0xFF0F172A),
            background = bgColor,
            onBackground = textPrimaryColor,
            surface = surfaceColor,
            onSurface = textPrimaryColor,
            surfaceVariant = surfaceVariantColor,
            onSurfaceVariant = textSecondaryColor,
            error = DangerCrimson,
            onError = Color.White
        )
    }

    val glassTop by animateColorAsState(
        if (darkTheme) GlassFillTopDark else GlassFillTopLight,
        tween(240),
        label = "glassTop"
    )
    val glassBottom by animateColorAsState(
        if (darkTheme) GlassFillBottomDark else GlassFillBottomLight,
        tween(240),
        label = "glassBottom"
    )
    val borderTop by animateColorAsState(
        if (darkTheme) GlassHighlightDark else GlassHighlightLight,
        tween(240),
        label = "borderTop"
    )
    val borderBottom by animateColorAsState(
        if (darkTheme) GlassBorderDark else GlassBorderLight,
        tween(240),
        label = "borderBottom"
    )

    val glassPalette = LiquidGlassPalette(
        isDark = darkTheme,
        primaryAccent = primaryAccent,
        secondaryAccent = secondaryAccent,
        ambientGlow = rawAmbientGlow,
        glassSurfaceTop = glassTop,
        glassSurfaceBottom = glassBottom,
        glassSpecularBorderTop = borderTop,
        glassSpecularBorderBottom = borderBottom,
        textPrimary = textPrimaryColor,
        textSecondary = textSecondaryColor
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalLiquidGlass provides glassPalette,
        LocalContentColor provides textPrimaryColor
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PrismTypography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    PrismScanTheme(
        themeMode = if (darkTheme) AppThemeMode.DARK else AppThemeMode.LIGHT,
        dynamicColor = dynamicColor,
        content = content
    )
}
