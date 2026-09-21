package com.android.purebilibili.feature.message

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatScreenStructureTest {

    @Test
    fun chatBubblesReuseCardGlassTintAndRelatedCoverMetrics() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/message/ChatScreen.kt"
        )
        assertTrue(source.contains("messageGlassContainer("))
        assertTrue(source.contains("rememberMessageGlassContentColors("))
        assertTrue(source.contains("HorizontalVideoCardFrame("))
        assertTrue(source.contains("MessageHorizontalVideoCard("))
        assertTrue(source.contains("feedContentTypography(FeedTitleHierarchy.Standard)"))
        assertTrue(source.contains("globalWallpaperAwareChromeColor("))
        assertFalse(source.contains(".height(100.dp)"))
        assertFalse(source.contains(".size(72.dp)"))
        assertFalse(source.contains("variant = AppCardVariant.Elevated"))
    }

    @Test
    fun replyFeedCardsUseSharedGlassSurface() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/message/feed/MessageFeedCommon.kt"
        )
        assertTrue(source.contains("messageGlassContainer("))
        assertTrue(source.contains("AppShapes.borderedContainer(surfaceSpec.cornerLevel)"))
        assertFalse(source.contains("AppSurface("))
    }

    @Test
    fun replyMeTopBarStaysTransparentForWallpaper() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/message/feed/ReplyMeScreen.kt"
        )
        assertTrue(source.contains("containerColor = androidx.compose.ui.graphics.Color.Transparent"))
        assertTrue(source.contains("ReplyMeCard("))
        assertTrue(source.contains("MessageFeedCard("))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        return listOf(File(path), File(normalizedPath)).firstOrNull(File::exists)?.readText()
            ?: error("Cannot locate $path from ${File(".").absolutePath}")
    }
}
