# Review Checklist

本清单给小艺使用。每个阶段 review 时都必须逐项确认；任一项不通过，本阶段不得进入下一步。

## 通用门禁

- 构建脚本没有引入新的临时开关，除非文档明确允许且记录了移除计划。
- 代码只包含当前阶段的目标改动，没有顺手混入业务重构。
- 版本号、依赖、Gradle 属性与阶段文档一致。
- Debug 包可以安装启动，关键路径不崩溃。
- 本阶段新增的日志、TODO、兼容分支都有后续清理计划。

## Compose 相关门禁

- Compose 依赖统一通过 BOM 管理，非 BOM 依赖单独写明版本。
- 没有同时保留过期的 Compose compiler 配置和 Kotlin 2.x Compose 插件配置。
- 新增 Compose 页面没有直接依赖 Activity/Fragment 具体实现，状态通过参数或 ViewModel 注入。
- 需要复用旧 View 的地方，使用了 `ComposeView`、`AndroidView` 或 `AndroidViewBinding`，没有绕开生命周期。

## AGP 9.1 相关门禁

- `kotlin-android` 已删除或有明确的阶段性 opt-out 说明。
- `android.newDsl` 和 `android.builtInKotlin` 的状态与阶段目标一致。\r\n- `android.enableJetifier` 已移除，除非能证明仍存在未迁移的 Support Library 依赖。
- 没有遗留依赖 `BaseExtension`、`applicationVariants` 等旧 DSL/Variant API 的自定义构建逻辑。
- `namespace`、`compileSdk`、`targetSdk`、JDK 版本在模块间一致。

## 测试与回归

- 执行过当前阶段文档要求的最低验证命令。
- 手工回归了当前阶段文档列出的重点页面和流程。
- 如果阶段涉及 UI 迁移，截图或录屏已随 PR 提交。
- 如果阶段涉及权限或媒体访问，至少验证一次首次授权、拒绝授权、从设置页返回三条路径。

## 通过标准

只有当以下条件同时满足时，小艺才能批准进入下一阶段：

- 所有 blocker 问题已关闭。
- 所有必改意见已修复。
- 当前阶段的 exit criteria 全部满足。
- 没有打开的“先欠着，下阶段再说”的高风险问题。


