# 分类记忆管理页 UI 改造 Spec

## Why
原平铺式记忆列表在词条超过100条后查找困难，需改为按一二级分类层级分组展示，提升管理效率。同时修复 reseedMemories 全量删除导致用户自定义记忆丢失的问题。

## What Changes
- MemoryManageScreen 从平铺列表改为三级层级分组展示（一级分类→二级分类→词条）
- reseedMemories 改为仅删除种子词(source="seed")，保留用户自定义记忆(source="user")，并用事务保证原子性
- UiState 新增 `expandedCategories: Set<String>` 管理折叠状态、`memoryGroups: List<MemoryGroup>` 替代 `allMemories`
- 新增搜索过滤（含200ms防抖）、删除/恢复二次确认 + Toast 反馈

## Impact
- Affected specs: v3-seed-multi-split (reseedsMemories 行为变更)
- Affected code: MemoryManageScreen.kt, MainViewModel.kt, UiState.kt, AppRepository.kt, CategoryMemoryDao.kt

---

## ADDED Requirements

### Requirement: 分类分组展示
系统 SHALL 将记忆词条按一级分类→二级分类→词条列表的三级层级结构分组展示，仅一级分类可折叠/展开，二级分类固定展开。

**设计约束**：二级分组不支持折叠，避免层级过深。

#### Scenario: 默认全展开
- **WHEN** 用户进入记忆管理页
- **THEN** 所有一级分组展开，显示二级子组和全部词条

#### Scenario: 折叠展开
- **WHEN** 用户点击一级分组标题
- **THEN** 折叠/展开该分组（仅影响该一级分类），旋转屏幕后折叠状态保持

#### Scenario: 空分组隐藏
- **WHEN** 二级分类下词条数为0
- **THEN** 该二级分组不显示
- **WHEN** 一级分类下所有二级分组均隐藏
- **THEN** 该一级分组整体不显示

#### Scenario: 视觉层级
- 一级分组标题：浅灰背景 + 加粗文字 + 折叠箭头 + 词条计数（如「餐饮(32)」）
- 二级分组标题：白色背景 + 左侧彩色竖线 + 次级字号
- 词条行：白色背景 + 普通字号 + 右侧来源标签(自定义/系统预置) + 删除按钮
- 长触发词单行末尾省略号截断，禁止换行

### Requirement: 折叠状态管理
系统 SHALL 将折叠状态存入 UiState.expandedCategories，由 ViewModel 统一维护。

#### Scenario: 搜索强制展开
- **WHEN** 搜索框非空
- **THEN** 所有分组强制全部展开
- **WHEN** 清空搜索框
- **THEN** 恢复用户之前的折叠状态，非一直保持展开

### Requirement: 搜索过滤
系统 SHALL 支持按关键词实时过滤词条，含200ms输入防抖。

#### Scenario: 搜索匹配
- **WHEN** 用户输入搜索关键词
- **THEN** 仅显示命中词条的分组，搜索时强制展开所有分组，输入防抖200ms避免频繁重组

#### Scenario: 搜索无结果
- **WHEN** 搜索无匹配词条
- **THEN** 显示"未找到匹配的记忆规则"

### Requirement: 恢复默认仅重置种子词（**BREAKING**）
系统 SHALL 在恢复默认记忆时仅删除 source="seed" 的词条，保留 source="user" 的用户自定义记忆。操作在事务中执行，异常时回滚保证原子性。种子词批量插入使用 INSERT OR IGNORE 避免与用户记忆主键冲突。

#### Scenario: 恢复默认保留用户记忆
- **WHEN** 用户点击恢复默认
- **THEN** 种子词被重置，用户自定义记忆完整保留

#### Scenario: 事务异常回滚
- **WHEN** 删除种子词成功但重新插入失败
- **THEN** 事务回滚，种子词不丢失

### Requirement: 词条排序
系统 SHALL 在单个二级分类内部按 user 优先、seed 其次、同来源按更新时间倒序排列词条。排序作用域为每个二级分组内部，不跨分类混排。

### Requirement: 操作反馈
- 删除词条：弹出二次确认弹窗，确认后 Toast"已删除记忆" + 列表即时刷新
- 恢复默认：弹出二次确认弹窗明确"仅重置系统词条，自定义记忆保留"，确认后 Toast"已恢复默认记忆" + 列表刷新
- 新增记忆：保存成功后自动重新加载分组数据，新词条归入对应分类

### Requirement: Tab 切换
- 切换收支 Tab 时：搜索框清空、滚动位置重置到顶部
- Tab 下方实时显示词条总数（如"共 128 条记忆规则"）
- 空状态：无词条时显示"暂无记忆规则，记账后修改分类即可自动学习"

---

## MODIFIED Requirements

### Requirement: reseedMemories 事务化
由 `deleteAll() + insertAll(seeds)` 改为 `deleteBySource("seed") + insertAll(seeds)`，DAO 用 `@Transaction` 包裹。
**Migration**: 现有调用方无需修改参数，语义由"全量重置"变为"仅重置种子词"。
