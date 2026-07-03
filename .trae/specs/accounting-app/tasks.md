# 记账App - 实施计划（已分解和优先排序）

## [x] Task 1: 搭建Android项目骨架与Gradle配置
- **Priority**: high
- **Depends On**: None
- **Description**:
  - 新建Android项目，Kotlin + Jetpack Compose
  - 配置 Gradle：Room 2.5.2、Compose BOM、ViewModel、Navigation、Retrofit + OkHttp + Gson、CSV导出相关
  - 配置 local.properties 读取 + BuildConfig 注入 API Key 作为默认值
  - 配置 DataStore Preferences 用于运行时存储用户修改的 API Key
  - .gitignore 包含 local.properties
  - 配置 allowBackup="true"
- **Acceptance Criteria Addressed**: AC-9
- **Test Requirements**:
  - `programmatic` TR-1.1: 项目可编译通过，`./gradlew assembleDebug` 无报错
  - `programmatic` TR-1.2: BuildConfig.API_KEY 可正常读取 local.properties 中的值
  - `human-judgement` TR-1.3: 依赖版本确认与项目内存档一致，Room 版本确认为 2.5.2
- **Notes**: 这是所有后续任务的基础，必须先完成且验证通过

## [x] Task 2: Room 数据库层 — Entity、Dao、Database
- **Priority**: high
- **Depends On**: Task 1
- **Description**:
  - 创建 `ExpenseEntity`（字段：id/amount/category/subcategory/merchant/time/note/confidence/rawInput/createdAt），amount 为 Long（分）
  - 创建 `IncomeEntity`（字段同 ExpenseEntity）
  - 创建 `CategoryMemoryEntity`（id/triggerWord/type/category/subcategory/hitCount/createdAt/updatedAt），triggerWord+type 联合唯一索引
  - 创建 `ExpenseDao`：insert/deleteById/getById/getAll/getByTimeRange
  - 创建 `IncomeDao`：同 ExpenseDao
  - 创建 `CategoryMemoryDao`：upsert（REPLACE）/getAllByType/getTop20ByType/deleteById/deleteAll/insertAll（IGNORE）
  - 创建 `AppDatabase`，添加 time 字段索引，配置 Room.Callback.onCreate 写入种子记忆数据（INSERT OR IGNORE）
  - 禁用 fallbackToDestructiveMigration()
  - 定义种子数据常量（约30-50条高频触发词，覆盖支出/收入）
- **Acceptance Criteria Addressed**: AC-2, AC-9
- **Test Requirements**:
  - `programmatic` TR-2.1: 数据库首次创建后，category_memory 表中种子数据条数 > 0
  - `programmatic` TR-2.2: 插入重复 triggerWord+type 的记录，旧记录被覆盖（hitCount/updatedAt 更新）
  - `programmatic` TR-2.3: 查询 Top20 按 hitCount 倒序返回正确
  - `programmatic` TR-2.4: 金额字段存储单位为分，写入 2550 读取仍为 2550
- **Notes**: 种子数据先放20-30条高频词即可，后续可扩展

## [x] Task 3: DeepSeek API 接入与 Repository 层
- **Priority**: high
- **Depends On**: Task 2
- **Description**:
  - Retrofit 配置 DeepSeek API baseUrl 和 Header（Authorization: Bearer $apiKey）
  - 定义请求/响应数据类（符合 DeepSeek Chat Completion API）
  - 定义 `AppRepository`，封装所有数据操作：expense/income CRUD、memory CRUD、AI 网络请求
  - Repository 内实现两套 Prompt 生成逻辑：
    1. 完整解析 Prompt（未命中记忆）：注入 Top20 记忆规则，要求返回 type + 全量字段
    2. 补全 Prompt（命中记忆）：已知分类，只要求返回 amount/merchant/time/note，不要求返回分类/type
  - JSON 解析 + 字段合法性校验（amount>0、category 非空、type 合法）
  - 记忆命中时，强制用本地记忆的 type/category/subcategory 覆盖 AI 返回值，AI 返回的对应字段直接忽略
- **Acceptance Criteria Addressed**: AC-1, AC-2, AC-4, AC-6
- **Test Requirements**:
  - `programmatic` TR-3.1: 使用 mock API 响应测试，JSON 解析成功返回正确结构
  - `programmatic` TR-3.2: 非法 JSON/负金额/空分类 等脏数据被拦截，走失败流程
  - `programmatic` TR-3.3: Prompt 中正确注入 Top20 记忆规则文本
  - `human-judgement` TR-3.4: 检查 Prompt 格式，确认无多余文字、明确 JSON 输出要求
