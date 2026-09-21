package com.android.purebilibili.feature.message

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.feature.home.components.cards.VideoCardAmbientDrawSpec
import com.android.purebilibili.feature.home.components.cards.resolveVideoCardAmbientDrawSpec
import com.android.purebilibili.feature.home.components.cards.WallpaperPalette

internal fun resolveMessageGlassYFraction(
    positionY: Float,
    screenHeightPx: Float,
): Float {
    if (!positionY.isFinite() || !screenHeightPx.isFinite() || screenHeightPx <= 0f) {
        return 0.5f
    }
    return (positionY / screenHeightPx).coerceIn(0f, 1f)
}

internal fun resolveMessageBubbleFallbackContainerColor(
    isOwnMessage: Boolean,
    primary: Color,
    surfaceVariant: Color,
): Color = if (isOwnMessage) primary else surfaceVariant

internal fun resolveMessageBubbleFallbackContentColor(
    isOwnMessage: Boolean,
    onPrimary: Color,
    onSurfaceVariant: Color,
): Color = if (isOwnMessage) onPrimary else onSurfaceVariant

internal fun resolveMessageGlassDrawSpec(
    wallpaperPalette: WallpaperPalette?,
    yFraction: Float,
    isDarkTheme: Boolean,
    defaultContainerColor: Color,
    defaultBorderColor: Color,
    wallpaperVisible: Boolean,
    dynamicTintEnabled: Boolean,
): VideoCardAmbientDrawSpec = resolveVideoCardAmbientDrawSpec(
    wallpaperPalette = wallpaperPalette,
    yFraction = yFraction,
    coverTint = null,
    wallpaperTintEnabled = wallpaperVisible,
    isDarkTheme = isDarkTheme,
    defaultContainerColor = defaultContainerColor,
    defaultBorderColor = defaultBorderColor,
    frostedGlassEnabled = dynamicTintEnabled,
)
