package com.android.purebilibili.core.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AppUiStyle
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppThemeAdaptiveTabRowStructureTest {
    @Test
    fun `global tab row uses native theme renderer and does not force liquid glass`() {
        val source = File(
            "app/src/main/java/com/android/purebilibili/core/ui/components/AppLiquidAwareTabRow.kt"
        ).readText()
        val adaptiveEntry = source
            .substringAfter("fun <T> AppThemeAdaptiveTabRow(")
            .substringBefore("fun <T> AppLiquidAwareTabRow(")

        assertFalse(adaptiveEntry.contains("AppLiquidAwareTabRow("))
        assertTrue(adaptiveEntry.contains("AppNativeTabRow("))
        assertTrue(adaptiveEntry.contains("AppNativeSegmentedControl("))
        assertTrue(adaptiveEntry.contains("LocalAppUiStyle"))
        assertTrue(adaptiveEntry.contains("liquidGlassEnabled = false"))
        assertEquals(
            2,
            source.lineSequence().count {
                it.contains("minTabWidth: Dp = Dp.Unspecified")
            },
        )

        val adaptiveRenderer = source.substringAfter("fun <T> AppLiquidAwareTabRow(")
        assertTrue(adaptiveRenderer.contains("LocalAppThemeConfig.current.liquidGlassEnabled"))
        assertTrue(adaptiveRenderer.contains("!liquidGlassEnabled"))
        assertTrue(adaptiveRenderer.contains("AppNativeTabRow("))
        assertTrue(adaptiveRenderer.contains("allowLabelOverflow = true"))
        assertTrue(adaptiveRenderer.contains("indicatorPositionProvider = indicatorPositionProvider"))
        assertTrue(adaptiveRenderer.contains("isScrollInProgressProvider = isScrollInProgressProvider"))
        assertTrue(adaptiveRenderer.contains("BottomBarLiquidSegmentedControl("))
        assertTrue(adaptiveRenderer.contains("resolvedDragSelectionEnabled"))
        assertTrue(adaptiveRenderer.contains("readableTabWidth > resolvedMinTabWidth"))
        assertTrue(adaptiveRenderer.contains("tapPressRefractionEnabled = tapPressRefractionEnabled"))
        assertTrue(adaptiveRenderer.contains("height = height"))
        assertTrue(adaptiveRenderer.contains("indicatorHeight = indicatorHeight"))
    }

    @Test
    fun `adaptive tabs keep beta21 liquid width without reverting non glass accessibility`() {
        assertEquals(
            72.dp,
            resolveAppAdaptiveTabMinWidth(
                requestedMinTabWidth = Dp.Unspecified,
                uiStyle = AppUiStyle.MIUIX,
                liquidGlassEnabled = true,
            ),
        )
        assertEquals(
            48.dp,
            resolveAppAdaptiveTabMinWidth(
                requestedMinTabWidth = Dp.Unspecified,
                uiStyle = AppUiStyle.MIUIX,
                liquidGlassEnabled = false,
            ),
        )
        assertEquals(
            72.dp,
            resolveAppAdaptiveTabMinWidth(
                requestedMinTabWidth = Dp.Unspecified,
                uiStyle = AppUiStyle.MATERIAL3,
                liquidGlassEnabled = false,
            ),
        )
        assertEquals(
            60.dp,
            resolveAppAdaptiveTabMinWidth(
                requestedMinTabWidth = 60.dp,
                uiStyle = AppUiStyle.MATERIAL3,
                liquidGlassEnabled = true,
            ),
        )
    }
}
