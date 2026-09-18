package com.android.purebilibili.feature.message.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import com.android.purebilibili.core.ui.components.AppButton
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.rememberContentCardSurfaceSpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun formatMessageFeedTime(timestampSeconds: Int): String {
    if (timestampSeconds <= 0) return ""
    val now = System.currentTimeMillis()
    val msgTime = timestampSeconds * 1000L
    val diff = now - msgTime
    return when {
        diff < 60_000L -> "刚刚"
        diff < 3_600_000L -> "${diff / 60_000L}分钟前"
        diff < 86_400_000L -> "${diff / 3_600_000L}小时前"
        diff < 172_800_000L -> "昨天"
        else -> SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(msgTime))
    }
}

internal fun firstNonBlank(vararg values: String?): String? {
    return values.firstOrNull { !it.isNullOrBlank() }?.trim()
}

internal fun buildMessageFeedCommentNavigationLink(
    nativeUri: String?,
    uri: String?,
    businessId: Int,
    subjectId: Long,
    rootId: Long,
    sourceId: Long,
    targetId: Long
): String? {
    val trimmedNative = nativeUri?.trim().orEmpty()
    val trimmedUri = uri?.trim().orEmpty()

    // If nativeUri is already a valid absolute URI (e.g. bilibili:// or https://)
    if (trimmedNative.contains("://")) {
        return trimmedNative
    }

    // Extract any IDs from query if nativeUri or uri is a query string or has parameters
    val queryCandidate = when {
        trimmedNative.startsWith("?") -> trimmedNative.removePrefix("?")
        trimmedNative.contains("?") -> trimmedNative.substringAfter("?")
        trimmedUri.contains("?") -> trimmedUri.substringAfter("?")
        else -> null
    }
    val queryParams = queryCandidate?.split("&")?.mapNotNull { param ->
        val pair = param.split("=", limit = 2)
        if (pair.isEmpty() || pair[0].isBlank()) null
        else pair[0].trim() to pair.getOrElse(1) { "" }.trim()
    }?.toMap().orEmpty()

    val queryRootId = listOf("comment_root_id", "root_reply_id", "root_id")
        .firstNotNullOfOrNull { key -> queryParams[key]?.toLongOrNull()?.takeIf { it > 0L } } ?: 0L
    val queryTargetId = listOf("comment_id", "reply_id", "rpid", "target_id", "anchor", "source_id")
        .firstNotNullOfOrNull { key -> queryParams[key]?.toLongOrNull()?.takeIf { it > 0L } } ?: 0L

    val resolvedRootId = listOf(rootId, queryRootId, sourceId, targetId, queryTargetId)
        .firstOrNull { it > 0L } ?: 0L
    val resolvedTargetId = listOf(targetId, queryTargetId, sourceId)
        .firstOrNull { it > 0L && it != resolvedRootId } ?: 0L

    // If we have businessId, subjectId and rootReplyId, build the canonical comment deep link
    if (businessId > 0 && subjectId > 0L && resolvedRootId > 0L) {
        val targetQuery = if (resolvedTargetId > 0L) "?comment_id=$resolvedTargetId" else ""
        val enterUriParam = if (trimmedUri.contains("://")) {
            val sep = if (targetQuery.isEmpty()) "?" else "&"
            val encodedEnterUri = runCatching {
                java.net.URLEncoder.encode(trimmedUri, "UTF-8")
            }.getOrDefault(trimmedUri)
            "${sep}enterUri=$encodedEnterUri"
        } else ""
        return "bilibili://comment/detail/$businessId/$subjectId/$resolvedRootId$targetQuery$enterUriParam"
    }

    // If uri is an absolute web or scheme link (e.g. video page), fallback to it
    if (trimmedUri.contains("://")) {
        return trimmedUri
    }

    return null
}

@Composable
internal fun MessageFeedAvatar(
    avatarUrl: String,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = avatarUrl,
        contentDescription = "头像",
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentScale = ContentScale.Crop
    )
}

@Composable
internal fun MessageFeedEmpty(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AppText(text = text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun MessageFeedError(
    text: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppText(text = text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AppButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
            AppText("重试")
        }
    }
}

@Composable
internal fun MessageFeedLoadMore(
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit
) {
    if (!hasMore && !isLoadingMore) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoadingMore) {
            com.android.purebilibili.core.ui.CutePersonLoadingIndicator(
                size = 24.dp
            )
        } else {
            AppTextButton(onClick = onLoadMore) {
                AppText("加载更多")
            }
        }
    }
}

@Composable
internal fun MessageFeedSectionHeader(text: String) {
    AppText(
        text = text,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
internal fun MessageFeedCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val surfaceSpec = rememberContentCardSurfaceSpec()
    AppSurface(
        modifier = modifier,
        shape = AppShapes.borderedContainer(surfaceSpec.cornerLevel),
        color = if (surfaceSpec.usesTonalContainerTreatment) {
            AppSurfaceTokens.surfaceContainer()
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        },
        border = if (surfaceSpec.usesTonalContainerTreatment) {
            androidx.compose.foundation.BorderStroke(
                surfaceSpec.borderWidthDp.dp,
                AppSurfaceTokens.divider().copy(alpha = surfaceSpec.borderAlpha)
            )
        } else {
            null
        }
    ) {
        content()
    }
}
