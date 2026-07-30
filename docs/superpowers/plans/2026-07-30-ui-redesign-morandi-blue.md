# UI 美化：莫兰迪雾蓝配色 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将记账 App 从微信绿主色调升级为莫兰迪雾蓝冷调配色系统，同时提升卡片质感（渐变、阴影、圆角），不改变任何功能逻辑和布局结构。

**Architecture:** 采用"变量名不变、改值不改名"策略——先更新 `Color.kt` 中所有色值定义（全局生效），再逐一修复各文件中的硬编码色值。新增两个辅助色变量（`DividerColor`、`SageGreen`）用于渐变和分割线。SummaryCard 圆角从 16dp 提升到 20dp，渐变改为莫兰迪色系。

**Tech Stack:** Kotlin + Jetpack Compose + Material3

---

## File Structure

### 修改文件

| 文件 | 职责 | 改动类型 |
|------|------|----------|
| `app/src/main/java/com/accounting/app/ui/theme/Color.kt` | 全部色值定义 | 改值 + 新增变量 |
| `app/src/main/java/com/accounting/app/ui/theme/Theme.kt` | LightColorScheme 引用 | 新增 secondaryContainer 引用 |
| `app/src/main/java/com/accounting/app/ui/screens/DashboardScreen.kt` | 统计页 | 硬编码渐变色 + Tab字色 + 进度条轨道色 + SummaryCard 圆角 |
| `app/src/main/java/com/accounting/app/ui/screens/ChatScreen.kt` | 记账页 | 输入栏分隔线 + TextField 指示线 |
| `app/src/main/java/com/accounting/app/ui/screens/SettingsScreen.kt` | 设置页 | 分割线 + TextField 指示线 |
| `app/src/main/java/com/accounting/app/ui/components/PlanPreviewCard.kt` | 计划预览卡 | 按钮底色 + 分割线 + Action 色 |
| `app/src/main/java/com/accounting/app/ui/components/CategorySelector.kt` | 分类选择器 | 硬编码 #07C160 ×2 |
| `app/src/main/java/com/accounting/app/ui/components/RuleSuggestionDialog.kt` | 规则建议弹窗 | Color.White/Gray + 按钮底色 |
| `app/src/main/java/com/accounting/app/ui/screens/MemoryManageScreen.kt` | 记忆管理页 | TextField 指示线 + 来源标签底色 |
| `app/src/main/java/com/accounting/app/ui/screens/categorymanagescreen.kt` | 分类管理页 | TextField 指示线 ×3 |
| `app/src/main/java/com/accounting/app/ui/screens/MappingManageScreen.kt` | 映射管理页 | TextField 指示线 + Switch 轨道色 |
| `app/build.gradle.kts` | 版本号 | versionCode +1, versionName 升级 |

### 不改动文件

- `Type.kt` — 不涉及色值
- `themes.xml` — 仅 Compose 层改动
- `MainActivity.kt` — 引用 `NavActive`/`NavInactive` 变量，自动继承新色值，无需改
- `ExpenseCard.kt` — 引用主题变量，无硬编码，自动继承
- `CategoryPicker.kt` — 引用主题变量，无硬编码，自动继承
- `ManualEntryDialog.kt` — 引用主题变量，无硬编码，自动继承

---

## Task 1: 更新 Color.kt 色值定义（核心）

**Files:**
- Modify: `app/src/main/java/com/accounting/app/ui/theme/Color.kt`

**说明：** 这是全局核心文件。改值后，所有引用 `WeChatGreen`、`TextAmount` 等变量的文件自动继承新色值。同时新增两个辅助色变量。

- [ ] **Step 1: 替换全部色值定义**

将 `Color.kt` 全文替换为以下内容：

