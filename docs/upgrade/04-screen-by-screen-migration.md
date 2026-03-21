# Step 4: Screen-By-Screen Migration

执行人：悠悠  
Reviewer：小艺

## 目标

把核心 UI 按页面逐个迁移到 Compose，但保留现有业务架构、服务层和导航壳，避免一次性重构全部逻辑。

## 页面优先级

按风险从低到高，建议顺序如下：

1. `ErrorActivity`
2. `ChooseStyleDialog`
3. `MediaHeadFragment`
4. 播放队列与弹窗
5. `BlScanActivity`
6. `MediaActivity`

说明：

- `MediaActivity` 是主播放页，状态、服务连接、ViewPager、列表和权限逻辑都集中在这里，必须最后处理。
- `BlScanActivity` 涉及蓝牙扫描和权限切换，也不适合过早改。

## 每个页面统一执行模板

1. 保留现有 Presenter / Model / Service，不先改架构。
2. 先把页面状态整理成 UI state。
3. 用 Compose 写页面壳和组件。
4. 通过参数把状态与事件注入 UI。
5. 若仍需旧 View 或系统组件，使用 `AndroidView` / `AndroidViewBinding`。
6. 页面迁移完成后再删对应 XML 与 Adapter，不要先删。

## `MediaActivity` 专项要求

- 先拆成几个 Compose 区域，不要一步替掉整页：
  - 顶部栏
  - 专辑区
  - 进度与播放控制区
  - 播放队列 BottomSheet
  - 倒计时面板
- `MediaBrowserCompat`、`MediaControllerCompat`、`MediaService` 交互保留原样。
- 先完成 UI 替换，再评估是否把 ViewPager / RecyclerView 改成 `HorizontalPager` / `LazyColumn`。

## `BlScanActivity` 专项要求

- 页面 UI 可迁移，但权限和蓝牙 API 的升级要与 UI 替换分开提交。
- 先把列表、按钮、状态栏和空态改为 Compose。
- 蓝牙扫描、广播接收器、配对逻辑继续留在原层。

## 权限与存储专项要求

这一阶段可以顺手迁移以下能力，但一项一 PR：

- `startActivityForResult/onActivityResult` -> Activity Result API
- 旧存储权限 -> 新媒体访问/文档树流程
- 旧蓝牙位置权限 -> 新蓝牙权限模型
- 本地 `easypermissions` -> 自研权限层或 AndroidX/Activity Result 方案

不要把上面四项和整页 UI 重写混在一个 PR。

## 最低验证

每迁移一个页面，都至少验证：

- 首次进入
- 返回
- 旋转屏幕
- 进程恢复或最小化后返回
- 相关权限流程
- 相关服务连接恢复

## Exit Criteria

- 所有用户可见主页面都已有 Compose 版本。
- XML 页面只剩少量兼容壳或尚未清理的遗留文件。
- 核心交互路径在 Compose 页面下稳定可用。

## 小艺 Review 重点

- 是否按页面拆 PR，而不是一个 PR 同时改多个核心页面。
- UI state 是否清晰，业务层是否没有被无关重写。
- 每移除一个 XML/Adapter，是否有对应 Compose 替代。
