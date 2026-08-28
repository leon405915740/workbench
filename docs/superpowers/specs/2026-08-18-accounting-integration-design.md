# 记账模块整合设计文档

> 日期：2026-08-18
> 项目：AI-Growth-OS 工作台 App
> 状态：已确认（v3，纳入第二轮架构审查反馈）

## 1. 架构概述

### 导航方式
工作台通过 `startActivity(Intent(context, com.accounting.app.MainActivity::class.java))` 启动记账模块。不嵌入工作台 NavHost，保持记账模块自包含，不引入跨模块导航耦合。

### 数据访问：AccountingBridge 模式

工作台**不直接访问**记账模块的 DAO 或 Repository。通过 `AccountingBridge` 接口隔离跨模块数据访问，避免初始化顺序、生命周期耦合和测试困难。

```
app 模块 (SettingsScreen, DashboardScreen)
        ↓ 依赖接口
AccountingBridge (interface, feature:accounting)
        ↓ 实现
AccountingBridgeImpl (feature:accounting)
        ↓
AppRepository → DAO
```

#### AccountingBridge 接口定义

```kotlin
// feature/accounting/.../AccountingBridge.kt
interface AccountingBridge {
    // Dashboard 数据（传原始 Double，UI 负责格式化）
    fun getMonthlyExpense(): Flow<Double?>

    // 记账设置
    fun isAutoLearnEnabled(): Flow<Boolean>
    suspend fun setAutoLearnEnabled(enabled: Boolean)

    // 记忆与分类管理（返回 UI Model，不暴露 Entity）
    fun getMemories(): Flow<List<MemoryItemUi>>
    fun getMappings(): Flow<List<MappingItemUi>>
    suspend fun addMemory(triggerWord: String, type: String, category: String)
    suspend fun deleteMemory(id: Long)
    suspend fun clearAllMemories()
    suspend fun restoreDefaultMemories()
    suspend fun addMapping(keyword: String, type: String, category: String)
    suspend fun updateMapping(id: Long, keyword: String, category: String)
    suspend fun deleteMapping(id: Long)
    suspend fun toggleMappingEnabled(id: Long, enabled: Boolean)
    suspend fun promoteMappingToManual(id: Long)
    suspend fun cleanStaleAutoMappings()

    // 数据导出
    fun exportCsv(context: Context, uri: Uri)
    fun exportLog(context: Context, uri: Uri)

    // 分类数据（供映射管理页 UI 使用）
    fun getExpenseCategories(): List<String>
    fun getIncomeCategories(): List<String>

    // 预留：AI 记账解析（下一阶段实现，当前不实现）
    // suspend fun parseAccountingInput(text: String): AccountingCandidate
}
```

#### UI Model 定义（不暴露 Entity 给 UI 层）

```kotlin
// feature/accounting/.../ui/model/MemoryItemUi.kt
data class MemoryItemUi(
    val id: Long,
    val triggerWord: String,
    val category: String,
    val type: String,       // "expense" | "income"
    val source: String     // "auto" | "preset" | "manual"
)

data class MappingItemUi(
    val id: Long,
    val keyword: String,
    val category: String,
    val type: String,
    val isManual: Boolean,
    val isEnabled: Boolean,
    val hitCount: Int
)
```

`AccountingBridgeImpl` 负责将 Entity → UiModel 转换，UI 层不感知数据库结构变化。

#### 实现与暴露（构造注入，不依赖单例）

```kotlin
// feature/accounting/.../AccountingBridgeImpl.kt
class AccountingBridgeImpl(
    private val repo: AppRepository,
    private val context: Context
) : AccountingBridge {
    // 所有方法委托给 repo，Entity → UiModel 转换在此完成
}

// AccountingApp.kt — Application 负责创建 Bridge 实例
companion object {
    @Volatile
    private var bridge: AccountingBridge? = null

    fun getBridge(): AccountingBridge {
        return bridge ?: synchronized(this) {
            bridge ?: AccountingBridgeImpl(
                instance!!.appRepository,
                instance!!
            ).also { bridge = it }
        }
    }
}
```

工作台通过 `AccountingApp.getBridge()` 获取接口实例，只依赖接口类型，不触碰 DAO/Repository 内部。