```kotlin
package com.accounting.app.ui.theme

import androidx.compose.ui.graphics.Color

// 主色系 — 雾蓝（原变量名保持不变）
val WeChatGreen = Color(0xFF7C81A4)
val WeChatGreenLight = Color(0xFFE8E8EE)
val WeChatGreenDark = Color(0xFF626788)

// 背景
val BackgroundGray = Color(0xFFF0F0F4)
val CardWhite = Color(0xFFFFFFFF)

// 文字
val TextPrimary = Color(0xFF5A5F7A)
val TextSecondary = Color(0xFFBFBCCB)
val TextAmount = Color(0xFF7C81A4)
val TextIncome = Color(0xFF527A72)
val TextDelete = Color(0xFFC97A7A)

// 气泡
val BubbleUser = Color(0xFFCEC7D9)
val BubbleAi = Color(0xFFF5F4F8)
val BubbleError = Color(0xFFF5EEEE)

// 置信度
val ConfidenceHigh = Color(0xFF527A72)
val ConfidenceMedium = Color(0xFFC49A5C)
val ConfidenceLow = Color(0xFFC97A7A)

// 导航
val NavInactive = Color(0xFFBFBCCB)
val NavActive = Color(0xFF7C81A4)

// 辅助色（新增）
val DividerColor = Color(0xFFE8E8EE)
val SageGreen = Color(0xFF99BFB2)
```

- [ ] **Step 2: 编译验证**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL（Color.kt 新增了两个变量，现有引用不受影响）

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/accounting/app/ui/theme/Color.kt
git commit -m "refactor(theme): 更新色值为莫兰迪雾蓝配色，新增 DividerColor 和 SageGreen"
```

---

## Task 2: 更新 Theme.kt LightColorScheme

**Files:**
- Modify: `app/src/main/java/com/accounting/app/ui/theme/Theme.kt`

**说明：** 添加 `secondaryContainer` 引用 `DividerColor`，使 Material3 组件（如进度条轨道）使用新分割线色。

- [ ] **Step 1: 更新 LightColorScheme**

将 `Theme.kt` 中 `LightColorScheme` 定义替换为：

```kotlin
private val LightColorScheme = lightColorScheme(
    primary = WeChatGreen,
    onPrimary = CardWhite,
    primaryContainer = WeChatGreenLight,
    secondary = TextAmount,
    secondaryContainer = DividerColor,
    background = BackgroundGray,
    surface = CardWhite,
    onSurface = TextPrimary,
    onBackground = TextPrimary,
    error = TextDelete,
)
```

- [ ] **Step 2: 编译验证**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/accounting/app/ui/theme/Theme.kt
git commit -m "refactor(theme): LightColorScheme 新增 secondaryContainer 引用 DividerColor"
```

---

## Task 3: DashboardScreen.kt 硬编码色值 + SummaryCard 质感

**Files:**
- Modify: `app/src/main/java/com/accounting/app/ui/screens/DashboardScreen.kt`

**说明：** 此文件有 4 处硬编码色值需替换 + SummaryCard 圆角从 16dp 提升到 20dp + 渐变改为莫兰迪色系。需新增 `DividerColor` 和 `SageGreen` 的 import。

- [ ] **Step 1: 新增 import**

在 DashboardScreen.kt 的 import 区域（`import com.accounting.app.ui.theme.TextSecondary` 之后）新增：

```kotlin
import com.accounting.app.ui.theme.DividerColor
import com.accounting.app.ui.theme.SageGreen
```

- [ ] **Step 2: 替换 DashTabButton 未选中文字色**

找到 DashTabButton 函数中的：

```kotlin
            color = if (isSelected) CardWhite else Color(0xFF666666)
```

替换为：

```kotlin
            color = if (isSelected) CardWhite else TextSecondary
```

- [ ] **Step 3: 替换 SummaryCard 渐变色 + 圆角**

找到 SummaryCard 函数中的渐变定义：

```kotlin
    val gradient = if (isExpense) {
        Brush.linearGradient(listOf(WeChatGreenLight, Color(0xFF5BECA3), WeChatGreen))
    } else {
        Brush.linearGradient(listOf(Color(0xFFDBEAFE), Color(0xFF93C5FD), TextAmount))
    }
```

替换为：

```kotlin
    val gradient = if (isExpense) {
        Brush.linearGradient(listOf(Color(0xFF8B91B0), WeChatGreen, WeChatGreenDark))
    } else {
        Brush.linearGradient(listOf(SageGreen, TextIncome, Color(0xFF496B61)))
    }
```

