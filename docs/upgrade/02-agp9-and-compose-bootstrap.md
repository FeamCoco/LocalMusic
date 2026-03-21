# Step 2: AGP 9.1 And Compose Bootstrap

执行人：悠悠  
Reviewer：小艺

## 目标

将工程升级到 AGP `9.1.0`，并完成 Compose 基础启用，但仍不大规模迁移页面。

本阶段结束时应达到：

- AGP `9.1.0`
- Gradle `9.3.1`
- JDK `17`
- Built-in Kotlin 启用
- App 模块可以编译最小 Compose 页面

## 目标版本

- AGP: `9.1.0`
- Gradle: `9.3.1`
- Kotlin: 优先兼容 AGP 9.1；当前仓库临时使用 `android.builtInKotlin=false` 过渡
- Compose BOM: `2026.02.01`
- Compose UI/Foundation/Runtime/Material: 跟随 BOM，对应 `1.10.5`
- Compose Material3: `1.4.0`

## 具体步骤

1. 升级 Gradle 与 AGP
- 更新 wrapper 到 `9.3.1`。
- 更新 Android Gradle Plugin 到 `9.1.0`。
- 优先使用 Android Studio Upgrade Assistant 做一次预迁移，再手工清理残留。

2. 迁移到 built-in Kotlin
- 删除 `org.jetbrains.kotlin.android` 或 `kotlin-android` 插件。
- 删除或迁移依赖该插件的扩展配置。
- 如必须临时保底，可短期设置：
  - `android.newDsl=false`
  - `android.builtInKotlin=false`
- 但这两个开关只能作为短期过渡，PR 中必须说明移除计划；目标是如果第三方插件或 kapt 还未完成迁移，可在本阶段末暂时保留 opt-out，但必须记录移除条件。

3. 处理 Kotlin 注解处理器
- 检查 `kapt` 是否仍是必须。
- 若 Glide 等依赖必须使用 `kapt`，先保留，但在 PR 中记录后续替换或最小化范围。

4. 启用 Compose 基础设施
- 在 `app` 模块开启 `buildFeatures.compose = true`。
- 按 Kotlin 2.x 官方方式引入 `org.jetbrains.kotlin.plugin.compose`。
- 引入 BOM 与最小依赖：
  - `ui`
  - `ui-tooling-preview`
  - `material3`
  - `activity-compose`
  - `lifecycle-runtime-compose`
- Debug 增加 `ui-tooling`
- AndroidTest 增加 `ui-test-junit4`

5. 创建最小可运行 Compose 入口
- 先不要改现有主页面。
- 新建一个内部调试用 Compose 页面或预览组件，用来验证 Compose 编译链路、主题和预览可用。

## 必改文件

- 根构建文件
- Wrapper 文件
- `app/build.gradle`
- 可能新增 `compose` 主题与示例文件

## 不要在本阶段做的事

- 不迁移 `MediaActivity`
- 不迁移 `BlScanActivity`
- 不替换权限流程
- 不切到 Navigation Compose

## 最低验证

- `./gradlew.bat :app:assembleDebug`
- `./gradlew.bat :app:compileDebugKotlin`
- `./gradlew.bat :app:connectedDebugAndroidTest` 或至少跑 Compose 相关单元/UI 烟测

## Exit Criteria

- AGP 已是 `9.1.0`，Gradle 已是 `9.3.1`。
- 不再依赖 `kotlin-android` 插件。
- `app` 模块可成功编译和运行最小 Compose UI。
- Android Studio Preview 能正常打开示例组件。

## 小艺 Review 重点

- 是否已经升级到 AGP 9.1，并明确记录 new DSL / built-in Kotlin 的临时兼容状态。
- Compose 插件是否按 Kotlin 2.x 新方式接入，没有保留旧 `composeOptions.kotlinCompilerExtensionVersion` 兼容写法。
- 变更是否只完成“启用能力”，没有提前重写业务页面。



