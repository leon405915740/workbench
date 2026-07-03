# 实施任务列表

## 依赖关系总览
```
Task1（AppLogger工具类）
   ↓
Task3 ┐
Task4 ├─ 并行执行（下游各业务方法加 requestId 参数 + 埋点）
Task5 │
Task6 │
Task7 ┘
   ↓
Task2（MainViewModel 生成 ID 并透传）
   ↓
Task8（全局验证与回归）
```

> **关键原则**：先改所有下游方法的入参（Task3-7），再改上游调用方传参（Task2），全程编译可通过。所有新增的 `requestId` 参数不设置默认值，通过编译报错强制调用方透传。

---

## [x] Task 1: 新增统一日志工具类 AppLogger
- **Priority**: high
- **Depends On**: None
- **Description**:
  - 新建文件 `app/src/main/java/com/accounting/app/util/AppLogger.kt`
  - 实现 `object AppLogger` 单例
  - 实现 `generateRequestId()` 方法，返回 `req_时间戳_4位随机数` 格式
  - 实现日志方法重载矩阵：
    - `d(requestId, node, message)` — 调试日志
    - `d(requestId, node, message, billIndex: Int)` — 多笔调试日志（自动追加 `[第N笔]`）
    - `e(requestId, node, message, throwable)` — 错误日志
    - `e(requestId, node, message, throwable, billIndex: Int)` — 多笔错误日志（自动追加 `[第N笔]`，多笔场景下单笔失败可精准定位）
    - `i(requestId, node, message)` — 信息日志
    - `i(requestId, node, message, billIndex: Int)` — 多笔信息日志
  - 暴露 **public 方法** `maskApiKey(key: String): String` — 前4位+****+后4位，业务代码显式调用
  - 内部 **private 方法** `truncateText(text: String, maxLen: Int = 2000): String` — 超长自动截断并追加 `...(已截断)`，由 `d/e/i` 内部自动调用
  - **截断范围限定**：2000 字符仅作用于 message 主体，`requestId` / `node` / `[第N笔]` 前缀不计入长度，始终完整保留
  - **预留扩展位**：内部保留 `private fun saveLogToFile(level: String, tag: String, msg: String)` 空方法占位（不抛异常、不影响主流程），当前版本不实现
  - 通过 `BuildConfig.DEBUG` 控制日志总开关
  - 统一 Tag 为 `"AccountingApp"`
  - 日志输出格式：`[requestId] [node] [第N笔] message`（无多笔则不含笔序号）
  - **零侵入约束**：本任务为新增工具类，不修改任何业务逻辑、算法、UI、数据库
- **Acceptance Criteria Addressed**: V-10, V-11, V-12, V-13, V-14, V-21
- **Test Requirements**:
  - `programmatic` TR-1.1: `generateRequestId()` 返回符合 `req_\d+_\d{4}` 正则的字符串
  - `programmatic` TR-1.2: Debug 包下 `d/e/i` 方法正常输出到 Logcat
  - `programmatic` TR-1.3: `maskApiKey("sk-12345678abcdef")` 返回 `sk-12****cdef`，且方法为 `public`
  - `programmatic` TR-1.4: `truncateText` 对超长字符串截断到 2000 字符并追加 `...(已截断)`，且方法为 `private`
  - `programmatic` TR-1.5: 多笔重载 `d(requestId, node, message, billIndex=2)` 输出包含 `[第2笔]`
  - `programmatic` TR-1.6: 错误日志多笔重载 `e(requestId, node, message, throwable, billIndex=2)` 输出包含 `[第2笔]`
  - `programmatic` TR-1.7: 超长 message 截断时，前缀 `requestId / node / [第N笔]` 完整保留
  - `programmatic` TR-1.8: `saveLogToFile` 存在且为空方法（不抛异常）
  - `human-judgement` TR-1.9: Release 包下日志无输出（可通过手动设置 `enableLog=false` 验证）

## [ ] Task 3: 意图分流节点埋点
- **Priority**: high
- **Depends On**: Task 1
- **Description**:
  - 对应文件：`IntentParser.kt`（或意图解析所在的类，根据实际项目结构调整）
  - 解析方法新增 `requestId: String` 参数（**无默认值**）
  - 节点2 埋点：打印原始文本、命中的关键词/规则、最终判定意图、判定依据
  - 核心记账链路内的 try-catch 异常分支使用 `AppLogger.e(requestId, node, message, throwable)` 打印
  - **零侵入约束**：不修改任何业务判断逻辑、不改动算法结果
- **Acceptance Criteria Addressed**: V-1, V-6, V-10, V-11
- **Test Requirements**:
  - `programmatic` TR-3.1: 意图解析方法签名包含 `requestId: String`（无默认值）
  - `programmatic` TR-3.2: 日志输出包含原始文本、命中的关键词/规则、最终判定意图、判定依据
  - `programmatic` TR-3.3: 业务判断结果与改造前完全一致
  - `programmatic` TR-3.4: 不传 requestId 时编译报错

