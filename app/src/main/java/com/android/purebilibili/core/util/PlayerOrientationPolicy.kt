package com.android.purebilibili.core.util

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import androidx.window.layout.WindowMetricsCalculator

internal data class PlayerWindowOrientationPolicy(
    val currentWindowWidthDp: Int,
    val currentWindowHeightDp: Int,
    val maximumWindowWidthDp: Int?,
    val maximumWindowHeightDp: Int?,
    val displayModeWidthPx: Int?,
    val displayModeHeightPx: Int?,
    val displayRotation: Int?,
    val isFoldableCoverWindow: Boolean,
    val isLandscapeNaturalDisplay: Boolean,
) {
    val usesInWindowFullscreen: Boolean
        get() = isFoldableCoverWindow && isLandscapeNaturalDisplay
}

internal fun isFoldableCoverWindow(
    smallestScreenWidthDp: Int,
    currentWindowWidthDp: Int?,
    currentWindowHeightDp: Int?,
    maximumWidthDp: Int? = null,
    maximumHeightDp: Int? = null,
): Boolean {
    val isDeviceLargeScreen = smallestScreenWidthDp >= LARGE_SCREEN_SMALLEST_WIDTH_DP ||
        (maximumWidthDp != null && maximumHeightDp != null &&
            minOf(maximumWidthDp, maximumHeightDp) >= LARGE_SCREEN_SMALLEST_WIDTH_DP)
    return isDeviceLargeScreen &&
        currentWindowWidthDp != null && currentWindowHeightDp != null &&
        minOf(currentWindowWidthDp, currentWindowHeightDp) < LARGE_SCREEN_SMALLEST_WIDTH_DP
}

@Suppress("UNUSED_PARAMETER")
internal fun shouldRequestPhysicalPlayerOrientation(
    smallestScreenWidthDp: Int,
    currentWindowWidthDp: Int? = null,
    currentWindowHeightDp: Int? = null,
    maximumWidthDp: Int? = null,
    maximumHeightDp: Int? = null,
    configurationOrientation: Int? = null,
    displayRotation: Int? = null,
    displayModeWidthPx: Int? = null,
    displayModeHeightPx: Int? = null,
    platformIgnoresLargeScreenOrientationRequests: Boolean =
        Build.VERSION.SDK_INT >= 36,
): Boolean {
    // Preserve the user's intent on ordinary phones and large screens. A landscape-natural
    // foldable cover is the exception: its fullscreen is an in-window presentation, and axis
    // requests can create portrait letterboxing or an orientation loop on vendor ROMs.
    val isCoverWindow = isFoldableCoverWindow(
        smallestScreenWidthDp = smallestScreenWidthDp,
        currentWindowWidthDp = currentWindowWidthDp,
        currentWindowHeightDp = currentWindowHeightDp,
        maximumWidthDp = maximumWidthDp,
        maximumHeightDp = maximumHeightDp,
    )
    val isLandscapeNaturalCover = isCoverWindow &&
        configurationOrientation != null &&
        isLandscapeNaturalDisplay(
            configurationOrientation = configurationOrientation,
            displayRotation = displayRotation,
            displayModeWidthPx = displayModeWidthPx,
            displayModeHeightPx = displayModeHeightPx,
        )
    return !isLandscapeNaturalCover
}

internal fun resolvePlayerWindowOrientationPolicy(
    smallestScreenWidthDp: Int,
    currentWindowWidthDp: Int,
    currentWindowHeightDp: Int,
    maximumWidthDp: Int?,
    maximumHeightDp: Int?,
    configurationOrientation: Int,
    displayRotation: Int?,
    displayModeWidthPx: Int? = null,
    displayModeHeightPx: Int? = null,
    isKnownFoldableCoverWindow: Boolean = false,
): PlayerWindowOrientationPolicy {
    val isCoverWindow = isKnownFoldableCoverWindow || isFoldableCoverWindow(
        smallestScreenWidthDp = smallestScreenWidthDp,
        currentWindowWidthDp = currentWindowWidthDp,
        currentWindowHeightDp = currentWindowHeightDp,
        maximumWidthDp = maximumWidthDp,
        maximumHeightDp = maximumHeightDp,
    )
    return PlayerWindowOrientationPolicy(
        currentWindowWidthDp = currentWindowWidthDp,
        currentWindowHeightDp = currentWindowHeightDp,
        maximumWindowWidthDp = maximumWidthDp,
        maximumWindowHeightDp = maximumHeightDp,
        displayModeWidthPx = displayModeWidthPx,
        displayModeHeightPx = displayModeHeightPx,
        displayRotation = displayRotation,
        isFoldableCoverWindow = isCoverWindow,
        isLandscapeNaturalDisplay = isLandscapeNaturalDisplay(
            configurationOrientation = configurationOrientation,
            displayRotation = displayRotation,
            displayModeWidthPx = displayModeWidthPx,
            displayModeHeightPx = displayModeHeightPx,
        ),
    )
}

