# Checklist

## 数据库层
- [ ] `ExpenseDao.updateAllFields` 方法存在，SQL 为 `UPDATE expense SET amount=:amount, category=:category, subcategory=:subcategory, merchant=:merchant, time=:time, note=:note WHERE id=:id`，不修改 confidence/rawInput/createdAt
- [ ] `IncomeDao.updateAllFields` 方法存在，签名与 `ExpenseDao.updateAllFields` 同构
- [ ] `AppRepository.updateExpenseFull` / `updateIncomeFull` 正确转发 DAO，参数顺序与 DAO 一致，返回 `Int`（rowsAffected）
- [ ] `amount` 类型为 `Long`（分），与 `ExpenseEntity`/`IncomeEntity` 的 `amount` 字段类型完全一致
- [ ] `subcategory` 参数由 ViewModel 从 `EditDialogData` 读取原值传入，不覆盖原有子分类数据
- [ ] `updateAllFields` 传入不存在的 id 时不抛异常，返回 `rowsAffected = 0`
- [ ] 保留原有 `updateExpenseCategory` / `updateIncomeCategory` 方法未删除（直到 Task 7 确认无引用后才删）

## UiState 与弹窗状态
- [ ] `EditDialogData` 包含 `recordId: Long?`、`type`、`category`、`subcategory`、`merchant`、`rawInput`、`amount`、`time`、`note`、`originalCategory` 共 10 个字段
- [ ] `EditDialogData.recordId` 为 `Long?`，新建模式为 `null`，编辑模式为非空
- [ ] `EditDialogData.subcategory` 保留原值，编辑弹窗不提供修改入口
- [ ] `EditDialogData.originalCategory` 保存打开编辑时的原分类，用于记忆学习判断
- [ ] `EditDialogData` 注释说明 `recordId` 双模式语义、全字段编辑、`subcategory` 保留原值、`originalCategory` 用途、空值处理约定（merchant/note 清空传 null）
- [ ] `UiState.showManualEntryDialog` 已删除，`showEditDialog: EditDialogData?` 是唯一弹窗状态来源
- [ ] `MainViewModel` 中原 `showManualEntryDialog` 的所有读写处已改为 `showEditDialog`（新建模式 `recordId = null`）

## EditRecordDialog 组件
- [ ] 文件名为 `EditRecordDialog.kt`，函数名为 `EditRecordDialog`（原 `ManualEntryDialog` 已重命名）
- [ ] 参数统一为 `data: EditDialogData`，**不使用 `EditMode` 枚举**，通过 `data.recordId` 是否为 null 判断模式
- [ ] `recordId == null`（新建模式）标题「手动记账」，`recordId != null`（编辑模式）标题「编辑账单」
- [ ] 编辑模式字段预填充：金额、分类、商家、备注、时间
- [ ] 编辑模式底部显示「删除记录」按钮，点击触发 `onDeleteRequest` 回调，**不在 Composable 内部管理删除确认状态**
- [ ] 新建模式不显示「删除记录」按钮
- [ ] 分类选择仍通过内嵌 `CategoryPicker` 实现（复用现有逻辑）
- [ ] 新建模式提交走 `onSubmit(data: EditDialogData)`，编辑模式提交走 `onEditConfirm(updatedData: EditDialogData)`，参数统一为 `EditDialogData`
- [ ] 编辑模式下 `type`（支出/收入）以只读标签展示在弹窗顶部，不可切换（`CategoryPicker` 组件未改造）
- [ ] 新建模式下 `type` 显示为可切换的「支出/收入」Tab（沿用原 `ManualEntryDialog` 行为），切换时候选分类列表同步刷新
- [ ] 项目中无残留的 `ManualEntryDialog` 引用（import、Preview、Test、package、文件名）

## MainViewModel
- [ ] `openEditDialogFromDashboard(record: RecentRecord)` 方法存在，构造的 `EditDialogData` 含全字段（含 `subcategory`、`originalCategory`），`recordId` 非空
- [ ] `openEditDialog(ChatMessage.CardMessage)` 已扩展，填充 `amount`、`time`、`note`、`subcategory`、`originalCategory`，`recordId` 非空
- [ ] `confirmEditRecord(updatedData: EditDialogData)` 方法存在，**所有上下文通过 `updatedData` 参数传入，不读取 `_uiState.value.showEditDialog`**
- [ ] `confirmEditRecord` 从 `updatedData` 提取 `recordId`（必须非空，否则 return）、`type`、`subcategory`、`originalCategory`、`amount`、`category`、`merchant`、`time`、`note`
- [ ] `confirmEditRecord` 传递 `subcategory` 原值给 DAO，不覆盖原有子分类数据
- [ ] `confirmEditRecord` 检查 `rowsAffected`，若为 0 则 log 警告并弹 Toast「更新失败」
- [ ] `confirmEditRecord` 在 `updatedData.category != updatedData.originalCategory` 且新商家能提取触发词时调用 `upsertMemory`，`source` = `"auto"`
- [ ] `confirmEditRecord` 同步更新 `messages` 列表中对应 `CardMessage` 的字段（金额、分类、子分类、商家、备注、时间），**`rawInput` 和 `confidence` 保持不变**
- [ ] `confirmEditRecord` 提交成功后弹出 Toast「已更新」，清空 `showEditDialog`
- [ ] 编辑弹窗内删除复用现有 `deleteRecord` 方法，该方法已包含 `messages` 列表对应 `CardMessage` 的同步移除逻辑
- [ ] 所有新增方法通过 `AppLogger` 打印日志（含 `rowsAffected`），携带 `requestId` 和 node 节点名
- [ ] 敏感信息（如有）脱敏后再打印
- [ ] 全项目搜索 `confirmEditCategory`（Kotlin 源码、Preview、Test、注释）确认无残留调用方
- [ ] `confirmEditCategory` 方法及其残留引用已清理（Task 7.6 搜索通过后删除）

