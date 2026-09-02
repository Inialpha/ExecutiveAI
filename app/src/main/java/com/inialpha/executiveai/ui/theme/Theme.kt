package com.inialpha.executiveai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ExecutiveColors = darkColorScheme(
    primary = Teal,
    secondary = Cyan,
    background = Navy,
    surface = Card,
    onPrimary = Navy,
    onSecondary = Navy,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = Border
)

@Composable
fun ExecutiveAITheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ExecutiveColors, content = content)
}
