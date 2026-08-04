# requestId 生成位置统一

- [x] requestId 仅在业务层入口（ViewModel 入口方法 / PlanExecutor / IntentRouter 等）调用 `AppLogger.generateRequestId()` 生成
- [x] 同一用户操作链路中 Repository / Service / DAO 层禁止重新生成 requestId，必须通过参数透传使用上游 requestId
- [x] 同一用户操作的 start / success / error 日志使用同一 requestId
- [x] `sendMessage` 在方法第一行生成 requestId（唯一生成位置），生命周期覆盖 sendMessage 方法执行期间，方法返回后 requestId 不再使用
- [x] `sendMessage` requestId 透传路径：parseAccountingInput → routeIntent → IntentRouter.route → PlanBuilder.buildPlan → AiPlanner.parse → ClassificationService.match → insertExpense/income → incrementMemoryHitCount
- [x] `sendMessage` 异常分支使用同一 requestId，打印 stage=error，不重新生成
- [x] `confirmEditRecord` 生成 requestId，update 与 upsertMemory 使用同一 requestId
- [x] `deleteRecord` 生成 requestId，透传给 deleteExpense/deleteIncome
- [x] `addMemory` / `deleteMemory` / `clearAllMemories` / `restoreDefaultMemories` / `confirmLearnKeyword` / `saveApiKey` 各自生成 requestId
- [x] 系统事件（AccountingApp 生命周期 / PaymentAccessibilityService 服务级事件 / MainViewModel.onCleared）使用空字符串 `""` 作为 requestId
- [x] 自动采集使用 `cap_<时间戳>_<4位随机数>` 作为 captureId，等价 requestId 全链路透传
- [x] requestId 不持久化到数据库或缓存，不跨方法复用

# 日志格式规范（stage/start/success/error，统一字段顺序）

- [x] AppLogger 自动组装前缀 `[requestId] [node] [第N笔]`（多笔场景含笔序号）
- [x] message 主体字段顺序固定：requestId, action, stage, result, 业务字段, rowsAffected, error
- [x] message 显式带 `requestId=<requestId>`（便于全文检索）
- [x] stage 取值：start（入口）/ success（成功出口）/ error（失败出口）
- [x] success 日志含 `result=success`
- [x] error 日志含 `result=failure, error=<异常信息>` + 异常对象
- [x] node 节名为操作类型 + 阶段描述（如「删除执行」「全字段更新执行」「记忆写入」「API Key 保存」）

# maskApiKey 脱敏规则（固定）

- [x] 所有涉及 API Key 的日志通过 `AppLogger.maskApiKey(key)` 脱敏后打印
- [x] 脱敏规则：长度 > 8 保留前 4 + `****` + 后 4；长度 ≤ 8 返回 `****`；空/null 返回 `<empty>`
- [x] AiPlanner.callDeepSeek 已脱敏（保留）
- [x] AppRepository.chatQuery 已脱敏（保留）
- [x] AiClassifier.correct 已脱敏（保留）
- [x] MainViewModel.saveApiKey 新增脱敏
- [x] AppRepository.setApiKey 新增脱敏
- [x] 业务代码无自行拼接脱敏字符串（grep 验证无 `substring(0,4) + "***"` 模式）
- [x] 禁止在 Toast / Snackbar / Debug 工具中明文打印 API Key
- [x] 禁止将 API Key 写入文件 / SharedPreferences 日志 / 崩溃堆栈

# Repository 调用规范（业务层入口包裹 requestId）

- [x] AppRepository 写入方法（按写操作语义判断，任何修改数据库状态的操作）由业务层入口包裹 requestId 调用
- [x] 业务层入口定义：ViewModel 写入方法 / PlanExecutor.execute / IntentRouter.autoCapture / 业务 Service 写入入口
- [x] UI Screen / Activity 禁止直接调用 repository 写入方法
- [x] 禁止在 UI 层或非业务入口处显式生成 requestId 透传
- [x] 扫描 ui/screens/ 所有文件（DashboardScreen / ChatScreen / SettingsScreen / MappingManageScreen / MemoryManageScreen / categorymanagescreen）
- [x] 扫描 ui/components/ 所有 Component 文件
- [x] 扫描 MainActivity.kt
- [x] 扫描 capture/ 所有文件（PaymentAccessibilityService / CaptureDispatcher / CaptureNotificationManager）
- [x] 扫描 domain/ 所有文件（CategoryService / RuleSuggestion）
- [x] 扫描 plan/ 所有文件（PlanBuilder / PlanMerger / PlanExecutor）
- [x] 扫描 parser/ 所有 matcher / parser 文件
- [x] 写入方法的 UI 直接调用迁移到业务层入口
- [x] 业务层入口已存在但无 requestId 的（PlanExecutor / IntentRouter 等）补 requestId 生成与透传
- [x] 输出扫描报告：调用点清单 + 处理方式

