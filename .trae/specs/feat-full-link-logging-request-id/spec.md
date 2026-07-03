# 全链路日志与请求ID追踪 改造 Spec

## Why
当前记账App缺乏统一的日志追踪机制，遇到「分流不准、分类错误、记账失败」等问题时需要逐行翻代码排查，定位时间长。需要零侵入改造实现「一次用户请求 → 唯一ID贯穿全链路 → 全节点可追溯」，将问题定位时间缩短到10秒以内。

## What Changes
- 新增统一日志工具类 `AppLogger`（基于 Android Logcat 封装，Release 包自动关闭）
- 引入请求ID全链路透传机制：单次用户输入生成唯一ID，贯穿所有后续调用
- 在 7 个核心业务节点（入口接收、意图分流、金额提取、分类匹配、AI请求发起、AI响应返回、入库执行）补充埋点日志
- 多笔拆分场景追加 `[第N笔]` 标识，跟随 requestId 一起透传；错误日志 `e(...)` 同步支持多笔重载
- AppLogger 暴露 **public 方法** `maskApiKey()` 供业务代码显式调用；`truncateText()` 保持 **private** 由日志打印方法内部自动调用
- 日志截断范围限定：2000 字符仅作用于 message 主体，`requestId` / `node` / `[第N笔]` 前缀不计入长度，始终完整保留
- 异常埋点收窄到核心记账链路（意图分流、金额提取、分类匹配、AI请求、数据库操作），纯 UI 兜底/空指针安全取值/防重复点击的 catch 不在改造范围
- 所有核心记账链路 try-catch 异常分支补充错误日志，携带完整堆栈
- 敏感信息（API Key）自动脱敏处理
- **预留扩展位**：AppLogger 内部保留 `private fun saveLogToFile(...)` 空方法占位（不抛异常、不影响主流程），当前版本不实现，仅为后续「用户反馈收集」或「本地日志持久化」留入口

## 不做变更清单（零侵入边界）
- ❌ 不修改任何业务逻辑（分流规则、分类匹配、记账入库逻辑一行不动）
- ❌ 不调整数据库结构、不新增表、不变更字段
- ❌ 不改变任何 UI 交互
- ❌ 不引入第三方依赖
- ❌ 不修改 Prompt 生成、JSON 解析、网络请求逻辑
- ❌ 不修改金额提取、账单拆分算法
- ❌ 不修改匹配规则和最终分类判定逻辑
- ❌ 不实现本地日志文件输出（仅预留扩展位）
- ❌ 不引入协程上下文携带 ID（保持显式传参，可控性优先）

## Impact
- Affected specs: accounting-app（核心记账流程）、v4-memory-grouped-ui（无影响）
- Affected code:
  - 新增：`app/src/main/java/com/accounting/app/util/AppLogger.kt`
  - 修改：`MainViewModel.kt`、`AppRepository.kt`、`DeepSeekApi.kt`、`AmountUtils.kt`
  - 业务规则逻辑文件（IntentParser/CategoryMatcher/MemoryManager 等）仅新增 `requestId` 参数与埋点，**不修改判断逻辑**

---

## 验收清单（需求-验收一一对应）

### 链路完整性
- **V-1**：输入「午饭12」，在 Logcat 中搜索对应 requestId，能按顺序看到全部 7 个节点的日志，无缺失
- **V-2**：多笔拆分场景（输入「午饭12晚饭15奶茶20」），每笔金额、对应描述、各自的分类匹配结果均可按 requestId + 笔序号追溯
- **V-3**：同一 requestId 下，多笔操作的日志顺序与业务执行顺序一致

### 异常可定位
- **V-4**：故意填错 API Key 发送请求，日志中能清晰看到 AI 请求节点的错误信息、错误码和异常堆栈
- **V-5**：所有核心记账链路 try-catch 分支均输出错误日志，携带 requestId 和完整堆栈

### 业务零影响
- **V-6**：相同输入文本，改造前后的意图判定结果完全一致
- **V-7**：相同输入文本，改造前后的分类结果完全一致
- **V-8**：相同输入文本，改造前后的入库数据完全一致（金额、分类、时间、备注零差异）
- **V-9**：原有所有功能（单笔记账、分类匹配、统计查询、记忆管理）完全正常

