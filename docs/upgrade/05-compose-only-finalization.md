# Step 5: Compose-Only Finalization

执行人：悠悠  
Reviewer：小艺

## 目标

完成从“混合工程”到“Compose 优先工程”的收尾，移除已不再需要的 View/XML 资产与旧构建兼容代码。

本阶段结束时应达到：

- 主 UI 以 Compose 为主
- 旧 XML 页面与无用 Adapter 被清理
- AGP 9.1 兼容开关全部移除
- 团队后续开发默认走 Compose

## 具体步骤

1. 清理混合期临时兼容
- 删除已不需要的 `ComposeView` 壳、桥接适配器和临时包装层。
- 移除为了 AGP 9.1 过渡而保留的 opt-out 配置。

2. 统一导航策略
- 只有当所有页面都已是 Composable destination，才评估从 Fragment/Activity 壳迁移到 Navigation Compose。
- 如果仍有大量系统页面或服务绑定壳，允许保留现有导航，不强行切换。

3. 清理旧 UI 资产
- 删除不再引用的 XML layout、Drawable 包装、旧 Adapter、旧自定义 View。
- 清理只为旧页面存在的样式和资源别名。

4. 固化团队默认模板
- 新页面模板改为 Compose。
- 新增 UI 测试模板优先选择 Compose 测试。
- README 或团队开发规范中写明：默认用 Compose，不再新增 XML 页面。

5. 回归与收尾
- 重点检查媒体播放、权限、蓝牙扫描、前后台切换、通知栏交互。
- 输出最终升级报告，列出仍保留的 View 资产与原因。

## 不要在本阶段做的事

- 不为了“纯 Compose”去重写稳定且非 UI 关键的业务层。
- 不把服务层、数据库层、DI 层的重构绑在这一步。

## 最低验证

- `assembleDebug`
- `assembleRelease`
- 关键路径手工回归
- 如果已有 UI 自动化，补一轮 Compose 覆盖

## Exit Criteria

- Compose 已成为主 UI 技术栈。
- AGP 9.1 兼容开关全部移除。
- 旧 XML 页面不再承担核心展示职责。
- 团队默认开发流程已切换到 Compose。

## 小艺 Review 重点

- 是否真的移除了临时兼容，而不是把过渡代码长期保留。
- 是否留下了明确的最终遗留清单。
- 是否可以宣布“后续默认 Compose 开发”。