> **后续拆分预留**：当前 Bridge 职责较宽，未来可拆分为 `AccountingDataProvider`（Dashboard/统计）、`MemoryProvider`（记忆/映射）、`ExportProvider`（导出）。现阶段保持单接口，避免过度设计。

### 约束
- 只修改工作台项目中的记账代码副本（`feature/accounting/`）
- 不碰原始记账 App（`d:\记账app\app\`）

## 2. 首页入口：双列数据网格

### 修改文件
- `feature/learning/.../DashboardScreen.kt` — 新增参数和网格 UI
- `app/.../DashboardAccountingViewModel.kt` — **新建**，提供月支出 StateFlow
- `app/.../AIGrowthOSApp.kt` — 注入 ViewModel，传入回调和数据

### DashboardScreen 新增参数
```
onAccountingClick: () -> Unit
monthlyExpense: StateFlow<Double?>   // 原始金额，如 3256.0 或 null
```

UI 层负责格式化：`monthlyExpense?.let { "¥${formatAmount(it)}" }`，ViewModel 不传格式化字符串。

### UI 布局
在 LazyColumn 中插入一行双列网格（位于 TodayStatsRow 之后、ModuleProgressSection 之前）：
- **左格**：💰 本月支出 + 金额 + "记一笔 →"
- **右格**：🧠 待复习 + 数量 + "去复习 →"

样式与现有 StatCard 保持一致：白色圆角卡片，轻阴影，图标 + 数据 + 副标题。

### 数据来源：DashboardAccountingViewModel

不在 Composable 中用 LaunchedEffect 直接查询。新增 `DashboardAccountingViewModel`：

```kotlin
// app/.../DashboardAccountingViewModel.kt
class DashboardAccountingViewModel : ViewModel() {
    private val bridge = AccountingApp.getBridge()