### 规范与脱敏
- **V-10**：业务代码中无直接调用 `Log.d/e/i` 的零散日志（`AppLogger.kt` 内部使用 `Log` 是唯一允许点）
- **V-11**：所有日志均携带 requestId 和节点名，格式统一为 `[requestId] [node] message`
- **V-12**：API Key 等敏感信息在日志中已脱敏，格式为 `前4位****后4位`，无明文泄露
- **V-13**：超长文本日志自动截断到 2000 字符，追加 `...(已截断)`
- **V-14**：所有 `requestId` 参数无默认值，漏传会直接编译报错

### 边界场景
- **V-15**：空输入、纯空格输入：日志正常输出，不崩溃
- **V-16**：超长文本输入：日志自动截断，不出现系统截断导致的格式混乱
- **V-17**：快速连续发送多条消息：每条请求对应独立 requestId，日志不串号、不混淆

### 性能与编译
- **V-18**：连续发送 10 条消息，无卡顿、无内存泄漏、无 ANR
- **V-19**：单条请求的日志打印总耗时 < 5ms
- **V-20**：`./gradlew assembleDebug` 编译通过，无报错、无未使用参数警告
- **V-21**：Release 包下日志自动关闭，无输出

---

## ADDED Requirements

### Requirement: 统一日志工具类 AppLogger
系统 SHALL 提供全局单例日志工具类 `AppLogger`，统一收口所有日志打印，禁止业务代码直接调用 `Log.d/e/i`。

#### Scenario: 日志开关
- **WHEN** 当前是 Debug 包（`BuildConfig.DEBUG == true`）
- **THEN** 日志正常输出
- **WHEN** 当前是 Release 包（`BuildConfig.DEBUG == false`）
- **THEN** 所有日志自动关闭，无输出

#### Scenario: 请求ID生成
- **WHEN** 调用 `AppLogger.generateRequestId()`
- **THEN** 返回格式为 `req_时间戳_4位随机数` 的唯一标识（如 `req_1720000000000_1234`）
- **AND** 同一毫秒内多次调用生成不同ID

#### Scenario: 调试日志
- **WHEN** 调用 `AppLogger.d(requestId, node, message)`
- **THEN** 输出格式：`[requestId] [node] message`，对应 Android `Log.d` 级别

#### Scenario: 错误日志
- **WHEN** 调用 `AppLogger.e(requestId, node, message, throwable)`
- **THEN** 输出错误日志，包含完整异常堆栈

#### Scenario: 信息日志
- **WHEN** 调用 `AppLogger.i(requestId, node, message)`
- **THEN** 输出关键状态变更日志

#### Scenario: 多笔标识支持
- **WHEN** 调用 `AppLogger.d(requestId, node, message, billIndex = N)`（或带笔序号的重载）
- **THEN** 日志输出包含 `[第N笔]` 标识，格式为 `[requestId] [node] [第N笔] message`
- **AND** 笔序号跟随 requestId 一起透传到下游方法
- **AND** 错误日志 `e(requestId, node, message, throwable, billIndex)` 多笔重载同步支持，确保多笔场景下单笔失败可精准定位

#### Scenario: 超长文本自动截断（仅限 message 主体）
- **WHEN** 单条日志的 **message 主体** 超过 2000 字符
- **THEN** AppLogger 仅对 message 主体截断到 2000 字符并追加 `...(已截断)`
- **AND** `requestId`、`node`、`[第N笔]` 等前缀标识**不计入长度限制**，始终完整保留在截断后的文本中
- **AND** 截断后日志仍可按 requestId 精确检索，不会因截断丢失关键 ID

#### Scenario: 脱敏工具方法封装（公开/私有分工）
- **WHEN** 业务代码需要打印 API Key 或长文本
- **THEN** **脱敏是特定字段行为**：
  - 业务代码必须显式调用 **public 方法** `AppLogger.maskApiKey(key)` 处理 API Key 后再传入日志
  - `maskApiKey("sk-12345678abcdef")` 返回 `sk-12****cdef`
- **AND** **截断是通用行为**：
  - `truncateText()` 保持 **private**，由 `d/e/i` 打印方法内部自动调用，对业务代码完全透明
  - 业务代码无需关心截断逻辑，禁止自行实现截断或拼接脱敏字符串
- **AND** 设计理由：脱敏需业务侧明确「此字段需脱敏」，所以公开；截断是所有日志的通用行为，自动化处理即可

