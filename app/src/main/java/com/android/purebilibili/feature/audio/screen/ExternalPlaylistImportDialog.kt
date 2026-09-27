// File: feature/audio/screen/ExternalPlaylistImportDialog.kt
package com.android.purebilibili.feature.audio.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface as M3Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.android.purebilibili.core.store.LocalPlaylist
import com.android.purebilibili.core.store.LocalPlaylistItem
import com.android.purebilibili.core.store.LocalPlaylistStore
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextField
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

    Dialog(onDismissRequest = onDismiss) {
        M3Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppText(
                        text = "导入外部歌单",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    AppIconButton(onClick = onDismiss) {
                        AppIcon(Icons.Outlined.Close, contentDescription = "关闭")
                    }
                }
                Spacer(Modifier.height(12.dp))

                val meta = playlist
                when {
                    // -------- 阶段 1：粘贴链接 --------
                    meta == null -> {
                        AppText(
                            text = "支持网易云音乐和 QQ 音乐歌单。复制分享链接粘贴到下面（歌单需为公开状态）。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        AppTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = "粘贴歌单链接或歌单 id",
                            singleLine = true
                        )
                        fetchError?.let {
                            Spacer(Modifier.height(8.dp))
                            AppText(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        AppButton(
                            onClick = {
                                if (fetching) return@AppButton
                                fetching = true
                                fetchError = null
                                scope.launch {
                                    val parsed = ExternalPlaylistRepository.parsePlaylistInput(inputText)
                                    val (source, id) = when {
                                        parsed != null -> parsed
                                        inputText.trim().matches(Regex("\\d{4,}")) -> {
                                            // 纯数字默认按网易云处理；QQ 链接通常带有域名
                                            ExternalPlaylistRepository.Source.NETEASE to inputText.trim()
                                        }
                                        else -> {
                                            fetching = false
                                            fetchError = "无法识别链接，请粘贴网易云或 QQ 音乐的完整分享链接"
                                            return@launch
                                        }
                                    }
                                    ExternalPlaylistRepository.fetchPlaylist(source, id)
                                        .onSuccess {
                                            if (it.tracks.isEmpty()) {
                                                fetchError = "歌单为空或为私密歌单"
                                            } else {
                                                playlist = it
                                            }
                                        }
                                        .onFailure { fetchError = it.message ?: "获取歌单失败" }
                                    fetching = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AppText(if (fetching) "获取中..." else "获取歌单信息")
                        }
                    }

                    // -------- 阶段 3：匹配完成后的核对与保存 --------
                    !matching && matchResults.isNotEmpty() && matchCompleted >= matchTotal -> {
                        val matchedCount = matchResults.count { it.video != null }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppText(
                                text = "${meta.name} · 匹配 $matchedCount/${matchResults.size} 首",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            AppButton(onClick = { savePlaylist() }) { AppText("保存") }
                        }
                        Spacer(Modifier.height(8.dp))
                        MatchResultList(
                            results = matchResults,
                            editingIndex = editingIndex,
                            manualKeyword = manualKeyword,
                            manualSearching = manualSearching,
                            manualResults = manualResults,
                            onKeywordChange = { manualKeyword = it },
                            onEditToggle = { index ->
                                if (editingIndex == index) {
                                    dismissEditing()
                                } else {
                                    editingIndex = index
                                    manualKeyword = ExternalPlaylistRepository
                                        .buildSearchQueryForManualMatch(matchResults[index].track)
                                    manualResults = emptyList()
                                }
                            },
                            onSearchClick = {
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
                                                        durationSec = it.duration
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
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        AppText(
                            text = "未匹配的曲目将被跳过，保存后可稍后再添加。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // -------- 阶段 2：获取成功，待匹配 / 匹配中 --------
                    else -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                AppText(
                                    text = meta.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                AppText(
                                    text = "${meta.author} · ${meta.tracks.size} 首",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (matching) {
                                AppButton(onClick = {
                                    matchJob?.cancel()
                                    matching = false
                                }) { AppText("停止") }
                            } else {
                                AppButton(onClick = { startMatching() }) { AppText("开始匹配") }
                            }
                        }
                        if (matchTotal > 0) {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = {
                                    if (matchTotal == 0) 0f else matchCompleted.toFloat() / matchTotal
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (matching) {
                                Spacer(Modifier.height(4.dp))
                                AppText(
                                    text = "正在匹配：$matchingTrackTitle ($matchCompleted/$matchTotal)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        MatchResultList(
                            results = matchResults.ifEmpty {
                                playlist?.tracks?.map {
                                    ExternalPlaylistRepository.MatchOutcome(it, null)
                                }.orEmpty()
                            },
                            editingIndex = null,
                            manualKeyword = "",
                            manualSearching = false,
                            manualResults = emptyList(),
                            onKeywordChange = {},
                            onEditToggle = {},
                            onSearchClick = {},
                            onPickVideo = { _, _ -> }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchResultList(
    results: List<ExternalPlaylistRepository.MatchOutcome>,
    editingIndex: Int?,
    manualKeyword: String,
    manualSearching: Boolean,
    manualResults: List<ExternalPlaylistRepository.MatchedVideo>,
    onKeywordChange: (String) -> Unit,
    onEditToggle: (Int) -> Unit,
    onSearchClick: () -> Unit,
    onPickVideo: (Int, ExternalPlaylistRepository.MatchedVideo) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(results.size) { index ->
            val outcome = results[index]
            M3Surface(
                shape = AppShapes.container(ContainerLevel.Card),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            AppText(
                                text = "${index + 1}. ${outcome.track.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val secondary = outcome.video?.let { "${it.author} · ${it.title}" }
                                ?: outcome.track.artists.joinToString("/")
                            AppText(
                                text = secondary.ifBlank { "未匹配" },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (outcome.video != null) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        outcome.video?.cover?.takeIf { it.isNotBlank() }?.let { cover ->
                            AsyncImage(
                                model = cover,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(AppShapes.container(ContainerLevel.Chip)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        AppIconButton(onClick = { onEditToggle(index) }, modifier = Modifier.size(36.dp)) {
                            AppIcon(Icons.Outlined.Edit, contentDescription = "手动修正")
                        }
                    }
                    if (editingIndex == index) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppTextField(
                                value = manualKeyword,
                                onValueChange = onKeywordChange,
                                placeholder = "搜索 B 站视频",
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            AppButton(onClick = onSearchClick, modifier = Modifier.padding(start = 8.dp)) {
                                AppText(if (manualSearching) "..." else "搜索")
                            }
                        }
                        manualResults.forEach { video ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPickVideo(index, video) }
                                    .padding(vertical = 6.dp)
                            ) {
                                AsyncImage(
                                    model = video.cover,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(width = 64.dp, height = 40.dp)
                                        .clip(AppShapes.container(ContainerLevel.Chip)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    AppText(
                                        text = video.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    AppText(
                                        text = video.author,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