    val monthlyExpense: StateFlow<Double?> = bridge.getMonthlyExpense()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
```

在 `AIGrowthOSApp.kt` 的 NavHost 中：
```kotlin
composable(Screen.Dashboard.route) {
    val accountingVm: DashboardAccountingViewModel = viewModel()
    DashboardScreen(
        // ... 现有参数 ...
        onAccountingClick = { context.startActivity(Intent(context, com.accounting.app.MainActivity::class.java)) },
        monthlyExpense = accountingVm.monthlyExpense
    )
}
```

learning 模块不依赖 accounting 模块，只接收参数。

## 3. 设置页面整合：分组卡片式

### 修改文件
- `app/.../SettingsScreen.kt` — 完全重建为三组卡片结构

### 三组卡片结构

#### AI 设置
| 设置项 | 存储 |
|--------|------|
| DeepSeek API Key | SharedPreferences `AI_Growth_OS_Prefs`（现有） |
| AI 模型选择 | SharedPreferences `AI_Growth_OS_Prefs`（现有） |

#### 记账设置
| 设置项 | 实现 |
|--------|------|
| 自动学习分类（开关） | 通过 `AccountingBridge.isAutoLearnEnabled()` / `setAutoLearnEnabled()` 读写 |
| 记忆与分类管理（入口） | 导航到 NavHost 路由 `Screen.MemoryMapping` |

#### 数据管理
| 设置项 | 实现 |
|--------|------|
| 导出 CSV | 通过 `AccountingBridge.exportCsv(context, uri)`，复用 SAF 文件选择器 |
| 导出日志 | 通过 `AccountingBridge.exportLog(context, uri)`，复用 SAF 文件选择器 |

### autoLearn 持久化迁移

当前 `autoLearn` 不持久化（只在内存 UiState 中）。

**数据丢失策略：明确接受旧状态丢失。** 原本就不持久化，运行时关闭后状态即消失，迁移到 DataStore 后行为一致——首次启动默认值为 `false`，与之前关闭后重新打开的行为相同。无需迁移逻辑。

迁移方案：
- 在 `UserPreferences.kt` 新增 `KEY_AUTO_LEARN` 布尔键，默认 `false`
- 新增 `getAutoLearn(): Flow<Boolean>` 和 `setAutoLearn(enabled: Boolean)` 方法
- `MainViewModel.toggleAutoLearn()` 改为先写 DataStore 再更新 UiState
- `AccountingBridgeImpl` 委托 `UserPreferences` 实现读写
- 工作台 SettingsScreen 通过 `AccountingBridge` 读写该开关

## 4. 自动捕获功能删除

### 删除文件（整个目录）
```
feature/accounting/src/main/java/com/accounting/app/capture/
├── CaptureNotificationManager.kt
├── CapturePreferences.kt
├── PaymentAccessibilityService.kt
├── PaymentDetector.kt
├── WindowChangeDeduplicator.kt
├── dispatcher/
│   └── CaptureDispatcher.kt
├── extractor/
│   └── NodeExtractor.kt
└── model/
    ├── CaptureSource.kt
    └── PaymentInfo.kt
```

### 资源文件
- 删除 `feature/accounting/src/main/res/xml/accessibility_service_config.xml`

### 清理引用

#### AccountingApp.kt
- 删除 `import com.accounting.app.capture.CaptureNotificationManager`
- 删除 `import com.accounting.app.data.model.BillExecutePlan`（如果仅为捕获使用）
- 删除 `CaptureNotificationManager.initChannel(this)` 调用
- 删除 `pendingCapturePlan: BillExecutePlan?` 字段
- 删除 `setPendingCapturePlan()` 和 `getAndClearPendingCapturePlan()` 方法
- 保留 `resumedCount`、`isAppInForeground()`、ActivityLifecycleCallbacks（非捕获部分）

#### MainActivity.kt（accounting）
- 删除 `EXTRA_PAYMENT_INFO` 常量
- 删除 `onPaymentCapturedFromIntent()` 调用和意图处理逻辑
- 删除相关 import

#### MainViewModel.kt
- 删除 `import com.accounting.app.capture.CapturePreferences`
- 删除 `import com.accounting.app.capture.model.PaymentInfo`（如存在）
- 删除 `onPaymentCapturedFromIntent(info: PaymentInfo)` 方法
- 删除 `toggleAutoCapture()` 方法
- 删除 `loadAutoCaptureEnabled()` 方法
- 删除 `autoCaptureEnabled` 相关 UiState 更新

#### UiState.kt
- 删除 `autoCaptureEnabled: Boolean` 字段

#### AndroidManifest.xml（accounting 模块）
- 删除 `<service android:name="..." />` 无障碍服务声明
- 删除 `FOREGROUND_SERVICE` 权限
- 删除 `POST_NOTIFICATIONS` 权限

#### AndroidManifest.xml（app 模块）
- 删除 `FOREGROUND_SERVICE` 权限
- 删除 `POST_NOTIFICATIONS` 权限

#### SettingsScreen.kt（accounting 模块）
- 删除 autoCapture 开关 UI
- 删除无障碍设置跳转按钮
- 删除 `onToggleAutoCapture`、`onNavigateToAccessibilitySettings`、`accessibilityEnabled` 参数

### 删除后回归验证

在步骤 1 完成后，执行**全项目**引用检查（不只限 feature/accounting，app 模块也可能引用）：

```bash
# 在项目根目录执行，搜索以下关键词，确保全项目无残留引用
grep -r "capture" --include="*.kt" .
grep -r "PaymentInfo" --include="*.kt" .
grep -r "BillExecutePlan" --include="*.kt" .
grep -r "EXTRA_PAYMENT_INFO" --include="*.kt" .
grep -r "autoCapture" --include="*.kt" .
grep -r "CapturePreferences" --include="*.kt" .
grep -r "CaptureNotificationManager" --include="*.kt" .
grep -r "PaymentAccessibilityService" --include="*.kt" --include="*.xml" .
```

**验收标准**：以上搜索结果全部为空，`assembleDebug` 编译通过。

## 5. 记忆与分类管理合并

### 新建文件
- `feature/accounting/.../ui/screens/MemoryMappingManageScreen.kt` — 合并后的 UI
- `app/.../MemoryMappingViewModel.kt` — 合并管理页的 ViewModel

### 合并方案
将 `MemoryManageScreen`（460行）和 `MappingManageScreen`（574行）合并为一个页面，内部用 Tab 切换：

- **Tab 1 - 分类记忆**：触发词 → 分类的记忆条目
  - 来源筛选：全部 / 自动学习 / 系统预置 / 手动添加
  - 搜索框（200ms debounce）
  - 可折叠分类组
  - 添加记忆对话框
  - 删除确认 + 恢复默认确认

- **Tab 2 - 分类映射**：关键词 → 分类的映射规则
  - 手动映射 / 自动学习 子 Tab
  - 手动映射：启用/禁用、编辑、删除、命中次数
  - 自动学习映射：固定为手动、删除、命中次数
  - 添加/编辑对话框
  - 清理无效自动映射

### 入口
工作台 SettingsScreen → "记忆与分类管理 →" → 合并管理页

### 导航方式
在工作台 NavHost 中新增路由 `Screen.MemoryMapping`。从 SettingsScreen 点击后导航到合并管理页。不使用 Intent 跳转，保持工作台内导航一致性。

### 数据源：MemoryMappingViewModel

**不直接访问 DAO**。通过 `AccountingBridge` 访问 Repository 层，返回 UI Model 而非 Entity：

```kotlin
// app/.../MemoryMappingViewModel.kt
class MemoryMappingViewModel : ViewModel() {
    private val bridge = AccountingApp.getBridge()

