package com.android.purebilibili.core.cache

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayUrlCachePolicyTest {
    private val nowMs = 1_700_000_000_000L
    private val maxAgeMs = 600_000L

    @Test
    fun unsignedUrlsUseTheExistingMaximumAge() {
        assertEquals(nowMs + maxAgeMs, resolvePlayUrlCacheExpiresAt(nowMs, maxAgeMs, listOf("https://example.com/video")))
    }

    @Test
    fun earliestTrackDeadlineExpiresBeforeTheUrlDoes() {
        val deadlineSec = nowMs / 1000L + 90L
        val urls = listOf("https://example.com/video?deadline=$deadlineSec", "https://example.com/audio?deadline=${deadlineSec + 30L}")
        assertEquals(nowMs + 60_000L, resolvePlayUrlCacheExpiresAt(nowMs, maxAgeMs, urls))
    }

    @Test
    fun pastDeadlinesCannotBeCachedAsFresh() {
        val urls = listOf("https://example.com/video?deadline=${nowMs / 1000L - 1L}")
        assertTrue(resolvePlayUrlCacheExpiresAt(nowMs, maxAgeMs, urls) < nowMs)
    }

    @Test
    fun malformedAndOverflowingDeadlinesDoNotCrashLoading() {
        val urls = listOf("bad url", "https://example.com/video?deadline=invalid", "https://example.com/video?deadline=9223372036854775807")
        assertEquals(nowMs + maxAgeMs, resolvePlayUrlCacheExpiresAt(nowMs, maxAgeMs, urls))
    }

    @Test
    fun serverDeadlineDoesNotExtendTheLocalMaximumAge() {
        val urls = listOf("https://example.com/video?deadline=${nowMs / 1000L + 3600L}")
        assertEquals(nowMs + maxAgeMs, resolvePlayUrlCacheExpiresAt(nowMs, maxAgeMs, urls))
    }
}