## [x] Task 4: 金额提取节点埋点
- **Priority**: high
- **Depends On**: Task 1
- **Description**:
  - 对应文件：`AmountUtils.kt`
  - 金额提取（`extractAmounts`）、账单拆分（`splitBills`）方法新增 `requestId: String` 参数（**无默认值**）
  - 节点3 埋点：打印原始文本、提取到的金额列表（金额值+起始位置）、拆分后的片段列表
  - 核心记账链路内的 try-catch 异常分支使用 `AppLogger.e(requestId, node, message, throwable)` 打印
  - **零侵入约束**：不修改金额提取和拆分算法、不改动算法结果
- **Acceptance Criteria Addressed**: V-1, V-2, V-8, V-10, V-11
- **Test Requirements**:
  - `programmatic` TR-4.1: `extractAmounts` 和 `splitBills` 方法签名包含 `requestId: String`（无默认值）
  - `programmatic` TR-4.2: 日志输出包含原始文本、金额列表、拆分片段列表
  - `programmatic` TR-4.3: 金额提取和拆分结果与改造前完全一致
  - `programmatic` TR-4.4: 不传 requestId 时编译报错

## [x] Task 5: 分类匹配节点埋点
- **Priority**: high
- **Depends On**: Task 1
- **Description**:
  - 对应文件：`CategoryMatcher.kt`、`MemoryManager.kt`（分类匹配相关类）
  - 匹配方法新增 `requestId: String` 参数（**无默认值**）
  - 节点4 埋点：打印待匹配文本、命中的触发词、来源(user/seed)、基础分类、时段规则命中情况、最终分类结果
  - **多笔场景**：每笔分类匹配日志必须使用 `AppLogger.d(requestId, node, message, billIndex)` 重载，自动追加 `[第N笔]` 标识
  - 核心记账链路内的 try-catch 异常分支使用 `AppLogger.e(requestId, node, message, throwable, billIndex)` 多笔重载
  - **不修改任何匹配规则和最终分类判定逻辑**（零侵入约束）
- **Acceptance Criteria Addressed**: V-1, V-2, V-3, V-7, V-10, V-11
- **Test Requirements**:
  - `programmatic` TR-5.1: 分类匹配方法签名包含 `requestId: String`（无默认值）
  - `programmatic` TR-5.2: 日志输出包含待匹配文本、命中的触发词、来源、最终分类结果
  - `programmatic` TR-5.3: 多笔场景下日志包含 `[第N笔]` 标识
  - `programmatic` TR-5.4: 分类匹配结果与改造前完全一致
  - `programmatic` TR-5.5: 不传 requestId 时编译报错

## [ ] Task 6: AI 请求/响应节点埋点
- **Priority**: high
- **Depends On**: Task 1
- **Description**:
  - 对应文件：`DeepSeekApi.kt`、`AppRepository.kt` 中 AI 请求方法、`RetrofitClient.kt`
  - 请求方法新增 `requestId: String` 参数（**无默认值**）
  - 节点5 埋点：打印模型名称、Prompt长度、请求开始时间
  - **API Key 脱敏**：调用 `AppLogger.maskApiKey()`（public 方法）处理后打印，格式为 `前4位****后4位`，示例：`API Key: sk-12****abcd`
  - 节点6 埋点：打印响应状态（成功/失败）、返回内容长度、关键片段摘要；错误时打印错误码和错误信息
  - 核心记账链路内的 try-catch 异常分支使用 `AppLogger.e(requestId, node, message, throwable)` 打印
  - **零侵入约束**：不修改网络请求、Prompt 生成、JSON 解析等业务逻辑、不改动算法结果
- **Acceptance Criteria Addressed**: V-1, V-4, V-12, V-13
- **Test Requirements**:
  - `programmatic` TR-6.1: AI 请求/响应日志不包含完整 API Key
  - `programmatic` TR-6.2: API Key 日志格式为 `前4位****后4位`
  - `programmatic` TR-6.3: AI 请求成功/失败均可按 requestId 查到完整日志
  - `programmatic` TR-6.4: AI 解析结果与改造前完全一致
  - `programmatic` TR-6.5: 错误时日志包含错误码和异常堆栈
  - `programmatic` TR-6.6: 不传 requestId 时编译报错

## [x] Task 7: 入库执行节点埋点
- **Priority**: high
- **Depends On**: Task 1
- **Description**:
  - 对应文件：`AppRepository.kt`
  - 单条/批量记账方法新增 `requestId: String` 参数（**无默认值**）
  - 节点7 埋点规则（**单笔 + 汇总双层日志**）：
    - **单笔明细**：批量方法内部对每笔循环调用 `AppLogger.i(requestId, "入库执行", "...", billIndex = N)`，每条带 `[第N笔]` 标识，格式与单笔记账日志完全一致
    - **汇总日志**：循环结束后额外追加 1 条 `AppLogger.i(requestId, "入库执行-汇总", "总条数: X, 成功: Y, 失败: Z")`
  - **失败处理**：单笔失败时使用 `AppLogger.e(requestId, "入库执行", "...", throwable, billIndex = N)` 多笔重载，携带完整堆栈
  - 核心记账链路内的 try-catch 异常分支补充 `AppLogger.e()` 打印
  - **不修改任何数据库写入逻辑、字段映射、类型判断**（零侵入约束）
