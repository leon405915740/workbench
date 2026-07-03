# 聊天式记账App - 产品需求文档

## Overview
- **Summary**: 一款个人自用的聊天式记账Android应用，用户通过自然语言输入消费或收入内容，AI自动解析并记账，同时支持分类记忆学习系统——用户手动修改一次分类后永久生效，下次同商家自动命中记忆。支持支出和收入双线记账，本地数据存储，无需联网即可使用记忆系统。
- **Purpose**: 解决传统记账App输入繁琐的问题，用自然语言聊天方式快速记账，通过AI解析+记忆学习越用越准，降低记账门槛。
- **Target Users**: 个人自用，日常消费记账为主，收入记录为辅。

## Goals
- 用户输入自然语言即可自动记账，AI解析准确率高
- 分类记忆系统让用户修改一次后，后续同商家/关键词自动命中，越用越准
- 收支双线管理，Dashboard清晰展示今日/本月/分类统计
- 数据100%本地存储，安全可控，支持CSV导出
- 极简架构，个人维护成本低

## Non-Goals (Out of Scope)
- 不实现语音输入（前期导致崩溃，已砍掉）
- 不实现消费人格分析/每周复盘
- 不实现多设备云同步
- 不实现自然语言查询分析
- 不实现多轮对话上下文记忆
- 不做隐私政策弹窗、防反编译、证书固定等安全加固
- 不做内存优化、包体积优化
- 不做试用模式、首次引导页

## Background & Context
- 前期项目经历：Room 2.6.1 导致启动崩溃，降级到 2.5.2
- 语音输入按钮导致崩溃，改为纯文本输入+AI解析
- 过度设计的EventRouter/ActionBus架构已废弃，改为极简ViewModel直接调用
- 分类自动学习系统前期不如手动选分类靠谱，改为用户修改触发学习
- 本次从零搭建项目，目录当前为空

## Functional Requirements
- **FR-1**: 聊天式记账输入 — 用户在聊天界面输入自然语言，AI解析后展示结构化记账卡片
- **FR-2**: AI智能解析 — 调用DeepSeek API解析金额、分类、时间、商家等信息，严格输出JSON
- **FR-3**: 分类记忆系统 — 用户手动修改分类后，触发词+分类写入记忆库，后续同词自动命中
- **FR-4**: 收支双线记账 — 支持支出和收入分别记账，分类体系独立
- **FR-5**: Dashboard数据统计 — 今日/本月收支总览、分类占比横向进度条、最近记录列表
- **FR-6**: 手动记账兜底 — AI解析失败时提供手动记账入口，预填原始输入
- **FR-7**: 记忆管理 — 设置页支持查看/删除/清空记忆条目，恢复默认种子记忆
- **FR-8**: CSV数据导出 — 支持导出expense/income表数据为CSV文件
- **FR-9**: API Key设置 — 支持在设置页修改DeepSeek API Key

## Non-Functional Requirements
- **NFR-1**: 性能 — 个人记账几千条数据，无需额外优化，Room+Flow天然支持
- **NFR-2**: 数据安全 — 禁用fallbackToDestructiveMigration()，allowBackup="true"，升级不丢数据
- **NFR-3**: 异常处理 — JSON解析try-catch包裹，网络错误有兜底，App不崩溃
- **NFR-4**: 协程安全 — 全部使用viewModelScope，suspend函数严格在协程中调用
- **NFR-5**: 金额精度 — 数据库存储单位为分（Long），避免浮点误差

## Constraints
- **Technical**: Kotlin + Jetpack Compose + Room 2.5.2（禁止2.6.1）+ ViewModel + StateFlow + Retrofit + DeepSeek API
- **Business**: 个人自用，API Key 默认从local.properties读取（BuildConfig注入）作为首次安装默认值；用户在设置页修改的Key存入DataStore，运行时优先读取用户存储的Key，为空则回退到默认值
- **Dependencies**: DeepSeek API需要有效Key，无Key时AI解析不可用但记忆系统仍工作
- **Architecture**: 只有UI层→ViewModel→Repository→Room四层，禁止任何中间层

