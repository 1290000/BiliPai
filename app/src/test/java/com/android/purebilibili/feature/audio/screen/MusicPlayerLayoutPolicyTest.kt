package com.android.purebilibili.feature.audio.screen

import com.android.purebilibili.core.theme.AppUiStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MusicPlayerLayoutPolicyTest {

    @Test
    fun `compact portrait uses horizontal pager`() {
        assertEquals(
            MusicPlayerLayout.COMPACT_PAGER,
            resolveMusicPlayerLayout(widthDp = 393, isInPipMode = false)
        )
    }

    @Test
    fun `wide screen keeps artwork and lyrics visible together`() {
        assertEquals(
            MusicPlayerLayout.EXPANDED_SPLIT,
            resolveMusicPlayerLayout(widthDp = 900, isInPipMode = false)
        )
    }

    @Test
    fun `pip always renders artwork only`() {
        assertEquals(
            MusicPlayerLayout.PIP_ARTWORK,
            resolveMusicPlayerLayout(widthDp = 900, isInPipMode = true)
        )
    }

    @Test
    fun `compact pager tabs stay on cover and lyrics`() {
        assertEquals(listOf("封面", "歌词"), resolveMusicPlayerPageTabs())
        assertEquals(70, MUSIC_PLAYER_COMPACT_DOCK_BOTTOM_PADDING_DP)
    }

    @Test
    fun `chrome follows md3 and miuix spacing and only immerses when glass is on`() {
        val md3 = resolveMusicPlayerChromeSpec(AppUiStyle.MATERIAL3, glassEnabled = false)
        assertEquals(18, md3.horizontalPaddingDp)
        assertEquals(80, md3.playButtonSizeDp)
        assertFalse(md3.usePaletteImmersiveBackdrop)
        assertTrue(md3.coverShapeIsCircle)

        val miuix = resolveMusicPlayerChromeSpec(AppUiStyle.MIUIX, glassEnabled = false)
        assertEquals(16, miuix.horizontalPaddingDp)
        assertEquals(72, miuix.playButtonSizeDp)
        assertFalse(miuix.usePaletteImmersiveBackdrop)

        val glass = resolveMusicPlayerChromeSpec(AppUiStyle.MATERIAL3, glassEnabled = true)
        assertTrue(glass.usePaletteImmersiveBackdrop)
        assertTrue(glass.coverShapeIsCircle)
    }

    @Test
    fun `foldable unfolded uses split layout`() {
        assertEquals(
            MusicPlayerLayout.EXPANDED_SPLIT,
            resolveMusicPlayerLayout(widthDp = 600, isInPipMode = false)
        )
        assertEquals(
            MusicPlayerLayout.EXPANDED_SPLIT,
            resolveMusicPlayerLayout(widthDp = 720, isInPipMode = false)
        )
    }

    @Test
    fun `compact artwork respects available height`() {
        assertEquals(
            288,
            resolveMusicArtworkSizeDp(
                availableWidthDp = 393,
                availableHeightDp = 720,
                layout = MusicPlayerLayout.COMPACT_PAGER
            )
        )
        assertEquals(
            240,
            resolveMusicArtworkSizeDp(
                availableWidthDp = 393,
                availableHeightDp = 320,
                layout = MusicPlayerLayout.COMPACT_PAGER
            )
        )
    }

    @Test
    fun `cover style cycles through card square and turntable`() {
        assertEquals(
            MusicCoverStyle.APPLE_MUSIC_SQUARE,
            resolveNextCoverStyle(MusicCoverStyle.APPLE_MUSIC_CARD)
        )
        assertEquals(
            MusicCoverStyle.TURNTABLE,
            resolveNextCoverStyle(MusicCoverStyle.APPLE_MUSIC_SQUARE)
        )
        assertEquals(
            MusicCoverStyle.APPLE_MUSIC_CARD,
            resolveNextCoverStyle(MusicCoverStyle.TURNTABLE)
        )
        assertEquals("宽屏", resolveCoverStyleShortLabel(MusicCoverStyle.APPLE_MUSIC_CARD))
        assertEquals("方图", resolveCoverStyleShortLabel(MusicCoverStyle.APPLE_MUSIC_SQUARE))
        assertEquals("转盘", resolveCoverStyleShortLabel(MusicCoverStyle.TURNTABLE))
    }

    @Test
    fun `large screen adaptive layout scales padding and gutters`() {
        assertEquals(48, resolveLargeScreenGutterDp(widthDp = 900))
        assertEquals(28, resolveLargeScreenGutterDp(widthDp = 700))
        assertEquals(48, resolveLargeScreenHorizontalPaddingDp(widthDp = 900))
        assertEquals(24, resolveLargeScreenHorizontalPaddingDp(widthDp = 700))
        assertEquals(1200, LARGE_SCREEN_MAX_CONTENT_WIDTH_DP)
    }
}
