package com.android.purebilibili.core.ui.transition

import org.junit.Assert.assertEquals
import org.junit.Test

class NowPlayingBarHandoffPolicyTest {

    @Test
    fun `idle handoff leaves the bar fully visible`() {
        assertEquals(
            1f,
            resolveNowPlayingBarReturnVisibility(
                handoff = NowPlayingBarHandoffState.Idle,
                currentBvid = "BV123",
            ),
        )
    }

    @Test
    fun `return morph hides the real bar only for the source owner`() {
        val returning = NowPlayingBarHandoffState.Returning(
            targetBvid = "BV123",
            isSourceOwner = true,
        )
        assertEquals(
            0f,
            resolveNowPlayingBarReturnVisibility(handoff = returning, currentBvid = "BV123"),
        )
        assertEquals(
            1f,
            resolveNowPlayingBarReturnVisibility(
                handoff = returning.copy(isSourceOwner = false),
                currentBvid = "BV123",
            ),
        )
    }

    @Test
    fun `return to another video does not hide this bar`() {
        val returning = NowPlayingBarHandoffState.Returning(
            targetBvid = "BV_OTHER",
            isSourceOwner = true,
        )
        assertEquals(
            1f,
            resolveNowPlayingBarReturnVisibility(handoff = returning, currentBvid = "BV123"),
        )
    }

    @Test
    fun `blank target bvid makes every bar yield to the return morph`() {
        val returning = NowPlayingBarHandoffState.Returning(
            targetBvid = null,
            isSourceOwner = true,
        )
        assertEquals(
            0f,
            resolveNowPlayingBarReturnVisibility(handoff = returning, currentBvid = "BV123"),
        )
    }

    @Test
    fun `blank current bvid never hides`() {
        assertEquals(
            1f,
            resolveNowPlayingBarReturnVisibility(
                handoff = NowPlayingBarHandoffState.Returning(
                    targetBvid = "BV123",
                    isSourceOwner = true,
                ),
                currentBvid = "",
            ),
        )
    }

    @Test
    fun `shell reveal ramps over the first settle window and stays dominant`() {
        assertEquals(0f, resolveNowPlayingBarSourceChromeReveal(morphDepthProgress = 1f))
        assertEquals(0.5f, resolveNowPlayingBarSourceChromeReveal(morphDepthProgress = 0.91f), 0.001f)
        assertEquals(1f, resolveNowPlayingBarSourceChromeReveal(morphDepthProgress = 0.82f))
        assertEquals(1f, resolveNowPlayingBarSourceChromeReveal(morphDepthProgress = 0.3f))
        assertEquals(1f, resolveNowPlayingBarSourceChromeReveal(morphDepthProgress = 0f))
    }
}
