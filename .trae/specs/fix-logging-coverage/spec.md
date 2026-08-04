# 补齐日志埋点覆盖范围 Spec

## Why

日志埋点审查发现 `AppRepository` 大量写入方法（删除、更新、记忆/映射 CRUD、API Key 设置）和 `MainViewModel` 多个写入入口（删除记录、记忆管理、API Key 保存、关键词学习确认）既无 requestId 透传也无 AppLogger 埋点，违反项目规则「所有数据库写入必须通过 PlanExecutor.execute()」「每个功能必须做日志埋点」。导致用户操作这些路径后无法在日志中追溯，问题排查盲区。

本次只补齐**埋点覆盖**与**requestId 透传**，不做架构变更（不强制回归 PlanExecutor，不调整 PlanValidator 红线），保持低侵入。

## What Changes

- **ADDED** 统一 requestId 生成位置规则（见下方「requestId 规则」章节）
- **ADDED** 统一日志格式规范：`stage=start/success/error`（见下方「日志格式规范」章节）
- **ADDED** 固定 maskApiKey 使用规则（见下方「脱敏规则」章节）
- **ADDED** Repository 调用规范：写入方法必须由业务层入口包裹 requestId（见下方「Repository 调用规范」章节）
- **MODIFIED** `AppRepository` 写入方法签名：补 `requestId: String` 参数 + start/success/error 三阶段埋点（message 中显式带 requestId 便于全文检索）
  - 删除：`deleteExpense` / `deleteIncome`
  - 分类更新：`updateExpenseCategory` / `updateIncomeCategory`
  - 全字段更新：`updateExpenseFull` / `updateIncomeFull`
  - 映射 CRUD：`upsertMapping` / `deleteMappingById` / `updateMappingEnabled` / `promoteMappingToManual` / `cleanStaleAutoMappings` / `incrementMappingHitCount`
  - 记忆 CRUD：`upsertMemory` / `deleteMemory` / `deleteAllMemories` / `reseedMemories` / `incrementMemoryHitCount`
  - API Key：`setApiKey`（脱敏后打印）
- **MODIFIED** `MainViewModel` 写入入口：生成 requestId 并打印 start/success/error 日志
  - `deleteRecord` / `confirmLearnKeyword`
  - `addMemory` / `deleteMemory` / `clearAllMemories` / `restoreDefaultMemories`
  - `saveApiKey`
  - 已有埋点保留：`sendMessage` / `submitManualEntry` / `confirmEditRecord` / `onPaymentCapturedFromIntent`
- **MODIFIED** `AiPlanner.parse` 失败日志：`d` → `w`
- **MODIFIED** `AiPlanner.parseJson` JSON 解析失败日志：`d` → `w`
- **MODIFIED** `BillTransaction` 所有方法日志：message 中显式带 requestId + id/type/category 关键字段，增加 error 阶段日志
- **NOT CHANGED** 架构层面：不强制记账主流程回归 `PlanExecutor.execute()`，不调整 `PlanValidator`
- **NOT CHANGED** 空 requestId 使用约定：`AccountingApp` 生命周期、`PaymentAccessibilityService` 服务级事件、`MainViewModel.onCleared` 维持 `""`（系统事件非用户请求）

## Impact

- Affected specs:
  - `accounting-app`（核心架构约束：日志规范、requestId 全链路透传）
  - `add-dashboard-edit-record`（依赖 `updateExpenseFull` / `updateIncomeFull` 签名）
- Affected code:
  - `app/src/main/java/com/accounting/app/data/repository/AppRepository.kt`
  - `app/src/main/java/com/accounting/app/ui/MainViewModel.kt`
  - `app/src/main/java/com/accounting/app/ai/service/AiPlanner.kt`
  - `app/src/main/java/com/accounting/app/plan/execution/BillTransaction.kt`
  - 间接调用方扫描范围（Task 5）：
    - `ui/screens/`：`DashboardScreen` / `ChatScreen` / `SettingsScreen` / `MappingManageScreen` / `MemoryManageScreen` / `categorymanagescreen`
    - `ui/components/`：所有 Component 文件
    - `MainActivity.kt`
    - `capture/`：`PaymentAccessibilityService` / `CaptureDispatcher` / `CaptureNotificationManager`
    - `domain/`：`CategoryService` / `RuleSuggestion`
    - `plan/`：`PlanBuilder` / `PlanMerger` / `PlanExecutor`
    - `parser/`：所有 matcher / parser

