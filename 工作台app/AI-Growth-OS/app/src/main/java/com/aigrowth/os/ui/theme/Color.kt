package com.aigrowth.os.ui.theme

import androidx.compose.ui.graphics.Color

// ===== 莫兰迪雾蓝紫设计体系（移植自记账 App v2 配色，对比度达 WCAG AA） =====

// 品牌主色
val WeChatGreen = Color(0xFF6366A0)
val WeChatGreenLight = Color(0xFFECEAF4)
val WeChatGreenDark = Color(0xFF4A4E7C)

// 背景与卡片
val BackgroundGray = Color(0xFFF5F6FA)
val CardWhite = Color(0xFFFFFFFF)

// 文字
val TextPrimary = Color(0xFF33374C)
val TextSecondary = Color(0xFF878B9E)
val TextAmount = Color(0xFF6366A0)
val TextIncome = Color(0xFF3D8B6F)
val TextDelete = Color(0xFFC25B5B)

// 气泡
val BubbleUser = Color(0xFFD4D0E8)
val BubbleAi = Color(0xFFF2F1F7)
val BubbleError = Color(0xFFF8E8E8)

// 置信度 / 状态
val ConfidenceHigh = Color(0xFF3D8B6F)
val ConfidenceMedium = Color(0xFFD4923F)
val ConfidenceLow = Color(0xFFC25B5B)

// 导航
val NavInactive = Color(0xFF9CA0B0)
val NavActive = Color(0xFF6366A0)

// 辅助色
val DividerColor = Color(0xFFE2E1EA)
val SageGreen = Color(0xFF8FD0BE)
val CardShadow = Color(0xFF6366A0)
val BorderDefault = Color(0xFFDDDCE6)

// ===== 兼容工作台旧命名的别名（现有屏幕代码引用，值已替换为莫兰迪体系） =====
val Primary = WeChatGreen
val PrimaryDark = WeChatGreenDark
val PrimaryLight = WeChatGreenLight
val Secondary = TextIncome
val SecondaryDark = Color(0xFF2E6B55)
val SecondaryLight = Color(0xFF6FD4B8)
val Background = BackgroundGray
val BackgroundDark = Color(0xFF33374C)
val Surface = CardWhite
val SurfaceDark = Color(0xFF3A3E55)
val OnBackground = TextPrimary
val OnBackgroundDark = CardWhite
val OnSurface = TextPrimary
val OnSurfaceDark = Color(0xFFF1F2F8)
val LearningAccent = WeChatGreen
val GrowthAccent = TextIncome
val CreatorAccent = ConfidenceMedium
val Success = TextIncome
val Warning = ConfidenceMedium
val Error = TextDelete
val Info = WeChatGreen