@Suppress("DEPRECATION")
internal fun Activity.resolvePlayerWindowOrientationPolicy(
    isKnownFoldableCoverWindow: Boolean = false,
): PlayerWindowOrientationPolicy {
    val configuration = resources.configuration
    val density = resources.displayMetrics.density.coerceAtLeast(1f)
    val maximumBounds = runCatching {
        WindowMetricsCalculator.getOrCreate().computeMaximumWindowMetrics(this).bounds
    }.getOrNull()
    val currentDisplay = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            windowManager.defaultDisplay
        }
    }.getOrNull()
    val displayMode = runCatching { currentDisplay?.mode }.getOrNull()
    return resolvePlayerWindowOrientationPolicy(
        smallestScreenWidthDp = configuration.smallestScreenWidthDp,
        currentWindowWidthDp = configuration.screenWidthDp,
        currentWindowHeightDp = configuration.screenHeightDp,
        maximumWidthDp = maximumBounds?.let { (it.width() / density).toInt() },
        maximumHeightDp = maximumBounds?.let { (it.height() / density).toInt() },
        configurationOrientation = configuration.orientation,
        displayRotation = currentDisplay?.rotation,
        displayModeWidthPx = displayMode?.physicalWidth,
        displayModeHeightPx = displayMode?.physicalHeight,
        isKnownFoldableCoverWindow = isKnownFoldableCoverWindow,
    )
}

private fun isPlayerAxisOrientationRequest(requestedOrientation: Int): Boolean {
    return requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED &&
        requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_LOCKED &&
        requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_BEHIND
}

internal fun resolveEffectivePlayerRequestedOrientation(
    requestedOrientation: Int,
    usesInWindowFullscreen: Boolean,
): Int {
    return if (usesInWindowFullscreen && isPlayerAxisOrientationRequest(requestedOrientation)) {
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    } else {
        requestedOrientation
    }
}

/**
 * Android 16+ ignores orientation restrictions for target-36+ apps on large screens,
 * and target 37 removes the manifest opt-out. Older Android releases still honor
 * requestedOrientation on tablets, so do not discard a user's fullscreen request there.
 */
internal fun Activity.applyPlayerRequestedOrientation(
    requestedOrientation: Int,
    isKnownFoldableCoverWindow: Boolean = false,
): Boolean {
    val policy = resolvePlayerWindowOrientationPolicy(
        isKnownFoldableCoverWindow = isKnownFoldableCoverWindow,
    )
    val effectiveOrientation = resolveEffectivePlayerRequestedOrientation(
        requestedOrientation = requestedOrientation,
        usesInWindowFullscreen = policy.usesInWindowFullscreen,
    )
    if (effectiveOrientation != requestedOrientation) {
        Logger.d(
            "PlayerOrientationPolicy",
            "Suppress axis request=$requestedOrientation on landscape-natural cover: " +
                "current=${policy.currentWindowWidthDp}x${policy.currentWindowHeightDp}dp, " +
                "maximum=${policy.maximumWindowWidthDp}x${policy.maximumWindowHeightDp}dp, " +
                "mode=${policy.displayModeWidthPx}x${policy.displayModeHeightPx}px, " +
                "rotation=${policy.displayRotation}"
        )
    }
    if (this.requestedOrientation == effectiveOrientation) return false
    this.requestedOrientation = effectiveOrientation
    return true
}
