package com.android.purebilibili.feature.home.components.cards

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 视频封面边缘取色提取与缓存管理
 * 复用官方 androidx.palette:palette-ktx 能力，纯异步单向流，严防循环采样。
 */
object VideoCardCoverColorStore {
    private const val MAX_CACHE_SIZE = 128
    private val colorCache = LruCache<String, Color>(MAX_CACHE_SIZE)

    /**
     * 同步获取已缓存的封面底部代表色
     */
    fun getCachedColor(cacheKey: String): Color? {
        if (cacheKey.isBlank()) return null
        return synchronized(colorCache) {
            colorCache.get(cacheKey)
        }
    }

    /**
     * 异步提取封面底部边缘主色并存入缓存
     */
    fun extractColorAsync(
        cacheKey: String,
        bitmap: Bitmap,
        scope: CoroutineScope,
        onColorExtracted: (Color) -> Unit
    ) {
        if (cacheKey.isBlank()) return

        // 命中内存缓存直接同步返回
        getCachedColor(cacheKey)?.let {
            onColorExtracted(it)
            return
        }

        scope.launch(Dispatchers.Default) {
            val color = extractBottomEdgeColor(bitmap) ?: return@launch
            synchronized(colorCache) {
                colorCache.put(cacheKey, color)
            }
            withContext(Dispatchers.Main) {
                onColorExtracted(color)
            }
        }
    }

    /**
     * 针对封面底部 25% 区域进行高效提取（内部限制最多 8 种颜色聚类，耗时极低）
     */
    internal fun extractBottomEdgeColor(bitmap: Bitmap): Color? {
        return runCatching {
            if (bitmap.isRecycled) return@runCatching null

            val safeBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                bitmap.config == Bitmap.Config.HARDWARE
            ) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return@runCatching null
            } else {
                bitmap
            }

            val region = resolveCoverBottomSamplingRegion(safeBitmap.width, safeBitmap.height)
            val androidRect = Rect(region.left, region.top, region.right, region.bottom)

            val palette = Palette.from(safeBitmap)
                .setRegion(androidRect.left, androidRect.top, androidRect.right, androidRect.bottom)
                .maximumColorCount(8)
                .clearFilters()
                .generate()

            val colorInt = palette.vibrantSwatch?.rgb
                ?: palette.lightVibrantSwatch?.rgb
                ?: palette.darkVibrantSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
                ?: palette.mutedSwatch?.rgb

            colorInt?.let { Color(it) }
        }.getOrNull()
    }

    fun clear() {
        synchronized(colorCache) {
            colorCache.evictAll()
        }
    }
}
