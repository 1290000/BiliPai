package com.android.purebilibili.feature.audio.screen

internal const val MUSIC_PLAYER_EXPANDED_WIDTH_DP = 600
internal const val MUSIC_PLAYER_COMPACT_DOCK_BOTTOM_PADDING_DP = 70

internal fun resolveMusicPlayerPageTabs(): List<String> = listOf("封面", "歌词")

internal enum class MusicCoverStyle {
    APPLE_MUSIC_CARD,
    APPLE_MUSIC_SQUARE,
    TURNTABLE
}

internal fun resolveNextCoverStyle(current: MusicCoverStyle): MusicCoverStyle = when (current) {
    MusicCoverStyle.APPLE_MUSIC_CARD -> MusicCoverStyle.APPLE_MUSIC_SQUARE
    MusicCoverStyle.APPLE_MUSIC_SQUARE -> MusicCoverStyle.TURNTABLE
    MusicCoverStyle.TURNTABLE -> MusicCoverStyle.APPLE_MUSIC_CARD
}

internal fun resolveCoverStyleLabel(style: MusicCoverStyle): String = when (style) {
    MusicCoverStyle.APPLE_MUSIC_CARD -> "Apple Music 宽屏卡片"
    MusicCoverStyle.APPLE_MUSIC_SQUARE -> "Apple Music 方形专辑"
    MusicCoverStyle.TURNTABLE -> "经典黑胶唱盘"
}

internal fun resolveCoverStyleShortLabel(style: MusicCoverStyle): String = when (style) {
    MusicCoverStyle.APPLE_MUSIC_CARD -> "宽屏"
    MusicCoverStyle.APPLE_MUSIC_SQUARE -> "方图"
    MusicCoverStyle.TURNTABLE -> "转盘"
}

internal enum class MusicPlayerLayout {
    COMPACT_PAGER,
    EXPANDED_SPLIT,
    PIP_ARTWORK
}

internal enum class ExpandedRightPaneTab {
    LYRICS,
    QUEUE
}

internal const val LARGE_SCREEN_MAX_CONTENT_WIDTH_DP = 1200

internal fun resolveLargeScreenGutterDp(widthDp: Int): Int =
    if (widthDp >= 840) 48 else 28

internal fun resolveLargeScreenHorizontalPaddingDp(widthDp: Int): Int =
    if (widthDp >= 840) 48 else 24

internal fun resolveMusicPlayerLayout(
    widthDp: Int,
    isInPipMode: Boolean
): MusicPlayerLayout = when {
    isInPipMode -> MusicPlayerLayout.PIP_ARTWORK
    widthDp >= MUSIC_PLAYER_EXPANDED_WIDTH_DP -> MusicPlayerLayout.EXPANDED_SPLIT
    else -> MusicPlayerLayout.COMPACT_PAGER
}

internal fun resolveMusicArtworkSizeDp(
    availableWidthDp: Int,
    availableHeightDp: Int,
    layout: MusicPlayerLayout
): Int {
    if (availableWidthDp <= 0 || availableHeightDp <= 0) return 0
    return when (layout) {
        MusicPlayerLayout.COMPACT_PAGER -> minOf(
            (availableWidthDp - 48).coerceAtLeast(0),
            (availableHeightDp - 80).coerceAtLeast(0),
            // 288 为两行标题 + 控制区留出空间，避免矮屏滚动后裁切底部 dock
            288
        )
        MusicPlayerLayout.EXPANDED_SPLIT -> minOf(
            (availableWidthDp / 2 - 48).coerceAtLeast(0),
            (availableHeightDp - 140).coerceAtLeast(0),
            360
        )
        MusicPlayerLayout.PIP_ARTWORK -> minOf(availableWidthDp, availableHeightDp)
    }
}
