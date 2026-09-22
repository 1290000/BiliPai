package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.android.purebilibili.feature.video.danmaku.VoteOption

/** Owned by the video session, not by the viewport or currently visible command list. */
internal class CommandDanmakuOverlayState {
    private val dismissed = mutableStateMapOf<String, Boolean>()
    private val selections = mutableStateMapOf<String, VoteOption>()
    var expandedItemId by mutableStateOf<String?>(null)
        private set

    fun isDismissed(id: String): Boolean = dismissed[id] == true
    fun selection(id: String): VoteOption? = selections[id]
    fun expand(id: String) { if (!isDismissed(id)) expandedItemId = id }
    fun collapse() { expandedItemId = null }
    fun dismiss(id: String) {
        dismissed[id] = true
        if (expandedItemId == id) collapse()
    }
    fun select(id: String, option: VoteOption): Boolean {
        if (isDismissed(id) || selections.containsKey(id)) return false
        selections[id] = option
        return true
    }
}

@Composable
internal fun rememberCommandDanmakuOverlayState(contentKey: Any?): CommandDanmakuOverlayState =
    remember(contentKey) { CommandDanmakuOverlayState() }

internal fun canShowCommandDanmaku(viewportWidthPx: Int, viewportHeightPx: Int, density: Float): Boolean =
    minOf(viewportWidthPx, viewportHeightPx) >= 48f * density

internal fun shouldExpandCommandDanmaku(viewportScale: Float): Boolean = viewportScale < 1f
