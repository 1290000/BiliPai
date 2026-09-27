// File: feature/audio/screen/ExternalPlaylistImportDialog.kt
package com.android.purebilibili.feature.audio.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import com.android.purebilibili.core.store.LocalPlaylist
import com.android.purebilibili.core.store.LocalPlaylistItem
import com.android.purebilibili.core.store.LocalPlaylistStore
import com.android.purebilibili.data.repository.ExternalPlaylistRepository
import com.android.purebilibili.data.repository.SearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 外部歌单导入：粘贴网易云 / QQ 音乐歌单链接 → 预览曲目 → 自动匹配 B 站视频
 * （可逐首手动修正）→ 保存为本地歌单。
 */
@Composable
fun ExternalPlaylistImportDialog(
    onDismiss: () -> Unit,
    onSaved: ((LocalPlaylist) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var inputText by remember { mutableStateOf("") }
    var fetchError by remember { mutableStateOf<String?>(null) }
    var fetching by remember { mutableStateOf(false) }
    var playlist by remember { mutableStateOf<ExternalPlaylistRepository.ExternalPlaylistMeta?>(null) }

    var matching by remember { mutableStateOf(false) }
    var matchCompleted by remember { mutableIntStateOf(0) }
    var matchTotal by remember { mutableIntStateOf(0) }
    var matchingTrackTitle by remember { mutableStateOf("") }
    var matchResults by remember {
        mutableStateOf<List<ExternalPlaylistRepository.MatchOutcome>>(emptyList())
    }
    var matchJob by remember { mutableStateOf<Job?>(null) }

    // 手动修正状态：正在编辑的曲目下标 + 搜索关键词 + 搜索结果
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var manualKeyword by remember { mutableStateOf("") }
    var manualSearching by remember { mutableStateOf(false) }
    var manualResults by remember {
        mutableStateOf<List<ExternalPlaylistRepository.MatchedVideo>>(emptyList())
    }

    fun dismissEditing() {
        editingIndex = null
        manualResults = emptyList()
        manualKeyword = ""
    }

    fun startMatching() {
        val meta = playlist ?: return
        matching = true
        matchCompleted = 0
        matchTotal = meta.tracks.size
        matchResults = meta.tracks.map { ExternalPlaylistRepository.MatchOutcome(it, null) }
        matchJob = scope.launch {
            ExternalPlaylistRepository.matchTracks(meta.tracks) { completed, _, outcome ->
                matchCompleted = completed
                matchingTrackTitle = outcome.track.title
                matchResults = matchResults.toMutableList().also { list ->
                    if (completed - 1 in list.indices) list[completed - 1] = outcome
                }
            }
            matching = false
        }
    }

    fun savePlaylist() {
        val meta = playlist ?: return
        val items = matchResults.mapNotNull { it.video }.map { video ->
            LocalPlaylistItem(
                bvid = video.bvid,
                title = video.title,
                cover = video.cover,
                owner = video.author,
                durationSec = video.durationSec
            )
        }
        if (items.isEmpty()) return
        scope.launch {
            val local = LocalPlaylist(
                id = UUID.randomUUID().toString(),
                name = meta.name,
                coverUrl = meta.coverUrl,
                source = meta.source.name.lowercase(),
                createdAtMs = System.currentTimeMillis(),
                items = items
            )
            LocalPlaylistStore.savePlaylist(context, local)
            onSaved?.invoke(local)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ExternalPlaylistNativeDialogContent(
            state = ExternalPlaylistNativeDialogState(
                inputText = inputText,
                fetchError = fetchError,
                fetching = fetching,
                playlist = playlist,
                matching = matching,
                matchCompleted = matchCompleted,
                matchTotal = matchTotal,
                matchingTrackTitle = matchingTrackTitle,
                matchResults = matchResults,
                editingIndex = editingIndex,
                manualKeyword = manualKeyword,
                manualSearching = manualSearching,
                manualResults = manualResults,
            ),
            actions = ExternalPlaylistNativeDialogActions(
                onDismiss = onDismiss,
                onInputChange = { inputText = it },
                onFetch = {
                    if (!fetching) {
                        fetching = true
                        fetchError = null
                        scope.launch {
                            val parsed = ExternalPlaylistRepository.parsePlaylistInput(inputText)
                            val sourceAndId = when {
                                parsed != null -> parsed
                                inputText.trim().matches(Regex("\\d{4,}")) -> {
                                    ExternalPlaylistRepository.Source.NETEASE to inputText.trim()
                                }
                                else -> null
                            }
                            if (sourceAndId == null) {
                                fetchError = "无法识别链接，请粘贴网易云或 QQ 音乐的完整分享链接"
                            } else {
                                ExternalPlaylistRepository.fetchPlaylist(sourceAndId.first, sourceAndId.second)
                                    .onSuccess { fetched ->
                                        if (fetched.tracks.isEmpty()) fetchError = "歌单为空或为私密歌单"
                                        else playlist = fetched
                                    }
                                    .onFailure { fetchError = it.message ?: "获取歌单失败" }
                            }
                            fetching = false
                        }
                    }
                },
                onStartMatching = { startMatching() },
                onStopMatching = {
                    matchJob?.cancel()
                    matching = false
                },
                onSave = { savePlaylist() },
                onToggleEdit = { index ->
                    if (editingIndex == index) {
                        dismissEditing()
                    } else {
                        editingIndex = index
                        manualKeyword = ExternalPlaylistRepository.buildSearchQueryForManualMatch(matchResults[index].track)
                        manualResults = emptyList()
                    }
                },
                onKeywordChange = { manualKeyword = it },
                onManualSearch = {
                    if (manualKeyword.isNotBlank() && !manualSearching) {
                        scope.launch {
                            manualSearching = true
                            SearchRepository.search(keyword = manualKeyword)
                                .onSuccess { (items, _) ->
                                    manualResults = items.take(8).map {
                                        ExternalPlaylistRepository.MatchedVideo(
                                            bvid = it.bvid,
                                            title = it.title,
                                            cover = it.pic,
                                            author = it.owner.name,
                                            durationSec = it.duration.toLong(),
                                        )
                                    }
                                }
                            manualSearching = false
                        }
                    }
                },
                onPickVideo = { index, video ->
                    matchResults = matchResults.toMutableList().also { list ->
                        list[index] = ExternalPlaylistRepository.MatchOutcome(list[index].track, video)
                    }
                    dismissEditing()
                },
            ),
            surfaceColor = MaterialTheme.colorScheme.surface,
            textColor = MaterialTheme.colorScheme.onSurface,
            secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            accentColor = MaterialTheme.colorScheme.primary,
            errorColor = MaterialTheme.colorScheme.error,
        )
    }
}
