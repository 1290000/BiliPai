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
