package com.android.purebilibili.feature.video.ui.section

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.core.ui.blur.rememberChromeBackdropSource
import com.android.purebilibili.feature.video.ui.overlay.playerControlGlassSurface

/** Capture only the detail surface below the actions, never the glass or its children. */
@Composable
internal fun DetailActionGlassDock(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    if (!LocalAppThemeConfig.current.liquidGlassEnabled) {
        Box(modifier, content = content)
        return
    }
    val source = rememberChromeBackdropSource()
    val surface = MaterialTheme.colorScheme.surface
    Box(modifier) {
        Box(Modifier.matchParentSize().then(source.modifier).background(surface))
        Box(
            modifier = Modifier.playerControlGlassSurface(
                backdrop = source.backdrop,
                shape = RoundedCornerShape(28.dp),
                darkTheme = surface.luminance() < 0.5f,
            ),
            content = content,
        )
    }
}
