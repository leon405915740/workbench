package com.aigrowth.os.ui.theme

import androidx.compose.ui.graphics.Color

// ===== 生活手记 · 统一设计 token（参考 android-ui-mockups） =====

// 页面与表面（暖米白）
val PaperBg = Color(0xFFF7F6F3)
val PaperCard = Color(0xFFFFFFFF)
val PaperNested = Color(0xFFF1F0EC)

// 边框
val PaperBorder = Color(0xFFE8E6E1)
val PaperBorderStrong = Color(0xFFDEDCD6)

// 文字（深灰墨）
val InkText = Color(0xFF1F1F1F)
val InkSecondary = Color(0xFF6B6B6B)
val InkTertiary = Color(0xFF9A9A9A)

// 主色（深青绿）
val AccentGreen = Color(0xFF397565)
val AccentGreenSoft = Color(0xFFE8F0ED)
val OnAccentGreen = Color(0xFFFFFFFF)

// 模块装饰色（绿/蓝/赭/陶/橄榄）
val ModuleGreen = Color(0xFF397565)
val ModuleBlue = Color(0xFF4A7C8C)
val ModuleOchre = Color(0xFFA67C00)
val ModuleClay = Color(0xFFA85D4A)
val ModuleOlive = Color(0xFF7A7A5A)

// 危险色
val DangerInk = Color(0xFFC45C4A)
val DangerInkMuted = Color(0xFFF7E8E5)

// 抽屉 / 侧栏（深色渐变面板）
val DrawerBg = Color(0xFF252522)
val DrawerBgTop = Color(0xFF2F2F2C)
val DrawerText = Color(0xFFF7F6F3)

// ===== 兼容旧命名别名（现有屏幕代码引用，值映射到生活手记 token，保证编译通过；新代码请使用上方新 token） =====
val WeChatGreen = AccentGreen
val WeChatGreenLight = AccentGreenSoft
val WeChatGreenDark = Color(0xFF2E5C4E)

val BackgroundGray = PaperBg
val CardWhite = PaperCard

val TextPrimary = InkText
val TextSecondary = InkSecondary
val TextAmount = ModuleGreen
val TextIncome = ModuleGreen
val TextDelete = DangerInk

val BubbleUser = Color(0xFFE8F0ED)
val BubbleAi = PaperNested
val BubbleError = DangerInkMuted

val ConfidenceHigh = ModuleGreen
val ConfidenceMedium = ModuleOchre
val ConfidenceLow = DangerInk

val NavInactive = InkTertiary
val NavActive = ModuleGreen

val DividerColor = PaperBorder
val SageGreen = Color(0xFFB9C6C0)
val CardShadow = Color(0xFF1F1F1F)
val BorderDefault = PaperBorderStrong

val Primary = AccentGreen
val PrimaryDark = Color(0xFF2E5C4E)
val PrimaryLight = AccentGreenSoft
val Secondary = ModuleGreen
val SecondaryDark = Color(0xFF2E5C4E)
val SecondaryLight = Color(0xFFB9C6C0)
val Background = PaperBg
val BackgroundDark = DrawerBg
val Surface = PaperCard
val SurfaceDark = Color(0xFF2E5C4E)
val OnBackground = InkText
val OnBackgroundDark = PaperCard
val OnSurface = InkText
val OnSurfaceDark = DrawerText
val LearningAccent = ModuleGreen
val GrowthAccent = ModuleOchre
val CreatorAccent = ModuleClay
val Success = ModuleGreen
val Warning = ModuleOchre
val Error = DangerInk
val Info = ModuleBlue
