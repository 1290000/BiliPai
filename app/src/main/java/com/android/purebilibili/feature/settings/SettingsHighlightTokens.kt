package com.android.purebilibili.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Shared tokens for "locate then flash" interactions.
 *
 * The settings search anchor and the comment sub-reply deep link both perform the same gesture:
 * scroll an item into view, tint it briefly, then fade back. They used to hardcode their own
 * durations and colors, which drifted apart. Keep the single source of truth here so both
 * surfaces stay visually aligned.
 */
internal object SettingsHighlightTokens {
    /** How long the highlight stays at full strength before fading back. */
    const val HOLD_MILLIS: Long = 1_400L

    /** Fade-in / fade-out duration of the highlight tint. */
    const val FADE_MILLIS: Int = 280

    /**
     * Extra grace period after [HOLD_MILLIS] before a stray focus request is force-cleared.
     * Covers the case where the target row is never composed, so no anchor claims the request.
     */
    const val STALE_GUARD_MILLIS: Long = 400L

    /**
     * Highlight fill for settings rows. Mirrors the comment sub-reply highlight, which is
     * `accentColor.copy(alpha = 0.14f)` where `accentColor` resolves to `colorScheme.primary`.
     */
    val fill: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)

    /** Corner radius applied to the highlight so it reads as a card, not a bare rectangle. */
    val cornerRadiusDp: Int = 16

    /** Horizontal inset so the highlight does not bleed into the group padding. */
    val horizontalInsetDp: Int = 8
}
