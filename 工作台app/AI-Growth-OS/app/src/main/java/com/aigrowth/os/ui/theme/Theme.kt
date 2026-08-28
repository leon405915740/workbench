package com.aigrowth.os.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 工作台主题：沿用记账 App 的莫兰迪雾蓝紫浅色体系。
 * 关闭动态取色与深色模式，保证全局视觉一致。
 */
private val MorandiColorScheme = lightColorScheme(
    primary = WeChatGreen,
    onPrimary = Color.White,
    primaryContainer = WeChatGreenLight,
    onPrimaryContainer = WeChatGreenDark,
    secondary = TextIncome,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F0EA),
    onSecondaryContainer = Color(0xFF2E6B55),
    tertiary = ConfidenceMedium,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF9EEDC),
    onTertiaryContainer = Color(0xFF8A5B1F),
    background = BackgroundGray,
    onBackground = TextPrimary,
    surface = CardWhite,
    onSurface = TextPrimary,
    surfaceVariant = WeChatGreenLight,
    onSurfaceVariant = TextSecondary,
    surfaceTint = WeChatGreen,
    inverseSurface = WeChatGreenDark,
    inverseOnSurface = CardWhite,
    inversePrimary = WeChatGreenLight,
    error = TextDelete,
    onError = Color.White,
    errorContainer = BubbleError,
    onErrorContainer = TextDelete,
    outline = BorderDefault,
    outlineVariant = DividerColor,
    scrim = Color(0x33000000),
    surfaceContainerLowest = CardWhite,
    surfaceContainerLow = CardWhite,
    surfaceContainer = BackgroundGray,
    surfaceContainerHigh = WeChatGreenLight,
    surfaceContainerHighest = DividerColor,
    surfaceBright = CardWhite,
    surfaceDim = BubbleAi,
)

@Composable
fun AIGrowthOSTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = MorandiColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
