# AI个人成长OS - 实现计划（AI快速迭代模式）

## 开发策略

采用 **AI快速全量搭骨架 → 核心闭环优先跑通 → 持续增强** 的开发模式。

**架构升级为：**

```
Core (基础框架)
  ↓
AI Engine (AI核心引擎) ← 提升为核心地位
  ↓
Feature Module (功能模块)
  ↓
Prompt Layer (提示词层)
  ↓
Database (数据存储)
```

不采用Dynamic Feature真动态插件，使用Feature Module已足够支撑快速迭代。

**最终开发目录：**
```
AI-Growth-OS
├── app
├── core
│   ├── database
│   ├── network
│   └── ai-engine
│       ├── AIClient
│       ├── PromptManager
│       ├── MemoryManager
│       └── AgentManager
├── feature
│   ├── learning
│   │   ├── Goal
│   │   ├── LearningPath
│   │   └── DailyTask
│   ├── growth
│   └── creator
└── data
    ├── Room
    └── API
```

---

## Sprint 1: AI导师Demo（第1周）

目标：7天做出一个会教你学AI的AI导师Demo

### [ ] Task 1: 项目初始化与基础架构搭建
- **Priority**: high
- **Depends On**: None
- **Description**:
  - 创建Android项目，配置Kotlin、Jetpack Compose、Room、Retrofit、Hilt依赖
  - 设置模块结构（core、core-ai、feature/learning、feature/growth、feature/creator）
  - 创建基础UI组件库（Theme、Typography、Colors、通用组件）
  - 配置Room数据库基础框架
- **Acceptance Criteria Addressed**: N/A（基础设施）
- **Test Requirements**:
  - `programmatic` TR-1.1: 项目编译通过，无依赖冲突
  - `programmatic` TR-1.2: 应用可正常启动，显示启动页
  - `human-judgement` TR-1.3: 项目结构清晰，模块划分合理

### [ ] Task 2: 核心数据模型设计与实现
- **Priority**: high
- **Depends On**: Task 1
- **Description**:
  - 实现Entity类：Goal、LearningPath、LearningLevel、DailyTask、KnowledgeCard、AIMemory、GrowthRecord、Content、AIConversation
  - 创建DAO接口和数据库Migration策略
  - 实现Repository层，提供数据访问接口
- **Acceptance Criteria Addressed**: AC-1, AC-7
- **Test Requirements**:
  - `programmatic` TR-2.1: 所有Entity类编译通过，Room数据库可正常创建
  - `programmatic` TR-2.2: Goal实体CRUD操作测试通过
  - `programmatic` TR-2.3: LearningPath与Goal关联查询测试通过
  - `programmatic` TR-2.4: AIConversation实体CRUD操作测试通过

### [ ] Task 3: AI Engine核心实现（AIClient + ResponseParser）
- **Priority**: high
- **Depends On**: Task 1
- **Description**:
  - 实现AIClient：统一管理AI API调用（Claude/OpenAI），处理请求/响应、错误处理、超时设置
  - 实现ResponseParser：解析AI响应，支持JSON格式解析，错误处理和数据验证
  - 创建Retrofit客户端配置
- **Acceptance Criteria Addressed**: AC-2, AC-3, AC-4, AC-5, AC-6, AC-8, AC-9
- **Test Requirements**:
  - `programmatic` TR-3.1: AIClient编译通过，API调用方法签名正确
  - `programmatic` TR-3.2: AI API调用超时测试通过（15秒超时）
  - `programmatic` TR-3.3: ResponseParser可正确解析JSON格式响应

### [ ] Task 4: Prompt Layer设计与实现（PromptManager）
- **Priority**: high
- **Depends On**: Task 3
- **Description**:
  - 创建Prompt目录结构：prompt/learning, prompt/memory, prompt/creator, prompt/system
  - 设计学习类Prompt：roadmap_prompt、daily_task_prompt、evaluation_prompt、knowledge_card_prompt
  - 设计系统类Prompt：personality_prompt（AI导师人格设定）
  - 实现PromptManager：加载和管理所有Prompt文件，支持版本管理和参数填充