然后找到 SummaryCard 的 Box modifier 中的圆角：

```kotlin
            .clip(RoundedCornerShape(16.dp))
```

替换为：

```kotlin
            .clip(RoundedCornerShape(20.dp))
```

- [ ] **Step 4: 替换进度条轨道色**

找到 CategoryRow 函数中的 LinearProgressIndicator：

```kotlin
            trackColor = Color(0xFFE5E5E5)
```

替换为：

```kotlin
            trackColor = DividerColor
```

- [ ] **Step 5: 编译验证**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/accounting/app/ui/screens/DashboardScreen.kt
git commit -m "refactor(dashboard): 替换硬编码色值为莫兰迪色系，SummaryCard 圆角提升至20dp"
```

---

## Task 4: ChatScreen.kt 硬编码色值

**Files:**
- Modify: `app/src/main/java/com/accounting/app/ui/screens/ChatScreen.kt`

**说明：** 2 处硬编码色值：输入栏顶部分隔线 `Color(0xFFE7E5E4)` 和 LearnConfirmDialog 的 TextField 指示线 `Color(0xFFE5E5E5)`。需新增 `DividerColor` import。

- [ ] **Step 1: 新增 import**

在 ChatScreen.kt 的 import 区域（`import com.accounting.app.ui.theme.CardWhite` 之后）新增：

```kotlin
import com.accounting.app.ui.theme.DividerColor
```

- [ ] **Step 2: 替换输入栏分隔线色**

找到 BottomInputBar 函数中的：

```kotlin
                .background(Color(0xFFE7E5E4))
```

替换为：

```kotlin
                .background(DividerColor)
```

- [ ] **Step 3: 替换 LearnConfirmDialog TextField 指示线色**

找到 LearnConfirmDialog 函数中的：

```kotlin
                        unfocusedIndicatorColor = Color(0xFFE5E5E5)
```

替换为：

```kotlin
                        unfocusedIndicatorColor = DividerColor
```

- [ ] **Step 4: 编译验证**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/accounting/app/ui/screens/ChatScreen.kt
git commit -m "refactor(chat): 替换输入栏分隔线和TextField指示线为 DividerColor"
```

---

## Task 5: SettingsScreen.kt 硬编码色值

**Files:**
- Modify: `app/src/main/java/com/accounting/app/ui/screens/SettingsScreen.kt`

**说明：** 2 处硬编码色值：SettingsDivider 的 `Color(0xFFEFEFEF)` 和 ApiKeyDialog 的 TextField 指示线 `Color(0xFFE5E5E5)`。需新增 `DividerColor` import。

- [ ] **Step 1: 新增 import**

在 SettingsScreen.kt 的 import 区域（`import com.accounting.app.ui.theme.CardWhite` 之后）新增：

```kotlin
import com.accounting.app.ui.theme.DividerColor
```

- [ ] **Step 2: 替换 SettingsDivider 背景色**

找到 SettingsDivider 函数中的：

```kotlin
            .background(Color(0xFFEFEFEF))
```

替换为：

```kotlin
            .background(DividerColor)
```

- [ ] **Step 3: 替换 ApiKeyDialog TextField 指示线色**

找到 ApiKeyDialog 函数中的：

```kotlin
                    unfocusedIndicatorColor = Color(0xFFE5E5E5)
```

替换为：

```kotlin
                    unfocusedIndicatorColor = DividerColor
```

