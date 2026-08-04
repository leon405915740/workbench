# Tasks

- [ ] Task 1: 数据库层新增全字段更新方法
  - [ ] SubTask 1.1: `ExpenseDao.kt` 新增 `@Query("UPDATE expense SET amount=:amount, category=:category, subcategory=:subcategory, merchant=:merchant, time=:time, note=:note WHERE id=:id") suspend fun updateAllFields(id: Long, amount: Long, category: String, subcategory: String?, merchant: String?, time: Long, note: String?): Int`（返回 `rowsAffected`）
  - [ ] SubTask 1.2: `IncomeDao.kt` 新增同构 `updateAllFields` 方法，返回 `Int`
  - [ ] SubTask 1.3: `AppRepository.kt` 新增 `suspend fun updateExpenseFull(id, amount, category, subcategory, merchant, time, note): Int` 转发 DAO，返回 `rowsAffected`
  - [ ] SubTask 1.4: `AppRepository.kt` 新增 `suspend fun updateIncomeFull(...): Int` 转发 DAO
  - [ ] SubTask 1.5: `amount` 类型为 `Long`（分），与 `ExpenseEntity`/`IncomeEntity` 的 `amount` 字段类型一致

- [ ] Task 2: UiState 扩展 EditDialogData 字段并统一弹窗状态
  - [ ] SubTask 2.1: `UiState.kt` 的 `EditDialogData` 将 `recordId: Long` 改为 `recordId: Long?`（null 表示新建模式，非空表示编辑模式），新增 `amount: Long`、`time: Long`、`note: String?`、`subcategory: String?`（保留原值）、`originalCategory: String`（记忆学习用）五个字段
  - [ ] SubTask 2.2: 更新 `EditDialogData` 的类注释，说明 `recordId` 的双模式语义、全字段编辑、`subcategory` 保留原值不修改、`originalCategory` 用于记忆学习判断、空值处理约定（merchant/note 清空传 null）
  - [ ] SubTask 2.3: 删除 `UiState.showManualEntryDialog` 状态，统一用 `showEditDialog: EditDialogData?` 作为唯一弹窗状态来源
  - [ ] SubTask 2.4: `MainViewModel` 中原 `showManualEntryDialog` 的所有读写处改为 `showEditDialog`（新建模式构造 `EditDialogData(recordId = null, ...)`）

- [ ] Task 3: 重命名 ManualEntryDialog 为 EditRecordDialog 并扩展双模式
  - [ ] SubTask 3.1: 文件重命名 `ManualEntryDialog.kt` → `EditRecordDialog.kt`，函数重命名 `ManualEntryDialog` → `EditRecordDialog`
  - [ ] SubTask 3.2: 参数统一为 `data: EditDialogData`（不再有 `mode`/`initialAmount` 等散参数），通过 `data.recordId` 是否为 null 判断模式（null=新建，非空=编辑），**不使用 `EditMode` 枚举**
  - [ ] SubTask 3.3: 根据 `data.recordId` 是否为 null 切换标题（null→"手动记账"，非空→"编辑账单"）
  - [ ] SubTask 3.4: 编辑模式（`recordId != null`）下底部新增「删除记录」按钮，点击触发 `onDeleteRequest` 回调；**不在 Composable 内部管理删除确认状态**，二次确认 Dialog 由 `MainActivity` 外层控制
  - [ ] SubTask 3.5: 新建模式提交回调 `onSubmit(data: EditDialogData)`（ViewModel 从 data 提取字段调用 `submitManualEntry`），编辑模式提交回调 `onEditConfirm(updatedData: EditDialogData)`（updatedData 包含新值和原上下文）
  - [ ] SubTask 3.6: 保留原有 `CategoryPicker` 内嵌调用逻辑（分类选择入口不变）
  - [ ] SubTask 3.7: 编辑模式下，`type`（支出/收入）以只读标签展示在弹窗顶部（分类选择器旁），不可切换；该只读标签在 `EditRecordDialog` 层实现，`CategoryPicker` 组件本身无需改造（其 `type` 参数本就固定不可切换）
  - [ ] SubTask 3.8: 新建模式下，`type` 显示为可切换的「支出/收入」Tab（沿用原 `ManualEntryDialog` 行为），切换时候选分类列表同步刷新；分类列表数据源为 `CategoryConstants` 常量（非数据库 `categories` 表）；新建模式 `subcategory` 默认 `null`