- **Acceptance Criteria Addressed**: AC-2, AC-3, AC-4, AC-5, AC-6, AC-8, AC-9
- **Test Requirements**:
  - `programmatic` TR-4.1: Prompt文件可正确加载和解析
  - `human-judgement` TR-4.2: Prompt设计合理，能够生成高质量的AI响应

### [ ] Task 5: AgentManager与ContextBuilder实现
- **Priority**: high
- **Depends On**: Task 3, Task 4
- **Description**:
  - 实现AgentManager：注册和管理各种AI Agent（LearningAgent等），统一的Agent调用接口
  - 实现ContextBuilder：构建AI请求的上下文，整合用户记忆、学习进度、历史对话
  - 注册LearningAgent，实现4个核心AI能力：generateLearningPath、generateDailyTask、evaluateAnswer、generateKnowledgeCard
- **Acceptance Criteria Addressed**: AC-2, AC-3, AC-4, AC-5
- **Test Requirements**:
  - `programmatic` TR-5.1: AgentManager可正确注册和调用Agent
  - `programmatic` TR-5.2: LearningAgent的4个核心方法可正常调用

### [ ] Task 6: 目标管理模块
- **Priority**: high
- **Depends On**: Task 2, Task 5
- **Description**:
  - 创建目标列表页面和目标详情页面
  - 实现目标创建/编辑/删除功能
  - 目标与学习路径关联展示
  - AI生成学习路线入口
- **Acceptance Criteria Addressed**: AC-1
- **Test Requirements**:
  - `programmatic` TR-6.1: 目标创建成功后可在列表中显示
  - `programmatic` TR-6.2: 目标状态变更（active→completed）测试通过

### [ ] Task 7: AI学习路线生成功能
- **Priority**: high
- **Depends On**: Task 2, Task 5, Task 6
- **Description**:
  - 实现AI生成学习路线UI（输入主题、等级选择）
  - 调用LearningAgent.generateLearningPath()生成五级学习阶梯
  - 保存学习路径和学习等级到数据库
  - 展示学习路线详情（各等级目标、知识点、错误、达标标准）
- **Acceptance Criteria Addressed**: AC-2
- **Test Requirements**:
  - `programmatic` TR-7.1: AI生成的学习路线数据可正确解析并保存到数据库
  - `programmatic` TR-7.2: LearningLevel表包含5个等级记录

### [ ] Task 8: 每日学习任务模块
- **Priority**: high
- **Depends On**: Task 2, Task 5, Task 7
- **Description**:
  - 调用LearningAgent.generateDailyTask()生成每日计划
  - 创建任务列表页面，展示今日任务
  - 实现任务完成状态切换
  - 记录任务用时和用户响应
  - 保存对话记录到AIConversation表
- **Acceptance Criteria Addressed**: AC-3
- **Test Requirements**:
  - `programmatic` TR-8.1: 每日计划生成后，DailyTask表包含多条任务记录
  - `programmatic` TR-8.2: 任务完成状态变更测试通过

**Sprint 1完成标志**: AI导师Demo可用
```
输入：我要学AI开发
  ↓
AI生成5级路线（Level1 Python → Level2 API → Level3 RAG → Level4 Agent → Level5 商业应用）
  ↓
每天任务自动生成
  ↓
完成任务获得AI反馈
  ↓
成长记录保存
```

---

## Sprint 2: 十倍学习系统（第2周）

目标：加入差异化的Claude十倍学习系统

### [ ] Task 9: AI动态考核系统
- **Priority**: high
- **Depends On**: Task 2, Task 5, Task 8
- **Description**:
  - 实现考核页面，单次提问模式
  - 调用LearningAgent.evaluateAnswer()获取评分和反馈
  - 保存考核结果到数据库（userResponse、aiFeedback、score）
  - 保存对话记录到AIConversation表
  - 循环提问逻辑实现
- **Acceptance Criteria Addressed**: AC-4
- **Test Requirements**:
  - `programmatic` TR-9.1: 考核记录正确保存到DailyTask表
  - `human-judgement` TR-9.2: 评分展示清晰，错误分析有帮助

