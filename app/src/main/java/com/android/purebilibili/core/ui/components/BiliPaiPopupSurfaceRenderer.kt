package com.android.purebilibili.core.ui.components

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppPopupSurfaceRenderer
import com.android.purebilibili.core.ui.AppPopupSurfaceType
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.core.ui.blur.LocalFloatingChromeBackdrop
import com.android.purebilibili.core.ui.performance.isLowBlurBudgetForced
import com.android.purebilibili.feature.home.components.BottomBarMatchedReusableLiquidDock

object BiliPaiPopupSurfaceRenderer : AppPopupSurfaceRenderer {
    @Composable
    override fun Render(
        type: AppPopupSurfaceType,
        modifier: Modifier,
        shape: Shape,
        containerColor: Color,
        contentColor: Color,
        tonalElevation: Dp,
        content: @Composable () -> Unit,
    ) {
        val glassEnabled = LocalAppThemeConfig.current.liquidGlassEnabled &&
            !isLowBlurBudgetForced()
        // 对话框 (DIALOG) 与弹出菜单 (MENU) 处于独立的系统 Window 中，
        // 无法跨 Window 读取主 Activity 的 LayerBackdrop。
        // 此类独立 Window 表面安全回退为标准主题容器，避免因空背景采样导致白/黑死色。
        if (type == AppPopupSurfaceType.DIALOG || type == AppPopupSurfaceType.MENU) {
            Surface(
                modifier = modifier,
                shape = shape,
                color = containerColor,
                contentColor = contentColor,
                tonalElevation = tonalElevation,
                content = content,
            )
            return
        }
        BottomBarMatchedReusableLiquidDock(
            shape = shape,
            modifier = modifier,
            backdrop = LocalFloatingChromeBackdrop.current,
            liquidGlassEffectsEnabled = glassEnabled,
            reuseEnabled = true,
            useNeutralLiquidContainer = true,
            drawShellLens = glassEnabled,
        ) { liquidChromeActive ->
            Surface(
                shape = shape,
                color = if (liquidChromeActive) Color.Transparent else containerColor,
                contentColor = contentColor,
                tonalElevation = if (liquidChromeActive) 0.dp else tonalElevation,
                content = content,
            )
        }
    }
}
