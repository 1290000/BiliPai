package com.android.purebilibili.core.ui.performance

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.Display
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 面板实际刷新率覆盖的标签策略与订阅钩子（诊断用）。
 *
 * LTPO 设备上系统会在应用投票之上叠加覆盖档（省电/温控/用户偏好），
 * [Display.OnFrameRateOverrideListener] 报告的就是这一层覆盖；
 * 覆盖为空表示按应用投票档位运行，标签留空（调试浮层隐藏该行）。
 */
internal fun resolvePanelFrameRateOverrideLabel(overrideFrameRate: Float?): String {
    val rate = overrideFrameRate ?: return ""
    if (rate <= 0f) return ""
    val rounded = rate.roundToInt().toFloat()
    val rateText = if (abs(rate - rounded) < 0.05f) {
        rounded.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", rate)
    }
    return "$rateText Hz（系统覆盖）"
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
internal fun rememberPanelFrameRateOverrideLabel(): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return ""
    val activity = LocalContext.current.findActivity()
    var overrideFrameRate by remember { mutableFloatStateOf(Float.NaN) }
    DisposableEffect(activity) {
        val display: Display = activity?.display ?: return@DisposableEffect onDispose { }
        val listener = object : Display.OnFrameRateOverrideListener {
            override fun onFrameRateOverride(overrides: Array<out Display.FrameRateOverride>) {
                overrideFrameRate = overrides.firstOrNull()?.frameRate ?: Float.NaN
            }
        }
        // 回调可能来自 binder 线程；snapshot state 写入线程安全。
        display.registerFrameRateOverrideListener({ it.run() }, listener)
        onDispose {
            display.unregisterFrameRateOverrideListener(listener)
        }
    }
    val override = overrideFrameRate
    return if (override.isNaN()) "" else resolvePanelFrameRateOverrideLabel(override)
}
