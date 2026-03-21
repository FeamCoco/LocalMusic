# Step 3: Compose Design System And Hybrid Screens

执行人：悠悠  
Reviewer：小艺

## 目标

在保留现有页面结构的前提下，先搭好 Compose 主题、基础组件和混合页面承载方式。

本阶段结束时应达到：

- Compose 主题与现有 XML 主题对齐
- 至少一个低风险页面或局部组件改为 Compose
- Views 与 Compose 可以稳定共存

## 推荐迁移策略

按照官方建议，先做增量迁移：

1. 新页面优先用 Compose
2. 先抽通用组件
3. 再按页面逐个替换

这个项目现状更适合“混合期较长”的方案，不适合一次性重写。

## 具体步骤

1. 建立 Compose 主题层
- 以现有 XML 颜色、字体、shape 为唯一视觉来源。
- 新建 `theme/` 目录，定义 `ColorScheme`、`Typography`、`Shapes`。
- 优先用 Material3；如果旧视觉暂时无法完全映射，可用包装层适配，不要直接在业务代码里散落颜色值。

2. 建立通用 Compose 组件
- 至少抽出以下可复用组件：
  - 顶部栏
  - 歌曲列表 item
  - 空状态
  - 权限提示卡片
  - 加载态

3. 建立混合承载方式
- Activity 仍可保持旧结构，但新局部内容用 `ComposeView` 嵌入。
- 如果某一整个页面适合先迁移，可保留 Fragment/Activity 外壳，内部 `setContent {}`。

4. 选择低风险目标先试点
- 推荐顺序：
  - `ErrorActivity`
  - `ChooseStyleDialog`
  - 静态列表 item
  - 非主流程弹窗
- 暂不建议先改 `MediaActivity` 主播放页。

## 不要在本阶段做的事

- 不切 Navigation Compose
- 不重写媒体播放、服务绑定或权限主流程
- 不处理蓝牙扫描重构

## 最低验证

- 低风险页面可打开、关闭、横竖屏切换不崩溃
- 主题颜色、字体、圆角与原 XML 页面基本一致
- Compose 预览可使用

## Exit Criteria

- 已有 Compose 主题层与基础组件层。
- 至少 1 个低风险页面或组件上线到主分支。
- 混合页面无明显生命周期问题。

## 小艺 Review 重点

- 主题是否来自现有 XML，而不是重新发明一套视觉。
- 组件是否具备复用性，而不是直接把老页面逻辑原样搬进一个大 Composable。
- Compose 和 View 的边界是否清晰。