- **Acceptance Criteria Addressed**: V-1, V-2, V-3, V-5, V-8
- **Test Requirements**:
  - `programmatic` TR-7.1: 入库方法签名包含 `requestId: String`（无默认值）
  - `programmatic` TR-7.2: 批量场景下同时存在单笔明细日志（带 `[第N笔]`）和 1 条汇总日志
  - `programmatic` TR-7.3: 多笔场景下入库日志包含 `[第N笔]` 标识
  - `programmatic` TR-7.4: 记账入库结果与改造前完全一致
  - `programmatic` TR-7.5: 不传 requestId 时编译报错

## [ ] Task 2: MainViewModel 入口生成 requestId 并透传
- **Priority**: high
- **Depends On**: Task 1, Task 3, Task 4, Task 5, Task 6, Task 7
- **Description**:
  - 在 `MainViewModel.sendMessage(input: String)` 方法第一行调用 `AppLogger.generateRequestId()` 生成 requestId
  - 节点1 埋点：使用 `AppLogger.d(requestId, "入口接收", "用户输入：$input")` 打印用户原始输入文本
  - 调用意图分流、金额提取、分类匹配、AI请求、入库执行等方法时，透传 requestId
  - **笔序号传递路径**（多笔循环中）：在 `AppRepository` 的多笔循环处理逻辑中，遍历拆分后的账单列表时，将当前索引（**从 1 开始**）作为 `billIndex` 参数传入分类匹配、入库方法，例如：
    ```kotlin
    bills.forEachIndexed { index, bill ->
        val billIndex = index + 1
        // 分类匹配
        matchCategory(requestId, bill, billIndex)
        // 入库
        insertBill(requestId, bill, billIndex)
    }
    ```
  - 核心记账链路内的 try-catch 异常分支补充 `AppLogger.e()` 打印
  - **零侵入约束**：不修改业务判断逻辑、不改动算法结果
- **Acceptance Criteria Addressed**: V-1, V-2, V-3, V-5, V-14
- **Test Requirements**:
  - `programmatic` TR-2.1: `sendMessage` 第一行生成的 requestId 贯穿后续所有调用
  - `programmatic` TR-2.2: Logcat 中可按 requestId 搜索到入口节点的日志
  - `programmatic` TR-2.3: 多笔拆分场景下每笔分类匹配、入库日志带有 `[第N笔]` 标识（从 1 开始）
  - `programmatic` TR-2.4: 异常分支打印了错误日志和堆栈
  - `programmatic` TR-2.5: 整体编译通过

## [x] Task 8: 全局验证与回归
- **Priority**: medium
- **Depends On**: Task 1, Task 2, Task 3, Task 4, Task 5, Task 6, Task 7
- **Description**:
  - 全局代码扫描：搜索 `Log\.(d|e|i|w|v)`，确认业务代码中无直接调用（排除 `AppLogger.kt` 文件本身）
  - 确认所有日志均携带 requestId 和节点名，格式统一
  - 确认 API Key 等敏感信息在日志中已脱敏
  - 编译通过，运行正常
  - 端到端验证：输入「午饭12」后，Logcat 可通过 requestId 查到完整 7 节点日志
  - 端到端验证：输入「午饭12晚饭15奶茶20」多笔拆分场景，每笔金额、对应描述、各自的分类匹配结果均可在日志中追溯
  - 端到端验证：故意填错 API Key 发送请求，日志中可清晰看到 AI 请求节点的错误信息、错误码和异常堆栈
  - 边界场景验证：空输入、纯空格输入、超长文本输入、快速连续发送多条消息
  - 业务零变更验证：相同输入的意图/分类/入库结果与改造前完全一致
  - 性能验证：连续发送 10 条消息，无卡顿、无内存泄漏、无 ANR
- **Acceptance Criteria Addressed**: V-1 ~ V-21（全部）
- **Test Requirements**:
  - `programmatic` TR-8.1: 全局搜索 `Log\.(d|e|i|w|v)` 业务调用仅存在于 `AppLogger.kt` 内部
  - `programmatic` TR-8.2: 端到端输入「午饭12」按 requestId 能查到 7 节点完整日志
  - `programmatic` TR-8.3: 多笔拆分场景日志中可看到每笔金额、描述、分类
  - `programmatic` TR-8.4: API Key 错误时 AI 请求节点日志包含错误码和异常堆栈
  - `programmatic` TR-8.5: 空输入、超长文本、快速连发均无崩溃、无日志串号
  - `programmatic` TR-8.6: Release 包下日志自动关闭
  - `programmatic` TR-8.7: 业务执行结果与改造前完全一致（数据库数据零差异）
  - `human-judgement` TR-8.8: 原有所有功能（单笔记账、分类匹配、统计查询、记忆管理）正常运行，无功能退化