### Requirement: 请求ID全链路透传
系统 SHALL 在 `MainViewModel.sendMessage` 方法第一行生成唯一 requestId，并贯穿本次请求的所有后续方法调用。

#### Scenario: ID生成入口
- **WHEN** 用户在 Chat 页点击发送
- **THEN** `sendMessage(input)` 方法第一行调用 `AppLogger.generateRequestId()` 生成 ID
- **AND** 该 ID 作为整条请求的唯一标识，不可中途更换

#### Scenario: 方法签名改造（强制透传）
- **WHEN** 任何被本次请求调用的方法需要执行
- **THEN** 方法签名新增 `requestId: String` 参数
- **AND** 参数不设置默认值（漏传会编译报错，天然保证透传完整）
- **AND** 禁止中途丢弃 ID、禁止重新生成 ID

#### Scenario: 多笔拆分场景透传
- **WHEN** 单次输入被拆分为多笔账单
- **THEN** 每笔的分类匹配、入库方法调用时必须携带 `[第N笔]` 标识
- **AND** 笔序号作为参数或显式字段跟随 requestId 一起透传

#### Scenario: 协程透传
- **WHEN** 协程切换线程或异步回调
- **THEN** requestId 必须作为参数显式透传

### Requirement: 7个核心埋点节点
系统 SHALL 在以下 7 个节点按顺序输出日志，每个节点必须打印指定字段：

| 序号 | 节点名称 | 所在位置 | 级别 | 必须打印内容 |
|------|----------|----------|------|----------------|
| 1 | 入口接收 | MainViewModel.sendMessage | d | 用户原始输入文本 |
| 2 | 意图分流 | IntentParser.parse | d | 原始文本、命中的关键词/规则、最终判定意图、判定依据 |
| 3 | 金额提取 | AmountUtils.extractAmounts / splitBills | d | 原始文本、提取到的金额列表（金额值+起始位置）、拆分后的片段列表 |
| 4 | 分类匹配 | CategoryMatcher.match | d | 待匹配文本、命中的触发词、来源(user/seed)、基础分类、时段规则命中情况、最终分类结果 |
| 5 | AI请求发起 | AiRepository.request / DeepSeekApi | i | 模型名称、Prompt长度、请求开始时间；**禁止打印完整API Key** |
| 6 | AI响应返回 | AI接口回调 | d | 响应状态（成功/失败）、返回内容长度、关键片段摘要；错误时打印错误码和错误信息 |
| 7 | 入库执行 | AppRepository.insertBill / 批量执行方法 | i | 执行结果（成功/失败）、入库条数、账单ID；失败时打印异常堆栈 |

#### Scenario: 节点1 入口接收
- **WHEN** 用户在 Chat 页点击发送
- **THEN** 在 MainViewModel.sendMessage 入口处打印用户原始输入文本
- **EXAMPLE**：`[req_1720000000000_1234] [入口接收] 用户输入：午饭12`

#### Scenario: 节点2 意图分流
- **WHEN** 意图解析方法执行
- **THEN** 打印原始文本、命中的关键词/规则、最终判定意图、判定依据
- **EXAMPLE**：`[req_1720000000000_1234] [意图分流] 原始文本：午饭12，命中关键词：午饭，判定意图：记账-支出，判定依据：餐饮关键词`

#### Scenario: 节点3 金额提取
- **WHEN** AmountUtils.extractAmounts 或 splitBills 执行
- **THEN** 打印原始文本、提取到的金额列表（含金额值和起始位置）、拆分后的片段列表
- **EXAMPLE**：`[req_1720000000000_1234] [金额提取] 原始文本：午饭12晚饭15，提取金额：[{value=12, start=2}, {value=15, start=6}]，拆分片段：[午饭12, 晚饭15]`

#### Scenario: 节点4 分类匹配
- **WHEN** CategoryMatcher.match 执行
- **THEN** 打印待匹配文本、命中的触发词、来源(user/seed)、基础分类、时段规则命中情况、最终分类结果
- **EXAMPLE**（单笔）：`[req_1720000000000_1234] [分类匹配] 待匹配：午饭，触发词：午饭，来源：seed，基础分类：餐饮-午餐，时段规则：午餐命中，最终分类：餐饮-午餐`
- **EXAMPLE**（多笔）：`[req_1720000000000_1234] [分类匹配] [第2笔] 待匹配：晚饭，触发词：晚饭，来源：seed，基础分类：餐饮-晚餐，时段规则：晚餐命中，最终分类：餐饮-晚餐`

