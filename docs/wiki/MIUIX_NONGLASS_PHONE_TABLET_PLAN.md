# Miuix 无玻璃适配计划（手机 + 平板）

> 日期：2026-09-15  
> 范围：**仅 `AppUiStyle.MIUIX` 且液态玻璃关闭**（`isMiuixNonGlassEnabled`）  
> 非范围：Material 3、液态玻璃开启、插件 API、公开路由  
> 上游：`top.yukonga.miuix.kmp` `0.9.4-4f86de92-SNAPSHOT`；规范见 [UI 设计规范 1.1](../wiki/ui-design/README.md)、[Miuix 对齐记录](../wiki/MIUIX_ALIGNMENT.md)

本计划按当前窗口尺寸类排版，不引入 `isTablet` 设备判断。手机 Compact、平板 Medium/Expanded、分屏与折叠外屏走同一套关玻璃分支。

## 1. 目标

关玻璃后的 Miuix 应像 HyperOS 实色界面：浅灰画布 `#F7F7F7`、白卡片、官方 `TabRow` / `Button` / `Card` / `NavigationBar`，而不是把液态分段条去掉背景后硬拉满。

官方页签几何（审计已核对）：

| Token | 上游 |
|---|---|
| `TabRowHeight` | 42dp（触摸高 ≥ 48dp） |
| `TabRowCornerRadius` | 12dp |
| `itemSpacing` | 9dp |
| 对齐 | 内容宽、起排；溢出横滑 |

平板与手机差异只在**可用宽度**：窄屏横滑，宽屏仍内容宽左对齐，禁止均分拉满。

## 2. 现状

已对齐：色板与字阶、设置 Preference、搜索 `InputField`、无玻璃 Button/Chip/FAB/Snackbar、设置 Scaffold、官方 NavigationBar（贴底模式）。

未对齐或会在平板放大失真：

| 面 | 手机 Compact | 平板 / 横屏 |
|---|---|---|
| 空间投稿二级分类 | 少量 tab 时 `Row.weight` 仍会拉宽 | 最长合集名定宽，短标签被撑开（已在本期修） |
| 空间主 Tab | 尚可 | 需确认关玻璃不走均分 |
| 首页顶部分类 | 自研/液态条降级 | 宽屏均分风险 |
| 搜索/动态/列表筛选 Chip | 部分已走官方 Chip | 宽屏可能拉满 |
| 底栏 | 默认悬浮胶囊，非官方贴底 | 横屏底栏过宽、侧栏已接 `NavigationRail` |
| Dialog / Sheet | Material 内核 + Miuix 色 | 平板过宽，未用 `WindowDialog` |
| 播放器面板 | 部分 Preference | 关玻璃 Folme/官方列表不完整 |
| 按压缩放 | 部分入口缺失 | 大触摸面更明显 |

## 3. 分期

### P0 空间与同类页签（手机 + 平板）

门控：`isMiuixNonGlassEnabled`。

1. **空间投稿二级分类**（本期）  
   内容宽、左对齐、最短 48dp、溢出横滑；短标签不再继承最长合集名。玻璃分支仍用原 `itemWidth` 液态条。
2. **空间主 Tab「主页 / 动态 / 投稿」**  
   关玻璃走官方 `TabRow` 或 `CONTENT` 宽；2–3 项在平板上不得 `weight(1f)` 拉满。
3. **搜索结果分类、动态分区、历史筛选、番剧时间表**  
   凡 `AppNativeTabRow` + `minTabWidth = 最长标题` 的关玻璃调用，改为 `CONTENT` + 横滑。

验收：

- 手机 360dp：3 个以内紧凑；更多可滑，标签完整可读
- 平板 840/1280dp：短标签紧挨，长合集名只加宽自己，右侧留白或可滑
- 分屏变窄：自动横滑
- MD3 / 开玻璃：像素级不变

### P1 外壳（手机底栏 / 平板 Rail）

1. 关玻璃 + 关闭悬浮底栏：继续官方 `NavigationBar`（52dp + 发丝分割）。
2. 关玻璃 + 默认悬浮胶囊：保持现有 Dock，但宽度随当前窗口，横屏不要拉成通栏。
3. 平板 Expanded：关玻璃侧栏已是官方 `NavigationRail`；检查折叠/展开与底栏互斥。
4. 二级页 `AppTopBar`：官方 `TopAppBar` 52dp；大标题页 102dp。首页顶栏关玻璃不要回退成无规范自定义 Row。

### P2 列表、卡片、空间内容

1. 信息流 / 空间投稿网格：关玻璃卡片走 `AppMiuixCard`（16dp squircle）；列数仍用当前窗口尺寸类。
2. 空间头图：沿用已落地的 135dp 桌面短条（宽窗口）与手机 1125:396。
3. 列表行、空状态、页脚：`BasicComponent` 间距，不要 Material 默认 16/8 混用。
4. 筛选 Chip：关玻璃官方 Chip（10/36/12 已有批次）；平板上 Chip 行左对齐横滑，不要均分。

### P3 弹层与播放器（关玻璃）

1. 确认/警告：评估 `WindowDialog`（28dp、folme 缩放），平板限宽 360–420dp。
2. 底栏弹层：官方底 sheet 弹簧；平板用居中对话框或侧板，避免通栏抽屉。
3. 播放器设置面板：关玻璃继续 Preference；不要液态面板残件。
4. 本阶段不改播放内核、方向锁、画中画。

## 4. 手机 / 平板共用规则

- 布局只读当前窗口宽高尺寸类，不用设备类型。
- 关玻璃页签：**内容宽 + 起排 + 9dp 级间距 + 溢出横滑**。
- 触摸高 ≥ 48dp；视觉药丸可以 42dp。
- 宽屏限制阅读宽度，不把短分段控件拉到 1280dp。
- 折叠外屏按 Compact 窗口处理；内屏按当前窗口，不要用内屏尺寸锁外屏布局。

## 5. 验证

- 结构/policy 测试：`isMiuixNonGlassEnabled` 分支、页签 CONTENT 宽、玻璃分支参数不变。
- 真机/模拟器（人工）：MIUIX + 关玻璃 × 浅/深色 × 手机竖屏、手机横屏、平板 800/1280、分屏。
- 回归：MD3、MIUIX 开玻璃各抽一页页签与底栏。
- 未授权不跑 Gradle 编译或全量测试。

## 6. 非目标

- 改液态玻璃材质、Backdrop、透镜
- 改 Material 3 视觉
- 改设置项、路由、插件 API
- 把 Folme 推进所有历史页面（仅新触摸面与 P3 弹层）
