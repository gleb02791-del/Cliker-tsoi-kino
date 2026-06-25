package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RockColorScheme = darkColorScheme(
    primary = RockYellow,
    secondary = RockRed,
    tertiary = RockWhite,
    background = RockBlack,
    surface = RockDarkGrey,
    onPrimary = RockBlack,
    onSecondary = RockWhite,
    onBackground = RockWhite,
    onSurface = RockWhite
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RockColorScheme,
        typography = Typography,
        content = content
    )
}
