# AI个人成长OS - 产品需求文档（AI Native升级版）

## Overview
- **Summary**: 开发一个AI驱动的个人成长操作系统，帮助用户通过AI学习教练、英语提升、健身管理、AI开发学习和自媒体创作五个核心模块实现全方位成长。核心创新是将AI从简单的问答工具升级为具备记忆能力的私人导师，形成完整的成长闭环。
- **Purpose**: 让AI不只是回答问题，而是成为用户的私人导师。通过目标驱动、路径规划、动态考核、知识沉淀和成长复盘，帮助用户十倍速学习和成长。
- **Target Users**: 希望通过AI加速学习的个人开发者、学生、自媒体创作者、职场人士等追求自我提升的人群。

## Goals
- 实现完整的AI学习闭环：目标输入 → AI拆解路径 → 每日学习任务 → AI动态考核 → 知识压缩总结 → 实践输出 → 成长复盘
- 支持五个核心成长方向：AI开发学习、英语提升、健身管理、自媒体创作、个人IP打造
- 建立AI记忆系统（AIMemory），让AI越来越了解用户的学习习惯和偏好
- 通过Feature Module架构实现模块化和可扩展性（不采用Dynamic Feature真动态插件）
- 提供移动端原生体验，支持离线学习和云端AI能力
- 支持AIAgent接口扩展，方便后续新增功能模块

## Non-Goals (Out of Scope)
- 不支持多人协作或团队管理功能
- 不提供视频直播或实时互动功能
- 不支持在线课程付费系统（初期）
- 不实现复杂的社交分享功能
- 不支持实时数据同步（采用本地优先策略）

## Background & Context
- 基于Claude十倍速学习方法论，核心是5级学习阶梯和20小时核心学习计划
- 技术栈确定为Android原生开发（Kotlin + Jetpack Compose + Room）
- 采用本地为主+云端AI的数据策略，数据隐私优先
- 架构采用Feature Module + Dynamic Feature方案，支持持续升级
- 用户目标是打造"AI时代年轻人的第二大脑 + 私人导师 + 创作助手"

## 架构设计

### 开发策略
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

### 整体架构图
```
┌──────────────────────────────────────────────────────────────┐
│                        UI 层                                 │
│  ├─ 今日成长驾驶舱(首页)                                      │
│  ├─ Goal目标管理页面                                          │
│  ├─ Learning学习模块 (Feature Module)                        │
│  ├─ Growth数据中心 (Feature Module)                          │
│  └─ Creator创作模块 (Feature Module)                         │
├──────────────────────────────────────────────────────────────┤
│                    AI Engine (核心引擎)                       │
│  ├─ AIClient (AI API客户端)                                  │
│  ├─ PromptManager (提示词管理器)                              │
│  ├─ MemoryManager (记忆管理器)                                │
│  ├─ AgentManager (Agent注册中心)                              │
│  ├─ ContextBuilder (上下文构建器)                             │
│  └─ ResponseParser (响应解析器)                               │
├──────────────────────────────────────────────────────────────┤
│                        数据层                                 │
│  ├─ Local Database (Room)                                    │
│  │   ├─ Goal / LearningPath / LearningLevel                  │
│  │   ├─ DailyTask / KnowledgeCard                           │
│  │   ├─ AIMemory / GrowthRecord                             │
│  │   ├─ AIConversation / Content                             │
│  │   └─ LearningLevel                                        │
│  └─ Cloud API (Retrofit + AI APIs)                           │
│      ├─ Claude API                                           │
│      └─ OpenAI API                                           │
├──────────────────────────────────────────────────────────────┤
│                        Core 层                                │
│  ├─ Database (Room配置)                                      │
│  ├─ Network (Retrofit配置)                                   │
│  ├─ Security安全与鉴权                                        │
│  └─ Utils基础工具集                                           │
└──────────────────────────────────────────────────────────────┘
```

### AI Engine模块设计

AI Engine是产品的核心引擎，所有业务模块通过它调用AI能力。

#### 目录结构
```
core-ai/
├── AIClient.kt           # AI API客户端
├── PromptManager.kt      # 提示词管理器
├── MemoryManager.kt      # 记忆管理器
├── AgentManager.kt       # Agent注册中心
├── ContextBuilder.kt     # 上下文构建器
└── ResponseParser.kt     # 响应解析器
```

#### AIClient
- 统一管理AI API调用（Claude/OpenAI）
- 处理请求/响应、错误处理、超时设置
- 支持多种AI模型切换

#### PromptManager
- 加载和管理所有Prompt文件
- 支持Prompt版本管理
- 支持Prompt参数填充

#### MemoryManager
- 管理用户记忆（偏好、薄弱点、习惯、成就）
- 从对话和学习行为中提取记忆
- 将记忆注入到AI上下文

#### AgentManager
- 注册和管理各种AI Agent
- LearningAgent / EnglishAgent / FitnessAgent / CreatorAgent
- 统一的Agent调用接口

#### ContextBuilder
- 构建AI请求的上下文
- 整合用户记忆、学习进度、历史对话
- 生成个性化的AI请求

#### ResponseParser
- 解析AI响应
- 支持JSON格式解析
- 错误处理和数据验证

### 新增AIConversation表

用户与AI的对话记录是AI Memory的重要数据源。