- **Notes**: API Key 为空时需提示用户去设置页配置

## [x] Task 4: MainViewModel 与核心记账流程
- **Priority**: high
- **Depends On**: Task 3
- **Description**:
  - 创建 `MainViewModel`，持有 `UiState`（StateFlow）
  - 实现发送消息流程：
    1. 用户气泡上屏，清空输入框
    2. 关键词预判收支类型（收入关键词命中则预判 income，否则 expense），预判仅用于记忆库检索范围，不影响最终结果
    3. 前置记忆匹配：查询同 type 所有记忆，最长触发词优先，判断是否包含于 rawInput
    4. 命中 → 锁死 type/category/subcategory，调用补全 Prompt，AI 只返回 amount/merchant/time/note，本地强制覆盖 AI 返回的分类字段
    5. 未命中 → 调用完整 Prompt，AI 返回 type + 全量字段，最终入库 type 以 AI 解析结果为准
    6. 字段校验 → 写入对应表（expense/income）→ 展示记账卡片
    7. 解析失败 → Toast 提示 + 「手动记账」按钮
  - 实现修改分类流程：底部弹窗两级分类选择器（仅改分类，不改收支类型） → 更新记录 → 提取触发词（merchant 不为空时）→ upsert 记忆；type 不可变更，记录始终留在原表
  - 实现删除记录流程
  - 加载状态管理（isLoading）
  - Toast 状态管理（显示后自动清空）
- **Acceptance Criteria Addressed**: AC-1, AC-2, AC-3, AC-4, AC-6
- **Test Requirements**:
  - `programmatic` TR-4.1: 输入"午饭 25 元 麦当劳"，未命中记忆时完整 AI 流程走通，数据正确写入 expense 表
  - `programmatic` TR-4.2: 输入包含已记忆触发词，记忆匹配命中，分类锁死为记忆值，hitCount 记账成功后 +1
  - `programmatic` TR-4.3: 输入"发工资 15000"，预判为 income，正确写入 income 表
  - `programmatic` TR-4.4: 模拟 AI 返回非法 JSON，流程走失败兜底，显示手动记账按钮
  - `programmatic` TR-4.5: 用户修改分类后，category_memory 表中新增/更新对应记录
  - `human-judgement` TR-4.6: 加载状态有明确视觉反馈（解析中提示或输入框禁用）
- **Notes**: 这是核心任务，需要仔细验证所有分支流程

## [x] Task 5: Chat 页面 UI
- **Priority**: high
- **Depends On**: Task 4
- **Description**:
  - 聊天消息列表（LazyColumn）：用户消息（绿色气泡右对齐）、AI 消息（灰色气泡左对齐）
  - 时间戳统一放在气泡右上角（11sp, #999999）
  - 记账卡片组件：白色圆角卡片，分类标签、记忆命中标识（浅绿背景+深绿文字「已匹配记忆」）、金额（支出蓝色#1677FF / 收入绿色#07C160）、商家时间、置信度进度条（绿/橙/红）、操作按钮（修改分类/删除，删除红色#F5222D）
  - 底部输入栏：圆角输入框 + 发送按钮（绿色 #07C160）
  - 欢迎提示：浅灰色提示文字（非气泡）
  - 解析失败提示 + 手动记账按钮
  - 底部导航栏（记账/统计/设置）：线性单色图标，选中态绿色
- **Acceptance Criteria Addressed**: AC-1, AC-6
- **Test Requirements**:
  - `human-judgement` TR-5.1: 消息列表滚动流畅，气泡样式与 mockup 一致
  - `human-judgement` TR-5.2: 记账卡片信息层级清晰，金额突出，置信度颜色正确
  - `human-judgement` TR-5.3: 底部导航图标风格统一为线性单色
  - `programmatic` TR-5.4: 点击发送后输入框清空，列表自动滚动到底部
- **Notes**: 底部导航使用 Compose Navigation，配合 NavHost

## [x] Task 6: Dashboard 统计页面 UI
- **Priority**: medium
- **Depends On**: Task 4
- **Description**:
  - 支出/收入切换 Tab：选中态绿色背景+白色文字，未选中态浅灰背景+深灰文字
  - 总览卡片：渐变背景（支出绿色/收入蓝色），本月总金额大字号突出，今日/日均小字
  - 分类占比：横向进度条 + 百分比数字 + 金额
  - 最近记录列表：图标+分类+商家时间+金额，最近10条，随收支Tab切换对应类型数据
  - 记录条目点击弹出编辑/删除弹窗（与Chat页卡片操作对齐）
