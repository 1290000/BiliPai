package com.android.purebilibili.feature.video.share

import android.content.ClipData
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri

internal const val WECHAT_PACKAGE_NAME = "com.tencent.mm"
internal const val QQ_PACKAGE_NAME = "com.tencent.mobileqq"

internal data class VideoSharePayload(
    val title: String,
    val bvid: String,
    val coverUrl: String,
    val url: String,
    val text: String,
    val upName: String = "",
    val playCountText: String = "",
)

internal enum class VideoShareTarget(val packageName: String?) {
    WECHAT(WECHAT_PACKAGE_NAME),
    QQ(QQ_PACKAGE_NAME),
    COPY_LINK(null),
    MORE(null)
}

internal enum class VideoShareStyle {
    LINK,
    CARD,
}

internal fun buildVideoSharePayload(
    title: String,
    bvid: String,
    coverUrl: String = "",
    upName: String = "",
    playCountText: String = "",
): VideoSharePayload {
    val cleanTitle = title.trim()
    val cleanBvid = bvid.trim()
    val fallbackTitle = cleanTitle.ifBlank { cleanBvid }
    val url = "https://www.bilibili.com/video/$cleanBvid"
    return VideoSharePayload(
        title = fallbackTitle,
        bvid = cleanBvid,
        coverUrl = coverUrl.trim(),
        url = url,
        text = "【$fallbackTitle】\n$url",
        upName = upName.trim(),
        playCountText = playCountText.trim(),
    )
}

internal fun resolveVideoShareCardMetaLine(payload: VideoSharePayload): String {
    val parts = buildList {
        if (payload.upName.isNotBlank()) {
            add("UP主：${payload.upName}")
        }
        if (payload.playCountText.isNotBlank()) {
            add("播放：${payload.playCountText}")
        }
    }
    return parts.joinToString("  ·  ")
}

/**
 * 宿主 App 常把 Display Name / 文件名当消息标题，因此用净化后的视频标题命名。
 */
internal fun resolveVideoShareCardFileName(payload: VideoSharePayload): String {
    val rawTitle = payload.title.ifBlank { payload.bvid }.ifBlank { "video" }
    val sanitized = rawTitle
        .map { ch ->
            if (ch.isLetterOrDigit() || ch in "._- 《》【】（）()、，。！？") ch else '_'
        }
        .joinToString("")
        .trim()
        .replace(Regex("\\s+"), "_")
        .take(40)
        .trim('_')
        .ifBlank { payload.bvid.ifBlank { "video" } }
    return "BiliPai_share_card_$sanitized.jpg"
}

internal fun buildVideoShareIntent(payload: VideoSharePayload): Intent {
    return Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, payload.title)
        putExtra(Intent.EXTRA_TEXT, payload.text)
    }
}

internal fun buildTargetedShareIntent(
    payload: VideoSharePayload,
    packageName: String
): Intent {
    return buildVideoShareIntent(payload).apply {
        setPackage(packageName)
    }
}

/**
 * 卡片图分享：附带标题元数据与「标题 + 链接」正文，便于宿主展示正确标题并可跳转。
 */
internal fun buildVideoCoverShareIntent(
    payload: VideoSharePayload,
    coverUri: Uri,
    mimeType: String,
    packageName: String? = null,
    contentResolver: ContentResolver? = null
): Intent {
    return Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_SUBJECT, payload.title)
        putExtra(Intent.EXTRA_TITLE, payload.title)
        putExtra(Intent.EXTRA_TEXT, payload.text)
        putExtra(Intent.EXTRA_STREAM, coverUri)
        clipData = ClipData.newUri(contentResolver, payload.title, coverUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        packageName?.let { setPackage(it) }
    }
}

internal fun resolveVideoShareChooserTitle(payload: VideoSharePayload): String {
    return "分享「${payload.title}」"
}
