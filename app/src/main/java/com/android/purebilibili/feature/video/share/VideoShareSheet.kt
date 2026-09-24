package com.android.purebilibili.feature.video.share
import com.android.purebilibili.core.ui.components.AppHorizontalDivider

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreHoriz
import com.android.purebilibili.core.ui.components.AppIcon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppModalBottomSheet
import com.android.purebilibili.core.ui.components.AppNativeSegmentedControl
import com.android.purebilibili.core.ui.components.AppSegmentOption
import com.android.purebilibili.core.ui.common.copyPlainTextToClipboard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideoShareSheet(
    payload: VideoSharePayload,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shareScope = rememberCoroutineScope()
    var sharingTarget by remember { mutableStateOf<VideoShareTarget?>(null) }
    var shareStyle by remember { mutableStateOf(VideoShareStyle.LINK) }
    val neutralIconBackground = MaterialTheme.colorScheme.surfaceContainerHighest
    val neutralIconContent = MaterialTheme.colorScheme.onSurface
    val styleOptions = remember {
        listOf(
            AppSegmentOption(VideoShareStyle.LINK, "链接"),
            AppSegmentOption(VideoShareStyle.CARD, "卡片"),
        )
    }
    val items = listOf(
        VideoShareSheetItem(
            target = VideoShareTarget.WECHAT,
            label = "微信",
            iconText = "微",
            iconVector = null,
            backgroundColor = Color(0xFF31C95B),
            contentColor = Color.White
        ),
        VideoShareSheetItem(
            target = VideoShareTarget.QQ,
            label = "QQ",
            iconText = "Q",
            iconVector = null,
            backgroundColor = Color(0xFF25A9F2),
            contentColor = Color.White
        ),
        VideoShareSheetItem(
            target = VideoShareTarget.COPY_LINK,
            label = "复制链接",
            iconText = null,
            iconVector = Icons.Outlined.Link,
            backgroundColor = neutralIconBackground,
            contentColor = neutralIconContent
        ),
        VideoShareSheetItem(
            target = VideoShareTarget.MORE,
            label = "更多",
            iconText = null,
            iconVector = Icons.Outlined.MoreHoriz,
            backgroundColor = neutralIconBackground,
            contentColor = neutralIconContent
        )
    )

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            AppText(
                text = "分享",
                modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 14.dp),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )

            AppNativeSegmentedControl(
                options = styleOptions,
                selectedValue = shareStyle,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                onSelectionChange = { shareStyle = it },
            )
            AppText(
                text = if (shareStyle == VideoShareStyle.CARD) {
                    "以封面卡片图分享，对方更直观"
                } else {
                    "以标题 + 链接分享，方便直接打开"
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                items.forEach { item ->
                    VideoShareSheetItemView(
                        item = item,
                        onClick = {
                            when (item.target) {
                                VideoShareTarget.WECHAT,
                                VideoShareTarget.QQ -> {
                                    val packageName = item.target.packageName ?: return@VideoShareSheetItemView
                                    if (sharingTarget != null) return@VideoShareSheetItemView
                                    sharingTarget = item.target
                                    shareScope.launch {
                                        val shareMedia = prepareVideoShareMedia(
                                            context = context,
                                            payload = payload,
                                            style = shareStyle,
                                            progressMessage = "正在准备视频封面",
                                        )
                                        context.startTargetedVideoShare(
                                            payload = payload,
                                            packageName = packageName,
                                            appName = item.label,
                                            shareMedia = shareMedia,
                                            onSuccess = onDismiss
                                        )
                                        sharingTarget = null
                                    }
                                }
                                VideoShareTarget.COPY_LINK -> {
                                    copyPlainTextToClipboard(context, payload.url, "视频链接")
                                    Toast.makeText(context, "已复制链接", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                                VideoShareTarget.MORE -> {
                                    if (sharingTarget != null) return@VideoShareSheetItemView
                                    sharingTarget = item.target
                                    shareScope.launch {
                                        val shareMedia = prepareVideoShareMedia(
                                            context = context,
                                            payload = payload,
                                            style = shareStyle,
                                            progressMessage = "正在准备视频封面",
                                        )
                                        context.startMoreVideoShare(
                                            payload = payload,
                                            shareMedia = shareMedia,
                                            onSuccess = onDismiss
                                        )
                                        sharingTarget = null
                                    }
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            AppHorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                AppText(
                    text = "取消",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

@Immutable
private data class VideoShareSheetItem(
    val target: VideoShareTarget,
    val label: String,
    val iconText: String?,
    val iconVector: ImageVector?,
    val backgroundColor: Color,
    val contentColor: Color
)

@Composable
private fun VideoShareSheetItemView(
    item: VideoShareSheetItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(item.backgroundColor)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (item.iconVector != null) {
                AppIcon(
                    imageVector = item.iconVector,
                    contentDescription = item.label,
                    modifier = Modifier.size(28.dp),
                    tint = item.contentColor
                )
            } else {
                AppText(
                    text = item.iconText.orEmpty(),
                    color = item.contentColor,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        AppText(
            text = item.label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 按分享形态准备媒体：链接模式不附图；卡片模式合成卡片图，失败时回退空（走链接）。
 */
private suspend fun prepareVideoShareMedia(
    context: Context,
    payload: VideoSharePayload,
    style: VideoShareStyle,
    progressMessage: String,
): VideoShareCoverFile? {
    return when (style) {
        VideoShareStyle.LINK -> null
        VideoShareStyle.CARD -> {
            Toast.makeText(context, progressMessage, Toast.LENGTH_SHORT).show()
            val cardFile = prepareVideoShareCardFile(context, payload)
            if (cardFile == null) {
                Toast.makeText(context, "卡片生成失败，已改用链接分享", Toast.LENGTH_SHORT).show()
            }
            cardFile
        }
    }
}

private fun Context.startTargetedVideoShare(
    payload: VideoSharePayload,
    packageName: String,
    appName: String,
    shareMedia: VideoShareCoverFile?,
    onSuccess: () -> Unit
) {
    try {
        val intent = if (shareMedia != null) {
            buildVideoCoverShareIntent(
                payload = payload,
                coverUri = shareMedia.uri,
                mimeType = shareMedia.mimeType,
                packageName = packageName,
                contentResolver = contentResolver
            )
        } else {
            buildTargetedShareIntent(payload, packageName)
        }
        startActivityWithTaskFlag(intent)
        onSuccess()
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "未安装$appName", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(this, "无法打开$appName", Toast.LENGTH_SHORT).show()
    }
}

private fun Context.startMoreVideoShare(
    payload: VideoSharePayload,
    shareMedia: VideoShareCoverFile?,
    onSuccess: () -> Unit
) {
    try {
        val sendIntent = if (shareMedia != null) {
            buildVideoCoverShareIntent(
                payload = payload,
                coverUri = shareMedia.uri,
                mimeType = shareMedia.mimeType,
                contentResolver = contentResolver
            )
        } else {
            buildVideoShareIntent(payload)
        }
        val chooser = Intent.createChooser(sendIntent, "分享视频到")
        if (shareMedia != null) {
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivityWithTaskFlag(chooser)
        onSuccess()
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "无法打开分享面板", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(this, "分享失败", Toast.LENGTH_SHORT).show()
    }
}

private fun Context.startActivityWithTaskFlag(intent: Intent) {
    if (this !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}
