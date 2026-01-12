package com.illa.cashvan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun CashVanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Check system setting
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable dynamic colors
    content: @Composable () -> Unit
) {
    // ALWAYS use light color scheme - ignore darkTheme parameter
    // This ensures the app stays in light mode regardless of system settings
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}