- **Acceptance Criteria Addressed**: AC-5
- **Test Requirements**:
  - `human-judgement` TR-6.1: 总览卡片视觉突出，数字字号差拉开
  - `human-judgement` TR-6.2: 分类占比进度条直观，百分比数字清晰
  - `human-judgement` TR-6.3: 收入记录金额显示为绿色+"+"前缀
  - `programmatic` TR-6.4: 切换支出/收入 Tab，总览、分类占比、最近记录列表全部正确刷新
- **Notes**: 统计查询通过 Repository 的 Flow 自动驱动 UI 更新

## [x] Task 7: 设置页面与记忆管理
- **Priority**: medium
- **Depends On**: Task 4
- **Description**:
  - 设置列表 UI：API Key 设置、分类记忆管理、导出 CSV、恢复默认记忆、关于（版本号）
  - API Key 设置：弹窗输入框，保存到 DataStore Preferences，运行时优先读取用户存储的 Key，为空回退到 BuildConfig 默认值
  - 分类记忆管理页：列表展示所有记忆（触发词→分类+type），支持单条删除、一键清空
  - 恢复默认记忆：确认弹窗 → 清空表 → 重写种子数据
  - 版本号显示在 UI 中
- **Acceptance Criteria Addressed**: AC-7, AC-8
- **Test Requirements**:
  - `programmatic` TR-7.1: API Key 修改后，下次 AI 请求使用新 Key
  - `programmatic` TR-7.2: 恢复默认记忆后，category_memory 表恢复到种子数据条数
  - `programmatic` TR-7.3: 单条删除记忆后，该触发词下次不再命中
  - `human-judgement` TR-7.4: 设置列表间距合适，点击区域友好
- **Notes**: API Key 实际存储方案需确认（local.properties 只读，运行时修改用 SharedPreferences/DataStore）

## [x] Task 8: CSV 数据导出
- **Priority**: low
- **Depends On**: Task 2
- **Description**:
  - 查询 expense 和 income 表全部数据
  - 格式化为 CSV（表头 + 数据行）
  - 使用 Android Storage Access Framework 保存到 Downloads 目录
  - 文件名含时间戳：`记账导出_YYYYMMDD_HHMMSS.csv`
- **Acceptance Criteria Addressed**: AC-7
- **Test Requirements**:
  - `programmatic` TR-8.1: CSV 文件成功生成并保存到 Downloads
  - `programmatic` TR-8.2: CSV 内容格式正确，字段完整，中文无乱码（UTF-8 BOM）
  - `human-judgement` TR-8.3: 导出后 Toast 提示保存路径
- **Notes**: 需要处理存储权限（Android 10+ 用 SAF，不需要运行时权限）

## [x] Task 9: 手动记账弹窗与分类选择器
- **Priority**: medium
- **Depends On**: Task 5
- **Description**:
  - 手动记账弹窗：顶部「支出/收入」切换Tab，表单包含金额输入、时间选择器、分类选择（两级联动，随Tab切换加载对应类型分类）、商家输入、备注输入
  - 预填原始输入文本到备注字段
  - 两级分类选择器：底部弹窗，先选一级分类，再选二级分类
  - 分类选择器用于「修改分类」操作时，type 固定不可切换（仅改分类层级）
  - 提交时根据选中的 type 写入对应表
  - 商家不为空时触发记忆学习（upsert），与修改分类逻辑一致
- **Acceptance Criteria Addressed**: AC-6
- **Test Requirements**:
  - `human-judgement` TR-9.1: 分类选择器交互流畅，两级联动正确
  - `programmatic` TR-9.2: 手动记账提交后，数据正确写入对应表
  - `human-judgement` TR-9.3: 表单布局清晰，输入框有合适提示文字
- **Notes**: 分类数据用代码常量维护，不存数据库

## [x] Task 10: 数据库迁移与版本升级策略
- **Priority**: medium
- **Depends On**: Task 2
- **Description**:
  - 定义数据库版本号规则
  - 手写 Migration（如有复杂变更）或 @AutoMigration
  - 测试迁移流程：旧版本数据 → 升级 → 验证数据完整
- **Acceptance Criteria Addressed**: AC-9
- **Test Requirements**:
  - `programmatic` TR-10.1: 数据库升级后所有表数据完整保留
  - `programmatic` TR-10.2: fallbackToDestructiveMigration() 未在代码中任何地方使用
- **Notes**: 当前首次创建无需迁移，但需预留机制
