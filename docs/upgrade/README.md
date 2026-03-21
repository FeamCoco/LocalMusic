# LocalMusic Upgrade Runbook

本目录是 `LocalMusic` 从当前 View/XML + AGP 7.1.1 工程，逐步迁移到 Jetpack Compose 最新稳定线与 AGP 9.1 的执行手册。

> 2026-03-21 更新：
> 当前仓库已经完成了大部分构建升级与首批 Compose 页面落地，本文中的“当前项目现状”已不再准确。
> 从现在开始，迁移控制请以 `docs/plans/2026-03-21-compose-migration-agent-plan.md` 为主；本目录继续保留为历史阶段文档与审查依据。

## 目标基线

截至 2026-03-15，本次升级的目标版本固定为：

- AGP `9.1.0`
- Gradle `9.3.1`
- JDK `17`
- Built-in Kotlin: `开启`
- Compose BOM: `2026.02.01`
- Compose UI/Foundation/Runtime/Material: `1.10.5`
- Compose Material3: `1.4.0`

说明：

- AGP 9.1.0 是截至 2026-03-15 的最新稳定版，本轮基线已更新到该版本。
- Compose 采用 BOM 统一管理；Material3 不跟随 BOM，单独固定版本。

## 当前项目现状

- 根构建仍使用 `buildscript` 风格。
- App 模块仍是 ViewBinding + XML 页面。
- `easypermissions` 为本地旧模块。
- 权限、存储、蓝牙流程仍是旧 Android 模型。
- 当前未启用 Compose，也未引入 Compose 编译插件。

## 执行流转

每一个阶段都必须严格按下面的顺序走，不允许并行推进多个阶段：

1. 悠悠按当前阶段文档实施代码变更。
2. 悠悠提交单独 PR，PR 标题格式建议为：`upgrade(step-N): <phase name>`。
3. 小艺按 [review-checklist.md](/C:/Users/pp/Project/Github/LocalMusic/docs/upgrade/review-checklist.md) review。
4. 只有当小艺明确给出“通过，可进入下一阶段”时，悠悠才能开始下一阶段。
5. 如果 review 发现问题，先修完当前阶段并重新 review，禁止跳步。

## 阶段列表

1. [01-build-baseline.md](/C:/Users/pp/Project/Github/LocalMusic/docs/upgrade/01-build-baseline.md)
2. [02-agp9-and-compose-bootstrap.md](/C:/Users/pp/Project/Github/LocalMusic/docs/upgrade/02-agp9-and-compose-bootstrap.md)
3. [03-compose-design-system-and-hybrid-screens.md](/C:/Users/pp/Project/Github/LocalMusic/docs/upgrade/03-compose-design-system-and-hybrid-screens.md)
4. [04-screen-by-screen-migration.md](/C:/Users/pp/Project/Github/LocalMusic/docs/upgrade/04-screen-by-screen-migration.md)
5. [05-compose-only-finalization.md](/C:/Users/pp/Project/Github/LocalMusic/docs/upgrade/05-compose-only-finalization.md)

## 升级原则

- 一次只改一层：构建链路、依赖、页面迁移、清理收尾分开做。
- 先让旧页面继续可运行，再引入 Compose；禁止“大爆炸式重写”。
- 新功能和新增页面从阶段 2 开始必须优先用 Compose。
- 在阶段 4 之前，允许 Views 与 Compose 共存。
- 在阶段 5 之前，不切换到 Navigation Compose。
- 每阶段都必须保留可回滚点。

## 官方依据

- AGP 9.1.0 release notes: <https://developer.android.com/tools/releases/gradle-plugin#9-1-0>
- Built-in Kotlin migration: <https://developer.android.com/build/migrate-to-built-in-kotlin>
- Compose setup and compiler: <https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler>
- Compose BOM: <https://developer.android.com/develop/ui/compose/bom>
- Compose BOM mapping: <https://developer.android.com/develop/ui/compose/bom/bom-mapping>
- Compose migration strategy: <https://developer.android.com/develop/ui/compose/migrate/strategy>
- Compose interoperability APIs: <https://developer.android.com/develop/ui/compose/migrate/interoperability-apis>
- XML theme to Compose theme migration: <https://developer.android.com/develop/ui/compose/designsystems/views-to-compose>


