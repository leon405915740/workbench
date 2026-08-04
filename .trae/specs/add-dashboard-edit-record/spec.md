# 统计页账单编辑功能 Spec

## Why
统计页面（DashboardScreen）点击账单仅支持删除，无法编辑。用户如需修改金额、分类、备注、时间、商家，必须切到记账页或重新手动记账。需要为统计页账单增加编辑能力，同时统一项目内的账单编辑入口，避免"记账页只能改分类、统计页无编辑"的体验割裂。

## What Changes
- 新建 `EditRecordDialog` 组件：基于 `ManualEntryDialog` 改造为「新建 + 编辑」双模式，参数统一为 `data: EditDialogData`（不再有 `mode`/`initialAmount` 等散参数），通过 `data.recordId` 是否为 null 判断模式（null=新建，非空=编辑），字段覆盖金额、分类、商家、备注、时间（5 个字段），`amount` 类型为 `Long`（分），与 `ExpenseEntity`/`IncomeEntity` 一致
- 编辑模式底部增加「删除记录」按钮，点击触发 `onDeleteRequest` 回调；二次确认 Dialog 由 `MainActivity` 外层控制（避免 Composable 内管理业务删除状态）
- 数据库层新增全字段更新方法（`ExpenseDao.updateAllFields` / `IncomeDao.updateAllFields` / `Repository.updateExpenseFull` / `Repository.updateIncomeFull`），`subcategory` 参数保留原值传入（编辑弹窗不修改子分类）
- `MainViewModel` 新增 `openEditDialogFromDashboard(RecentRecord)` 入口和 `confirmEditRecord(updatedData: EditDialogData)` 全字段更新方法，**所有上下文通过 `EditDialogData` 参数传入，不读取 `_uiState.value.showEditDialog`**（避免隐式依赖 UI State）
- `EditDialogData` 扩展字段：新增 `amount`、`time`、`note`、`subcategory`（保留原值）、`originalCategory`（记忆学习用，保存打开编辑时的原分类）
- **统一弹窗状态管理**：删除 `showManualEntryDialog` 状态，统一用 `showEditDialog: EditDialogData?`，`recordId == null` 表示新建模式，`recordId != null` 表示编辑模式（避免双状态混淆）
- 统计页（DashboardScreen）点击账单（整行可点击）→ 直接打开编辑弹窗（移除原删除确认弹窗）
- 记账页（ChatScreen + ExpenseCard）"修改分类"按钮 → 改为"编辑"，打开同一 `EditRecordDialog`
- `MainActivity` 弹窗挂载点改造：`CategoryPicker`（编辑场景）和 `ManualEntryDialog`（新建场景）统一替换为 `EditRecordDialog`，新增删除二次确认 Dialog 外层控制
- 记忆学习 `source` 保持 `"auto"` 不变（本次改造是「统一编辑入口」，不是「修正映射来源」；`manual` 映射的生成应等到真正有「用户显式保存规则」的场景，如 MappingManageScreen 升级，不应跟编辑账单耦合）
- 编辑后 `CardMessage` 的 `rawInput` 和 `confidence` 字段保持不变（编辑不修改原始输入和 AI 置信度）
- 新建模式下 `subcategory` 默认为 `null`（与现有 `submitManualEntry` 行为一致）；分类列表数据源为 `CategoryConstants` 常量（非数据库 `categories` 表），避免 categories 表清空后分类列表为空
- 删除账单时**不清理 `category_memory` 表**（记忆映射是独立的学习数据，不随单条账单删除）
- 空值处理约定：`merchant`、`note` 字段用户清空后传 `null`（与现有实体字段可空性一致，不传空字符串）
- **BREAKING**：`ManualEntryDialog` 重命名为 `EditRecordDialog`，原文件名变更，所有引用点同步更新

## Impact
- Affected specs:
  - `memory-category-picker`：`CategoryPicker` 文件保留（`MemoryManageScreen` 仍在用），不受影响
  - `refactor-category-system`：分类常量来源不变，`EditRecordDialog` 复用 `CategoryConstants`，无冲突