- [ ] **Step 4: 编译验证**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/accounting/app/ui/screens/SettingsScreen.kt
git commit -m "refactor(settings): 替换分割线和TextField指示线为 DividerColor"
```

---

## Task 6: PlanPreviewCard.kt 硬编码色值

**Files:**
- Modify: `app/src/main/java/com/accounting/app/ui/components/PlanPreviewCard.kt`

**说明：** 4 处硬编码色值：展开按钮底色 `Color(0xFFF5F5F5)` ×2、分割线 `Color(0xFFEFEFEF)` ×2、UPDATE 色 `Color(0xFFF5A623)`、DELETE 色 `Color(0xFFE53935)`。需新增 `DividerColor`、`BackgroundGray`、`ConfidenceMedium`、`TextDelete` import（部分已 import 则跳过）。

- [ ] **Step 1: 新增 import**

在 PlanPreviewCard.kt 的 import 区域（`import com.accounting.app.ui.theme.*` 已存在，所以 `BackgroundGray`、`TextDelete`、`ConfidenceMedium` 已可用）。但 `DividerColor` 不在通配符范围内（因为它是新增变量，通配符 import 已覆盖）。

确认 import 行 `import com.accounting.app.ui.theme.*` 存在。如果存在，则 `DividerColor` 已通过通配符导入，无需额外 import。

- [ ] **Step 2: 替换展开按钮底色**

找到 PlanPreviewCard 函数中第一处（展开明细按钮）：

```kotlin
                            containerColor = Color(0xFFF5F5F5),
```

替换为：

```kotlin
                            containerColor = BackgroundGray,
```

- [ ] **Step 3: 替换取消按钮底色**

找到 PlanPreviewCard 函数中第二处（取消按钮）：

```kotlin
                    containerColor = Color(0xFFF5F5F5),
```

替换为：

```kotlin
                    containerColor = BackgroundGray,
```

- [ ] **Step 4: 替换两处分割线色**

找到 PlanPreviewCard 函数中两处：

```kotlin
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEFEFEF)))
```

替换为（两处都要改）：

```kotlin
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))
```

- [ ] **Step 5: 替换 getActionColor 函数中的硬编码色**

找到 getActionColor 函数：

```kotlin
private fun getActionColor(action: PlanAction): Color {
    return when (action) {
        PlanAction.ADD -> WeChatGreen
        PlanAction.UPDATE -> Color(0xFFF5A623)
        PlanAction.DELETE -> Color(0xFFE53935)
    }
}
```

替换为：

```kotlin
private fun getActionColor(action: PlanAction): Color {
    return when (action) {
        PlanAction.ADD -> WeChatGreen
        PlanAction.UPDATE -> ConfidenceMedium
        PlanAction.DELETE -> TextDelete
    }
}
```

- [ ] **Step 6: 编译验证**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/accounting/app/ui/components/PlanPreviewCard.kt
git commit -m "refactor(plan-preview): 替换硬编码色值为主题变量引用"
```

---

## Task 7: CategorySelector.kt 硬编码色值

**Files:**
- Modify: `app/src/main/java/com/accounting/app/ui/components/CategorySelector.kt`

**说明：** 2 处硬编码 `Color(0xFF07C160)` 需替换为 `WeChatGreen` 变量引用。需新增 import。

- [ ] **Step 1: 新增 import**

在 CategorySelector.kt 的 import 区域新增（在 `import androidx.compose.ui.graphics.Color` 之后）：

```kotlin
import com.accounting.app.ui.theme.WeChatGreen
```

- [ ] **Step 2: 替换第一处硬编码色**

找到 CategorySelector 函数中（一级分类选中态）：

```kotlin
                color = if (id == selectedCategoryId) Color(0xFF07C160) else Color.Unspecified,
```

替换为：

```kotlin
                color = if (id == selectedCategoryId) WeChatGreen else Color.Unspecified,
```

- [ ] **Step 3: 替换第二处硬编码色**

找到 CategorySelector 函数中（二级分类选中态）：

```kotlin
                        color = if (id == selectedSubcategoryId) Color(0xFF07C160) else Color.Unspecified,
```

替换为：

```kotlin
                        color = if (id == selectedSubcategoryId) WeChatGreen else Color.Unspecified,
```