## ADDED Requirements

### Requirement: requestId 生成位置统一

所有由**用户操作触发的写入路径** SHALL 遵循「单一生成位置」原则：

1. **生成位置**：仅在 ViewModel 入口方法（用户操作第一站）调用 `AppLogger.generateRequestId()` 生成唯一 requestId。
2. **禁止多处生成**：同一用户操作链路中，Repository / Service / DAO 层禁止重新生成 requestId，必须通过参数透传使用上游 requestId。
3. **格式**：`req_<时间戳>_<4位随机数>`（由 `AppLogger.generateRequestId()` 保证）。
4. **透传**：requestId 通过方法参数显式透传到 Repository / Service / DAO 调用链，禁止中途丢失。
5. **复用**：同一用户操作的 start / success / error 日志必须使用同一个 requestId。
6. **系统事件豁免**：`AccountingApp` 生命周期回调、`PaymentAccessibilityService` 服务级事件、`MainViewModel.onCleared` 使用空字符串 `""` 作为 requestId（系统事件非用户请求，无统一入口）。
7. **自动采集豁免**：`PaymentAccessibilityService` 使用 `cap_<时间戳>_<4位随机数>` 作为 captureId，等价于 requestId，全链路透传到 `IntentRouter.autoCapture` / `PlanExecutor`。

#### Scenario: 用户操作触发写入
- **WHEN** 用户在 UI 点击按钮触发写入（删除/编辑/记忆管理/API Key 保存）
- **THEN** ViewModel 入口方法生成 requestId（唯一生成位置），透传到 repository 方法，repository 在 start / success / error 日志中使用同一 requestId，禁止 repository 内重新生成

#### Scenario: 同一操作多步写入
- **WHEN** 同一用户操作触发多次 repository 写入（如 `confirmEditRecord` 先 update 后 upsertMemory）
- **THEN** 两次写入使用同一 requestId（由 ViewModel 入口生成），便于关联为同一操作链

#### Scenario: 系统事件
- **WHEN** `AccountingApp.onCreate` / `onActivityResumed` 等生命周期回调
- **THEN** 使用空字符串 `""` 作为 requestId，日志节点名标识来源（如「应用启动」「应用生命周期」）

### Requirement: 日志格式规范（stage/start/success/error）

所有新增埋点 SHALL 遵循统一格式与字段顺序：

- **前缀**：由 `AppLogger` 自动组装 `[requestId] [node] [第N笔]`（多笔场景含笔序号）
- **message 主体字段顺序**（固定，不可调整）：
  1. `requestId=<requestId>`（显式带，便于全文检索）
  2. `action=<操作类型>`
  3. `stage=<阶段>`
  4. `result=<success/failure>`（仅 success/error 阶段）
  5. 业务关键字段（id/type/triggerWord/keyword/category 等）
  6. `rowsAffected=<N>`（仅 update 类操作）
  7. `error=<异常信息>`（仅 error 阶段）
- **stage 取值**：
  - `start`：方法入口，含 requestId + action + stage=start + 关键参数（id/type/triggerWord/keyword 等），不含敏感数据
  - `success`：方法成功出口，含 requestId + action + stage=success + result=success + 关键结果（rowsAffected/insertedId 等）
  - `error`：方法失败出口，含 requestId + action + stage=error + result=failure + error=<异常信息> + 异常对象
- **示例**：
  - start：`requestId=req_xxx, action=DELETE, stage=start, id=123, type=expense`
  - success：`requestId=req_xxx, action=DELETE, stage=success, result=success, id=123, type=expense`
  - error：`requestId=req_xxx, action=DELETE, stage=error, result=failure, id=123, type=expense, error=<msg>`
- **node 节名**：操作类型 + 阶段描述，如「删除执行」「全字段更新执行」「记忆写入」「API Key 保存」

#### Scenario: 删除记录日志格式
- **WHEN** 调用 `repository.deleteExpense(id=123, requestId="req_xxx")`
- **THEN** start 日志：`[req_xxx] [删除执行] requestId=req_xxx, action=DELETE, stage=start, id=123, type=expense`
- **AND** success 日志：`[req_xxx] [删除执行] requestId=req_xxx, action=DELETE, stage=success, result=success, id=123, type=expense`
- **AND** error 日志：`[req_xxx] [删除执行] requestId=req_xxx, action=DELETE, stage=error, result=failure, id=123, type=expense, error=<msg>`

