package com.android.purebilibili.feature.settings

/**
 * Shared timing tokens for "locate then flash" interactions.
 *
 * The settings search anchor and the comment sub-reply deep link both perform the same gesture:
 * scroll an item into view, tint it briefly, then fade back. They used to hardcode their own
 * durations, which drifted apart. Keep the single source of truth here so both stay aligned.
 */
internal object SettingsHighlightTokens {
    /** How long the highlight stays at full strength before fading back. */
    const val HOLD_MILLIS: Long = 1_400L

    /** Fade-in / fade-out duration of the highlight tint. */
    const val FADE_MILLIS: Int = 280
}