- [ ] **Step 4: 编译验证**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/accounting/app/ui/components/CategorySelector.kt
git commit -m "refactor(category-selector): 替换硬编码微信绿为 WeChatGreen 变量"
```

---

## Task 8: RuleSuggestionDialog.kt 硬编码色值

**Files:**
- Modify: `app/src/main/java/com/accounting/app/ui/components/RuleSuggestionDialog.kt`

**说明：** 此文件使用 `Color.White`、`Color.Black`、`Color.Gray` 等 Material3 未引用主题色的硬编码。需替换为主题变量以保持配色一致性。需新增多个 import。

- [ ] **Step 1: 新增 import**

在 RuleSuggestionDialog.kt 的 import 区域，将现有：

```kotlin
import com.accounting.app.ui.theme.WeChatGreen
```

替换为：

```kotlin
import com.accounting.app.ui.theme.BackgroundGray
import com.accounting.app.ui.theme.CardWhite
import com.accounting.app.ui.theme.TextPrimary
import com.accounting.app.ui.theme.TextSecondary
import com.accounting.app.ui.theme.WeChatGreen
```

- [ ] **Step 2: 替换 Card 容器色和标题色**

找到 Card 的 colors 参数：

```kotlin
            colors = CardDefaults.cardColors(containerColor = Color.White),
```

替换为：

```kotlin
            colors = CardDefaults.cardColors(containerColor = CardWhite),
```

找到标题 Text：

```kotlin
                    color = Color.Black,
```

替换为：

```kotlin
                    color = TextPrimary,
```

- [ ] **Step 3: 替换说明文字色**

找到两处说明 Text 的颜色（`color = Color.Gray`），都替换为：

```kotlin
                    color = TextSecondary,
```

第一处（"当输入包含..."）：

```kotlin
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
```

替换为：

```kotlin
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
```

第二处（"自动归为..."）：

```kotlin
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
```

替换为：

```kotlin
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
```

- [ ] **Step 4: 替换按钮底色和文字色**

找到"仅本次使用"按钮：

```kotlin
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF5F5F5),
                            contentColor = Color.Gray
                        ),
```

替换为：

```kotlin
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BackgroundGray,
                            contentColor = TextSecondary
                        ),
```

找到"保存规则"按钮的 Text：

```kotlin
                        Text("保存规则", fontSize = 14.sp, color = Color.White)
```

替换为：

```kotlin
                        Text("保存规则", fontSize = 14.sp, color = CardWhite)
```

- [ ] **Step 5: 编译验证**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/accounting/app/ui/components/RuleSuggestionDialog.kt
git commit -m "refactor(rule-dialog): 替换 Color.White/Gray 硬编码为主题变量"
```

---

## Task 9: MemoryManageScreen.kt 硬编码色值

**Files:**
- Modify: `app/src/main/java/com/accounting/app/ui/screens/MemoryManageScreen.kt`

**说明：** 2 处硬编码：搜索框 TextField 指示线 `Color(0xFFE5E5E5)`、AddMemoryDialog TextField 指示线 `Color(0xFFE5E5E5)`、来源标签 seed 底色 `Color(0xFFE8E8E8)`。需新增 `DividerColor` import。

- [ ] **Step 1: 新增 import**

在 MemoryManageScreen.kt 的 import 区域（`import com.accounting.app.ui.theme.WeChatGreen` 之后）新增：

```kotlin
import com.accounting.app.ui.theme.DividerColor
```

- [ ] **Step 2: 替换搜索框 TextField 指示线色**

找到搜索框 OutlinedTextField 中的：

```kotlin
                focusedIndicatorColor = WeChatGreen,
                unfocusedIndicatorColor = Color(0xFFE5E5E5)
```

替换为：

```kotlin
                focusedIndicatorColor = WeChatGreen,
                unfocusedIndicatorColor = DividerColor
```

- [ ] **Step 3: 替换来源标签 seed 底色**

找到 sourceBg 的 when 分支：

```kotlin
                                    val sourceBg = when (item.source) {
                                        "seed" -> Color(0xFFE8E8E8)
                                        "auto" -> WeChatGreen.copy(alpha = 0.12f)
                                        else -> NavActive.copy(alpha = 0.12f)
                                    }
```

替换为：

```kotlin
                                    val sourceBg = when (item.source) {
                                        "seed" -> DividerColor
                                        "auto" -> WeChatGreen.copy(alpha = 0.12f)
                                        else -> NavActive.copy(alpha = 0.12f)
                                    }
```