# Repository 日志位置规范（start/success/error 三阶段，message 显式带 requestId）

- [x] AppRepository.deleteExpense 三阶段日志（start d / success i / error e），message 显式带 requestId
- [x] AppRepository.deleteIncome 三阶段日志，message 显式带 requestId
- [x] AppRepository.updateExpenseCategory 三阶段日志（含 rowsAffected，=0 时仍打 stage=success），message 显式带 requestId
- [x] AppRepository.updateIncomeCategory 三阶段日志（含 rowsAffected，=0 时仍打 stage=success），message 显式带 requestId
- [x] AppRepository.updateExpenseFull 三阶段日志（含 rowsAffected，=0 时仍打 stage=success），message 显式带 requestId
- [x] AppRepository.updateIncomeFull 三阶段日志（含 rowsAffected，=0 时仍打 stage=success），message 显式带 requestId
- [x] AppRepository.upsertMapping 三阶段日志，message 显式带 requestId
- [x] AppRepository.deleteMappingById 三阶段日志，message 显式带 requestId
- [x] AppRepository.updateMappingEnabled 三阶段日志（含 rowsAffected），message 显式带 requestId
- [x] AppRepository.promoteMappingToManual 三阶段日志（含 rowsAffected），message 显式带 requestId
- [x] AppRepository.cleanStaleAutoMappings 三阶段日志（含 rowsAffected），message 显式带 requestId
- [x] AppRepository.incrementMappingHitCount 三阶段日志，message 显式带 requestId
- [x] AppRepository.upsertMemory 三阶段日志，message 显式带 requestId
- [x] AppRepository.deleteMemory 三阶段日志，message 显式带 requestId
- [x] AppRepository.deleteAllMemories 三阶段日志，message 显式带 requestId
- [x] AppRepository.reseedMemories 三阶段日志，message 显式带 requestId
- [x] AppRepository.incrementMemoryHitCount 三阶段日志，message 显式带 requestId
- [x] AppRepository.setApiKey 三阶段日志（脱敏 Key），message 显式带 requestId

# rowsAffected=0 日志行为

- [x] update 类方法返回 rowsAffected=0 时打印 stage=success（非 error）
- [x] rowsAffected=0 日志显式标注 `rowsAffected=0`
- [x] rowsAffected=0 不抛异常，由调用方判断是否提示用户

# MainViewModel 写入入口埋点

- [x] MainViewModel.deleteRecord 生成 requestId + start/success/error 日志 + 透传给 repository.deleteExpense/deleteIncome
- [x] MainViewModel.addMemory 生成 requestId + start/success/error 日志 + 透传给 repository.upsertMemory
- [x] MainViewModel.deleteMemory 生成 requestId + start/success/error 日志 + 透传给 repository.deleteMemory
- [x] MainViewModel.clearAllMemories 生成 requestId + start/success/error 日志 + 透传给 repository.deleteAllMemories
- [x] MainViewModel.restoreDefaultMemories 生成 requestId + start/success/error 日志 + 透传给 repository.reseedMemories
- [x] MainViewModel.saveApiKey 生成 requestId + 脱敏 Key 日志（空/短/正常）+ 透传给 repository.setApiKey
- [x] MainViewModel.confirmLearnKeyword 生成 requestId + start/success/error 日志 + 透传给 repository.upsertMemory
- [x] MainViewModel.sendMessage 内 incrementMemoryHitCount 调用透传 requestId
- [x] MainViewModel.confirmEditRecord 内 upsertMemory 调用透传 requestId

# insert/update/delete 链路 requestId 来源

