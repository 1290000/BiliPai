package com.android.purebilibili.data.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class VideoRequestAccountScopeTest {
    @Test
    fun accountCredentialsAreNotStoredInCacheKeys() {
        val scope = buildVideoRequestAccountScope(listOf("private-session", "123", "true"))
        assertEquals(64, scope.length)
        assertFalse(scope.contains("private-session"))
    }

    @Test
    fun accountAndPermissionChangesProduceDifferentScopes() {
        val base = buildVideoRequestAccountScope(listOf("session-one", "123", "false"))
        assertNotEquals(base, buildVideoRequestAccountScope(listOf("session-two", "456", "false")))
        assertNotEquals(base, buildVideoRequestAccountScope(listOf("session-one", "123", "true")))
        assertEquals(base, buildVideoRequestAccountScope(listOf("session-one", "123", "false")))
    }

    @Test
    fun boundariesBetweenCredentialsAreUnambiguous() {
        assertNotEquals(
            buildVideoRequestAccountScope(listOf("ab", "c")),
            buildVideoRequestAccountScope(listOf("a", "bc"))
        )
    }
}