- [ ] **Step 4: 替换 AddMemoryDialog TextField 指示线色**

找到 AddMemoryDialog 中的 OutlinedTextField colors：

```kotlin
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = WeChatGreen, unfocusedIndicatorColor = Color(0xFFE5E5E5)))
```

替换为：

```kotlin
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = WeChatGreen, unfocusedIndicatorColor = DividerColor))
```

- [ ] **Step 5: 编译验证**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/accounting/app/ui/screens/MemoryManageScreen.kt
git commit -m "refactor(memory-manage): 替换 TextField 指示线和来源标签底色为 DividerColor"
```

---

## Task 10: categorymanagescreen.kt 硬编码色值

**Files:**
- Modify: `app/src/main/java/com/accounting/app/ui/screens/categorymanagescreen.kt`

**说明：** 3 处硬编码 `Color(0xFFE5E5E5)`：AddCategoryDialog TextField 指示线、EditCategoryDialog TextField 指示线。需新增 `DividerColor` import。

- [ ] **Step 1: 新增 import**

在 categorymanagescreen.kt 的 import 区域（`import com.accounting.app.ui.theme.WeChatGreenLight` 之后）新增：

```kotlin
import com.accounting.app.ui.theme.DividerColor
```

- [ ] **Step 2: 替换 AddCategoryDialog TextField 指示线色**

找到 AddCategoryDialog 函数中的：

```kotlin
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = WeChatGreen,
                        unfocusedIndicatorColor = Color(0xFFE5E5E5)
                    )
```

替换为：

```kotlin
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = WeChatGreen,
                        unfocusedIndicatorColor = DividerColor
                    )
```

- [ ] **Step 3: 替换 EditCategoryDialog TextField 指示线色**

找到 EditCategoryDialog 函数中的：

```kotlin
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = WeChatGreen,
                    unfocusedIndicatorColor = Color(0xFFE5E5E5)
                )
```

替换为：

```kotlin
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = WeChatGreen,
                    unfocusedIndicatorColor = DividerColor
                )
```

- [ ] **Step 4: 编译验证**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/accounting/app/ui/screens/categorymanagescreen.kt
git commit -m "refactor(category-manage): 替换 TextField 指示线为 DividerColor"
```

---

## Task 11: MappingManageScreen.kt 硬编码色值

**Files:**
- Modify: `app/src/main/java/com/accounting/app/ui/screens/MappingManageScreen.kt`

**说明：** 2 处硬编码：AddMappingDialog TextField 指示线 `Color(0xFFE5E5E5)`、MappingCard Switch 未选中轨道色 `Color(0xFFE5E5E5)`。还需将 `Color.Gray`（Switch 未选中 thumb）替换。需新增 `DividerColor` import。

- [ ] **Step 1: 新增 import**

在 MappingManageScreen.kt 的 import 区域（`import com.accounting.app.ui.theme.WeChatGreen` 之后）新增：

```kotlin
import com.accounting.app.ui.theme.DividerColor
```

- [ ] **Step 2: 替换 AddMappingDialog TextField 指示线色**

找到 AddMappingDialog 函数中的：

```kotlin
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = WeChatGreen,
                        unfocusedIndicatorColor = Color(0xFFE5E5E5)
                    )
```

替换为：

```kotlin
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = WeChatGreen,
                        unfocusedIndicatorColor = DividerColor
                    )
```

- [ ] **Step 3: 替换 MappingCard Switch 轨道色**

找到 MappingCard 函数中 Switch 的 colors：

```kotlin
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = WeChatGreen,
                            checkedTrackColor = WeChatGreen.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFFE5E5E5)
                        )
```

替换为：

```kotlin
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = WeChatGreen,
                            checkedTrackColor = WeChatGreen.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = DividerColor
                        )
```