- Affected code:
  - `app/src/main/java/com/accounting/app/ui/components/ManualEntryDialog.kt` → 重命名为 `EditRecordDialog.kt`，扩展为双模式
  - `app/src/main/java/com/accounting/app/ui/components/CategoryPicker.kt` — 文件保留，仅移除 `MainActivity` 和 `ManualEntryDialog` 中的调用
  - `app/src/main/java/com/accounting/app/ui/screens/DashboardScreen.kt` — 点击行为改造，删除弹窗移除
  - `app/src/main/java/com/accounting/app/ui/screens/ChatScreen.kt` — 回调命名调整（`onEditCategory` → `onEditRecord`）
  - `app/src/main/java/com/accounting/app/ui/components/ExpenseCard.kt` — 按钮文案与回调调整
  - `app/src/main/java/com/accounting/app/ui/MainViewModel.kt` — 新增编辑入口与全字段更新方法
  - `app/src/main/java/com/accounting/app/ui/model/UiState.kt` — `EditDialogData` 扩展字段
  - `app/src/main/java/com/accounting/app/MainActivity.kt` — 弹窗挂载点替换、回调注入调整
  - `app/src/main/java/com/accounting/app/data/local/dao/ExpenseDao.kt` — 新增 `updateAllFields`
  - `app/src/main/java/com/accounting/app/data/local/dao/IncomeDao.kt` — 新增 `updateAllFields`
  - `app/src/main/java/com/accounting/app/data/repository/AppRepository.kt` — 新增 `updateExpenseFull` / `updateIncomeFull`
  - `app/build.gradle.kts` — versionCode +1，versionName 调整

## ADDED Requirements

### Requirement: 统计页账单编辑入口
统计页面（DashboardScreen）点击账单 SHALL 直接打开编辑弹窗（`EditRecordDialog` 编辑模式），不再弹出删除确认框。

#### Scenario: 从统计页打开编辑
- **WHEN** 用户在统计页点击某条账单（`RecentRecordItem` 整行可点击）
- **THEN** 打开 `EditRecordDialog` 编辑模式，弹窗初始值填充该账单的金额、分类、商家、备注、时间
- **AND** 弹窗底部显示「删除记录」按钮

#### Scenario: 统计页编辑后数据刷新
- **WHEN** 用户在编辑弹窗修改字段并确认
- **THEN** 调用 `Repository.updateExpenseFull` / `updateIncomeFull` 全字段更新数据库
- **AND** `recentRecords` 列表通过 Flow 自动刷新，统计页展示最新数据
- **AND** 弹出 Toast 提示「已更新」

#### Scenario: 统计页从编辑弹窗删除
- **WHEN** 用户在编辑弹窗点击「删除记录」按钮
- **THEN** `EditRecordDialog` 触发 `onDeleteRequest` 回调，不内部管理确认状态
- **AND** `MainActivity` 外层显示删除二次确认 Dialog
- **WHEN** 用户确认删除
- **THEN** 关闭顺序：先关闭二次确认 Dialog → 再关闭编辑弹窗（清空 `showEditDialog`）→ 调用 `MainViewModel.deleteRecord(recordId, type)` 异步删除，`type` 从 `EditDialogData` 读取
- **AND** `recentRecords` 列表通过 Flow 自动刷新
- **AND** `messages` 列表中对应的 `CardMessage` 同步移除（复用现有 `deleteRecord` 逻辑，该方法已包含 messages 同步移除）
- **AND** **不清理 `category_memory` 表**（记忆映射是独立的学习数据，不随单条账单删除）
- **AND** 弹出 Toast 提示「已删除记录」

### Requirement: EditRecordDialog 双模式组件
系统 SHALL 提供统一的 `EditRecordDialog` 组件，参数统一为 `data: EditDialogData`，通过 `data.recordId` 是否为 null 判断模式（不使用 `EditMode` 枚举，避免与 `recordId` 双状态）。字段覆盖金额、分类、商家、备注、时间。`type`（支出/收入）在两种模式下的 UI 呈现方式不同：新建模式可切换，编辑模式只读。

#### Scenario: 新建模式（recordId == null）
- **WHEN** 用户点击记账页底部「手动记账」按钮
- **THEN** 打开 `EditRecordDialog`，`data.recordId` 为 null，标题为「手动记账」，所有字段为空或默认值
- **AND** `type` 显示为可切换的「支出/收入」Tab（沿用原 `ManualEntryDialog` 行为），切换时候选分类列表同步刷新
- **AND** 提交时调用 `onSubmit(data)`，ViewModel 内部从 `data` 提取字段调用 `Repository.insertExpense` / `insertIncome`（沿用现有 `submitManualEntry` 逻辑）

