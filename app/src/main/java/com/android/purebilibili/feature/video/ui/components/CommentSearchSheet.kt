package com.android.purebilibili.feature.video.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.android.purebilibili.core.ui.AppModalBottomSheet
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppOutlinedTextField
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.ReplyItem

/**
 * 评论区搜索排序模式
 */
enum class CommentSearchSortMode(val label: String) {
    HOT("最热"),
    TIME("最新")
}

/**
 * 评论搜索项包装
 */
data class CommentSearchEntry(
    val reply: ReplyItem,
    val rootReply: ReplyItem?,
    val isSubReply: Boolean,
)

/**
 * 本视频评论区搜索抽屉
 * 支持在当前已加载的评论（包括主评论与楼中楼子评论）中快速模糊搜索关键字，
 * 高亮显示匹配内容、UP主专属标记、发布时间、楼层与点赞数，点击可定位或跳转回复。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentSearchSheet(
    replies: List<ReplyItem>,
    upMid: Long = 0L,
    onCommentClick: (ReplyItem) -> Unit,
    onSubReplyClick: (ReplyItem) -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var searchQuery by remember { mutableStateOf("") }
    var onlyUp by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(CommentSearchSortMode.HOT) }

    // 扁平化收集所有评论（主评论 + 二级子评论）
    val allEntries = remember(replies) {
        val list = mutableListOf<CommentSearchEntry>()
        for (root in replies) {
            list.add(CommentSearchEntry(reply = root, rootReply = null, isSubReply = false))
            root.replies?.forEach { sub ->
                list.add(CommentSearchEntry(reply = sub, rootReply = root, isSubReply = true))
            }
        }
        list
    }

    val filteredResults by remember(allEntries, searchQuery, onlyUp, sortMode, upMid) {
        derivedStateOf {
            val query = searchQuery.trim()
            if (query.isEmpty()) {
                emptyList()
            } else {
                val matched = allEntries.filter { entry ->
                    val messageMatches = entry.reply.content.message.contains(query, ignoreCase = true)
                    val authorMatches = entry.reply.member.uname.contains(query, ignoreCase = true)
                    val passesUpFilter = !onlyUp || entry.reply.member.mid == upMid.toString()
                    (messageMatches || authorMatches) && passesUpFilter
                }
                when (sortMode) {
                    CommentSearchSortMode.HOT -> matched.sortedByDescending { it.reply.like }
                    CommentSearchSortMode.TIME -> matched.sortedByDescending { it.reply.ctime }
                }
            }
        }
    }

    val listState = rememberLazyListState()

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // 顶栏：标题 + 关闭按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppText(
                        text = "搜索评论",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (searchQuery.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        AppText(
                            text = "找到 ${filteredResults.size} 条",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                AppIconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp),
                ) {
                    AppIcon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // 搜索输入框
            AppOutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                placeholder = {
                    AppText(
                        text = "搜索本视频评论内容或作者昵称...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
                leadingIcon = {
                    AppIcon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        AppIconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(28.dp),
                        ) {
                            AppIcon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "清空",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                },
                singleLine = true,
            )

            // 筛选标签栏 (全部 / 只看UP主 / 排序)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // UP 主筛选开关
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CommentSearchFilterChip(
                        selected = !onlyUp,
                        label = "全部评论",
                        onClick = { onlyUp = false },
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    CommentSearchFilterChip(
                        selected = onlyUp,
                        label = "只看UP主",
                        onClick = { onlyUp = true },
                    )
                }

                // 排序模式切换
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppSurfaceTokens.surfaceContainerHigh())
                        .padding(2.dp),
                ) {
                    CommentSearchSortMode.entries.forEach { mode ->
                        val isSelected = mode == sortMode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { sortMode = mode }
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            AppText(
                                text = mode.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 列表或提示
            if (searchQuery.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AppIcon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        AppText(
                            text = "输入关键词搜索已加载的评论",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AppText(
                            text = "支持搜索主评论、楼中楼及作者名称",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        )
                    }
                }
            } else if (filteredResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AppText(
                            text = "未找到包含「$searchQuery」的评论",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AppText(
                            text = "可滚动评论区加载更多评论后再试",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = filteredResults,
                        key = { "${it.reply.rpid}_${it.isSubReply}" }
                    ) { entry ->
                        CommentSearchResultRow(
                            entry = entry,
                            searchQuery = searchQuery,
                            isUp = entry.reply.member.mid == upMid.toString(),
                            onClick = {
                                onDismiss()
                                if (entry.isSubReply && entry.rootReply != null) {
                                    onSubReplyClick(entry.rootReply)
                                } else {
                                    onCommentClick(entry.reply)
                                }
                            },
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(entry.reply.content.message))
                                Toast.makeText(context, "评论已复制", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentSearchFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                else AppSurfaceTokens.surfaceContainerHigh()
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CommentSearchResultRow(
    entry: CommentSearchEntry,
    searchQuery: String,
    isUp: Boolean,
    onClick: () -> Unit,
    onCopy: () -> Unit,
) {
    val context = LocalContext.current
    val item = entry.reply
    val primaryColor = MaterialTheme.colorScheme.primary

    AppSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = AppShapes.container(ContainerLevel.Card),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 头部：头像 + 用户名 + UP主标签 + 楼中楼标记 + 时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(FormatUtils.fixImageUrl(item.member.avatar))
                            .crossfade(true)
                            .build(),
                        contentDescription = item.member.uname,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AppText(
                        text = highlightQuery(item.member.uname, searchQuery, primaryColor),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isUp) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            AppText(
                                text = "UP主",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (entry.isSubReply) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            AppText(
                                text = "楼中楼回复",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }

                // 发布时间
                val timeStr = if (item.ctime > 0) {
                    FormatUtils.formatPublishTime(item.ctime * 1000L)
                } else ""
                if (timeStr.isNotBlank()) {
                    AppText(
                        text = timeStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 评论内容（关键字高亮）
            AppText(
                text = highlightQuery(item.content.message, searchQuery, primaryColor),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 底部：点赞数 + 回复数 + IP属地
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.like > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(
                                imageVector = Icons.Outlined.ThumbUp,
                                contentDescription = "点赞",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            AppText(
                                text = FormatUtils.formatStat(item.like.toLong()),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    if (item.rcount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "回复",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            AppText(
                                text = "${item.rcount}条回复",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    val location = item.replyControl?.location
                    if (!location.isNullOrBlank()) {
                        AppText(
                            text = location,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        )
                    }
                }

                // 复制按钮
                AppText(
                    text = "复制",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable(onClick = onCopy)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * 将匹配查询文本高亮
 */
private fun highlightQuery(
    text: String,
    query: String,
    primaryColor: Color
): AnnotatedString {
    val trimmedQuery = query.trim()
    if (trimmedQuery.isEmpty() || !text.contains(trimmedQuery, ignoreCase = true)) {
        return AnnotatedString(text)
    }
    return buildAnnotatedString {
        var startIndex = 0
        val lowerText = text.lowercase()
        val lowerQuery = trimmedQuery.lowercase()
        while (startIndex < text.length) {
            val index = lowerText.indexOf(lowerQuery, startIndex)
            if (index == -1) {
                append(text.substring(startIndex))
                break
            }
            if (index > startIndex) {
                append(text.substring(startIndex, index))
            }
            withStyle(
                SpanStyle(
                    color = primaryColor,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(text.substring(index, index + trimmedQuery.length))
            }
            startIndex = index + trimmedQuery.length
        }
    }
}
