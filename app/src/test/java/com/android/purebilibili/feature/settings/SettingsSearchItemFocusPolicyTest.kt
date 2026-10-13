package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Guards the contract that makes "tap a search result -> land on that exact row" work.
 *
 * The search list shows concrete rows from [settingsItemDirectory]. Tapping one submits a
 * `settingId`; the page then needs the LazyColumn item key of the card that owns that row, which
 * is the row's `sectionKey`. If a row's section key is missing from the page's `sectionKeys`, the
 * scroll silently no-ops and the anchor never highlights anything.
 */
class SettingsSearchItemFocusPolicyTest {

    @Test
    fun everyDirectoryEntryResolvesItsOwnCardKey() {
        val missing = settingsItemDirectory
            .filter { it.settingId.isNotBlank() }
            .filter { resolveSettingsItemScrollKey(it.settingId) == null }
            .map { it.settingId }
        assertTrue(missing.isEmpty(), "settings rows without a resolvable scroll key: $missing")

        settingsItemDirectory.forEach { item ->
            assertEquals(
                item.sectionKey,
                resolveSettingsItemScrollKey(item.settingId),
                "scroll key for ${item.settingId} should be its owning card key",
            )
        }
    }

    @Test
    fun blankLegacyOrUnknownSettingIdHasNoScrollKey() {
        assertNull(resolveSettingsItemScrollKey(null))
        assertNull(resolveSettingsItemScrollKey(""))
        assertNull(resolveSettingsItemScrollKey("  "))
        // Legacy entries are category shortcuts, not concrete rows, so they have no anchor.
        assertNull(resolveSettingsItemScrollKey("legacy.001"))
    }

    @Test
    fun playbackPageKeysCoverTheSectionsRenderedOnThatPage() {
        // The playback page builds its scroll keys via `flatMap { listOf(it + "_title", it) }`
        // over playbackSettingsSectionKeys, and that page renders every section in that list.
        // A concrete row must map onto one of those keys for the scroll to land.
        val sampledRows = listOf(
            "playback.remember_last_playback_speed",
            "playback.hw_decode",
            "playback.background_playback_enabled",
            "playback.fullscreen_swipe_seek_enabled",
            "playback.detailed_comment_time_enabled",
        )
        sampledRows.forEach { settingId ->
            val key = assertNotNull(resolveSettingsItemScrollKey(settingId), "no key for $settingId")
            assertTrue(
                key in playbackSettingsSectionKeys,
                "card key $key for $settingId is not listed on the playback page",
            )
        }
    }

    @Test
    fun concreteSearchResultsCarryADirectoryEntrySoTheListCanShowLiveValues() {
        // Non-legacy results render their live setting value, read from the directory. A result
        // without a directory entry would show a blank value column.
        val directoryIds = settingsItemDirectory.map { it.settingId }.toSet()
        val results = resolveSettingsSearchResults(query = "播放", maxResults = Int.MAX_VALUE)
        assertTrue(results.isNotEmpty(), "expected the probe query to match something")
        val orphaned = results
            .filterNot { it.settingId.startsWith("legacy.") }
            .filterNot { it.settingId in directoryIds }
            .map { it.settingId }
        assertTrue(orphaned.isEmpty(), "search results with no directory entry: $orphaned")
    }

    @Test
    fun titleMatchRangePointsAtTheQueryInsideTheVisibleTitle() {
        val results = resolveSettingsSearchResults(query = "硬件解码", maxResults = Int.MAX_VALUE)
        val hit = results.firstOrNull { it.settingId == "playback.hw_decode" }
        assertNotNull(hit, "expected the hardware decode row to match the query")
        val range = assertNotNull(hit.titleMatchRange, "concrete title hit should expose a range")
        assertEquals("硬件解码", hit.title.substring(range))
    }

    @Test
    fun aliasOnlyMatchExposesNoTitleRange() {
        // "图片立体翻页3D" is an alias, so the visible title has no matching substring and the
        // list must not tint anything.
        val results = resolveSettingsSearchResults(query = "图片立体翻页3D", maxResults = Int.MAX_VALUE)
        val hit = results.firstOrNull { it.settingId == "playback.image_preview3d_page_enabled" }
        assertNotNull(hit, "expected the alias query to find the image preview row")
        assertNull(hit.titleMatchRange, "alias-only hits must not produce a title tint range")
    }
}