- [ ] Task 4: MainViewModel 新增编辑入口与全字段更新方法
  - [ ] SubTask 4.1: 新增 `fun openEditDialogFromDashboard(record: RecentRecord)`，构造 `EditDialogData`（含全字段，`originalCategory = record.category`）写入 `showEditDialog`
  - [ ] SubTask 4.2: 现有 `openEditDialog(ChatMessage.CardMessage)` 同步扩展，填充 `amount`、`time`、`note`、`subcategory`、`originalCategory` 字段
  - [ ] SubTask 4.3: 新增 `fun confirmEditRecord(updatedData: EditDialogData)`，**所有上下文通过 `updatedData` 参数传入，不读取 `_uiState.value.showEditDialog`**；从 `updatedData` 提取 `recordId`（必须非空，否则 return）、`type`、`subcategory`、`originalCategory`、`amount`、`category`、`merchant`、`time`、`note`
  - [ ] SubTask 4.4: `confirmEditRecord` 在 IO 线程根据 `type` 调用 `updateExpenseFull` / `updateIncomeFull`，`subcategory` 传原值避免覆盖；检查返回值 `rowsAffected`，若为 0 则 log 警告并弹 Toast「更新失败」
  - [ ] SubTask 4.5: `confirmEditRecord` 内保留记忆学习逻辑：`updatedData.category != updatedData.originalCategory` 且新商家能提取触发词时 `upsertMemory`（`source` = `"auto"`）；分类未变更（`category == originalCategory`）时不触发任何记忆操作
  - [ ] SubTask 4.6: `confirmEditRecord` 内同步更新 `messages` 列表中对应 `CardMessage` 的字段（金额、分类、子分类、商家、备注、时间），**`rawInput` 和 `confidence` 保持不变**
  - [ ] SubTask 4.7: `confirmEditRecord` 提交成功后弹出 Toast「已更新」，清空 `showEditDialog`
  - [ ] SubTask 4.8: 保留现有 `confirmEditCategory` 方法暂不删除（避免破坏未迁移的引用），在 Task 7 搜索确认无引用后删除
  - [ ] SubTask 4.9: 所有新增方法通过 `AppLogger` 打印关键节点日志（入口、数据库写入、rowsAffected、记忆学习、UI 同步、异常分支），携带 `requestId` 和 node 节点名

- [ ] Task 5: DashboardScreen 点击行为改造
  - [ ] SubTask 5.1: 移除 `pendingDeleteId` / `pendingDeleteType` 状态和删除确认 `AlertDialog`
  - [ ] SubTask 5.2: `RecentRecordItem` 的 `onClick` 回调改为触发 `onEditRecord(record)`，由外层传入
  - [ ] SubTask 5.3: `DashboardScreen` 签名新增 `onEditRecord: (RecentRecord) -> Unit` 参数，移除 `onDeleteRecord` 参数
  - [ ] SubTask 5.4: 列表项点击直接打开编辑弹窗（编辑弹窗内提供删除入口）

- [ ] Task 6: ChatScreen + ExpenseCard 编辑入口改造
  - [ ] SubTask 6.1: `ExpenseCard.kt` 按钮文案「修改分类」→「编辑」，回调名 `onEditCategory` → `onEditRecord`
  - [ ] SubTask 6.2: `ChatScreen.kt` 回调参数名 `onEditCategory` → `onEditRecord`，调用时传 `message`（`ChatMessage.CardMessage`）
  - [ ] SubTask 6.3: `MainActivity.kt` 中 `ChatScreen` 的 `onEditCategory = viewModel::openEditDialog` 改为 `onEditRecord = viewModel::openEditDialog`

- [ ] Task 7: MainActivity 弹窗挂载点统一替换与删除确认外层控制
  - [ ] SubTask 7.1: 移除 `CategoryPicker` 编辑场景挂载点（`MainActivity.kt:271-279`），替换为 `EditRecordDialog` 挂载
  - [ ] SubTask 7.2: 移除 `ManualEntryDialog` 挂载点，替换为 `EditRecordDialog` 新建模式挂载
  - [ ] SubTask 7.3: 统一根据 `uiState.showEditDialog` 是否为 null 决定弹窗显示，根据 `recordId` 是否为 null 决定模式（CREATE/EDIT）；**删除原 `showManualEntryDialog` 的所有引用**
  - [ ] SubTask 7.4: 更新 `DashboardScreen` 调用处，`onDeleteRecord` 替换为 `onEditRecord = viewModel::openEditDialogFromDashboard`
  - [ ] SubTask 7.5: `MainActivity` 新增删除二次确认 Dialog 外层控制：`EditRecordDialog` 的 `onDeleteRequest` 回调触发外层显示确认 Dialog；用户确认后**关闭顺序：先关确认 Dialog → 再清空 `showEditDialog` 关闭编辑弹窗 → 调用 `viewModel::deleteRecord(recordId, type)` 异步删除**（`type` 从 `EditDialogData` 读取，**不清理 `category_memory` 表**）
  - [ ] SubTask 7.6: 全项目搜索 `confirmEditCategory`（范围：Kotlin 源码、Preview、Test、注释），确认无残留调用方（除 `MainViewModel` 自身定义外），记录搜索结果
  - [ ] SubTask 7.7: 确认无引用后删除 `confirmEditCategory` 方法及其残留引用
  - [ ] SubTask 7.8: 全项目搜索 `ManualEntryDialog`（范围：import、Preview、Test、package、文件名），确认无残留引用
  - [ ] SubTask 7.9: 清理 `MainActivity.kt` 中 `CategoryPicker` 和 `ManualEntryDialog` 的 import

- [ ] Task 8: 版本号更新与编译验证
  - [ ] SubTask 8.1: `app/build.gradle.kts` 的 `versionCode` +1，`versionName` 从 2.12.0 调整为 2.13.0
  - [ ] SubTask 8.2: 执行 `./gradlew assembleDebug` 编译验证，确保无错误
  - [ ] SubTask 8.3: 确认 APK 输出文件名自动更新为 `记账_v2.13.0_debug.apk`

# Task Dependencies
- Task 1（数据库层）与 Task 2（UiState）无依赖，可并行
- Task 3 依赖 Task 2（EditRecordDialog 需要 EditDialogData 的全字段）
- Task 4 依赖 Task 1、Task 2（ViewModel 调用 Repository 和构造 EditDialogData）
- Task 5、Task 6 可并行（独立页面改造）
- Task 7 依赖 Task 3、Task 4、Task 5、Task 6（弹窗挂载需组件和回调就绪）
- Task 8 依赖 Task 7（全部代码修改完成后编译验证）