### [ ] Task 10: 知识压缩系统（知识卡片）
- **Priority**: high
- **Depends On**: Task 2, Task 5, Task 8
- **Description**:
  - 调用LearningAgent.generateKnowledgeCard()生成知识卡片
  - 创建知识卡片列表和详情页面
  - 记录知识掌握度（masteryScore）
- **Acceptance Criteria Addressed**: AC-5
- **Test Requirements**:
  - `programmatic` TR-10.1: 知识卡片正确保存到KnowledgeCard表
  - `human-judgement` TR-10.2: 卡片内容完整，结构清晰

### [ ] Task 11: 费曼学习模式
- **Priority**: high
- **Depends On**: Task 2, Task 5
- **Description**:
  - 创建费曼学习页面
  - 实现AI对话逻辑（模拟12岁孩子）
  - 调用LearningAgent.feynmanDialog()获取评分和改进建议
  - 保存对话记录到AIConversation表
  - 循环直到评分≥90分
- **Acceptance Criteria Addressed**: AC-6
- **Test Requirements**:
  - `human-judgement` TR-11.1: AI回复符合"12岁孩子"的理解水平
  - `human-judgement` TR-11.2: 评分和改进建议有针对性

**Sprint 2完成标志**: 十倍学习系统形成
```
学习
  ↓
测试
  ↓
纠错
  ↓
知识压缩
  ↓
费曼解释
```

---

## Sprint 3: AI记忆与数据中心（第3周）

目标：让AI开始"认识用户"，产品有生命感

### [ ] Task 12: MemoryManager实现
- **Priority**: high
- **Depends On**: Task 2, Task 5
- **Description**:
  - 实现MemoryManager：管理用户记忆（偏好、薄弱点、习惯、成就）
  - 从AIConversation对话记录中提取记忆
  - 将记忆注入到AI上下文（通过ContextBuilder）
  - 实现记忆查看和管理入口
- **Acceptance Criteria Addressed**: AC-7
- **Test Requirements**:
  - `programmatic` TR-12.1: AIMemory表可正确保存记忆记录
  - `programmatic` TR-12.2: 记忆类型分类正确（weakness/preference/habit/achievement）

### [ ] Task 13: 成长数据中心
- **Priority**: high
- **Depends On**: Task 2, Task 5
- **Description**:
  - 创建成长记录页面，展示历史成长数据
  - 实现AI成长复盘功能（调用LearningAgent.generateGrowthReview()）
  - 创建成长数据可视化图表（周/月趋势）
- **Acceptance Criteria Addressed**: AC-8
- **Test Requirements**:
  - `programmatic` TR-13.1: GrowthRecord表可正确保存每日成长记录
  - `human-judgement` TR-13.2: 成长复盘报告内容有价值，建议具体

### [ ] Task 14: 今日成长驾驶舱（首页）
- **Priority**: high
- **Depends On**: Task 1, Task 2, Task 13
- **Description**:
  - 创建首页UI，展示今日成长值仪表盘
  - 显示各模块（AI开发、英语、健身、创作）的成长进度
  - 实现每日计划卡片展示
  - 添加导航入口到各模块
- **Acceptance Criteria Addressed**: FR-8
- **Test Requirements**:
  - `programmatic` TR-14.1: 首页可正常加载，显示模拟数据
  - `human-judgement` TR-14.2: UI布局合理，信息层级清晰

**Sprint 3完成标志**: AI开始认识用户
```
AI知道：
  用户：晚上学习效率最高
  薄弱：Python基础
  偏好：案例学习
```

---

## Sprint 4: 自媒体工作台（第4周）

目标：完成自媒体创作模块，重点打造"成长过程自动变内容"的差异化能力

### [ ] Task 15: CreatorAgent注册与实现
- **Priority**: high
- **Depends On**: Task 5
- **Description**:
  - 注册CreatorAgent到AgentManager
  - 实现CreatorAgent的核心能力：generateContentIdea、generateGrowthReport
  - 设计创作类Prompt：idea_prompt、growth_report_prompt
- **Acceptance Criteria Addressed**: AC-9
- **Test Requirements**:
  - `programmatic` TR-15.1: CreatorAgent可正确注册和调用
  - `human-judgement` TR-15.2: AI生成的内容创意质量高，有实用性

