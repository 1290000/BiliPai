package com.android.purebilibili.feature.video.ui.overlay

import com.android.purebilibili.feature.video.danmaku.VoteOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommandDanmakuOverlayStateTest {
    @Test
    fun `reopening a preview cannot submit an already selected command again`() {
        val state = CommandDanmakuOverlayState()
        val option = VoteOption("four", "four", 8)
        state.expand("grade")
        assertTrue(state.select("grade", option))
        state.collapse()
        state.expand("grade")
        assertFalse(state.select("grade", VoteOption("five", "five", 10)))
        assertEquals(option, state.selection("grade"))
    }

    @Test
    fun `dismissed commands stay dismissed without affecting another command`() {
        val state = CommandDanmakuOverlayState()
        state.expand("first")
        state.dismiss("first")
        state.expand("first")
        assertNull(state.expandedItemId)
        assertTrue(state.isDismissed("first"))
        state.expand("second")
        assertEquals("second", state.expandedItemId)
        assertFalse(state.isDismissed("second"))
    }

    @Test
    fun `scaled controls use a preview only when a full touch target fits`() {
        assertTrue(shouldExpandCommandDanmaku(0.56f))
        assertFalse(shouldExpandCommandDanmaku(1f))
        assertFalse(canShowCommandDanmaku(143, 608, 3f))
        assertTrue(canShowCommandDanmaku(144, 608, 3f))
    }
}