#### Scenario: 节点5 AI请求发起
- **WHEN** AI 接口调用方法执行
- **THEN** 打印模型名称、Prompt长度、请求开始时间，**不打印完整 API Key**
- **EXAMPLE**：`[req_1720000000000_1234] [AI请求发起] 模型：deepseek-chat，Prompt长度：450字符，开始时间：2026-07-03 10:30:15，API Key：sk-12****abcd`

#### Scenario: 节点6 AI响应返回
- **WHEN** AI 接口回调执行
- **THEN** 打印响应状态（成功/失败）、返回内容长度、关键片段摘要；错误时打印错误码和错误信息
- **EXAMPLE**（成功）：`[req_1720000000000_1234] [AI响应返回] 状态：成功，返回长度：128字符，摘要：{"amount":12,...}`
- **EXAMPLE**（失败）：`[req_1720000000000_1234] [AI响应返回] 状态：失败，错误码：401，错误信息：Invalid API Key`

#### Scenario: 节点7 入库执行
- **WHEN** AppRepository.insertBill 或批量执行方法执行
- **THEN** 打印执行结果（成功/失败）、入库条数、账单ID；失败时打印异常堆栈
- **EXAMPLE**（单笔）：`[req_1720000000000_1234] [入库执行] 结果：成功，条数：1，账单ID：1001`
- **EXAMPLE**（多笔）：`[req_1720000000000_1234] [入库执行] [第1笔] 结果：成功，账单ID：1001`
- **EXAMPLE**（失败）：`[req_1720000000000_1234] [入库执行] [第2笔] 结果：失败，异常：SQLiteConstraintException ...`

### Requirement: 异常埋点（限定范围）
系统 SHALL 在**核心记账链路**的所有 try-catch 块的 catch 分支调用 `AppLogger.e()` 打印异常，携带 requestId 和完整堆栈。

**核心记账链路范围（仅限以下 5 类业务方法）**：
1. 意图分流相关方法
2. 金额提取相关方法
3. 分类匹配相关方法
4. AI 请求相关方法
5. 数据库操作相关方法

**不在改造范围内的 catch 分支（明确排除）**：
- 纯 UI 层防崩溃 catch（如 `try { ... } catch (e: Exception) { /* 静默 */ }` 兜底）
- 防止空指针的安全取值 catch（如 `try { list[0] } catch (e: IndexOutOfBoundsException) { null }`）
- UI 层防重复点击的 catch
- 三方库内部异常处理
- 已有的、仅做兜底不涉及业务的 try-catch

> **判断标准**：只有 catch 分支会**影响业务执行结果**（如分流判断、金额提取、分类匹配、AI 解析、数据库写入失败），才需要打日志；纯防御性、纯 UI 兜底不打。

#### Scenario: 数据库操作异常
- **WHEN** 数据库操作（CRUD）发生异常
- **THEN** catch 分支调用 `AppLogger.e()` 打印异常，不吞掉异常

#### Scenario: 网络请求异常
- **WHEN** 网络请求（AI API）发生异常
- **THEN** catch 分支调用 `AppLogger.e()` 打印异常，不吞掉异常

#### Scenario: 业务方法异常
- **WHEN** 意图分流、金额提取、分类匹配等业务方法发生异常
- **THEN** catch 分支调用 `AppLogger.e()` 打印异常，不吞掉异常

### Requirement: 敏感信息脱敏
系统 SHALL 在日志打印前对敏感信息进行脱敏处理，所有脱敏操作通过 AppLogger 内部工具方法完成。

#### Scenario: API Key 脱敏
- **WHEN** 日志中需要展示 API Key
- **THEN** 调用 `maskApiKey(key)` 处理，日志格式为 `前4位****后4位`（如 `sk-12****abcd`）
- **AND** 禁止完整打印 API Key 明文

#### Scenario: 用户账单数据脱敏
- **WHEN** 日志中需要展示用户自定义的隐私备注
- **THEN** 调用 `truncateText(text, 20)` 截断到 20 字
- **AND** 金额和分类允许打印

