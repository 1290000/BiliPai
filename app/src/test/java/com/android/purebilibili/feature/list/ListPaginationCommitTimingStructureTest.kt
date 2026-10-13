package com.android.purebilibili.feature.list

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 收藏夹 / 空间番剧分页的「页码提交时机」结构约束：
 * - 页码只在请求成功且响应仍属于当前请求（generation/mediaId/order 未变化）后才提交；
 * - hasMore 采用服务器返回的 has_more / total / ps，而不是「请求前」的猜测。
 */
class ListPaginationCommitTimingStructureTest {

    @Test
    fun `favorite folder page commits only after the current request succeeds`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/list/ListViewModel.kt")

        val guardIndex = source.indexOf("if (!isCurrentFolderRequest(index, requestGeneration, mediaId, requestedOrder)) return@launch")
        val commitIndex = source.indexOf("pagination.currentPage = nextPage")
        assertTrue(guardIndex in 0 until commitIndex) {
            "folder page must be committed only after the stale-generation guard passes"
        }
        // hasMore 来自服务器 has_more，而不是本地推算。
        assertTrue(source.contains("pagination.hasMore = result.has_more"))
    }

    @Test
    fun `space bangumi pagination uses server page size and total`() {
        val viewModel = loadSource("app/src/main/java/com/android/purebilibili/feature/space/SpaceViewModel.kt")
        val policy = loadSource("app/src/main/java/com/android/purebilibili/feature/space/SpaceLoadPolicy.kt")

        // 页码提交使用服务器返回的 pn（不复用请求前猜测）。
        assertTrue(viewModel.contains("bangumiPage = data.pn.coerceAtLeast(page)"))
        assertTrue(viewModel.contains("shouldContinueSpaceBangumiPagination("))
        // 到底判断用服务器 pn/ps/total。
        assertTrue(policy.contains("responsePage.coerceAtLeast(1) * pageSize.coerceAtLeast(1) < total"))
    }

    @Test
    fun `watch later pagination trusts server ps over requested size`() {
        val source = loadSource("core-data/src/main/java/com/android/purebilibili/data/repository/WatchLaterRepository.kt")

        // 优先使用服务器返回的 ps；无 ps 时以实际返回条数兜底，而不是请求前写死的 30。
        assertTrue(source.contains("data?.ps?.takeIf { it > 0 }"))
        assertTrue(source.contains("minOf(PAGE_SIZE, items.size)"))
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
