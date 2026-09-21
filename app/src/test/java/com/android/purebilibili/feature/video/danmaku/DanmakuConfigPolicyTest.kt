package com.android.purebilibili.feature.video.danmaku

import com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_BOTTOM
import com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_SCROLL
import com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_TOP
import com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_REVERSE
import org.junit.Assert.assertEquals
import org.junit.Test

class DanmakuConfigPolicyTest {

    @Test
    fun `bilibili font grades should remain relative to user font scale`() {
        assertEquals(0.72f, resolveBilibiliDanmakuFontScale(18f), 0.001f)
        assertEquals(1.0f, resolveBilibiliDanmakuFontScale(25f), 0.001f)
        assertEquals(1.44f, resolveBilibiliDanmakuFontScale(36f), 0.001f)
        assertEquals(1.0f, resolveBilibiliDanmakuFontScale(0f), 0.001f)
    }

    @Test
    fun `minimum visible lines should not degrade to single line`() {
        assertEquals(2, resolveDanmakuMinimumVisibleLines(0.25f))
        assertEquals(3, resolveDanmakuMinimumVisibleLines(0.5f))
        assertEquals(5, resolveDanmakuMinimumVisibleLines(0.75f))
        assertEquals(6, resolveDanmakuMinimumVisibleLines(1.0f))
    }

    @Test
    fun `fallback max lines should remain stable by area ratio`() {
        assertEquals(4, resolveDanmakuFallbackMaxLines(0.25f))
        assertEquals(8, resolveDanmakuFallbackMaxLines(0.5f))
        assertEquals(12, resolveDanmakuFallbackMaxLines(0.75f))
        assertEquals(16, resolveDanmakuFallbackMaxLines(1.0f))
    }

    @Test
    fun `scroll duration should respect explicit duration seconds and speed factor`() {
        assertEquals(
            7000L,
            resolveDanmakuScrollDurationMillis(
                scrollDurationSeconds = 7.0f,
                speedFactor = 1.0f,
                scrollFixedVelocity = false,
                viewportWidthPx = 1080
            )
        )
        assertEquals(
            10500L,
            resolveDanmakuScrollDurationMillis(
                scrollDurationSeconds = 7.0f,
                speedFactor = 1.5f,
                scrollFixedVelocity = false,
                viewportWidthPx = 1080
            )
        )
    }

    @Test
    fun `fixed velocity should scale scroll duration with viewport width`() {
        assertEquals(
            14000L,
            resolveDanmakuScrollDurationMillis(
                scrollDurationSeconds = 7.0f,
                speedFactor = 1.0f,
                scrollFixedVelocity = true,
                viewportWidthPx = 2160
            )
        )
    }

    @Test
    fun `pinned duration should clamp to safe bounds`() {
        assertEquals(2000L, resolveDanmakuPinnedDurationMillis(0.5f))
        assertEquals(4000L, resolveDanmakuPinnedDurationMillis(4.0f))
        assertEquals(15000L, resolveDanmakuPinnedDurationMillis(18.0f))
    }

    @Test
    fun `known viewport line count should honor engine line budget`() {
        val regularLines = resolveDanmakuVisibleLineCount(
            visibleHeightPx = 500f,
            areaRatioHint = 0.5f,
            fontSize = 20f,
            strokeWidth = 1.5f,
            strokeEnabled = true,
            lineHeight = 1.6f,
            massiveMode = false
        )
        val massiveLines = resolveDanmakuVisibleLineCount(
            visibleHeightPx = 500f,
            areaRatioHint = 0.5f,
            fontSize = 20f,
            strokeWidth = 1.5f,
            strokeEnabled = true,
            lineHeight = 1.6f,
            massiveMode = true
        )

        assertEquals(9, regularLines)
        assertEquals(10, massiveLines)
    }

    @Test
    fun `short viewport should not force a minimum line beyond the pixel budget`() {
        assertEquals(
            1,
            resolveDanmakuVisibleLineCount(
                visibleHeightPx = 42f,
                areaRatioHint = 0.5f,
                fontSize = 42f,
                strokeWidth = 1.5f,
                strokeEnabled = true,
                lineHeight = 1.0f,
                massiveMode = false
            )
        )
        assertEquals(
            0,
            resolveDanmakuVisibleLineCount(
                visibleHeightPx = 41f,
                areaRatioHint = 0.5f,
                fontSize = 42f,
                strokeWidth = 1.5f,
                strokeEnabled = true,
                lineHeight = 1.0f,
                massiveMode = true
            )
        )
    }

    @Test
    fun `text size should scale the 20dp baseline by density and user scale`() {
        assertEquals(20f, resolveDanmakuTextSizePx(density = 1f, fontScale = 1f), 0.001f)
        assertEquals(40f, resolveDanmakuTextSizePx(density = 2f, fontScale = 1f), 0.001f)
        assertEquals(30f, resolveDanmakuTextSizePx(density = 1f, fontScale = 1.5f), 0.001f)
        assertEquals(12f, resolveDanmakuTextSizePx(density = 2f, fontScale = 0.3f), 0.001f)
    }

    @Test
    fun `unknown viewport should retain area based fallback line count`() {
        assertEquals(
            8,
            resolveDanmakuVisibleLineCount(
                visibleHeightPx = 0f,
                areaRatioHint = 0.5f,
                fontSize = 42f,
                strokeWidth = 1.5f,
                strokeEnabled = true,
                lineHeight = 1.6f,
                massiveMode = true
            )
        )
    }

    @Test
    fun `line height multiplier should be converted to engine px spacing`() {
        assertEquals(
            67.2f,
            resolveDanmakuLayerLineHeightPx(
                fontSize = 42f,
                lineHeightMultiplier = 1.6f
            ),
            0.001f
        )
    }

    @Test
    fun `static to scroll should remap pinned danmaku to scrolling layer`() {
        assertEquals(
            DANMAKU_LAYER_SCROLL,
            resolveDanmakuRenderLayerType(
                type = 4,
                staticDanmakuToScroll = true
            )
        )
        assertEquals(
            DANMAKU_LAYER_SCROLL,
            resolveDanmakuRenderLayerType(
                type = 5,
                staticDanmakuToScroll = true
            )
        )
        assertEquals(
            DANMAKU_LAYER_BOTTOM,
            resolveDanmakuRenderLayerType(
                type = 4,
                staticDanmakuToScroll = false
            )
        )
        assertEquals(
            DANMAKU_LAYER_TOP,
            resolveDanmakuRenderLayerType(
                type = 5,
                staticDanmakuToScroll = false
            )
        )
        assertEquals(
            DANMAKU_LAYER_REVERSE,
            resolveDanmakuRenderLayerType(type = 6, staticDanmakuToScroll = false)
        )
    }
}
