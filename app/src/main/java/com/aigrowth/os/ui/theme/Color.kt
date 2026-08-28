package com.aigrowth.os.ui.theme

import androidx.compose.ui.graphics.Color

// ===== 猫咪生活报 · 复古报刊编辑部色板（卡通小猫主题） =====

// 页面与表面（米白旧报纸）
val PaperBg = Color(0xFFF2E9D5)
val PaperCard = Color(0xFFFAF5E9)
val PaperNested = Color(0xFFF5EDDA)

// 边框
val PaperBorder = Color(0xFFE6DCC6)
val PaperBorderStrong = Color(0xFFD8C9A9)

// 文字（浅棕墨字）
val InkText = Color(0xFF4A3B28)
val InkSecondary = Color(0xFF8A7457)
val InkTertiary = Color(0xFFB19A77)

// 主色（橘黄点缀，特色主色）
val AccentOrange = Color(0xFFD9832B)
val AccentOrangeMuted = Color(0xFFF6E7CF)
val OnAccentOrange = Color(0xFF3A2A18)

// 模块色（复古暖色：墨绿 / 墨蓝 / 赭黄 / 陶土 / 橄榄）
val ModuleGreen = Color(0xFF3F6B52)
val ModuleBlue = Color(0xFF5B718F)
val ModuleOchre = Color(0xFFB5742C)
val ModuleClay = Color(0xFFB35B3E)
val ModuleOlive = Color(0xFF8A6D44)

// 危险色
val DangerInk = Color(0xFFB3483A)
val DangerInkMuted = Color(0xFFF6E2DB)

// 抽屉 / 侧栏（旧墨棕面板）
val DrawerBg = Color(0xFF4A3828)
val DrawerBgTop = Color(0xFF5A4531)
val DrawerText = Color(0xFFF6ECD7)

// ===== 兼容旧命名别名（现有屏幕代码引用，值已替换为报刊色板，保证编译通过） =====
val WeChatGreen = AccentOrange
val WeChatGreenLight = AccentOrangeMuted
val WeChatGreenDark = Color(0xFF8A5B1F)

val BackgroundGray = PaperBg
val CardWhite = PaperCard

val TextPrimary = InkText
val TextSecondary = InkSecondary
val TextAmount = ModuleGreen
val TextIncome = ModuleGreen
val TextDelete = DangerInk

val BubbleUser = Color(0xFFEADFC6)
val BubbleAi = PaperNested
val BubbleError = DangerInkMuted

val ConfidenceHigh = ModuleGreen
val ConfidenceMedium = ModuleOchre
val ConfidenceLow = DangerInk

val NavInactive = InkTertiary
val NavActive = ModuleGreen

val DividerColor = PaperBorder
val SageGreen = Color(0xFF9DBAA9)
val CardShadow = Color(0xFF4A3B28)
val BorderDefault = PaperBorderStrong

val Primary = AccentOrange
val PrimaryDark = Color(0xFF8A5B1F)
val PrimaryLight = AccentOrangeMuted
val Secondary = ModuleGreen
val SecondaryDark = Color(0xFF2E553F)
val SecondaryLight = Color(0xFF9DBAA9)
val Background = PaperBg
val BackgroundDark = DrawerBg
val Surface = PaperCard
val SurfaceDark = Color(0xFF3A2D1C)
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