- [ ] **Step 4: 编译验证**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/accounting/app/ui/screens/MappingManageScreen.kt
git commit -m "refactor(mapping-manage): 替换 TextField 指示线和 Switch 轨道色为 DividerColor"
```

---

## Task 12: 版本号升级

**Files:**
- Modify: `app/build.gradle.kts`

**说明：** 按项目规则，每次代码修改后必须更新版本号。

- [ ] **Step 1: 更新 versionCode 和 versionName**

找到 `app/build.gradle.kts` 中的：

```kotlin
        versionCode = 20
        versionName = "2.10.4"
```

替换为：

```kotlin
        versionCode = 21
        versionName = "2.11.0"
```

- [ ] **Step 2: 编译验证**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle.kts
git commit -m "chore: 版本号升级至 v2.11.0 (versionCode 21) — UI 莫兰迪雾蓝配色"
```

---

## Task 13: 全量编译 + APK 构建

**Files:**
- None (verification only)

**说明：** 所有色值修改完成后，执行完整编译和 APK 构建，确保无编译错误。

- [ ] **Step 1: 清理 + 编译**

Run: `.\gradlew.bat clean :app:assembleDebug`
Expected: BUILD SUCCESSFUL，生成 APK 文件 `记账_v2.11.0_debug.apk`

- [ ] **Step 2: 检查 APK 生成**

Run: `dir app\build\outputs\apk\debug\*.apk`
Expected: 显示 `记账_v2.11.0_debug.apk` 文件

- [ ] **Step 3: 全局搜索残留硬编码色值**

Run: 以下命令检查是否还有旧色值残留：

```
findstr /s /i "07C160\|1677FF\|5BECA3\|DBEAFE\|93C5FD\|F5A623\|E53935\|FFF2F0\|95EC69" app\src\main\java\com\accounting\app\ui\
```

Expected: 无输出（或仅注释中的残留，非代码逻辑）

如果有残留，回到对应 Task 修复。

- [ ] **Step 4: 最终 Commit（如有修复）**

如果 Step 3 发现残留并修复了：

```bash
git add -A
git commit -m "fix: 清理残留硬编码色值"
```

如果没有残留，此步骤跳过。

---

## Self-Review

### 1. Spec coverage

| Spec 要求 | 对应 Task |
|-----------|-----------|
| Color.kt 全部色值更新 | Task 1 |
| Theme.kt LightColorScheme 引用 | Task 2 |
| 新增 DividerColor、SageGreen 辅助色 | Task 1 |
| DashboardScreen 渐变色 + 进度条 + Tab字色 | Task 3 |
| DashboardScreen SummaryCard 圆角 16dp→20dp | Task 3 |
| ChatScreen 分隔线 + 指示线 | Task 4 |
| SettingsScreen 分割线 + 指示线 | Task 5 |
| PlanPreviewCard 按钮底 + 分割线 + Action色 | Task 6 |
| CategorySelector 硬编码 #07C160 ×2 | Task 7 |
| RuleSuggestionDialog Color.White/Gray | Task 8 |
| MemoryManageScreen 指示线 + 来源标签 | Task 9 |
| categorymanagescreen 指示线 ×3 | Task 10 |
| MappingManageScreen 指示线 + Switch | Task 11 |
| 版本号升级 | Task 12 |
| 全量编译验证 | Task 13 |
| ExpenseCard — 引用变量，自动继承 | 无需改动 ✅ |
| CategoryPicker — 引用变量，自动继承 | 无需改动 ✅ |
| ManualEntryDialog — 引用变量，自动继承 | 无需改动 ✅ |
| MainActivity — 引用 NavActive/NavInactive，自动继承 | 无需改动 ✅ |

### 2. Placeholder scan

- 无 "TBD"、"TODO"、"implement later" 等
- 每个 Step 都有完整代码块
- 无 "Similar to Task N" 引用

### 3. Type consistency

- `DividerColor` — Task 1 定义，Task 3/4/5/6/9/10/11 引用，名称一致 ✅
- `SageGreen` — Task 1 定义，Task 3 引用，名称一致 ✅
- `WeChatGreen` — 变量名未变，值已更新 ✅
- `TextSecondary` — 用作 Tab 未选中字色替代 `Color(0xFF666666)` 和 `Color.Gray`，语义正确 ✅

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-30-ui-redesign-morandi-blue.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