- [x] insert 链路：sendMessage 生成 requestId → repository.insertExpense/insertIncome
- [x] insert 链路：submitManualEntry 生成 requestId → repository.insertExpense/insertIncome
- [x] update 链路：confirmEditRecord 生成 requestId → repository.updateExpenseFull/updateIncomeFull/upsertMemory
- [x] delete 链路：deleteRecord 生成 requestId → repository.deleteExpense/deleteIncome
- [x] delete 链路：deleteMemory 生成 requestId → repository.deleteMemory
- [x] delete 链路：clearAllMemories 生成 requestId → repository.deleteAllMemories
- [x] memory 链路：addMemory 生成 requestId → repository.upsertMemory
- [x] memory 链路：restoreDefaultMemories 生成 requestId → repository.reseedMemories
- [x] memory 链路：confirmLearnKeyword 生成 requestId → repository.upsertMemory
- [x] mapping 链路：业务层入口（ViewModel 或其他业务 Service）生成 requestId（Task 5 扫描后补充）

# AiPlanner 日志级别

- [x] AiPlanner.parse 失败日志从 d 改为 w
- [x] AiPlanner.parseJson 失败日志从 d 改为 w

# BillTransaction 日志内容（含 error 阶段）

- [x] BillTransaction.insert 三阶段日志（start/success/error），message 显式带 requestId + id/type/category
- [x] BillTransaction.update 三阶段日志，message 显式带 requestId（保持与 insert 一致格式，action=UPDATE）
- [x] BillTransaction.delete 三阶段日志，message 显式带 requestId（保持与 insert 一致格式，action=DELETE）
- [x] 不再仅打印「金额已脱敏」（金额由 sanitizeLog 自动脱敏）

# 通用约束

- [x] 所有新增日志携带 requestId 和 node 节点名
- [x] 敏感信息（API Key）脱敏后再打印
- [x] 金额字段由 sanitizeLog 自动脱敏，不手动拼接脱敏字符串
- [x] 编译通过（assembleDebug）
- [x] versionCode 自增 / versionName 调整
- [x] APK 输出文件名为 `记账_v{versionName}_debug.apk`

# 全链路验收测试（含失败链路 + requestId 唯一性）

- [x] 用户记账全链路（成功）：sendMessage → parseAccountingInput → routeIntent → buildPlan → AiPlanner → ClassificationService → insertExpense → incrementMemoryHitCount，同一 requestId 贯穿
- [x] 用户记账全链路（失败）：sendMessage → 解析失败/入库异常，失败分支使用同一 requestId 且打印 stage=error 日志
- [x] 编辑账单全链路（成功）：confirmEditRecord → updateExpenseFull → upsertMemory，同一 requestId
- [x] 编辑账单全链路（rowsAffected=0）：confirmEditRecord → updateExpenseFull 返回 rowsAffected=0，打印 stage=success 含 rowsAffected=0
- [x] 删除账单全链路（成功）：deleteRecord → deleteExpense，requestId 生成与透传
- [x] 删除账单全链路（失败）：deleteRecord → deleteExpense 异常，打印 stage=error 日志
- [x] 记忆管理全链路（成功+失败）：addMemory/deleteMemory/clearAllMemories/restoreDefaultMemories → repository，requestId 生成与透传，失败分支打印 stage=error
- [x] API Key 保存全链路（成功+失败+边界）：saveApiKey → setApiKey，脱敏 + requestId，空 Key（`<empty>`）/ 短 Key（`****`）/ 失败分支
- [x] 关键词学习全链路（成功+失败）：confirmLearnKeyword → upsertMemory，requestId 生成与透传
- [x] requestId 唯一性：同一用户操作全链路中 requestId 不重复生成（grep 日志，每个操作仅出现一次 requestId 生成日志）
- [x] requestId 唯一性：不同用户操作生成不同 requestId（连续操作两次，验证 requestId 不同）
- [x] 日志字段顺序校验：所有新增日志符合 `requestId, action, stage, result, 业务字段, rowsAffected, error` 顺序
- [x] 脱敏校验：grep 日志文件无明文 API Key
- [x] grep 验证：所有 AppRepository 写入方法均带 requestId 参数（无遗漏）
- [x] grep 验证：无直接调用 `Log.d/e/i/w`（范围限制：排除 `app/src/main/java/com/accounting/app/log/` 目录 + `build/` + `test/` + `androidTest/`，仅 log 目录内部允许使用 android.util.Log）
