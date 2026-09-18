package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.RelationStatData
import com.android.purebilibili.data.model.response.UpStatData

internal data class SpaceHeaderMetricItem(
    val label: String,
    val value: Long
)

internal fun resolveSpaceHeaderMetricItems(
    relationStat: RelationStatData?,
    upStat: UpStatData?
): List<SpaceHeaderMetricItem> {
    return listOf(
        SpaceHeaderMetricItem("粉丝", relationStat?.follower?.toLong() ?: 0L),
        SpaceHeaderMetricItem("关注", relationStat?.following?.toLong() ?: 0L),
        SpaceHeaderMetricItem("获赞", upStat?.likes ?: 0L)
    )
}

/**
 * Resolves the action button label on the UP space header:
 * - isOwner: "编辑资料"
 * - isFollowed: "已关注"
 * - otherwise: "关注"
 */
internal fun resolveSpaceFollowActionLabel(
    isOwner: Boolean,
    relationStatus: Int = 0,
    isFollowed: Boolean = false,
): String {
    if (isOwner) return "编辑资料"
    val followed = isFollowed || (relationStatus in setOf(1, 2, 4, 6, -10))
    return if (followed) "已关注" else "关注"
}

internal const val SPACE_PINNED_TOP_CHROME_FADE_RANGE_PX = 120

/** 0 at rest over the banner, 1 after the header has scrolled under the pinned chrome. */
internal fun resolveSpacePinnedTopChromeScrim(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    fadeRangePx: Int = SPACE_PINNED_TOP_CHROME_FADE_RANGE_PX,
): Float {
    if (firstVisibleItemIndex > 0) return 1f
    if (fadeRangePx <= 0) return 0f
    return (firstVisibleItemScrollOffset.toFloat() / fadeRangePx).coerceIn(0f, 1f)
}

internal fun resolveSpaceBannerAlignment(dy: Float): androidx.compose.ui.Alignment {
    return androidx.compose.ui.BiasAlignment(0f, dy.coerceIn(-1f, 1f))
}

internal fun resolveSpaceBannerColorFilter(
    isLight: Boolean,
    hasFilter: Boolean = true
): androidx.compose.ui.graphics.ColorFilter? {
    if (!hasFilter) return null
    return if (isLight) {
        androidx.compose.ui.graphics.ColorFilter.tint(
            androidx.compose.ui.graphics.Color(0x5DFFFFFF),
            androidx.compose.ui.graphics.BlendMode.Lighten
        )
    } else {
        androidx.compose.ui.graphics.ColorFilter.tint(
            androidx.compose.ui.graphics.Color(0x8D000000),
            androidx.compose.ui.graphics.BlendMode.Darken
        )
    }
}