#### Scenario: AI 返回内容脱敏
- **WHEN** 日志中需要展示 AI 返回内容
- **THEN** 仅打印关键片段摘要，禁止全量打印长文本
- **AND** 触发 AppLogger 的 2000 字符自动截断逻辑

### Requirement: 强制规范
系统 SHALL 强制所有业务代码通过 `AppLogger` 打印日志，禁止直接调用 `Log.d/e/i`（`AppLogger.kt` 内部使用 `Log` 是唯一允许点）。

#### Scenario: 代码规范检查
- **WHEN** 检查业务代码
- **THEN** 不存在直接调用 `android.util.Log` 的零散日志（搜索 `Log\.(d|e|i|w|v)` 时应排除 `AppLogger.kt` 文件本身）
- **AND** 所有日志均携带 requestId 和节点名
- **AND** 日志格式统一为 `[requestId] [node] message`（多笔场景含 `[第N笔]`）

---

## 需求 - 任务 - 验收 追踪矩阵

| 需求编号 | 需求名称 | 对应 Task | 对应验收项 | 覆盖状态 |
|---------|---------|-----------|-----------|---------|
| R-1 | 统一日志工具类 AppLogger（含 generateRequestId / d / e / i / 多笔重载） | Task 1 | V-10, V-11, V-12, V-13, V-14, V-21 | ✅ |
| R-2 | maskApiKey public 方法（业务侧显式调用脱敏） | Task 1 | V-12, V-13 | ✅ |
| R-3 | truncateText private 方法（日志方法内部自动调用） | Task 1 | V-13 | ✅ |
| R-4 | 超长 message 自动截断（2000 字符，仅限主体，前缀不计入） | Task 1 | V-13, V-16 | ✅ |
| R-5 | saveLogToFile 预留空方法占位 | Task 1 | —（无验收，预留扩展） | ✅ |
| R-6 | 请求ID生成入口（MainViewModel.sendMessage 第一行） | Task 2 | V-1, V-2, V-3, V-14 | ✅ |
| R-7 | requestId 全链路透传（无默认值，漏传编译报错） | Task 2, Task 3, Task 4, Task 5, Task 6, Task 7 | V-1, V-14, V-17 | ✅ |
| R-8 | 笔序号 billIndex 透传（多笔循环，索引从 1 开始） | Task 2, Task 5, Task 7 | V-2, V-3 | ✅ |
| R-9 | 节点1 入口接收埋点 | Task 2 | V-1 | ✅ |
| R-10 | 节点2 意图分流埋点 | Task 3 | V-1, V-6 | ✅ |
| R-11 | 节点3 金额提取埋点 | Task 4 | V-1, V-2, V-8 | ✅ |
| R-12 | 节点4 分类匹配埋点（含多笔标识） | Task 5 | V-1, V-2, V-3, V-7 | ✅ |
| R-13 | 节点5 AI请求发起埋点（含 API Key 脱敏） | Task 6 | V-1, V-4, V-12 | ✅ |
| R-14 | 节点6 AI响应返回埋点 | Task 6 | V-1, V-4 | ✅ |
| R-15 | 节点7 入库执行埋点（含多笔单笔明细 + 汇总日志） | Task 7 | V-1, V-2, V-3, V-5, V-8 | ✅ |
| R-16 | 核心记账链路异常埋点（限定 5 类业务方法） | Task 3, Task 4, Task 5, Task 6, Task 7 | V-5 | ✅ |
| R-17 | 错误日志多笔重载 `e(... billIndex)` | Task 1, Task 5, Task 7 | V-2, V-5 | ✅ |
| R-18 | 业务零变更（意图/分类/入库结果与改造前完全一致） | Task 3, Task 4, Task 5, Task 6, Task 7, Task 8 | V-6, V-7, V-8, V-9 | ✅ |
| R-19 | 规范强制（业务代码不直接调用 Log） | Task 8 | V-10, V-20 | ✅ |
| R-20 | 边界场景（空输入、超长、快速连发） | Task 8 | V-15, V-16, V-17 | ✅ |
| R-21 | 性能验证（10 条无卡顿、单条 < 5ms） | Task 8 | V-18, V-19 | ✅ |

> 矩阵覆盖度：21 条需求 → 8 个 Task → 21 条验收项（V-1 ~ V-21），全部一一对应，无遗漏、无错位。