#### Scenario: 编辑模式（recordId != null）
- **WHEN** 用户从统计页或记账页卡片点击「编辑」
- **THEN** 打开 `EditRecordDialog`，`data.recordId` 非空，标题为「编辑账单」，字段预填充现有数据
- **AND** `type` 以只读标签展示在弹窗顶部（分类选择器旁），不可切换（避免跨表迁移）；该只读标签在 `EditRecordDialog` 层实现，`CategoryPicker` 组件本身无需改造（其 `type` 参数本就固定不可切换）
- **AND** 提交时调用 `onEditConfirm(updatedData: EditDialogData)`，`updatedData` 包含新值和原上下文（`type`/`subcategory`/`originalCategory`），ViewModel **不读取 `_uiState.value.showEditDialog`**，所有上下文通过参数传入
- **AND** 底部显示「删除记录」按钮，点击触发 `onDeleteRequest` 回调（不内部管理确认状态），由 `MainActivity` 外层显示二次确认 Dialog（新建模式不显示删除按钮）

#### Scenario: 编辑模式分类变更触发记忆学习
- **WHEN** 编辑模式下用户修改了分类（`updatedData.category != updatedData.originalCategory`），且新商家字段能提取出有效触发词
- **THEN** 调用 `Repository.upsertMemory` 写入/更新记忆词条，`source` 保持 `"auto"`
- **AND** `originalCategory` 从 `EditDialogData` 读取（打开编辑时保存的原分类），用于判断分类是否变更
- **AND** 行为与现有 `confirmEditCategory` 的记忆学习逻辑一致

#### Scenario: 编辑模式分类未变更时不触发记忆操作
- **WHEN** 编辑模式下用户未修改分类（`updatedData.category == updatedData.originalCategory`），仅修改了金额/商家/备注/时间
- **THEN** 不调用 `upsertMemory`，不新增/更新任何记忆映射
- **AND** 不更新记忆命中次数（保持现有行为，避免过度设计）

### Requirement: 数据库全字段更新
系统 SHALL 提供按 id 全字段更新账单的 DAO 方法，避免多次单字段更新。`subcategory` 参数由 ViewModel 从 `EditDialogData` 读取原值传入（编辑弹窗不修改子分类，避免覆盖原有数据）。`amount` 类型为 `Long`（分），与 `ExpenseEntity`/`IncomeEntity` 的 `amount` 字段类型完全一致。

#### Scenario: 更新支出全字段
- **WHEN** 调用 `ExpenseDao.updateAllFields(id, amount, category, subcategory, merchant, time, note)`
- **THEN** 执行 `UPDATE expense SET amount=:amount, category=:category, subcategory=:subcategory, merchant=:merchant, time=:time, note=:note WHERE id=:id`
- **AND** `subcategory` 参数值为原账单的子分类（保留原值，不覆盖）
- **AND** `amount` 类型为 `Long`（分），与 `ExpenseEntity.amount` 一致
- **AND** 不修改 `confidence`、`rawInput`、`createdAt` 字段

#### Scenario: 更新收入全字段
- **WHEN** 调用 `IncomeDao.updateAllFields(id, amount, category, subcategory, merchant, time, note)`
- **THEN** 执行 `UPDATE income SET amount=:amount, category=:category, subcategory=:subcategory, merchant=:merchant, time=:time, note=:note WHERE id=:id`
- **AND** `subcategory` 参数值为原账单的子分类（保留原值，不覆盖）
- **AND** `amount` 类型为 `Long`（分），与 `IncomeEntity.amount` 一致
- **AND** 不修改 `confidence`、`rawInput`、`createdAt` 字段

#### Scenario: 更新不存在的 id
- **WHEN** 调用 `updateAllFields` 传入不存在的 `id`
- **THEN** 不抛异常，`rowsAffected = 0`
- **AND** ViewModel 根据返回值判断是否更新成功，更新失败时 log 警告并弹 Toast「更新失败」

## MODIFIED Requirements

