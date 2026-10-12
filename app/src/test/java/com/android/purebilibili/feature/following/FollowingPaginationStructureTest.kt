package com.android.purebilibili.feature.following

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 关注列表滚动分页的结构约束（不可用单元测试覆盖的 ViewModel 行为）：
 * - 请求去重：同一时刻只允许一个在飞请求（followingRequestActive）；
 * - 每页成功才推进页码：nextFollowingPage = page + 1 只能在 response.code == 0 之后；
 * - 旧响应拒绝：generation 不匹配时直接丢弃结果，不写状态；
 * - 显式全量入口：全量加载只由 loadAllForSearch / loadMore 触发，首屏不再自动灌全量。
 */
class FollowingPaginationStructureTest {

    @Test
    fun `following page advances only after a successful response`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/following/FollowingListScreen.kt")

        // 首页成功后才把 nextFollowingPage 推进到 2。
        assertTrue(source.contains("nextFollowingPage = 2"))
        // 后续页必须在拿到 code/data 之后才推进页码。
        val advanceIndex = source.indexOf("nextFollowingPage = page + 1")
        val successIndex = source.indexOf("if (response.code != 0 || data == null) return false")
        assertTrue(successIndex in 0 until advanceIndex) {
            "page advance must happen after the success check"
        }
    }

    @Test
    fun `following requests are de-duplicated and stale responses are rejected`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/following/FollowingListScreen.kt")

        assertTrue(source.contains("followingRequestActive"))
        assertTrue(source.contains("if (generation != followingGeneration) return false"))
        // 首屏加载也要拒绝旧响应。
        assertTrue(source.contains("if (generation != followingGeneration) return@launch"))
    }

    @Test
    fun `full following load stays behind an explicit entry`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/following/FollowingListScreen.kt")

        // 首屏不再自动 loadAllRemainingPages。
        assertFalse(source.contains("loadAllRemainingPages("))
        assertTrue(source.contains("fun loadAllForSearch()"))
        // 显式入口按钮存在。
        assertTrue(source.contains("viewModel.loadAllForSearch()"))
        assertTrue(source.contains("加载全部"))
    }

    private fun loadSource(path: String): String {
        val candidates = listOf(
            File(path),
            File("app/$path"),
            File("../$path"),
            File("../core-data/$path"),
            File("core-data/$path"),
        )
        val sourceFile = candidates.firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText().replace("\r\n", "\n")
    }
}