## Assumptions
- 用户有DeepSeek API Key，或愿意在设置页配置
- 日常记账90%以上是支出，收入场景较少
- 用户愿意手动修正AI错误分类以训练记忆系统
- 种子记忆数据约30-100条高频触发词即可覆盖大部分场景

## Acceptance Criteria

### AC-1: 自然语言记账
- **Given**: 用户在Chat页输入框输入文本
- **When**: 点击发送
- **Then**: AI解析成功后在聊天列表中展示记账卡片，包含分类、金额、商家、时间、置信度
- **Verification**: `human-judgment`
- **Notes**: 卡片样式：白色圆角卡片，金额蓝色高亮（支出）/绿色高亮（收入），置信度进度条（≥0.9绿/0.7-0.89橙/<0.7红）

### AC-2: 记忆命中自动分类
- **Given**: 用户输入包含已记忆触发词（如"麦当劳"）
- **When**: 发送消息
- **Then**: 分类直接使用记忆值，置信度强制1.0，卡片标注「已匹配记忆」，AI仅补全金额/时间/商家/备注，AI返回的type/category/subcategory字段直接忽略，绝不覆盖记忆值
- **Verification**: `programmatic`
- **Notes**: 记忆匹配规则：最长触发词优先，只匹配同type的记忆

### AC-3: 用户修改触发学习
- **Given**: 用户看到AI生成的记账卡片
- **When**: 点击「修改分类」，选择新分类（可同时修改收支类型）并确认
- **Then**: 更新该条记录分类/类型，同时写入/更新category_memory表（triggerWord+type联合唯一，覆盖更新），Toast提示「✅ 已学习（支出/收入）：[触发词] → [分类]」；用户修改type时，按新type+触发词执行upsert，原type下的旧记忆保留不动
- **Verification**: `programmatic`
- **Notes**: 触发词优先取merchant字段，merchant为空则不写入记忆

### AC-4: 收支类型判断
- **Given**: 用户输入包含收入场景（如"发工资 15000"）
- **When**: 发送消息
- **Then**: AI正确识别为income类型，使用收入分类体系，金额展示为+¥xxx（绿色）
- **Verification**: `programmatic`
- **Notes**: 收入关键词（收到/入账/工资/奖金/报销/退款/赚到/兼职收入/理财收益/补贴/到账/收了）仅用于前置加速记忆匹配，最终收支类型以AI语义判断为准

### AC-5: Dashboard统计
- **Given**: 用户切换到统计Tab
- **When**: 页面展示
- **Then**: 显示今日/本月支出总览、分类占比横向进度条（Top5）、最近10条记录列表
- **Verification**: `human-judgment`
- **Notes**: 支持支出/收入切换Tab，分类占比显示百分比数字

### AC-6: 手动记账兜底
- **Given**: AI解析失败或网络异常
- **When**: 用户发送消息后
- **Then**: Toast提示失败原因，聊天列表显示「解析失败」提示+「手动记账」按钮，点击弹出表单预填原始输入
- **Verification**: `human-judgment`

### AC-7: 数据导出
- **Given**: 用户在设置页点击「导出数据CSV」
- **When**: 点击后
- **Then**: 生成包含所有expense和income记录的CSV文件，保存到Downloads目录
- **Verification**: `programmatic`

### AC-8: 记忆管理
- **Given**: 用户在设置页进入「分类记忆管理」
- **When**: 页面展示
- **Then**: 列出所有记忆条目（触发词→分类+type），支持单条删除和一键清空
- **Verification**: `human-judgment`
- **Notes**: 同时支持「恢复默认记忆」按钮，清空全表后重写种子数据

### AC-9: 数据库升级安全
- **Given**: App版本升级涉及数据库结构变更
- **When**: 用户打开新版本App
- **Then**: 数据完整保留，不丢失任何记账记录或记忆条目
- **Verification**: `programmatic`
- **Notes**: 禁用fallbackToDestructiveMigration()，简单变更用@AutoMigration，复杂用手写Migration

## Open Questions
- [ ] 是否需要暗黑模式？（当前不做，后续可选）
- [ ] 收入分类是否需要更细的二级分类？（当前用基础分类即可）
- [ ] 最近记录列表是否支持无限滚动加载？（当前只展示Top10，点击查看全部可跳转完整列表）