## DashboardScreen
- [ ] `pendingDeleteId` / `pendingDeleteType` 状态和删除确认 `AlertDialog` 已移除
- [ ] `DashboardScreen` 签名移除 `onDeleteRecord`，新增 `onEditRecord: (RecentRecord) -> Unit`
- [ ] `RecentRecordItem` 整行可点击，点击触发 `onEditRecord(record)`，直接打开编辑弹窗
- [ ] 列表项点击不再触发任何删除流程

## ChatScreen + ExpenseCard
- [ ] `ExpenseCard` 按钮文案为「编辑」（原「修改分类」）
- [ ] `ExpenseCard` 回调名为 `onEditRecord`（原 `onEditCategory`）
- [ ] `ChatScreen` 回调参数名为 `onEditRecord`，调用时传 `message`
- [ ] `MainActivity` 中 `ChatScreen` 注入 `onEditRecord = viewModel::openEditDialog`

## MainActivity
- [ ] `CategoryPicker` 编辑场景挂载点已移除，替换为 `EditRecordDialog` 挂载
- [ ] `ManualEntryDialog` 挂载点已移除，替换为 `EditRecordDialog` 新建模式挂载
- [ ] 统一根据 `uiState.showEditDialog` 是否为 null 决定弹窗显示，根据 `recordId` 是否为 null 决定模式（CREATE/EDIT）
- [ ] 原 `showManualEntryDialog` 的所有引用已删除
- [ ] `DashboardScreen` 调用处注入 `onEditRecord = viewModel::openEditDialogFromDashboard`，移除 `onDeleteRecord`
- [ ] 新增删除二次确认 Dialog 外层控制：`EditRecordDialog` 的 `onDeleteRequest` 回调触发外层确认 Dialog
- [ ] 删除确认后的关闭顺序：先关确认 Dialog → 再清空 `showEditDialog` 关闭编辑弹窗 → 调用 `deleteRecord(recordId, type)` 异步删除
- [ ] `CategoryPicker` 和 `ManualEntryDialog` 的 import 已清理
- [ ] `MemoryManageScreen` 中的 `CategoryPicker` 引用未受影响（独立复用场景）

## 版本与编译
- [ ] `app/build.gradle.kts` 的 `versionCode` 已 +1，`versionName` 从 2.12.0 调整为 2.13.0
- [ ] `./gradlew assembleDebug` 编译通过，无错误
- [ ] APK 输出文件名为 `记账_v2.13.0_debug.apk`

## 行为一致性验证
- [ ] 统计页点击账单 → 打开编辑弹窗（编辑模式），字段预填充正确
- [ ] 统计页编辑弹窗内点击删除 → 外层二次确认 → 确认后删除成功，统计页列表刷新且记账页 messages 同步移除
- [ ] 统计页编辑账单后，记账页消息列表（如存在对应 CardMessage）同步更新
- [ ] 记账页卡片「编辑」按钮 → 打开编辑弹窗（编辑模式），字段预填充正确
- [ ] 记账页手动记账按钮 → 打开编辑弹窗（新建模式），字段为空/默认值
- [ ] 编辑后修改分类（newCategory != originalCategory）→ 记忆学习触发，`source` = `"auto"`
- [ ] 编辑后未修改分类（newCategory == originalCategory）→ 不触发记忆学习
- [ ] 编辑后 `messages` 中对应 `CardMessage` 同步更新（金额、分类、子分类、商家、备注、时间），`rawInput` 和 `confidence` 不变
- [ ] 编辑后原有 `subcategory` 数据保留不丢失（未被覆盖为 null）
- [ ] `merchant`、`note` 清空后存储为 `null`，不出现空字符串
- [ ] 编辑不存在的 id → `rowsAffected = 0` → Toast「更新失败」
- [ ] 完整刷新链路：数据库更新 → Dashboard Flow 刷新 → 列表显示新数据 → 返回 Chat 页面消息同步
- [ ] 编辑模式下 `type` 只读标签展示，无法切换（`CategoryPicker` 组件未改造）
- [ ] 新建模式下 `type` Tab 可切换，候选分类列表同步刷新
- [ ] `MemoryManageScreen` 新增记忆弹窗的 `CategoryPicker` 仍正常工作（未受影响）