### Requirement: maskApiKey 脱敏规则（固定）

所有涉及 API Key 的日志 SHALL 通过 `AppLogger.maskApiKey(key)` 脱敏后再打印，禁止明文打印 API Key。此规则为固定规则，不可变通。

- **脱敏规则**：
  - Key 长度 > 8：保留前 4 位 + `****` + 后 4 位（如 `sk-12345678abcdef` → `sk-12****cdef`）
  - Key 长度 ≤ 8：返回 `****`（避免短 Key 被反推）
  - Key 为空或 null：返回 `<empty>`
- **适用位置**：
  - `AiPlanner.callDeepSeek`（已有，保留）
  - `AppRepository.chatQuery`（已有，保留）
  - `AiClassifier.correct`（已有，保留）
  - `MainViewModel.saveApiKey`（新增）
  - `AppRepository.setApiKey`（新增）
- **禁止**：
  - 业务代码自行拼接脱敏字符串（如 `key.substring(0,4) + "***"`）
  - 在日志、Toast、Snackbar、Debug 工具中明文打印 API Key
  - 将 API Key 写入文件、SharedPreferences 日志、崩溃堆栈

#### Scenario: API Key 保存日志
- **WHEN** 用户保存 API Key `sk-12345678abcdef`
- **THEN** ViewModel 与 Repository 日志均打印 `key=sk-12****cdef`，不打印原始 Key

#### Scenario: 空 API Key
- **WHEN** 用户保存空 API Key
- **THEN** 日志打印 `key=<empty>`，不抛异常

#### Scenario: 短 API Key
- **WHEN** 用户保存 API Key `sk-1234`（长度 7 ≤ 8）
- **THEN** 日志打印 `key=****`，不暴露任何字符

### Requirement: Repository 调用规范

`AppRepository` 的写入方法（任何会修改数据库状态的操作）SHALL 由「业务层入口」包裹 requestId 后调用，禁止 UI 层裸调用。

- **判断标准**：按「写操作」语义判断，不按方法名前缀。任何会修改数据库状态的操作（insert/update/delete/upsert/clean/reseed/promote/increment/setApiKey 等）均视为写入方法，不论方法名。
- **业务层入口定义**：调用 repository 写入方法的业务链路起点，包括：
  - `MainViewModel` 的写入方法（deleteRecord / confirmEditRecord / addMemory / deleteMemory / clearAllMemories / restoreDefaultMemories / saveApiKey / confirmLearnKeyword / submitManualEntry / sendMessage 等）
  - `PlanExecutor.execute`（批量执行入口）
  - `IntentRouter.autoCapture`（自动采集入口）
  - 其他业务 Service 的写入入口
- **调用规范**：
  - 业务层入口负责生成 requestId（唯一生成位置）并透传到 repository
  - UI Screen / Activity 必须调用业务层入口方法（如 ViewModel 方法），不直接持有 repository 引用
  - 禁止在 UI 层或非业务入口处显式生成 requestId 透传

#### Scenario: UI 直接调用 repository 写入方法
- **WHEN** Grep 发现 UI Screen 或 Activity 直接调用 repository 写入方法
- **THEN** 必须迁移到业务层入口（ViewModel 方法或其他业务 Service），由业务层入口生成 requestId 并透传

#### Scenario: 业务层入口已存在
- **WHEN** Grep 发现 repository 写入方法由 PlanExecutor / IntentRouter 等业务层调用
- **THEN** 该业务层入口需补 requestId 生成与透传（如已有 requestId 则透传，无则生成）

### Requirement: Repository 日志位置规范

所有 `AppRepository` 写入方法的日志 SHALL 在以下三个 stage 打印：

1. **start 日志**（`AppLogger.d`）：方法入口处，含 `action` + `stage=start` + 关键参数（id/type/triggerWord/keyword 等），不含敏感数据
2. **success 日志**（`AppLogger.i`）：方法成功出口处，含 `stage=success` + `result=success` + 关键结果（rowsAffected/insertedId 等）
3. **error 日志**（`AppLogger.e`）：方法失败出口处，含 `stage=error` + `result=failure` + `error=<异常信息>` + 异常对象