### Requirement: 记账页卡片编辑入口
记账页（ChatScreen）的账单卡片「修改分类」按钮 SHALL 改为「编辑」按钮，点击后打开 `EditRecordDialog` 编辑模式，支持修改金额、分类、商家、备注、时间。

#### Scenario: 从记账页卡片打开编辑
- **WHEN** 用户在记账页点击某条账单卡片的「编辑」按钮
- **THEN** 打开 `EditRecordDialog` 编辑模式，字段预填充该卡片对应账单数据
- **AND** 提交后同步更新 `messages` 列表中对应的 `CardMessage`（金额、分类、子分类、商家、备注、时间全部同步）
- **AND** `CardMessage` 的 `rawInput` 和 `confidence` 字段保持不变（编辑不修改原始输入和 AI 置信度）

### Requirement: EditDialogData 数据结构与统一弹窗状态
`EditDialogData` SHALL 包含编辑弹窗所需的全部字段上下文，并通过 `recordId` 是否为空区分新建/编辑模式。`UiState.showEditDialog: EditDialogData?` 是唯一的弹窗状态来源，删除原 `showManualEntryDialog`。

#### Scenario: 编辑弹窗数据完整
- **WHEN** 打开编辑弹窗
- **THEN** `EditDialogData` 包含 `recordId: Long?`、`type`、`category`、`subcategory`、`merchant`、`rawInput`、`amount`、`time`、`note`、`originalCategory` 共 10 个字段
- **AND** `recordId` 为 `null` 表示新建模式，`recordId` 非空表示编辑模式（语义清晰，避免与 Room autoGenerate 从 1 开始的 id 混淆）
- **AND** `type` 字段在编辑模式下不可修改（沿用现有"不修改 type"约束，避免跨表迁移）
- **AND** `subcategory` 字段保留原值，编辑弹窗不提供修改入口，`confirmEditRecord` 传递原值给 DAO 避免覆盖
- **AND** `originalCategory` 字段保存打开编辑时的原分类，用于记忆学习判断分类是否变更
- **AND** `merchant`、`note` 字段用户清空后以 `null` 存储（与现有实体字段可空性一致，不传空字符串）

#### Scenario: 统一弹窗状态管理
- **WHEN** 用户触发新建或编辑
- **THEN** `UiState.showEditDialog` 设置为 `EditDialogData`（新建模式 `recordId = null`，编辑模式 `recordId` 非空）
- **AND** `UiState.showManualEntryDialog` 已删除，不再存在
- **AND** `MainActivity` 根据 `showEditDialog` 是否为 null 决定弹窗显示，根据 `recordId` 是否为 null 决定模式（CREATE/EDIT）

## REMOVED Requirements

### Requirement: DashboardScreen 删除确认弹窗
**Reason**: 删除操作已移入 `EditRecordDialog` 编辑模式底部按钮，外层删除确认弹窗不再需要。
**Migration**: 统计页点击账单改为直接打开编辑弹窗；用户通过编辑弹窗内「删除记录」按钮触发删除，复用现有 `MainViewModel.deleteRecord` 方法。

### Requirement: ManualEntryDialog 组件名
**Reason**: 组件职责从「手动新建」扩展为「新建 + 编辑」，`ManualEntryDialog` 名称不再准确表达能力。
**Migration**: 文件重命名为 `EditRecordDialog.kt`，组件函数重命名为 `EditRecordDialog`，所有引用点（`MainActivity` 挂载、ViewModel 回调、import 语句）同步更新。新建模式行为与原 `ManualEntryDialog` 完全一致。

## Changelog

### v2.13.0
- **新增**：统计页点击账单可直接编辑（金额、分类、商家、备注、时间）
- **新增**：编辑弹窗内提供删除入口（二次确认）
- **重构**：`ManualEntryDialog` → `EditRecordDialog`，支持新建 + 编辑双模式
- **重构**：记账页卡片「修改分类」升级为「编辑」全字段
- **重构**：统一弹窗状态管理（删除 `showManualEntryDialog`，用 `showEditDialog` + `recordId` 区分模式）
- **优化**：`EditRecordDialog` 参数统一为 `EditDialogData`，`confirmEditRecord` 不依赖 UI State
- **保留**：记忆学习 `source` 保持 `"auto"`，`rawInput`/`confidence` 编辑后不变