### [ ] Task 16: 自媒体创作工具
- **Priority**: high
- **Depends On**: Task 2, Task 15
- **Description**:
  - 创建内容创意生成页面
  - 调用CreatorAgent.generateContentIdea()生成创意
  - 爆款视频拆解分析展示
  - 内容作品管理（草稿、发布状态）
  - 核心差异化功能：成长报告自动生成（第N天学习报告、学习曲线变化、错误总结）
- **Acceptance Criteria Addressed**: AC-9
- **Test Requirements**:
  - `human-judgement` TR-16.1: 爆款拆解分析维度完整（标题、前三秒、情绪、需求、转化）
  - `programmatic` TR-16.2: Content表可正确保存创作内容
  - `human-judgement` TR-16.3: 成长报告自动生成功能可用，内容有价值

### [ ] Task 17: AI资源推荐功能
- **Priority**: medium
- **Depends On**: Task 2, Task 5
- **Description**:
  - 创建资源推荐页面
  - 实现AI资源推荐接口调用
  - 资源列表展示（名称、适合人群、难度、用途）
  - 7天学习路线生成
- **Acceptance Criteria Addressed**: FR-6
- **Test Requirements**:
  - `human-judgement` TR-17.1: 推荐资源列表展示清晰
  - `human-judgement` TR-17.2: 7天学习路线合理，可执行

**Sprint 4完成标志**: 自媒体模块可用，成长过程可自动生成内容

---

## Sprint 5: 优化体验与发布准备（第5周）

目标：完善体验，准备发布

### [ ] Task 18: 设置页面与系统配置
- **Priority**: high
- **Depends On**: Task 1, Task 3
- **Description**:
  - 创建设置页面（AI API密钥配置、数据管理、关于页面）
  - 实现AI模型切换（Claude/OpenAI）
  - 数据备份和恢复功能
  - 深色模式切换
- **Acceptance Criteria Addressed**: NFR-2, NFR-4
- **Test Requirements**:
  - `programmatic` TR-18.1: API密钥配置可保存并正确读取
  - `human-judgement` TR-18.2: 设置页面功能完整，用户体验良好

### [ ] Task 19: 测试与优化
- **Priority**: high
- **Depends On**: All previous tasks
- **Description**:
  - 运行所有单元测试，确保通过
  - 修复发现的bug
  - 优化UI体验和性能
  - 准备发布内容（截图、说明）
- **Acceptance Criteria Addressed**: N/A（质量保障）
- **Test Requirements**:
  - `programmatic` TR-19.1: 所有单元测试通过
  - `human-judgement` TR-19.2: 应用启动时间 < 3秒
  - `human-judgement` TR-19.3: 界面无明显bug，用户体验流畅

**Sprint 5完成标志**: 产品可发布，开发过程可作为自媒体内容

---

## 开发节奏建议

| 阶段 | 时间 | 内容 | 产出 |
|------|------|------|------|
| Sprint 1 | 第1周 | AI导师Demo（Task1-8） | AI学习闭环跑通 |
| Sprint 2 | 第2周 | 十倍学习系统（Task9-11） | 差异化训练系统形成 |
| Sprint 3 | 第3周 | AI记忆+数据中心+首页（Task12-14） | AI认识用户，产品有生命感 |
| Sprint 4 | 第4周 | 自媒体工作台（Task15-17） | 成长过程自动变内容 |
| Sprint 5 | 第5周 | 优化体验+发布（Task18-19） | 可发布版本 |

每个Sprint结束后：
1. 运行测试验证核心功能
2. 录制开发过程视频
3. 发布自媒体内容
4. 收集反馈迭代优化

---

## 第一个开发目标

> **7天内做出一个会教你学AI的AI导师Demo。**

Demo验证标准：
1. 输入学习目标（如"我要学AI开发"）
2. AI生成5级学习路线（Level1 Python → Level2 API → Level3 RAG → Level4 Agent → Level5 商业应用）
3. 自动生成今日学习任务
4. 完成任务后获得AI反馈
5. 对话记录保存到AIConversation表
6. 成长记录保存

这个Demo出来，就已经有第一个自媒体内容：
《我用AI开发了一个私人AI导师，它每天监督我学习AI》