#### Scenario: 三阶段日志
- **WHEN** 调用任意 repository 写入方法
- **THEN** 必须同时存在 start / success 或 start / error 日志，便于定位是「未进入方法」还是「方法内失败」

### Requirement: sendMessage requestId 生命周期明确

`MainViewModel.sendMessage` SHALL 在方法第一行生成 requestId（`AppLogger.generateRequestId()`），requestId 生命周期覆盖整个 sendMessage 方法执行期间，方法返回后 requestId 不再使用。

- **生成**：sendMessage 方法第一行调用 `AppLogger.generateRequestId()`
- **生命周期**：
  - 生成点：sendMessage 方法入口
  - 透传路径：`repository.parseAccountingInput` → `routeIntent` → `IntentRouter.route` → `PlanBuilder.buildPlan` → `AiPlanner.parse` → `ClassificationService.match` → `repository.insertExpense/insertIncome` → `repository.incrementMemoryHitCount`
  - 销毁点：sendMessage 方法返回（requestId 不持久化，不跨方法复用）
- **多笔场景**：同一 requestId 下通过 `billIndex` 区分不同账单
- **异常分支**：金额超限、解析失败、入库异常等所有分支使用同一 requestId
- **禁止**：下游方法重新生成 requestId；下游方法将 requestId 持久化到数据库或缓存

#### Scenario: sendMessage 全链路透传
- **WHEN** 用户发送记账消息
- **THEN** `sendMessage` 生成 requestId，透传到 `parseAccountingInput` → `routeIntent` → `IntentRouter.route` → `PlanBuilder.buildPlan` → `AiPlanner.parse` → `ClassificationService.match` → `insertExpense/income` → `incrementMemoryHitCount`，全链路同一 requestId

#### Scenario: sendMessage 异常分支
- **WHEN** sendMessage 执行过程中发生异常（解析失败/入库异常）
- **THEN** 异常分支使用同一 requestId，打印 stage=error 日志，不重新生成 requestId

### Requirement: rowsAffected=0 日志行为定义

`AppRepository` 的 update 类方法（`updateExpenseFull` / `updateIncomeFull` / `updateExpenseCategory` / `updateIncomeCategory` / `updateMappingEnabled` / `promoteMappingToManual`）返回 `rowsAffected=0` 时 SHALL 按以下规则处理：

- **日志阶段**：打印 `stage=success`（方法本身执行成功，无异常）
- **result 字段**：`result=success`
- **额外字段**：`rowsAffected=0`（显式标注，便于排查「目标记录不存在」场景）
- **不抛异常**：rowsAffected=0 不是错误，调用方根据业务逻辑判断是否需要提示用户

#### Scenario: update 返回 rowsAffected=0
- **WHEN** 调用 `repository.updateExpenseFull(id=999, ...)` 但 id=999 不存在
- **THEN** 打印 `requestId=req_xxx, action=UPDATE_FULL, stage=success, result=success, recordId=999, type=expense, rowsAffected=0`，不抛异常，由调用方判断是否提示用户

### Requirement: insert/update/delete 链路 requestId 来源明确

所有写入链路 SHALL 明确 requestId 的生成来源（从哪个 ViewModel 方法生成）：

- **insert 链路**：
  - `MainViewModel.sendMessage` 生成 requestId → `repository.insertExpense` / `repository.insertIncome`
  - `MainViewModel.submitManualEntry` 生成 requestId → `repository.insertExpense` / `repository.insertIncome`
- **update 链路**：
  - `MainViewModel.confirmEditRecord` 生成 requestId → `repository.updateExpenseFull` / `repository.updateIncomeFull` / `repository.upsertMemory`
- **delete 链路**：
  - `MainViewModel.deleteRecord` 生成 requestId → `repository.deleteExpense` / `repository.deleteIncome`
  - `MainViewModel.deleteMemory` 生成 requestId → `repository.deleteMemory`
  - `MainViewModel.clearAllMemories` 生成 requestId → `repository.deleteAllMemories`
- **memory 链路**：
  - `MainViewModel.addMemory` 生成 requestId → `repository.upsertMemory`
  - `MainViewModel.restoreDefaultMemories` 生成 requestId → `repository.reseedMemories`
  - `MainViewModel.confirmLearnKeyword` 生成 requestId → `repository.upsertMemory`
