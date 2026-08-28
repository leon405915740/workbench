package com.aigrowth.os.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ===== 记账 App 设计体系（莫兰迪雾蓝紫）核心组件 =====

object Morandi {
    val BrandPrimary = Color(0xFF78B9A6)
    val BrandPrimaryLight = Color(0xFFA7D4C5)
    val BrandPrimaryDark = Color(0xFF397565)
    val BackgroundGray = Color(0xFFF8F8F4)
    val CardWhite = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF403F3A)
    val TextSecondary = Color(0xFF777A74)
    val TextIncome = Color(0xFF3D8B6F)
    val TextDelete = Color(0xFFC25B5B)
    val TextWarning = Color(0xFFD4923F)
    val NavInactive = Color(0xFF89918A)
    val DividerColor = Color(0xFFE6E1D5)
    val BorderDefault = Color(0xFFE2DDCF)
    val CardShadow = Color(0xFF6C756F)
    val BubbleAi = Color(0xFFF0F4F1)

    /** 品牌渐变（135°，供总览卡/按钮使用） */
    val BrandGradient: List<Color> = listOf(BrandPrimaryLight, BrandPrimary, BrandPrimaryDark)

    /** 收入/成长正向渐变 */
    val IncomeGradient: List<Color> = listOf(Color(0xFF6FD4B8), TextIncome, Color(0xFF2E6B55))
}

/**
 * 通用莫兰迪卡片：圆角 + 品牌色柔阴影 + 白底。
 * 对应记账规范：通用卡片 16dp 圆角、shadow(blur=12dp, y=3dp, #6366A0@8%)、内边距 16dp。
 */
@Composable
fun MorandiCard(
    modifier: Modifier = Modifier,
    radius: Dp = 16.dp,
    shadowBlur: Dp = 12.dp,
    shadowAlpha: Float = 0.08f,
    background: Color = Morandi.CardWhite,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = shadowBlur,
                shape = RoundedCornerShape(radius),
                ambientColor = Morandi.CardShadow.copy(alpha = shadowAlpha),
                spotColor = Morandi.CardShadow.copy(alpha = shadowAlpha)
            )
            .clip(RoundedCornerShape(radius))
            .background(background)
            .padding(contentPadding),
        content = content
    )
}

/**
 * 渐变总览卡：135° 品牌渐变 + 白字 + 大柔阴影（20dp 圆角，24dp 内边距）。
 */
@Composable
fun GradientSummaryCard(
    modifier: Modifier = Modifier,
    colors: List<Color> = Morandi.BrandGradient,
    radius: Dp = 20.dp,
    shadowAlpha: Float = 0.20f,
    contentPadding: PaddingValues = PaddingValues(24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(radius),
                ambientColor = Morandi.CardShadow.copy(alpha = shadowAlpha),
                spotColor = Morandi.CardShadow.copy(alpha = shadowAlpha)
            )
            .clip(RoundedCornerShape(radius))
            .background(Brush.linearGradient(colors = colors))
            .padding(contentPadding),
        content = content
    )
}

/**
 * 胶囊 Tab 组：灰底 24dp 圆角容器，选中项白底 + 品牌色文字 + 轻阴影。
 */
@Composable
fun CapsuleTabGroup(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Morandi.BackgroundGray)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (selected) {
                            Modifier
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(20.dp),
                                    ambientColor = Morandi.CardShadow.copy(alpha = 0.12f),
                                    spotColor = Morandi.CardShadow.copy(alpha = 0.12f)
                                )
                                .clip(RoundedCornerShape(20.dp))
                                .background(Morandi.CardWhite)
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelected(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Morandi.BrandPrimary else Morandi.TextSecondary
                )
            }
        }
    }
}

/**
 * 渐变按钮（主操作按钮，135° 品牌渐变）。
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: List<Color> = listOf(Morandi.BrandPrimaryLight, Morandi.BrandPrimary)
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled) Brush.linearGradient(colors = colors)
                else Brush.linearGradient(colors = listOf(Morandi.DividerColor, Morandi.DividerColor))
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) Morandi.CardWhite else Morandi.TextSecondary
        )
    }
}

/**
 * 渐变进度条：圆角胶囊 + 90° 品牌渐变。
 */
@Composable
fun GradientProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(Morandi.BrandPrimary, Color(0xFF8B8FC8))
) {
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Morandi.DividerColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(4.dp))
                .background(Brush.horizontalGradient(colors = colors))
        )
    }
}

/**
 * 分区标题（左标题 + 可选右操作）。
 */
@Composable
fun MorandiSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Morandi.TextPrimary
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.bodySmall,
                color = Morandi.TextSecondary,
                modifier = Modifier.clickable(onClick = onActionClick)
            )
        }
    }
}

/**
 * 空状态占位（图标 + 标题 + 说明）。
 */
@Composable
fun MorandiEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = Morandi.NavInactive
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Morandi.TextPrimary
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Morandi.TextSecondary
        )
    }
}
