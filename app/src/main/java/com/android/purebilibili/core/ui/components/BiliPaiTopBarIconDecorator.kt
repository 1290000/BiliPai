package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.purebilibili.feature.home.components.BottomBarMatchedReusableLiquidDock
import com.android.purebilibili.feature.home.components.resolveFloatingDockGeometryScale

/** Same circular dock material used by floating chrome, without sampling its own icon. */
object BiliPaiTopBarIconDecorator : AppTopBarIconDecorator {
    @Composable
    override fun Render(modifier: Modifier, enabled: Boolean, content: @Composable (Modifier) -> Unit) {
        BottomBarMatchedReusableLiquidDock(
            shape = CircleShape,
            modifier = modifier.size(48.dp),
            backdrop = LocalAppTopBarIconBackdrop.current,
            reuseEnabled = true,
            elasticFeedbackEnabled = enabled,
            useNeutralLiquidContainer = true,
            shellLensIntensity = resolveFloatingDockGeometryScale(48f),
        ) { _ ->
            Box(Modifier.size(48.dp)) { content(Modifier.matchParentSize()) }
        }
    }
}