- **mapping 链路**：通过 ViewModel 方法包装（Task 5 扫描后补充）

#### Scenario: 写入链路 requestId 来源可追溯
- **WHEN** 排查日志中发现某条写入记录
- **THEN** 可通过 requestId 反查到生成它的 ViewModel 入口方法，便于定位用户操作来源

### Requirement: ViewModel 写入入口埋点

`MainViewModel` 中所有触发数据库写入的方法 SHALL 在入口生成 requestId（唯一生成位置），并通过 `AppLogger.d` 打印 start 日志（含 action + stage=start + 关键参数），通过 `AppLogger.i` / `AppLogger.e` 打印 success / error 日志。

#### Scenario: 删除记录入口
- **WHEN** 用户在卡片点击删除按钮
- **THEN** `MainViewModel.deleteRecord` 生成 requestId，打印 start 日志 `action=DELETE_RECORD, stage=start, recordId=X, type=Y`，调用 `repository.deleteExpense(id, requestId)`，更新 UI 后打印 success 日志

#### Scenario: 记忆管理操作
- **WHEN** 用户在记忆管理页新增/删除/清空/恢复默认
- **THEN** 对应方法生成 requestId 并打印 start / success / error 日志

#### Scenario: API Key 保存
- **WHEN** 用户在设置页保存 API Key
- **THEN** `saveApiKey` 生成 requestId，打印脱敏 Key，调用 `repository.setApiKey(key, requestId)`

### Requirement: Repository 调用扫描范围

扫描范围 SHALL 覆盖所有可能直接调用 `AppRepository` 写入方法的位置，确保无遗漏：

- `ui/screens/`：`DashboardScreen` / `ChatScreen` / `SettingsScreen` / `MappingManageScreen` / `MemoryManageScreen` / `categorymanagescreen`
- `ui/components/`：所有 Component 文件
- `MainActivity.kt`
- `capture/`：`PaymentAccessibilityService` / `CaptureDispatcher` / `CaptureNotificationManager`
- `domain/`：`CategoryService` / `RuleSuggestion`
- `plan/`：`PlanBuilder` / `PlanMerger` / `PlanExecutor`
- `parser/`：所有 matcher / parser

#### Scenario: 扫描结果处理
- **WHEN** Grep 发现非 ViewModel 位置直接调用 repository 写入方法
- **THEN** 必须迁移到 ViewModel 方法包装，禁止在调用点显式生成 requestId

## MODIFIED Requirements

### Requirement: AI 降级日志级别

`AiPlanner.parse` 与 `parseJson` 在 AI 解析失败/JSON 解析失败时 SHALL 使用 `AppLogger.w` 而非 `AppLogger.d`，确保 Release 包关闭详细日志时仍能追踪降级路径。

### Requirement: BillTransaction 日志内容（含 error 阶段）

`BillTransaction` 的 insert / update / delete 方法 SHALL 遵循统一日志规范：

1. **message 中显式带 requestId**（便于全文检索）
2. **包含 `id` / `type` / `category` 关键字段**
3. **三阶段日志**（start / success / error）：
   - start：方法入口，含 requestId + action + stage=start + 关键参数
   - success：成功出口，含 requestId + action + stage=success + result=success + id/type/category
   - error：失败出口，含 requestId + action + stage=error + result=failure + error=<异常信息> + 异常对象
4. **不再仅打印「金额已脱敏」**（金额由 `sanitizeLog` 自动脱敏）

#### Scenario: insert 成功日志
- **WHEN** `BillTransaction.insert` 成功插入账单
- **THEN** 日志格式：`[requestId] [BillTransaction] [第N笔] requestId=<requestId>, action=INSERT, stage=success, result=success, id=<id>, type=<type>, category=<category>`

#### Scenario: insert 失败日志
- **WHEN** `BillTransaction.insert` 插入失败（如 DAO 异常）
- **THEN** 日志格式：`[requestId] [BillTransaction] [第N笔] requestId=<requestId>, action=INSERT, stage=error, result=failure, id=<id>, type=<type>, category=<category>, error=<msg>`

#### Scenario: update / delete 日志
- **WHEN** 调用 `BillTransaction.update` / `BillTransaction.delete`
- **THEN** 与 insert 一致的三阶段日志格式，action 分别为 UPDATE / DELETE

## REMOVED Requirements

无（本次不删除任何既有能力）
