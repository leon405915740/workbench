# Checklist（精简版）

## Task 1: 设置页自动采集开关交互

### 代码实现 ✅
- [x] SettingsToggleItem 整行可点击（toggleable）
- [x] 服务未启用弹窗引导（无障碍/通知监听）
- [x] 警告横幅显示逻辑
- [x] 从系统设置返回后自动同步状态

### 待手动验证
- [ ] 整行点击正常触发
- [ ] 弹窗引导流程正确
- [ ] 警告横幅显示/隐藏正确
- [ ] 状态同步无误

---

## Task 2: 分类管理页 emoji 展示

### 代码实现 ✅
- [x] CategoryEmojiUtils 公共工具提取
- [x] CategoryCard/SubCategoryCard 显示 emoji
- [x] AddCategoryDialog 上级分类下拉列表显示 emoji
- [x] CategoryPicker 使用公共工具，行为不变

### 待验证
- [ ] emoji 显示正常
- [ ] 支出/收入 Tab 切换映射正确
- [ ] 记账页和映射管理页无回归

---

## Task 3: 构建与回归
- [x] versionCode +1 (22→23)
- [x] Debug APK 构建成功
- [x] Lint 无新增 Error
- [ ] 手动验证全部功能点
