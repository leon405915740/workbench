# AI个人成长OS - 验证清单

## Sprint 1: AI导师Demo验证

### 基础设施验证
- [ ] Checkpoint 1: 项目结构验证 - 确认模块结构正确（core、core-ai、feature/learning、feature/growth、feature/creator）
- [ ] Checkpoint 2: 依赖验证 - 确认Kotlin、Jetpack Compose、Room、Retrofit、Hilt依赖配置正确
- [ ] Checkpoint 3: 编译验证 - 项目可正常编译，无依赖冲突和语法错误

### 数据层验证
- [ ] Checkpoint 4: Entity验证 - 所有Entity类（Goal、LearningPath、LearningLevel、DailyTask、KnowledgeCard、AIMemory、GrowthRecord、Content、AIConversation）编译通过
- [ ] Checkpoint 5: DAO验证 - 所有DAO接口方法签名正确，可与Entity匹配
- [ ] Checkpoint 6: 数据库验证 - Room数据库可正常创建，Migration策略配置正确
- [ ] Checkpoint 7: Repository验证 - 所有Repository接口方法实现完整，数据访问逻辑正确
- [ ] Checkpoint 8: 数据关联验证 - Goal→LearningPath→LearningLevel→DailyTask关联查询测试通过
- [ ] Checkpoint 9: AIConversation验证 - AIConversation实体CRUD操作测试通过

### AI Engine验证
- [ ] Checkpoint 10: AIClient验证 - AIClient编译通过，API调用方法签名正确
- [ ] Checkpoint 11: ResponseParser验证 - ResponseParser可正确解析JSON格式响应
- [ ] Checkpoint 12: Retrofit配置验证 - Retrofit客户端配置正确，支持HTTPS和超时设置
- [ ] Checkpoint 13: AgentManager验证 - AgentManager可正确注册和调用Agent
- [ ] Checkpoint 14: ContextBuilder验证 - ContextBuilder可正确构建AI请求上下文
- [ ] Checkpoint 15: LearningAgent验证 - LearningAgent的4个核心方法（generateLearningPath、generateDailyTask、evaluateAnswer、generateKnowledgeCard）可正常调用

### Prompt Layer验证
- [ ] Checkpoint 16: Prompt目录结构验证 - 确认prompt/learning, prompt/memory, prompt/creator, prompt/system目录结构正确
- [ ] Checkpoint 17: 学习类Prompt验证 - roadmap_prompt、daily_task_prompt、evaluation_prompt、knowledge_card_prompt文件存在且格式正确
- [ ] Checkpoint 18: 系统类Prompt验证 - personality_prompt文件存在，包含完整的人格设定
- [ ] Checkpoint 19: PromptManager验证 - Prompt文件可正确加载和解析，支持版本管理

### 目标管理验证
- [ ] Checkpoint 20: 目标创建验证 - 目标创建表单字段完整，验证逻辑合理
- [ ] Checkpoint 21: 目标列表验证 - 目标列表正确显示所有active目标
- [ ] Checkpoint 22: 目标状态验证 - 目标状态变更（active→completed）测试通过

### 学习路线验证
- [ ] Checkpoint 23: 路线生成验证 - AI生成学习路线数据可正确解析并保存到数据库
- [ ] Checkpoint 24: 等级验证 - LearningLevel表包含5个等级记录，等级数据完整
- [ ] Checkpoint 25: 路线展示验证 - 学习路线详情展示清晰，等级递进合理

### 每日任务验证
- [ ] Checkpoint 26: 计划生成验证 - 每日计划生成后，DailyTask表包含多条任务记录
- [ ] Checkpoint 27: 任务状态验证 - 任务完成状态变更测试通过
- [ ] Checkpoint 28: 任务记录验证 - 任务用时和用户响应可正确记录
- [ ] Checkpoint 29: 对话记录验证 - 对话记录正确保存到AIConversation表

**Sprint 1完成标志**: AI导师Demo可用

---

## Sprint 2: 十倍学习系统验证

### AI考核验证
- [ ] Checkpoint 30: 考核流程验证 - 单次提问模式流程顺畅，无信息过载
- [ ] Checkpoint 31: 评分验证 - AI评分（0-100）正确保存到DailyTask表
- [ ] Checkpoint 32: 反馈验证 - AI反馈和错误分析保存正确，展示清晰
- [ ] Checkpoint 33: 循环验证 - 循环提问逻辑实现，直到达标
- [ ] Checkpoint 34: 对话记录验证 - 考核对话记录正确保存到AIConversation表

### 知识卡片验证
- [ ] Checkpoint 35: 卡片生成验证 - 知识卡片正确保存到KnowledgeCard表
- [ ] Checkpoint 36: 内容验证 - 卡片包含完整的7个部分（主题、核心定义、关键概念、应用案例、常见错误、检查清单、自测问题）
- [ ] Checkpoint 37: 掌握度验证 - 知识掌握度分数（masteryScore）可正确记录和显示

### 费曼学习验证
- [ ] Checkpoint 38: 对话流程验证 - AI对话流程顺畅，符合"12岁孩子"的理解水平
- [ ] Checkpoint 39: 评分验证 - AI评分和改进建议有针对性
- [ ] Checkpoint 40: 循环验证 - 循环机制合理，直到评分≥90分
- [ ] Checkpoint 41: 对话记录验证 - 费曼对话记录正确保存到AIConversation表

