# Step 1: Build Baseline

执行人：悠悠  
Reviewer：小艺

## 目标

先把工程整理成“可稳定升级”的状态，但本阶段不切到 AGP 9.1，也不引入 Compose UI。

本阶段结束时应达到：

- 保留当前业务行为不变。
- 构建脚本中的旧写法和分叉配置被收敛。
- 为 AGP 9.1 和 Compose 引入做准备。

## 当前已知问题

- 根工程使用 `buildscript` 方式管理插件。
- `app` 与 `easypermissions` 的 `compileSdk/targetSdk` 不一致。
- `easypermissions` 子模块仍停留在旧 Android 兼容策略。\r\n- `greenDAO` Gradle 插件与 Gradle 9.x 不兼容，升级时需要先移除构建期插件依赖。
- `gradlew` 文件为空，Linux/macOS CI 不可直接使用。

## 具体步骤

1. 修复 Wrapper 文件
- 重新生成或补全 `gradlew`，确保仓库同时支持 `gradlew` 和 `gradlew.bat`。

2. 统一模块 SDK 与 Java 基线
- 将 `app` 和 `easypermissions` 的 `compileSdk`、`targetSdk` 对齐到同一版本。
- 统一 JDK 目标到 `17`，即使当前 AGP 仍未升级，也先确保源码和 CI 能使用 JDK 17。

3. 收敛版本管理
- 把根工程与子模块中的硬编码版本聚合。
- 准备迁移到 `plugins {}` DSL；本阶段可以先不完全切换，但要消除明显分散配置。

4. 清点构建兼容风险
- 搜索是否有自定义 Gradle 逻辑依赖 `BaseExtension`、旧 Variant API 或 Kotlin 插件扩展。
- 如果发现风险点，记录到 PR 描述，不在本阶段硬改第三方插件。

5. 给 `easypermissions` 打标签
- 在 PR 中明确把 `easypermissions` 标为“阶段 4/5 移除对象”，本阶段不重写权限流程。

## 必改文件

- 根构建文件
- Wrapper 相关文件
- `app` 与 `easypermissions` 的 Gradle 配置

## 不要在本阶段做的事

- 不引入 Compose 依赖
- 不升级到 AGP 9.1
- 不改页面 UI
- 不改权限业务逻辑

## 最低验证

- `./gradlew.bat help`
- `./gradlew.bat :app:assembleDebug`
- `./gradlew.bat :easypermissions:assemble`

## Exit Criteria

- 本地 Debug 构建通过。
- 所有模块的 SDK/JDK 版本已统一。
- Wrapper 可在 Windows 与类 Unix 环境使用。
- 没有新增功能回归。

## 小艺 Review 重点

- 这一步是否只做“升级前收口”，没有偷跑 AGP/Compose 改造。
- `easypermissions` 是否仍然能独立构建。
- SDK/JDK 统一后，是否引入新警告或新崩溃。


