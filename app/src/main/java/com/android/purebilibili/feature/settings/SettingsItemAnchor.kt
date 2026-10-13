package com.android.purebilibili.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

@Composable
internal fun SettingsItemAnchor(
    settingId: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val request by SettingsSearchFocusController.request.collectAsStateWithLifecycle()
    val bringIntoView = remember { BringIntoViewRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    var highlighted by remember { mutableStateOf(false) }
    // Runs on every anchor. Whichever anchor owns the request clears it after the hold window; if
    // the target row is never composed (hidden section, wrong page) no anchor claims it, so this
    // watcher is what keeps the request from going stale and blocking subsequent taps.
    LaunchedEffect(request?.token) {
        val current = request ?: return@LaunchedEffect
        if (current.settingId == null) return@LaunchedEffect
        delay(SettingsHighlightTokens.HOLD_MILLIS + SettingsHighlightTokens.STALE_GUARD_MILLIS)
        SettingsSearchFocusController.clear(current.token)
    }
    LaunchedEffect(request?.token, settingId) {
        val current = request ?: return@LaunchedEffect
        if (current.settingId != settingId) return@LaunchedEffect
        try {
            keyboard?.hide()
            withFrameNanos { }
            bringIntoView.bringIntoView()
            highlighted = true
            view.announceForAccessibility("已定位到${settingsItemDirectory.firstOrNull { it.settingId == settingId }?.title.orEmpty()}")
            delay(SettingsHighlightTokens.HOLD_MILLIS)
        } finally {
            highlighted = false
            SettingsSearchFocusController.clear(current.token)
        }
    }
    // Mirrors the comment sub-reply highlight: fade the tint in and out instead of snapping it.
    val highlightColor by animateColorAsState(
        targetValue = if (highlighted) SettingsHighlightTokens.fill else Color.Transparent,
        animationSpec = tween(durationMillis = SettingsHighlightTokens.FADE_MILLIS),
        label = "settingsItemSearchHighlight",
    )
    val shape = RoundedCornerShape(SettingsHighlightTokens.cornerRadiusDp.dp)
    Box(
        modifier.fillMaxWidth()
            .bringIntoViewRequester(bringIntoView)
            .padding(horizontal = SettingsHighlightTokens.horizontalInsetDp.dp)
            .clip(shape)
            .background(highlightColor),
    ) { content() }
}
