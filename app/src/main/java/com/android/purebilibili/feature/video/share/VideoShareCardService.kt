package com.android.purebilibili.feature.video.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.Log
import androidx.core.content.FileProvider
import com.android.purebilibili.core.util.FormatUtils
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val CARD_WIDTH = 1080
private const val CARD_PADDING = 56
private const val CARD_CORNER_RADIUS = 28f
private const val CARD_BRAND = "BiliPai"
private const val CARD_TITLE_MAX_LINES = 3

/**
 * 合成可分享的视频卡片图（标题 + 数据 + 封面 + 应用署名）。
 * 成功时返回可被微信/QQ 当作图片消息接收的 FileProvider Uri。
 */
internal suspend fun prepareVideoShareCardFile(
    context: Context,
    payload: VideoSharePayload
): VideoShareCoverFile? {
    if (payload.coverUrl.isBlank()) return null
    val coverUrl = FormatUtils.resolveVideoCoverUrl(
        url = payload.coverUrl,
        useLowQuality = false
    )
    if (coverUrl.isBlank()) return null
    return withContext(Dispatchers.IO) {
        runCatching {
            val coverBitmap = downloadVideoShareBitmap(coverUrl)
                ?: error("Cover bitmap decode failed")
            val cardBitmap = renderVideoShareCardBitmap(
                payload = payload,
                coverBitmap = coverBitmap
            )
            coverBitmap.recycle()
            val cacheDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
            cleanupVideoShareCardCache(cacheDir)
            val outputFile = File(cacheDir, resolveVideoShareCardFileName(payload))
            outputFile.outputStream().use { output ->
                cardBitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
            }
            cardBitmap.recycle()
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )
            VideoShareCoverFile(uri = uri, mimeType = "image/jpeg")
        }.onFailure { error ->
            Log.e("VideoShare", "Prepare video share card failed", error)
        }.getOrNull()
    }
}

internal fun renderVideoShareCardBitmap(
    payload: VideoSharePayload,
    coverBitmap: Bitmap
): Bitmap {
    val contentWidth = CARD_WIDTH - CARD_PADDING * 2
    val coverHeight = (contentWidth * 9f / 16f).toInt()
    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1F1F1F")
        textSize = 52f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7A7A7A")
        textSize = 36f
    }
    val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9A9A9A")
        textSize = 32f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val titleLayout = buildShareCardTitleLayout(
        title = payload.title,
        paint = titlePaint,
        maxWidth = contentWidth
    )
    val metaLine = resolveVideoShareCardMetaLine(payload)
    val titleBlockHeight = titleLayout.height
    val metaBlockHeight = if (metaLine.isBlank()) 0 else 52
    val gapTitleMeta = if (metaBlockHeight == 0) 0f else 18f
    val gapMetaCover = if (metaBlockHeight == 0) 36f else 28f
    val gapCoverBrand = 36f
    val brandHeight = 44
    val cardHeight = (CARD_PADDING * 2 +
        titleBlockHeight +
        gapTitleMeta +
        metaBlockHeight +
        gapMetaCover +
        coverHeight +
        gapCoverBrand +
        brandHeight).toInt()

    val bitmap = Bitmap.createBitmap(CARD_WIDTH, cardHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.WHITE)

    var y = CARD_PADDING + 48f
    canvas.save()
    canvas.translate(CARD_PADDING.toFloat(), y)
    titleLayout.draw(canvas)
    canvas.restore()
    y += titleBlockHeight
    if (metaBlockHeight > 0) {
        y += gapTitleMeta
        canvas.drawText(metaLine, CARD_PADDING.toFloat(), y, metaPaint)
        y += metaBlockHeight
    }
    y += gapMetaCover
    drawRoundedBitmap(
        canvas = canvas,
        bitmap = coverBitmap,
        left = CARD_PADDING.toFloat(),
        top = y,
        width = contentWidth.toFloat(),
        height = coverHeight.toFloat(),
        radius = CARD_CORNER_RADIUS
    )
    y += coverHeight + gapCoverBrand
    canvas.drawText(CARD_BRAND, CARD_PADDING.toFloat(), y, brandPaint)
    return bitmap
}

internal fun buildShareCardTitleLayout(
    title: String,
    paint: TextPaint,
    maxWidth: Int
): StaticLayout {
    val source = title.trim().ifBlank { " " }
    return StaticLayout.Builder
        .obtain(source, 0, source.length, paint, maxWidth.coerceAtLeast(1))
        .setMaxLines(CARD_TITLE_MAX_LINES)
        .setEllipsize(TextUtils.TruncateAt.END)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setIncludePad(false)
        .build()
}

private fun drawRoundedBitmap(
    canvas: Canvas,
    bitmap: Bitmap,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    radius: Float
) {
    val path = Path()
    val rect = RectF(left, top, left + width, top + height)
    path.addRoundRect(rect, radius, radius, Path.Direction.CW)
    canvas.save()
    canvas.clipPath(path)
    val src = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
    canvas.drawBitmap(bitmap, src, rect, Paint(Paint.ANTI_ALIAS_FLAG))
    canvas.restore()
}

private fun downloadVideoShareBitmap(coverUrl: String): Bitmap? {
    val connection = URL(coverUrl).openConnection() as HttpURLConnection
    try {
        connection.setRequestProperty("Referer", "https://www.bilibili.com/")
        connection.connect()
        check(connection.responseCode in 200..299) {
            "Cover download failed: ${connection.responseCode}"
        }
        return connection.inputStream.use { input ->
            BitmapFactory.decodeStream(input)
        }
    } finally {
        connection.disconnect()
    }
}

private fun cleanupVideoShareCardCache(cacheDir: File) {
    val now = System.currentTimeMillis()
    cacheDir.listFiles()
        ?.filter { file ->
            file.name.startsWith("BiliPai_share_card_") &&
                now - file.lastModified() > VIDEO_SHARE_CARD_CACHE_TTL_MS
        }
        ?.forEach { file -> file.delete() }
}

private const val VIDEO_SHARE_CARD_CACHE_TTL_MS = 24L * 60L * 60L * 1000L
