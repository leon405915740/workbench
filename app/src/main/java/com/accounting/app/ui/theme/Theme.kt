package com.accounting.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = WeChatGreen,
    onPrimary = CardWhite,
    primaryContainer = WeChatGreenLight,
    secondary = TextAmount,
    background = BackgroundGray,
    surface = CardWhite,
    onSurface = TextPrimary,
    onBackground = TextPrimary,
    error = TextDelete,
)

@Composable
fun AccountingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
