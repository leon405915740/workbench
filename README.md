# AI成长操作系统 (AI Growth OS)

> 让AI成为你的私人学习教练，实现10倍速成长

## 项目简介

AI成长操作系统是一款AI Native的Android应用，通过AI学习教练、成长追踪、内容创作等功能，帮助用户实现全方位成长。核心创新是将AI从简单问答工具升级为具备记忆能力的私人导师。

### 核心功能

1. **AI学习教练**
   - 学习目标管理
   - AI生成学习路径
   - 每日学习任务
   - 费曼学习法训练

2. **成长追踪**
   - 成长数据仪表盘
   - 知识卡片复习
   - 成长值量化

3. **AI记忆**
   - 对话记忆
   - 知识持久化

4. **自媒体工作台**
   - AI内容创意生成
   - 爆款内容分析
   - 内容脚本生成
   - 成长报告生成
   - AI资源推荐
   - 7天学习计划

## 技术架构

```
AI-Growth-OS/
├── app/                           # 应用模块
│   └── src/main/java/com/aigrowth/os/
│       ├── AIGrowthOSApp.kt       # 导航和路由
│       ├── AIGrowthOSApplication.kt # Application初始化
│       ├── MainActivity.kt        # 主Activity
│       └── ui/
│           ├── theme/             # 主题
│           ├── onboarding/        # 首次启动引导
│           ├── splash/            # 启动页
│           └── common/            # 通用组件
├── core/
│   ├── ai-engine/                 # AI引擎模块
│   │   └── src/main/java/com/aigrowth/os/core/aiengine/
│   │       ├── AIClient.kt        # AI API客户端
│   │       ├── PromptManager.kt   # Prompt管理
│   │       ├── CreatorAgent.kt    # 创作Agent
│   │       ├── LearningAgent.kt   # 学习Agent
│   │       └── ApiKeyService.kt   # API Key服务
│   └── database/                  # 数据库模块
│       └── src/main/java/com/aigrowth/os/core/database/
│           ├── AppDatabase.kt     # Room数据库
│           ├── DatabaseInitializer.kt # 数据库初始化
│           ├── entity/            # 数据实体
│           └── dao/               # 数据访问对象
└── feature/
    ├── learning/                  # 学习模块
    │   └── src/main/java/com/aigrowth/os/feature/learning/
    │       ├── domain/            # 业务逻辑
    │       └── presentation/      # UI层
    ├── creator/                   # 创作模块
    │   └── src/main/java/com/aigrowth/os/feature/creator/
    │       ├── domain/
    │       └── presentation/
    └── growth/                    # 成长模块
```

## 数据模型

- **Goal**: 学习目标
- **LearningPath**: 学习路径
- **LearningLevel**: 学习阶段
- **DailyTask**: 每日任务
- **KnowledgeCard**: 知识卡片
- **AIMemory**: AI记忆
- **AIConversation**: 对话记录
- **FeynmanSession**: 费曼学习会话
- **GrowthRecord**: 成长记录
- **Content**: 内容创作

## 开发模式

本项目采用 **AI Native开发模式**：

1. **AI快速全量搭骨架**：使用AI辅助快速生成代码框架
2. **核心闭环优先跑通**：优先实现核心功能流程
3. **持续增强**：迭代优化和功能扩展

### Sprint开发周期

| Sprint | 目标 | 状态 |
|--------|------|------|
| Sprint 1 | 数据层设计 | ✅ 完成 |
| Sprint 2 | AI引擎开发 | ✅ 完成 |
| Sprint 3 | 学习模块开发 | ✅ 完成 |
| Sprint 4 | 自媒体工作台 | ✅ 完成 |
| Sprint 5 | 整合测试和发布准备 | ✅ 完成 |

## 构建运行

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34
- Kotlin 1.9.23

### 构建步骤

```bash
# 克隆项目
git clone [project-url]

# 进入项目目录
cd AI-Growth-OS

# 构建Debug版本
./gradlew assembleDebug

# 或在Android Studio中点击Run按钮
```

### API Key配置

首次启动应用时，需要配置AI API Key：

1. 点击"下一步"进入配置页面
2. 输入Claude或OpenAI的API Key
3. 选择AI模型（Claude/GPT）
4. 点击"保存设置"

**获取API Key：**
- Claude: https://console.anthropic.com/
- OpenAI: https://platform.openai.com/

## 核心AI能力

### CreatorAgent

自媒体创作AI代理，提供以下能力：

- `generateContentIdea()`: 生成内容创意
- `generateGrowthReport()`: 生成成长报告
- `analyzeViralContent()`: 爆款内容分析
- `generateContentScript()`: 生成内容脚本
- `recommendResources()`: 推荐学习资源
- `generateWeeklyPlan()`: 生成7天学习计划

### LearningAgent

学习AI代理，提供以下能力：

- 学习路径规划
- 每日任务生成
- 知识卡片生成
- 费曼学习指导

## 特色功能

### 1. 费曼学习法训练

通过AI扮演初学者，用户尝试教授知识点，AI提问和反馈，实现深度学习。

### 2. AI记忆系统

自动记录学习对话，形成知识库，支持语义检索。

### 3. 成长值量化

每天生成成长报告，量化学习成果，激励持续学习。

### 4. 自媒体创作工作台

将学习成果转化为自媒体内容，实现知识变现。

## 项目成果

- **代码文件数**: 40+
- **功能页面数**: 20+
- **数据实体数**: 10
- **AI Agent数**: 2
- **Prompt模板数**: 10+

## 工作台整合更新（记账 App 设计语言版）

### 本次变更

- **产品名**: App 更名为「工作台」，启动图标换用记账 App 的金色标识
- **UI 设计体系**: 全面沿用记账 App 的莫兰迪雾蓝紫设计语言（品牌色 #6366A0、WCAG AA 对比度规范），关闭 Material You 动态取色与深色模式，全局浅色莫兰迪配色
- **新增模块 core:design**: 统一设计组件（MorandiCard 圆角卡片+品牌柔阴影、GradientSummaryCard 渐变总览卡、CapsuleTabGroup 胶囊切换组、GradientButton 渐变按钮、GradientProgressBar 渐变进度条、MorandiEmptyState 空状态等）
- **停用示例数据预填充**: 首次启动不再写入演示数据，以真实用户数据为准
- **第一批功能（本地闭环）**: 目标管理 + 每日任务 + 成长记录仪表盘已打通
  - 目标：创建 / 编辑 / 完成 / 删除
  - 每日任务：支持**手动添加任务**（不依赖 AI）；AI 生成任务需在设置页配置 API Key
  - 成长记录：完成任务后**自动写入今日成长记录**，首页仪表盘实时刷新（无需手动点击刷新）
  - 首页入口：今日学习计划卡片右上角「今日任务」直达每日任务页
  - 首页 / 目标 / 每日任务 / 成长 / 新建目标等页面均已换为记账设计风格

### 构建

\`\`\`bash
gradlew.bat :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
\`\`\`

## 后续规划

1. **云端同步**: 支持多设备数据同步
2. **社交功能**: 学习伙伴、打卡分享
3. **更多AI模型**: 支持本地大模型
4. **Web版**: 提供Web端访问
5. **数据分析**: 深度学习分析报告

## 开源协议

MIT License

## 联系方式

- 项目地址: [GitHub URL]
- 问题反馈: [Issues URL]

---

**让AI成为你的成长伙伴，一起实现10倍速成长！** 🚀