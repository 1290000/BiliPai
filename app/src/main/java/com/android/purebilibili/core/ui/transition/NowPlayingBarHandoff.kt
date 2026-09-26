package com.android.purebilibili.core.ui.transition

/**
 * 小横条（听视频 NowPlaying 条）与视频共享过渡的返回交接协议（纯 Kotlin，无 Compose）。
 *
 * 单一握手状态替代原先散装在调用点的四个布尔
 * （isReturningFromDetail / returningDetailBvid / isSharedTransitionRunning /
 * isSharedTransitionSourceOwner）。真条只向协议问一个问题：「此刻显现多少」。
 *
 * 展开方向（点击小横条 → 详情页）的 bounds/快照记录协议由
 * [CardPositionManager.recordVideoCardPosition] 承担，不在本协议内。
 */
sealed interface NowPlayingBarHandoffState {

    /** 无返回会话；真条完全自主。 */
    data object Idle : NowPlayingBarHandoffState

    /**
     * 详情 → 小横条的返回 morph 进行中。
     *
     * @param targetBvid 返回目标视频；null 表示无明确目标（任一来源条都让位）。
     * @param isSourceOwner 共享过渡宿主当前是否持有源像素。
     */
    data class Returning(
        val targetBvid: String?,
        val isSourceOwner: Boolean,
    ) : NowPlayingBarHandoffState
}

/**
 * 返回 morph 中真条的显现度：0 = 飞行 chrome 持有源像素（真条隐藏），
 * 1 = 真条自主显现。落定瞬间真条直接以此完全显现落位，不再播第二次进场
 * （唯一几何时间轴原则）；morph 期间几何不收缩，隐藏只发生在 alpha 层。
 */
internal fun resolveNowPlayingBarReturnVisibility(
    handoff: NowPlayingBarHandoffState,
    currentBvid: String,
): Float {
    val returning = handoff as? NowPlayingBarHandoffState.Returning
        ?: return 1f
    if (currentBvid.isBlank()) return 1f
    val isReturnTarget = returning.targetBvid.isNullOrBlank() ||
        returning.targetBvid == currentBvid
    if (!returning.isSourceOwner || !isReturnTarget) return 1f
    return 0f
}

/**
 * 小横条飞行壳填充窗口（settle 0→[NOW_PLAYING_BAR_SHELL_REVEAL_END]）。
 *
 * 小横条源没有视频图层：若等 MEDIA_RETURN（0.82→0.98）窗口才交接，飞行壳会长时间是
 * 黑壳。因此冻结快照在返回 settle 的前段就接管壳内容，之后保持主导到落位。
 * 旧的 `/0.18f` 字面量收口于此；settle 语义与 [resolveVideoCardReturnSettleFromMorphDepth] 同源。
 */
internal const val NOW_PLAYING_BAR_SHELL_REVEAL_START = 0f
internal const val NOW_PLAYING_BAR_SHELL_REVEAL_END = 0.18f

/** 飞行壳中小横条冻结快照的显现进度（0 刚开始缩回，1 已完成壳填充）。 */
internal fun resolveNowPlayingBarSourceChromeReveal(
    morphDepthProgress: Float,
): Float = resolveVideoCardTimelineWindowProgress(
    progress = resolveVideoCardReturnSettleFromMorphDepth(morphDepthProgress),
    start = NOW_PLAYING_BAR_SHELL_REVEAL_START,
    end = NOW_PLAYING_BAR_SHELL_REVEAL_END,
)
