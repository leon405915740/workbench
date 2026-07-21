# Tasks（精简版）

## Task 1: 修复设置页自动采集开关交互 ✅ 已完成

### 待手动验证
- [ ] 点击文字区域、图标区域、Switch 本身均能正确触发状态切换

**已实现**：整行可点击(toggleable)、服务未启用弹窗引导、警告横幅、返回后自动同步状态

---

## Task 2: 统一分类管理页 UI 为 emoji 风格 ✅ 已完成

### 待手动验证
- [ ] 分类管理页一级/二级分类显示 emoji
- [ ] 记账页 CategoryPicker 的 emoji 显示不受影响
- [ ] AddMappingDialog 中的 CategorySelector 不受影响

**已实现**：CategoryEmojiUtils 公共工具提取、CategoryCard/SubCategoryCard 显示 emoji、AddCategoryDialog 显示 emoji

---

## Task 3: 版本更新与回归验证 ✅ 代码完成

### 待手动验证
- [ ] 设置页 3 个自动采集开关整行可点击
- [ ] 关闭无障碍服务后设置页显示警告横幅
- [ ] 开启无障碍服务后从设置返回，开关自动变为 ON
- [ ] 分类管理页 emoji 显示正常
- [ ] 记账页 CategoryPicker 正常可用

**已完成**：versionCode 22→23, versionName 2.17.0→2.18.0, Debug APK 构建成功, Lint 无新增 Error
