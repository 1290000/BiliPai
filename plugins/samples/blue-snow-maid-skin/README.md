# 蓝雪女仆皮肤

数据型 `.bpskin` 示例，提供首页顶部背景、底栏饰面和颜色，不包含自定义导航图标或动效。

## 导入

在插件中心选择 [`blue-snow-maid.bpskin`](blue-snow-maid.bpskin)，预览后点击“保存并启用”。也可从应用内皮肤目录选择“蓝雪女仆”。

## 资源

| 文件 | Manifest 字段 |
|---|---|
| `assets/top_atmosphere.png` | `assets.topAtmosphere` |
| `assets/bottom_trim.png` | `assets.bottomBarTrim` |

字段及渲染范围见 [BPSkin 开发规范](../../../docs/BPSKIN_DEVELOPMENT.md)。

## 打包

从本目录执行以下命令，仅打包资源，无需 Android SDK：

```bash
../../../gradlew -p . packageBpSkin
```

输出：`build/distributions/blue-snow-maid.bpskin`。打包后检查 ZIP 根目录直接包含 `skin-manifest.json` 和 `assets/`。

## 授权

包声明 `containsOfficialAssets=true`、`communityShareable=false`。含官方角色主题素材，仅供应用内个人使用，不可作为社区皮肤包分发。