    val memories: StateFlow<List<MemoryItemUi>> =
        bridge.getMemories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mappings: StateFlow<List<MappingItemUi>> =
        bridge.getMappings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addMemory(triggerWord: String, type: String, category: String) {
        viewModelScope.launch { bridge.addMemory(triggerWord, type, category) }
    }
    // ... 其他操作委托给 bridge
}
```

数据流路径：`MemoryMappingViewModel → AccountingBridge → AppRepository → DAO`，UI 层只接触 `MemoryItemUi` / `MappingItemUi`，不感知数据库结构变化。

### 删除旧文件
合并完成后，删除 `MemoryManageScreen.kt` 和 `MappingManageScreen.kt`（其功能已全部迁移到 `MemoryMappingManageScreen.kt`）。

**删除前检查**：
- 全项目 grep `MemoryManageScreen` 和 `MappingManageScreen` 确认无其他引用
- 确认 `MainViewModel` 和 `MainActivity` 中对旧屏幕的引用已迁移或移除

### UI 回归验收清单

合并涉及 ~1000 行代码整合，不只是文件替换。完成开发后逐项验证：

- [ ] Tab 切换正常（分类记忆 ↔ 分类映射）
- [ ] 分类记忆 Tab：来源筛选 chips（全部/自动学习/系统预置/手动添加）正常切换
- [ ] 分类记忆 Tab：搜索框 200ms debounce 正常
- [ ] 分类记忆 Tab：添加记忆对话框正常（触发词输入 + 类型切换 + 分类选择）
- [ ] 分类记忆 Tab：删除记忆确认弹窗正常
- [ ] 分类记忆 Tab：恢复默认记忆确认弹窗正常
- [ ] 分类映射 Tab：手动/自动学习子 Tab 切换正常
- [ ] 分类映射 Tab：手动映射启用/禁用开关正常
- [ ] 分类映射 Tab：添加/编辑映射对话框正常
- [ ] 分类映射 Tab：删除映射正常
- [ ] 分类映射 Tab：自动学习映射"固定为手动"正常
- [ ] 分类映射 Tab："清理无效自动映射"按钮正常
- [ ] 空状态显示正常（无记忆/无映射时）

## 6. 记账模块 Tab 调整

### 修改文件
- `feature/accounting/.../MainActivity.kt`
- `feature/accounting/.../ui/model/UiState.kt`

### 变更
- `AppTab` 枚举从 3 个（CHAT, DASHBOARD, SETTINGS）减为 2 个（CHAT, DASHBOARD）
- 底部导航栏从 3 个 tab 减为 2 个
- 移除 SETTINGS tab 的 composable 分支和导航逻辑
- accounting 模块的 `SettingsScreen.kt` 不再被 MainActivity 调用，**直接删除**

### SettingsScreen.kt 删除前安全检查
1. 全项目 grep `SettingsScreen`（accounting 模块内的），确认引用方只有 MainActivity
2. 确认工作台 SettingsScreen 是独立文件（不同包名，不冲突）
3. 确认 `ApiKeyDialog`（accounting SettingsScreen 的私有 composable）没有其他引用
4. 删除后执行 `assembleDebug` 验证编译通过

## 7. 版本升级策略

### 数据库
本次变更**不修改数据库结构**（Room schema 无变化）：
- 不新增 Entity
- 不修改现有 Entity 字段
- 不需要 Room Migration
- `AppDatabase` 版本号保持不变

### DataStore（UserPreferences）
新增 `KEY_AUTO_LEARN` 布尔键：
- DataStore 自动兼容新键——旧 DataStore 文件不存在该键时，`getAutoLearn()` 返回默认值 `false`
- 无需迁移逻辑，无需版本管理

### APK 版本号
- `versionCode` +1
- `versionName` 调整（如 `1.x.x → 1.x+1.x`）
- APK 输出文件名自动更新为 `工作台_v{versionName}_{buildType}.apk`

## 8. 实施顺序

| 步骤 | 内容 | 影响范围 | 依赖 | 验收 |
|------|------|----------|------|------|
| 1 | 删除 `capture/` 目录 + `accessibility_service_config.xml` + 清理 AccountingApp/MainViewModel/UiState/Manifest | accounting 模块 + app manifest | 无 | grep 无残留引用 |
| 2 | 创建 `AccountingBridge` 接口 + `AccountingBridgeImpl` 实现 | accounting 模块 | 步骤 1 | 接口编译通过 |
| 3 | autoLearn 持久化迁移到 UserPreferences + Bridge 暴露 | accounting 模块 | 步骤 2 | DataStore 读写正常 |
| 4 | 合并 MemoryManageScreen + MappingManageScreen → MemoryMappingManageScreen + 删除旧文件 | accounting 模块 | 无 | grep 旧文件名无残留 |
| 5 | 重建工作台 SettingsScreen（三组卡片 + 记忆管理入口）+ MemoryMappingViewModel | app 模块 | 步骤 2, 3, 4 | 设置页功能正常 |
| 6 | 移除 accounting MainActivity SETTINGS tab + 删除 accounting SettingsScreen.kt | accounting 模块 | 步骤 5 | grep SettingsScreen 无残留 |
| 7 | DashboardScreen 添加双列数据网格 + DashboardAccountingViewModel | learning + app 模块 | 步骤 2 | 首页显示月支出 |
| 8 | AIGrowthOSApp 接线 onAccountingClick + NavHost MemoryMapping 路由 | app 模块 | 步骤 5, 7 | 导航正常 |
| 9 | 版本号更新 + 全项目编译验证 + 修复编译错误 | 全项目 | 步骤 1-8 | `assembleDebug` 通过 |

## 9. 风险与注意事项

1. **AccountingBridge 初始化顺序**：`AccountingApp.getBridge()` 依赖 `instance` 已初始化。`instance` 在 `AccountingApp.onCreate()` 第一行赋值，而 `AIGrowthOSApplication.onCreate()` 调用 `super.onCreate()` 即触发。工作台 Composable 在 Activity 之后执行，此时 Application 已初始化，无空指针风险。

2. **autoLearn 数据丢失**：明确接受。原有 autoLearn 不持久化，关闭即丢失。迁移后首次启动默认 `false`，行为一致。

3. **合并管理页的数据流**：通过 `AccountingBridge → AppRepository → DAO` 三层访问，UI 层只接触 `MemoryItemUi` / `MappingItemUi`，不直接碰 DAO 和 Entity。`MemoryMappingViewModel` 只依赖 `AccountingBridge` 接口，后续 Repository 或 Entity 变更不影响 UI。

4. **编译依赖**：步骤 1 删除 capture/ 后，所有引用 capture 类的文件必须同时清理，否则编译失败。需要一次性完成，随后立即执行全项目 grep 验证。

5. **SettingsScreen 包名冲突**：工作台 `SettingsScreen` 在 `com.aigrowth.os.feature.settings` 包下，accounting `SettingsScreen` 在 `com.accounting.app.ui.screens` 包下。删除 accounting 的 SettingsScreen 后不会冲突，但删除前需确认 import 路径。

6. **实施顺序：先建后删**：步骤 5（重建工作台 SettingsScreen）必须在步骤 6（删除 accounting SettingsScreen）之前完成。否则中间会出现功能断层——旧设置入口已删但新入口未就绪。

7. **版本号更新**：按项目规则，代码修改后必须更新 versionCode +1。数据库结构无变化，无需 Migration。DataStore 新增键自动兼容。

8. **BridgeImpl 构造注入**：`AccountingBridgeImpl` 通过构造函数接收 `AppRepository`，不通过 `AccountingApp.getInstance()` 获取单例。降低了全局状态依赖，便于测试（可传入 mock repository）。Bridge 实例由 `AccountingApp` 在 `getBridge()` 中创建并注入 repo。
