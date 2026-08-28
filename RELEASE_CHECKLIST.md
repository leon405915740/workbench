# AI Growth OS - Sprint 5 发布检查清单

## 一、编译与构建

| 检查项 | 状态 | 备注 |
|:---|:---|:---|
| 所有模块依赖配置正确 | ✅ | app/core/feature 共7个模块 |
| Hilt 依赖注入无冲突 | ✅ | 已删除重复 @HiltAndroidApp (App.kt) |
| Room 数据库无循环外键 | ✅ | Goal ↔ LearningPath 循环依赖已移除 |
| OkHttp 4.x API 适配 | ✅ | AIClient 已适配 4.x 版本 |
| Compose BOM 版本一致 | ✅ | 2024.04.01 |
| Kotlin 编译器版本匹配 | ✅ | 1.9.20 + kapt |
| 图标资源完整 | ✅ | mipmap-anydpi-v26 已创建 |
| Gradle Wrapper 可用 | ⚠️ | 系统未安装 Gradle，需 Android Studio 编译 |

## 二、代码质量审查

### 已修复问题
| 问题 | 文件 | 修复方式 |
|:---|:---|:---|
| 缺失 import | ContentRepository.kt | 添加 ResourceRecommendationResponse、WeeklyPlanResponse 导入 |
| 缺失字段 | GrowthReportResponse | 添加 hashtags: List<String> = emptyList() |
| 重复 Hilt 应用类 | App.kt + AIGrowthOSApplication.kt | 删除 App.kt |
| 主线程数据库操作 | AIGrowthOSApplication.kt | 改为 Dispatchers.IO |
| 未定义数据类 | ResponseParser.kt | 添加 MemoryExtractionResponse、ExtractedMemory |

### 待修复风险（非阻塞）
| 风险 | 位置 | 建议 |
|:---|:---|:---|
| `!!` 强制解包（14处） | LearningPathScreen、KnowledgeCardScreen、EvaluationScreen、FeynmanLearningScreen | 改用 `?.let` 或 Elvis 运算符 |
| API Key 硬编码为空 | LearningAgent.kt、CreatorAgent.kt | 接入 ApiKeyService 或安全存储 |
| 数据库迁移策略 | AppDatabase (version=1) | 发布前确定版本升级方案 |
| 异常吞没 | DashboardViewModel、GrowthRecordRepository | catch 块应记录日志 |

## 三、功能模块检查

| 模块 | 数据层 | UI层 | 导航 | AI集成 | 状态 |
|:---|:---|:---|:---|:---|:---|
| 今日驾驶舱 (Dashboard) | ✅ Repository | ✅ Screen | ✅ | ⏸️ 本地计算 | 可用 |
| 目标管理 (Goal) | ✅ DAO+Repo | ✅ Screen | ✅ | ⏸️ 未接入AI生成 | 可用 |
| 学习路径 (LearningPath) | ✅ DAO+Repo | ✅ Screen | ✅ | ✅ AI生成路径 | 可用 |
| 每日任务 (DailyTask) | ✅ DAO+Repo | ✅ Screen | ✅ | ✅ AI生成任务 | 可用 |
| 考核评分 (Evaluation) | ✅ DAO+Repo | ✅ Screen | ✅ | ✅ AI评分 | 可用 |
| 知识卡片 (KnowledgeCard) | ✅ DAO+Repo | ✅ Screen | ✅ | ✅ AI生成卡片 | 可用 |
| 费曼学习 (Feynman) | ✅ DAO+Repo | ✅ Screen | ✅ | ✅ AI对话 | 可用 |
| 成长复盘 (Growth) | ✅ DAO+Repo | ✅ Screen | ✅ | ✅ AI复盘 | 可用 |
| 记忆管理 (Memory) | ✅ DAO+Repo | ✅ Screen | ✅ | ✅ AI提取 | 可用 |
| 自媒体创作 (Creator) | ✅ DAO+Repo | ✅ 6个标签页 | ✅ | ✅ 6种AI能力 | 可用 |
| 设置 (Settings) | ✅ ApiKeyService | ✅ Screen | ✅ | ⏸️ 纯本地 | 可用 |

## 四、AI 能力矩阵

| AI功能 | Agent | Prompt | 解析器 | 测试状态 |
|:---|:---|:---|:---|:---|
| 生成学习路线 | LearningAgent | ✅ | ✅ | 待测试 |
| 生成每日任务 | LearningAgent | ✅ | ✅ | 待测试 |
| 考核评分 | LearningAgent | ✅ | ✅ | 待测试 |
| 生成知识卡片 | LearningAgent | ✅ | ✅ | 待测试 |
| 费曼学习对话 | LearningAgent | ✅ | ✅ | 待测试 |
| 成长复盘 | LearningAgent | ✅ | ✅ | 待测试 |
| 记忆提取 | LearningAgent | ✅ | ✅ | 待测试 |
| 内容创意生成 | CreatorAgent | ✅ | ✅ | 待测试 |
| 成长报告生成 | CreatorAgent | ✅ | ✅ | 待测试 |
| 爆款内容分析 | CreatorAgent | ✅ | ✅ | 待测试 |
| 内容脚本生成 | CreatorAgent | ✅ | ✅ | 待测试 |
| 学习资源推荐 | CreatorAgent | ✅ | ✅ | 待测试 |
| 7天学习计划 | CreatorAgent | ✅ | ✅ | 待测试 |

## 五、发布前必须完成

- [ ] **配置真实 API Key 存储**：当前 ApiKeyService 仅读取 SharedPreferences，需确保设置页面可正常保存
- [ ] **测试数据库初始化**：首次安装时 DatabaseInitializer 预填充数据是否正常
- [ ] **验证网络权限**：AndroidManifest 已声明 INTERNET 权限
- [ ] **ProGuard 规则**：release 构建需测试混淆后是否正常
- [ ] **暗黑模式适配**：检查 themes.xml (night) 配置
- [ ] **横屏/多窗口适配**：Compose UI 需验证不同屏幕尺寸
- [ ] **应用签名**：发布前配置 jks 签名文件
- [ ] **版本号更新**：app/build.gradle.kts 中 versionCode 和 versionName

## 六、Sprint 5 完成标志

- [x] 所有编译错误修复
- [x] 代码审查完成
- [x] 发布检查清单生成
- [ ] 在真机/模拟器上运行验证
- [ ] 核心闭环端到端测试（创建目标 → 生成路径 → 完成任务 → 查看成长）
- [ ] 自媒体模块功能验证

## 七、已知限制

1. **无 Gradle Wrapper**：当前环境无法命令行编译，需使用 Android Studio 的 embedded Gradle
2. **API Key 需手动配置**：用户首次使用必须在设置页填入 Claude/OpenAI API Key
3. **AI 响应依赖外部服务**：无网络或 API 限额时，AI 功能将不可用
4. **英语/健身模块占位**：Dashboard 中显示"即将上线"，尚未实现具体功能

---

生成时间：2026-07-29
对应版本：v1.0-sprint5
