package com.android.purebilibili.feature.bangumi

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 任务17 结构测试：Bangumi 单一 uiState + 旧属性 map 兼容 + 单次 copy 事务 + generation/session 回包拒绝
 * + 追番缓存读取均经 ensureFollowCacheSession。
 *
 * 纯源码/结构断言，不依赖 Compose 运行时。
 */
class BangumiViewModelSingleStateStructureTest {

    private val vm = loadSource(
        "app/src/main/java/com/android/purebilibili/feature/bangumi/BangumiViewModel.kt"
    )

    @Test
    fun `single uiState source of truth`() {
        val exposedSources = Regex("""val\s+_uiState\s*=\s*MutableStateFlow\(""").findAll(vm).count()
        assertTrue(exposedSources == 1, "expected exactly one backing _uiState flow, found $exposedSources")
        assertTrue(vm.contains("val uiState = _uiState.asStateFlow()"))
    }

    @Test
    fun `legacy properties derive from uiState with distinctUntilChanged stateIn Eagerly`() {
        val legacyNames = listOf(
            "displayMode", "selectedType", "listState", "timelineState",
            "detailState", "searchState", "myFollowState", "myFollowType",
            "myFollowStats", "filter", "searchKeyword"
        )
        legacyNames.forEach { name ->
            assertTrue(
                Regex("""val\s+$name\s*:\s*StateFlow<""").containsMatchIn(vm),
                "legacy property $name must still be exported"
            )
        }
        // 每个旧属性均由 uiState.map + distinctUntilChanged + stateIn(Eagerly) 派生
        val mapDerived = Regex("""uiState\.map\s*\{[^}]*\}\s*\.distinctUntilChanged\(\)\.stateIn\(""").findAll(vm).count()
        assertTrue(
            mapDerived >= legacyNames.size,
            "expected >= ${legacyNames.size} uiState.map{}.distinctUntilChanged().stateIn() derivations, found $mapDerived"
        )
        assertTrue(vm.contains("SharingStarted.Eagerly"))
        // 不应再存在第二个独立可变的状态源
        assertFalse(Regex("""private\s+val\s+_\w*State\w*\s*=\s*MutableStateFlow\(""").containsMatchIn(vm))
    }

    @Test
    fun `list search timeline and myFollow all reject stale generation responses`() {
        // 列表
        assertTrue(vm.contains("val generation = ++listGeneration"))
        assertTrue(vm.contains("if (generation != listGeneration || session != ensureFollowCacheSession()) return@fold"))
        // 搜索
        assertTrue(vm.contains("val generation = ++searchGeneration"))
        assertTrue(vm.contains("if (generation != searchGeneration) return@fold"))
        // 时间线
        assertTrue(vm.contains("val generation = ++timelineGeneration"))
        // 追番分页
        assertTrue(vm.contains("val generation = ++myFollowGeneration"))
        assertTrue(vm.contains("isActiveFollowRequest(session, generation)"))
    }

    @Test
    fun `timeline responses are guarded by generation and session`() {
        // 任务17 修复：timeline 回包须拒绝旧 generation/session
        val timelineFn = extractFunction("loadTimeline")
        assertTrue(
            timelineFn.contains("if (generation != timelineGeneration || session != ensureFollowCacheSession()) return@fold"),
            "loadTimeline must reject stale generation/session responses"
        )
    }

    @Test
    fun `follow cache reads go through ensureFollowCacheSession`() {
        // 每个读取追番缓存的函数体里都必须（直接或间接经由 ensure/isActive）先取 session
        val readers = listOf(
            "verifyDetailFollowStatus" to "followStatusCache.containsKey",
            "correctFollowStatus" to "followStatusCache",
            "loadSeasonDetail" to "cachedFollow = followStatusCache",
            "updateFollowStatus" to "followStatusCache[seasonId] == true"
        )
        readers.forEach { (fn, marker) ->
            val body = extractFunction(fn)
            assertTrue(body.contains(marker), "$fn should contain $marker (sanity)")
            assertTrue(
                body.contains("ensureFollowCacheSession"),
                "$fn must call ensureFollowCacheSession before reading follow caches"
            )
        }
    }

    @Test
    fun `session change invalidates follow caches and generations`() {
        val body = extractFunction("ensureFollowCacheSession")
        assertTrue(body.contains("followStatusCache.clear()"))
        assertTrue(body.contains("followStatusValueCache.clear()"))
        assertTrue(body.contains("followedSeasonIds.clear()"))
        assertTrue(body.contains("loadedFollowStatsTypes.clear()"))
        assertTrue(body.contains("++detailRequestGeneration"))
        assertTrue(body.contains("++myFollowGeneration"))
    }

    @Test
    fun `myFollow pagination commits page only on success`() {
        val body = extractFunction("loadMoreMyFollow")
        // 成功路径内提交页码，失败路径不得提交
        assertTrue(body.contains("if (!isActiveFollowRequest(session, generation)) return@fold"))
        assertTrue(body.contains("myFollowPage = requestedPage"))
        assertFalse(body.contains("} else { myFollowPage"))
    }
}

private fun extractFunction(source: String, name: String): String {
    val start = source.indexOf("fun $name(")
    if (start < 0) return ""
    // 粗略截取到下一个顶层 } 之后（结构测试足够）
    val nextFun = source.indexOf("\n    private fun ", start + 1)
        .takeIf { it > 0 } ?: source.indexOf("\n    fun ", start + 1).takeIf { it > 0 } ?: source.length
    return source.substring(start, minOf(nextFun, source.length))
}

private fun loadSource(path: String): String {
    val normalizedPath = path.removePrefix("app/")
    val sourceFile = listOf(
        File(path),
        File(normalizedPath),
        File("app", normalizedPath)
    ).firstOrNull { it.exists() }
    require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
    return sourceFile.readText()
}