**Sprint 2完成标志**: 十倍学习系统形成

---

## Sprint 3: AI记忆与数据中心验证

### MemoryManager验证
- [ ] Checkpoint 42: 记忆记录验证 - AIMemory表可正确保存记忆记录
- [ ] Checkpoint 43: 分类验证 - 记忆类型分类正确（weakness/preference/habit/achievement）
- [ ] Checkpoint 44: 提取验证 - MemoryManager可从AIConversation对话记录中提取记忆
- [ ] Checkpoint 45: 注入验证 - 记忆可正确注入到AI上下文（通过ContextBuilder）
- [ ] Checkpoint 46: 查看入口验证 - 记忆查看和管理入口可用

### 成长数据验证
- [ ] Checkpoint 47: 记录验证 - GrowthRecord表可正确保存每日成长记录
- [ ] Checkpoint 48: 复盘验证 - AI成长复盘报告内容有价值，建议具体
- [ ] Checkpoint 49: 可视化验证 - 成长数据可视化图表（周/月趋势）清晰，易于理解

### 首页验证
- [ ] Checkpoint 50: 仪表盘验证 - 今日成长值仪表盘显示正确（0-100分）
- [ ] Checkpoint 51: 模块进度验证 - 各模块（AI开发、英语、健身、创作）成长进度显示正确
- [ ] Checkpoint 52: 每日计划验证 - 每日计划卡片展示正确，可点击进入详情
- [ ] Checkpoint 53: 导航验证 - 各模块导航入口可正常跳转

**Sprint 3完成标志**: AI开始认识用户

---

## Sprint 4: 自媒体工作台验证

### CreatorAgent验证
- [ ] Checkpoint 54: 注册验证 - CreatorAgent可正确注册到AgentManager
- [ ] Checkpoint 55: 能力验证 - CreatorAgent的核心方法（generateContentIdea、generateGrowthReport）可正常调用
- [ ] Checkpoint 56: Prompt验证 - 创作类Prompt（idea_prompt、growth_report_prompt）文件存在且格式正确

### 自媒体创作验证
- [ ] Checkpoint 57: 创意生成验证 - AI生成的内容创意质量高，有实用性
- [ ] Checkpoint 58: 爆款拆解验证 - 爆款拆解分析维度完整（标题、前三秒、情绪、需求、转化）
- [ ] Checkpoint 59: 作品管理验证 - Content表可正确保存创作内容，草稿和发布状态管理正确
- [ ] Checkpoint 60: 成长报告自动生成验证 - 系统可自动生成学习天数、学习曲线、错误总结等内容

### AI资源推荐验证
- [ ] Checkpoint 61: 资源列表验证 - 推荐资源列表展示清晰（名称、适合人群、难度、用途）
- [ ] Checkpoint 62: 路线生成验证 - 7天学习路线合理，可执行

**Sprint 4完成标志**: 自媒体模块可用，成长过程可自动生成内容

---

## Sprint 5: 优化体验与发布验证

### 设置验证
- [ ] Checkpoint 63: API密钥验证 - API密钥配置可保存并正确读取
- [ ] Checkpoint 64: 数据管理验证 - 数据备份和恢复功能正常
- [ ] Checkpoint 65: 主题验证 - 深色模式切换功能正常
- [ ] Checkpoint 66: AI模型切换验证 - Claude/OpenAI切换功能正常

### 性能验证
- [ ] Checkpoint 67: 启动时间验证 - 应用启动时间 < 3秒
- [ ] Checkpoint 68: 数据查询验证 - 本地数据查询响应时间 < 100ms
- [ ] Checkpoint 69: API超时验证 - AI API调用超时时间 < 15秒，并有加载状态提示

### 测试验证
- [ ] Checkpoint 70: 单元测试验证 - 所有单元测试通过
- [ ] Checkpoint 71: UI验证 - 界面无明显bug，用户体验流畅

**Sprint 5完成标志**: 产品可发布

---

## 整体验收标准

- [ ] AC-1: 用户目标创建 - 目标成功保存到本地数据库，状态为active
- [ ] AC-2: AI学习路线生成 - 系统保存包含5个等级的学习路径
- [ ] AC-3: 每日任务生成 - AI生成包含学习、练习、测试等多种类型的任务列表
- [ ] AC-4: AI考核评分 - 系统显示评分（0-100）、错误分析和补充知识
- [ ] AC-5: 知识卡片生成 - 知识卡片包含完整的7个部分
- [ ] AC-6: 费曼学习模式 - AI给出评分和改进建议，循环直到评分≥90
- [ ] AC-7: AI记忆记录 - 系统记录用户偏好、薄弱点、学习习惯到AIMemory表
- [ ] AC-8: 成长复盘生成 - AI生成本周成长总结、问题分析和下周建议
- [ ] AC-9: 内容创意生成 - 系统显示标题建议、内容结构、情绪分析
- [ ] AC-10: 对话记录保存 - 用户与AI的对话记录正确保存到AIConversation表
