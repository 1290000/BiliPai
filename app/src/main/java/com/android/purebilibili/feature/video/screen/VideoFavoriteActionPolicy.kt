package com.android.purebilibili.feature.video.screen

internal enum class VideoFavoriteEntryPoint {
    FullscreenOverlay,
    DetailActionRow,
    BottomInputBar
}

internal enum class VideoFavoriteAction {
    /** 直接默认收藏夹开关（兼容旧路径）。 */
    ToggleFavorite,
    /** 打开收藏夹选择，支持多选到自己的收藏夹。 */
    OpenFavoriteFolders,
}

/**
 * 长按始终打开收藏夹选择。
 * 点按按设置分流：开启快捷收藏时进默认收藏夹，否则打开收藏夹选择。
 */
internal fun resolveVideoFavoriteAction(
    entryPoint: VideoFavoriteEntryPoint,
    isLongPress: Boolean,
    quickSaveDefaultFolder: Boolean,
): VideoFavoriteAction {
    return when {
        isLongPress -> VideoFavoriteAction.OpenFavoriteFolders
        quickSaveDefaultFolder -> VideoFavoriteAction.ToggleFavorite
        else -> when (entryPoint) {
            VideoFavoriteEntryPoint.FullscreenOverlay,
            VideoFavoriteEntryPoint.DetailActionRow,
            VideoFavoriteEntryPoint.BottomInputBar -> VideoFavoriteAction.OpenFavoriteFolders
        }
    }
}
