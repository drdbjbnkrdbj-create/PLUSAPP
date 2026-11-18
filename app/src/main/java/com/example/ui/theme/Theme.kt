package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.ThemeMode

@Immutable
data class PulseSemanticColors(
    val background: Color,
    val secondaryBackground: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val primaryAccent: Color,
    val success: Color,
    val failure: Color,
    val warning: Color,
    val textPrimary: Color,
    val textSecondary: Color
)

val LocalPulseColors = staticCompositionLocalOf {
    PulseSemanticColors(
        background = PulseDarkBackground,
        secondaryBackground = PulseDarkSecondaryBg,
        cardBackground = PulseDarkCard,
        cardBorder = PulseDarkBorder,
        primaryAccent = PulsePrimary,
        success = PulseSuccess,
        failure = PulseFailure,
        warning = PulseWarning,
        textPrimary = PulseDarkTextPrimary,
        textSecondary = PulseDarkTextSecondary
    )
}

private val PulseDarkColorScheme = darkColorScheme(
    primary = PulsePrimary,
    onPrimary = PulseDarkBackground,
    primaryContainer = PulseDarkCard,
    onPrimaryContainer = PulsePrimary,
    secondary = PulseSuccess,
    onSecondary = PulseDarkBackground,
    secondaryContainer = PulseDarkSecondaryBg,
    onSecondaryContainer = PulseDarkTextPrimary,
    tertiary = PulseWarning,
    error = PulseFailure,
    onError = PulseDarkBackground,
    background = PulseDarkBackground,
    onBackground = PulseDarkTextPrimary,
    surface = PulseDarkCard,
    onSurface = PulseDarkTextPrimary,
    surfaceVariant = PulseDarkSecondaryBg,
    onSurfaceVariant = PulseDarkTextSecondary,
    outline = PulseDarkBorder,
    outlineVariant = PulseDarkBorder
)

private val PulseLightColorScheme = lightColorScheme(
    primary = PulseLightPrimary,
    onPrimary = Color.White,
    primaryContainer = PulseLightSecondaryBg,
    onPrimaryContainer = PulseLightPrimary,
    secondary = PulseLightSuccess,
    onSecondary = Color.White,
    secondaryContainer = PulseLightSecondaryBg,
    onSecondaryContainer = PulseLightTextPrimary,
    tertiary = PulseWarning,
    error = PulseLightFailure,
    onError = Color.White,
    background = PulseLightBackground,
    onBackground = PulseLightTextPrimary,
    surface = PulseLightCard,
    onSurface = PulseLightTextPrimary,
    surfaceVariant = PulseLightSecondaryBg,
    onSurfaceVariant = PulseLightTextSecondary,
    outline = PulseLightBorder,
    outlineVariant = PulseLightBorder
)

private val PulseShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PulseTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    language: AppLanguage = AppLanguage.EN,
    content: @Composable () -> Unit
) {
    val isDark = themeMode == ThemeMode.DARK
    val colorScheme = if (isDark) PulseDarkColorScheme else PulseLightColorScheme
    val semanticColors = if (isDark) {
        PulseSemanticColors(
            background = PulseDarkBackground,
            secondaryBackground = PulseDarkSecondaryBg,
            cardBackground = PulseDarkCard,
            cardBorder = PulseDarkBorder,
            primaryAccent = PulsePrimary,
            success = PulseSuccess,
            failure = PulseFailure,
            warning = PulseWarning,
            textPrimary = PulseDarkTextPrimary,
            textSecondary = PulseDarkTextSecondary
        )
    } else {
        PulseSemanticColors(
            background = PulseLightBackground,
            secondaryBackground = PulseLightSecondaryBg,
            cardBackground = PulseLightCard,
            cardBorder = PulseLightBorder,
            primaryAccent = PulseLightPrimary,
            success = PulseLightSuccess,
            failure = PulseLightFailure,
            warning = PulseWarning,
            textPrimary = PulseLightTextPrimary,
            textSecondary = PulseLightTextSecondary
        )
    }

    val layoutDirection = if (language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalPulseColors provides semanticColors,
        LocalLayoutDirection provides layoutDirection
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = pulseTypography(language),
            shapes = PulseShapes,
            content = content
        )
    }
}
