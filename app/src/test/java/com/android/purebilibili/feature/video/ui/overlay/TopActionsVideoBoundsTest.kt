package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals

class TopActionsVideoBoundsTest {
    @Test fun reservesRightLetterboxAndRefractionClearance() {
        // Row ends at x=1530, while actual video content ends at x=1420.
        val inset = resolveTopActionsVideoInsetPx(30f, 1500, 1420f, 12)
        assertEquals(122, inset)
        assertEquals(1408, 30 + 1500 - inset)
    }

    @Test fun fullWidthVideoKeepsExistingScreenMargin() {
        assertEquals(0, resolveTopActionsVideoInsetPx(30f, 1500, 1560f, 12))
        assertEquals(12, resolveTopActionsVideoInsetPx(30f, 1500, 1530f, 12))
    }
}
