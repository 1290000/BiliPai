package com.android.purebilibili.feature.list

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ListBottomBarScrollHideStructureTest {

    @Test
    fun dynamicHistoryFavoriteAndWatchLaterShareScrollHidePolicy() {
        val dynamicSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt"
        )
        val commonListSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/list/CommonListScreen.kt"
        )
        val watchLaterSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/watchlater/WatchLaterScreen.kt"
        )
        val favoriteCategorySource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/list/FavoriteCategoryScreen.kt"
        )

        assertTrue(dynamicSource.contains("rememberBottomBarScrollHideConnection("))
        assertTrue(commonListSource.contains("rememberBottomBarScrollHideConnection("))
        assertTrue(watchLaterSource.contains("rememberBottomBarScrollHideConnection("))
        assertTrue(commonListSource.contains("shouldAutoHideBottomBarOnScroll("))
        assertTrue(watchLaterSource.contains("shouldAutoHideBottomBarOnScroll("))

        // 收藏分区与收藏夹卡片列表必须把真实滚动状态交回 CommonList 追踪。
        assertTrue(commonListSource.contains("gridState = favoriteCategoryGridState"))
        assertTrue(commonListSource.contains("listState = favoriteFolderListState"))
        assertTrue(favoriteCategorySource.contains("state = gridState"))
    }

    private fun loadSource(relativePath: String): String {
        val root = File(System.getProperty("user.dir") ?: ".")
        val candidates = listOf(
            File(root, relativePath),
            File(root.parentFile, relativePath),
        )
        return candidates.firstOrNull { it.exists() }?.readText()
            ?: error("Missing source: $relativePath")
    }
}
