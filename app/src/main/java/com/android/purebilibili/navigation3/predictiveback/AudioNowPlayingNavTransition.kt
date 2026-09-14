package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import com.android.purebilibili.core.ui.motion.AppMotionEasing
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.navDirectionalTransition
import top.yukonga.miuix.kmp.nav.transition.navGraphicsTransition

private const val AUDIO_NOW_PLAYING_TRANSITION_DURATION_MILLIS = 320
private const val AUDIO_NOW_PLAYING_MIN_SCALE = 0.84f
private const val AUDIO_NOW_PLAYING_TRANSLATION_FRACTION = 0.08f

private val AudioNowPlayingMotion = NavMotion(
    commit = NavSettleSpec.Tween(
        durationMillis = AUDIO_NOW_PLAYING_TRANSITION_DURATION_MILLIS,
        easing = AppMotionEasing.Continuity,
    ),
    cancel = NavSettleSpec.Tween(
        durationMillis = AUDIO_NOW_PLAYING_TRANSITION_DURATION_MILLIS,
        easing = AppMotionEasing.Continuity,
    ),
    programmatic = NavSettleSpec.Tween(
        durationMillis = AUDIO_NOW_PLAYING_TRANSITION_DURATION_MILLIS,
        easing = AppMotionEasing.Continuity,
    ),
)

private fun audioNowPlayingTransform(
    scope: top.yukonga.miuix.kmp.nav.transition.NavTransitionScope,
): Modifier = Modifier.graphicsLayer {
    if (scope.relativeDepth <= 0f) {
        val progress = AppMotionEasing.Continuity.transform(topProgress(scope.relativeDepth))
        val scale = AUDIO_NOW_PLAYING_MIN_SCALE +
            (1f - AUDIO_NOW_PLAYING_MIN_SCALE) * progress
        scaleX = scale
        scaleY = scale
        translationY = (1f - progress) *
            scope.layoutSize.height * AUDIO_NOW_PLAYING_TRANSLATION_FRACTION
        alpha = 0.92f + 0.08f * progress
        transformOrigin = TransformOrigin(0.5f, 1f)
    }
}

private val AudioNowPlayingPush: NavTransition = navGraphicsTransition(
    opaqueDepth = 1f,
    motion = AudioNowPlayingMotion,
    scrim = { scope -> coverProgress(scope.relativeDepth) },
) { scope ->
    audioNowPlayingTransform(scope)
}

private val AudioNowPlayingPop: NavTransition = navGraphicsTransition(
    opaqueDepth = 1f,
    motion = AudioNowPlayingMotion,
    scrim = { scope -> coverProgress(scope.relativeDepth) },
) { scope ->
    audioNowPlayingTransform(scope)
}

internal val AudioNowPlayingNavTransition: NavTransition = navDirectionalTransition(
    push = AudioNowPlayingPush,
    pop = AudioNowPlayingPop,
    predictivePop = AudioNowPlayingPop,
)
