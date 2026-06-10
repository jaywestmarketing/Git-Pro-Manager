package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPurple,
    onPrimary = CardBackground,
    primaryContainer = PrimaryPurpleContainer,
    onPrimaryContainer = OnPrimaryPurpleContainer,
    secondary = SecondaryBlueAccent,
    secondaryContainer = SecondaryBlueContainer,
    onSecondaryContainer = OnSecondaryBlueContainer,
    background = BgLight,
    surface = CardBackground,
    onBackground = TextMain,
    onSurface = TextMain,
    onSurfaceVariant = SubtitleTextColor,
    error = StatusOverdueText
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColorScheme, typography = Typography, content = content)
}
