# 项目记忆路由地图

> 本文件只负责把后续任务路由到权威来源和可验证证据，不是第二份交接文档，不复制 Current State，也不保存完整产品功能清单。

## 根入口

- 项目唯一根目录：D:\项目。
- Current State 唯一权威来源：D:\项目\工作交接文档.md，当前版本为 v3.0。
- UI、交互和功能参考：D:\项目\猫咪生活报\workbench-desktop.html。
- 网页只提供参考；用户当前明确要求与工作交接文档优先。

## 证据优先级

遇到冲突时按以下顺序判断：

1. 用户当前明确要求。
2. 工作交接文档中的当前结论、范围和数据边界。
3. 实时 Compose、Room、Repository、Manifest 和 Gradle 源码。
4. 自动化测试、迁移测试和构建输出。
5. 可定位的设备证据。
6. HTML 参考、旧文档、旧 APK 和历史提交。

计划或源码存在不等于完成；构建成功不等于设备验收。

## 按任务路由

### 产品范围与当前状态

先读工作交接文档的“文档元数据与当前结论”“最终产品结构”“记账保留范围”和“当前工程事实与风险”。不要从旧五模块、旧 APK 或网页示例反推当前目标。

### UI、导航与移动适配

按以下路径核对：

工作交接文档“最终产品结构”和“移动端适配规则”
→ 猫咪生活报/workbench-desktop.html 的布局、交互和功能参考
→ app/src/main/java/com/aigrowth/os/ 下的实时 Compose 导航、页面和共享 UI
→ 响应式、键盘、返回栈、Photo Picker、相机和设备证据

这里的目标是常驻侧边栏与主区；底部三 Tab、抽屉主导航和全局 Dock 都是过时方案。

### 新工作台数据与迁移

按以下路径核对：

工作交接文档“数据与迁移计划”
→ feature/accounting 中现有 Room、Repository 和真实记账数据边界
→ 后续独立 Room 数据库 workbench.db 的实体、DAO、Repository 和一次性播种实现
→ 两个数据库各自的显式 Migration 与迁移测试
→ 重启、升级和设备持久化证据

旧 simple_workbench DataStore 只用于识别历史健身、自媒体和英语数据的位置；这些数据不迁移、不展示，也不主动删除。不得把旧 DataStore 或通用 BoardItem 方案当成 v3.0 数据基线。

### 记账、通知与账单图片

先读工作交接文档“记账保留范围”和“数据与迁移计划”，再核对：

- feature/accounting/src/main/java/com/accounting/app/ 下的现有页面、ViewModel、Repository、Room、分类学习、通知服务、设置和导出入口。
- app 与 feature/accounting 的宿主集成、Bridge 和导航返回栈。
- 记账数据库当前 v6 定义、后续显式 6 → 7 附件迁移及迁移测试。
- 付款通知、AI 解析、手动记账、统计问答、图片生命周期和设备证据。

路由原则：现有记账能力、真实账单和设置数据必须完整保留；宿主只经 Repository 或 Bridge 访问。当前仅新增一张 App 私有本地账单图片，预算、还款提醒、计划消费、账户与标签系统、记账日历、OCR 和云端图片同步不在范围内。

### 首页、洞察、状态趋势与番茄钟

先读工作交接文档“网页端功能清单”，再查 workbench.db 的领域数据与聚合层、首页和洞察 Compose 源码、后台计时及通知实现，最后核对进程重建、后台计时、到时通知和设备省电场景证据。

### 构建、APK 与历史产物

先读工作交接文档“当前工程事实与风险”和“后续实施与验收顺序”，再核对实时 Gradle 配置、测试输出、app/build/outputs/apk/debug/output-metadata.json、实际 APK 文件和 SHA-256。旧 APK 仅作历史指针，不能作为 v3.0 交付物。

## 漂移检查

每次开始实现或宣称完成前，至少检查：

- git status 和目标文件 diff，确认没有覆盖用户已有改动。
- 常驻导航顺序、冷启动首页和设置底部固定是否与交接文档一致。
- 新模块是否使用独立 workbench.db，而不是继续写旧简单 DataStore。
- 记账是否仍通过原 Repository 或 Bridge 使用唯一真实数据库。
- v6 → v7 是否为显式、非破坏性迁移，金额是否继续按分和 BigDecimal 转换。
- 中文输入、键盘、通知、图片和设备行为是否有真实证据。
- 文档是否把计划、旧 APK 或单次构建误写为已完成。

## 维护边界

- 只有用户明确要求时才更新本地图或持久记忆。
- 更新本地图时只维护章节名称、文件路径、证据类型和路由顺序，不复制交接文档正文。
- 持久记忆更新只写入 C:\Users\Administrator\.codex\memories\extensions\ad_hoc\notes\ 的最小授权备注；不得直接修改 MEMORY.md、memory_summary.md 或历史汇总。
- 不保存 API Key、DeepSeek Key、个人数据、完整聊天、推理过程或原始日志。
- 不用历史文档、旧 README、旧 APK、文件名或记忆条目覆盖实时源码、测试和设备事实。