```kotlin
@Entity(tableName = "ai_conversations")
data class AIConversation(
    @PrimaryKey val id: String,
    val sessionId: String,
    val agentType: String,      // LearningAgent / EnglishAgent / FitnessAgent / CreatorAgent
    val role: String,           // user / ai
    val content: String,
    val relatedTaskId: String?,
    val createdAt: Long
)
```

**关系链：**
```
User
  ↓
AIConversation (对话记录)
  ↓
AIMemory (记忆提取)
  ↓
Personalized AI (个性化AI)
```

**用途：**
- 保存用户与AI的所有互动
- 为AI Memory提供学习数据
- 支持对话历史查看
- 支持多轮对话上下文

### 数据模型关系图
```
Goal (目标)
  │
  └── LearningPath (学习路径) 1:N
        │
        ├── LearningLevel (学习等级) 1:N
        ├── DailyTask (每日任务) 1:N
        └── KnowledgeCard (知识卡片) 1:N
              │
              └── masteryScore (掌握度)

AIMemory (AI记忆)
  │
  ├── weakness (薄弱点)
  ├── preference (偏好)
  ├── habit (习惯)
  └── achievement (成就)

GrowthRecord (成长记录)
```

### 开发节奏（5个Sprint）
| Sprint | 时间 | 目标 | 完成标志 |
|--------|------|------|----------|
| Sprint 1 | 第1周 | AI学习闭环 | 输入目标→AI生成路线→每日任务→完成→AI反馈→成长记录 |
| Sprint 2 | 第2周 | 十倍学习系统 | AI考核→纠错→知识压缩→费曼解释 |
| Sprint 3 | 第3周 | AI记忆+数据中心+首页 | AI开始认识用户（偏好、薄弱点、习惯） |
| Sprint 4 | 第4周 | 自媒体工作台 | 内容创意生成→爆款拆解→成长报告自动生成 |
| Sprint 5 | 第5周 | 优化体验+发布 | 产品可发布，开发过程可作为自媒体内容 |

### Prompt Layer设计（核心资产）

Prompt Layer是产品的核心竞争力，定义了AI导师的人格和能力。

#### 目录结构
```
prompt/
├── learning/
│   ├── roadmap_prompt.txt      # 生成学习路线
│   ├── daily_task_prompt.txt   # 生成每日任务
│   ├── evaluation_prompt.txt   # 考核评分
│   ├── knowledge_card_prompt.txt  # 知识卡片生成
│   └── feynman_prompt.txt      # 费曼学习模式
├── memory/
│   └── memory_extract_prompt.txt  # 从学习行为提取记忆
├── creator/
│   ├── idea_prompt.txt         # 内容创意生成
│   ├── script_prompt.txt       # 脚本生成
│   └── growth_report_prompt.txt   # 成长报告自动生成
└── system/
    └── personality_prompt.txt  # AI导师人格设定
```

#### AI导师人格设定（personality_prompt）
```
你是一位专业的AI学习导师，名叫"成长助手"。

人格特征：
- 耐心：对初学者友好，从不嘲笑错误
- 专业：基于科学学习方法提供指导
- 鼓励：善于发现用户的进步并给予肯定
- 严格：对学习效果有高标准要求
- 个性化：根据用户的学习习惯调整教学方式

沟通风格：
- 使用简洁明了的语言
- 避免使用过于专业的术语
- 用比喻和案例帮助理解
- 给出具体的行动建议
- 用积极向上的语气

核心价值观：
- 学习是一个持续迭代的过程
- 实践比理论更重要
- 错误是学习的机会
- 每个人都有能力掌握新技能
```

#### 学习类Prompt设计要点
| Prompt名称 | 核心功能 | 输出格式 |
|------------|----------|----------|
| roadmap_prompt | 生成五级学习路线 | JSON（包含等级、目标、知识点、错误、达标标准） |
| daily_task_prompt | 生成每日学习任务 | JSON（包含任务类型、标题、描述、预计时间） |
| evaluation_prompt | 考核评分 | JSON（包含评分、理解程度、应用能力、错误分析、补充知识） |
| knowledge_card_prompt | 知识卡片生成 | JSON（包含主题、核心定义、关键概念、应用案例、常见错误、检查清单、自测问题） |
| feynman_prompt | 费曼学习模式 | JSON（包含评分、改进建议、追问问题） |

#### 记忆类Prompt设计要点
| Prompt名称 | 核心功能 | 输出格式 |
|------------|----------|----------|
| memory_extract_prompt | 从学习行为提取记忆 | JSON（包含记忆类型、内容、重要性） |

#### 创作类Prompt设计要点
| Prompt名称 | 核心功能 | 输出格式 |
|------------|----------|----------|
| idea_prompt | 内容创意生成 | JSON（包含标题、结构、情绪、需求、转化） |
| script_prompt | 脚本生成 | JSON（包含场景、对话、画面描述） |
| growth_report_prompt | 成长报告自动生成 | JSON（包含学习天数、学习曲线、错误总结、改进建议） |

#### Prompt版本管理
- Prompt文件存储在assets目录，支持动态加载
- 每个Prompt文件包含版本号和更新日期
- 支持A/B测试不同版本的Prompt效果
- Prompt更新不需要重新编译应用
