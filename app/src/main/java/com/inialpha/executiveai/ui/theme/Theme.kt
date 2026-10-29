package com.inialpha.executiveai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ExecutiveColors = lightColorScheme(
    primary = Gold,
    onPrimary = Card,
    primaryContainer = GoldSoft,
    onPrimaryContainer = GoldDeep,
    secondary = Slate,
    onSecondary = Card,
    secondaryContainer = GoldSoft,
    onSecondaryContainer = GoldDeep,
    background = Canvas,
    onBackground = TextPrimary,
    surface = Card,
    onSurface = TextPrimary,
    surfaceVariant = SlateSoft,
    onSurfaceVariant = TextSecondary,
    outline = Border,
    outlineVariant = Border,
)

@Composable
fun ExecutiveAITheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ExecutiveColors, content = content)
}
