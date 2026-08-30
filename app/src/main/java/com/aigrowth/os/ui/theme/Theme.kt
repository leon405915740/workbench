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
 * 工作台主题：生活手记统一色板（暖米白画布 + 深青绿主色 + 模块装饰色）。
 * 关闭动态取色与深色模式，保证全局视觉一致。
 */
private val MorandiColorScheme = lightColorScheme(
    primary = AccentGreen,
    onPrimary = OnAccentGreen,
    primaryContainer = AccentGreenSoft,
    onPrimaryContainer = PrimaryDark,
    secondary = ModuleGreen,
    onSecondary = Color.White,
    secondaryContainer = AccentGreenSoft,
    onSecondaryContainer = SecondaryDark,
    tertiary = ModuleOchre,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF6E7CF),
    onTertiaryContainer = Color(0xFFA67C00),
    background = PaperBg,
    onBackground = InkText,
    surface = PaperCard,
    onSurface = InkText,
    surfaceVariant = PaperNested,
    onSurfaceVariant = InkSecondary,
    surfaceTint = AccentGreen,
    inverseSurface = DrawerBg,
    inverseOnSurface = PaperCard,
    inversePrimary = AccentGreenSoft,
    error = DangerInk,
    onError = Color.White,
    errorContainer = DangerInkMuted,
    onErrorContainer = DangerInk,
    outline = PaperBorderStrong,
    outlineVariant = PaperBorder,
    scrim = Color(0x33000000),
    surfaceContainerLowest = PaperCard,
    surfaceContainerLow = PaperCard,
    surfaceContainer = PaperNested,
    surfaceContainerHigh = AccentGreenSoft,
    surfaceContainerHighest = PaperBorder,
    surfaceBright = PaperCard,
    surfaceDim = PaperNested,
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
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
