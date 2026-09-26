package com.android.purebilibili.feature.audio.screen

import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import com.android.purebilibili.core.ui.motion.emphasizedEnterTween
import com.android.purebilibili.core.ui.motion.emphasizedExitTween

/**
 * 小横条（听视频 NowPlaying 条）presence 出入场动效 token。
 *
 * 出入场采用非对称节奏（enter 减速落定 / exit 加速离场），与 MD3 emphasized 曲线体系一致；
 * 可逆手势 morph（dock merge/search）保持 easeInOut tween——这是官方对跟随手势的可逆 morph
 * 的推荐形态。所有档位在此收口，禁止在调用点散落字面量。
 */
internal const val AUDIO_NOW_PLAYING_PRESENCE_ENTER_DURATION_MILLIS = 300
internal const val AUDIO_NOW_PLAYING_PRESENCE_EXIT_DURATION_MILLIS = 220
internal const val AUDIO_NOW_PLAYING_PRESENCE_REDUCED_MOTION_DURATION_MILLIS = 160

/** Linked dock merge/search morph 档位，统一收口到小横条动效 token。 */
internal const val LINKED_DOCK_MERGE_DURATION_MILLIS = 280
internal const val LINKED_DOCK_SEARCH_DURATION_MILLIS = 240

/** 会话激活（进场）时 presence 进度动画规格。 */
internal fun resolveAudioNowPlayingPresenceAnimationSpec(
    active: Boolean,
    reduceMotion: Boolean,
): TweenSpec<Float> = when {
    reduceMotion -> tween(AUDIO_NOW_PLAYING_PRESENCE_REDUCED_MOTION_DURATION_MILLIS)
    active -> emphasizedEnterTween(AUDIO_NOW_PLAYING_PRESENCE_ENTER_DURATION_MILLIS)
    else -> emphasizedExitTween(AUDIO_NOW_PLAYING_PRESENCE_EXIT_DURATION_MILLIS)
}

/**
 * presence alpha 窗口：几何先展开，alpha 在后半段跟上，避免空壳提前可见。
 * 与 [AudioNowPlayingBarRowMetrics] 体系的阈值窗口风格一致。
 */
internal fun resolveAudioNowPlayingPresenceAlpha(progress: Float): Float =
    ((progress.coerceIn(0f, 1f) - 0.25f) / 0.75f).coerceIn(0f, 1f)

/** presence 几何乘数：驱动 audio 槽宽度（Compact）或高度（Expanded）的展开量。 */
internal fun resolveAudioNowPlayingPresenceGeometryFactor(progress: Float): Float =
    progress.coerceIn(0f, 1f)
