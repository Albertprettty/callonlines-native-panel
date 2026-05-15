package com.callonlines.nativepanel.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Cyan = Color(0xFF22D3EE)
private val Purple = Color(0xFFA855F7)
private val Bg = Color(0xFF07111F)

private val Colors = darkColorScheme(
    primary = Cyan,
    secondary = Purple,
    tertiary = Purple,
    background = Bg,
    surface = Color(0xFF0F1F33),
    onPrimary = Color(0xFF06111F),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
)

@Composable
fun CallOnLinesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        content = content,
    )
}
