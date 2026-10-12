// 文件路径: core/cache/PlayUrlCache.kt
package com.android.purebilibili.core.cache

import android.util.LruCache
import com.android.purebilibili.data.model.response.PlayUrlData
import com.android.purebilibili.data.repository.videoPlaybackAccountScope

/**
 * 播放地址缓存管理器
 * 
 * 使用 LruCache 缓存视频播放 URL，减少重复网络请求。
 * 缓存上限 50 条，有效期 10 分钟。
 */
object PlayUrlCache {
    
    private const val TAG = "PlayUrlCache"
    private const val MAX_CACHE_SIZE = 80  //  优化：增加缓存容量
    private const val CACHE_DURATION_MS = 10 * 60 * 1000L // 10 分钟
    
    /**
     * 缓存条目
     */
    data class CachedPlayUrl(
        val bvid: String,
        val cid: Long,
        val data: PlayUrlData,
        val quality: Int,
        val requestedQuality: Int?,
        val requestedAudioLang: String?,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        val expiresAt: Long = resolvePlayUrlCacheExpiresAt(
            timestampMs = timestamp,
            maxAgeMs = CACHE_DURATION_MS,
            urls = buildList {
                data.dash?.video.orEmpty().forEach { track ->
                    add(track.baseUrl)
                    addAll(track.backupUrl.orEmpty())
                }
                val audios = data.dash?.audio.orEmpty() + data.dash?.dolby?.audio.orEmpty() +
                    listOfNotNull(data.dash?.flac?.audio)
                audios.forEach { track ->
                    add(track.baseUrl)
                    addAll(track.backupUrl.orEmpty())
                }
                data.durl.orEmpty().forEach { stream ->
                    add(stream.url)
                    addAll(stream.backupUrl.orEmpty())
                }
            }
        )
        
        fun isExpired(): Boolean = System.currentTimeMillis() >= expiresAt
    }
    
    /**
     * 生成缓存键
     */
    private fun generateKey(
        bvid: String,
        cid: Long,
        requestedQuality: Int?,
        audioLang: String?,
        accountScope: String
    ): String {
        val qualityKey = requestedQuality?.toString() ?: "auto"
        val audioKey = audioLang ?: "default"
        return "$accountScope:$bvid:$cid:q=$qualityKey:a=$audioKey"
    }

    private fun generateVideoPrefix(bvid: String, cid: Long, accountScope: String): String =
        "$accountScope:$bvid:$cid:"
    
    /**
     * 缓存实例
     */
    private val cache: LruCache<String, CachedPlayUrl> = LruCache(MAX_CACHE_SIZE)
    
    /**
     * 获取缓存的播放地址
     * 
     * @param bvid 视频 BV 号
     * @param cid 视频 CID
     * @param requestedQuality 请求画质（null 表示自动）
     * @param audioLang 音轨语言（null 表示默认）
     * @return 缓存的播放数据，如果缓存不存在或已过期则返回 null
     */
    @Synchronized
    fun get(
        bvid: String,
        cid: Long,
        requestedQuality: Int? = null,
        audioLang: String? = null,
        accountScope: String = videoPlaybackAccountScope()
    ): PlayUrlData? {
        val key = generateKey(bvid, cid, requestedQuality, audioLang, accountScope)
        val exact = cache.get(key)

        val (matchedKey, cached) = when {
            exact != null -> key to exact
            requestedQuality == null && audioLang == null -> {
                findNewestValidCacheForVideo(bvid, cid, accountScope)
                    ?: (key to null)
            }
            else -> key to null
        }

        return when {
            cached == null -> {
                com.android.purebilibili.core.util.Logger.d(
                    TAG,
                    " Cache miss: bvid=$bvid, cid=$cid, reqQ=${requestedQuality ?: "auto"}, lang=${audioLang ?: "default"}"
                )
                null
            }
            cached.isExpired() -> {
                com.android.purebilibili.core.util.Logger.d(
                    TAG,
                    "⏰ Cache expired: bvid=$bvid, cid=$cid, reqQ=${requestedQuality ?: "auto"}, lang=${audioLang ?: "default"}"
                )
                cache.remove(matchedKey)
                null
            }
            else -> {
                val remainingMs = cached.expiresAt - System.currentTimeMillis()
                com.android.purebilibili.core.util.Logger.d(
                    TAG,
                    " Cache hit: bvid=$bvid, cid=$cid, reqQ=${cached.requestedQuality ?: "auto"}, actualQ=${cached.quality}, lang=${cached.requestedAudioLang ?: "default"}, expires in ${remainingMs / 1000}s"
                )
                cached.data
            }
        }
    }
    
    /**
     * 添加播放地址到缓存
     * 
     * @param bvid 视频 BV 号
     * @param cid 视频 CID
     * @param data 播放数据
     * @param quality 请求画质（null 表示自动）
     * @param audioLang 音轨语言（null 表示默认）
     */
    @Synchronized
    fun put(
        bvid: String,
        cid: Long,
        data: PlayUrlData,
        quality: Int? = null,
        audioLang: String? = null,
        accountScope: String = videoPlaybackAccountScope()
    ) {
        val key = generateKey(bvid, cid, quality, audioLang, accountScope)
        val entry = CachedPlayUrl(
            bvid = bvid,
            cid = cid,
            data = data,
            quality = data.quality,
            requestedQuality = quality,
            requestedAudioLang = audioLang
        )
        if (entry.isExpired()) {
            cache.remove(key)
            return
        }
        cache.put(key, entry)
        com.android.purebilibili.core.util.Logger.d(
            TAG,
            " Cached: bvid=$bvid, cid=$cid, reqQ=${quality ?: "auto"}, actualQ=${data.quality}, lang=${audioLang ?: "default"}"
        )
    }
    
    /**
     * 使指定视频的缓存失效
     */
    @Synchronized
    fun invalidate(
        bvid: String,
        cid: Long,
        requestedQuality: Int? = null,
        audioLang: String? = null,
        accountScope: String = videoPlaybackAccountScope()
    ) {
        if (requestedQuality == null && audioLang == null) {
            val prefix = generateVideoPrefix(bvid, cid, accountScope)
            var removedCount = 0
            val keys = cache.snapshot().keys
            keys.forEach { key ->
                if (key.startsWith(prefix)) {
                    cache.remove(key)
                    removedCount++
                }
            }
            com.android.purebilibili.core.util.Logger.d(
                TAG,
                " Invalidated all variants: bvid=$bvid, cid=$cid, removed=$removedCount"
            )
            return
        }

        val key = generateKey(bvid, cid, requestedQuality, audioLang, accountScope)
        cache.remove(key)
        com.android.purebilibili.core.util.Logger.d(
            TAG,
            " Invalidated variant: bvid=$bvid, cid=$cid, reqQ=${requestedQuality ?: "auto"}, lang=${audioLang ?: "default"}"
        )
    }
    
    /**
     * 清除所有缓存
     */
    @Synchronized
    fun clear() {
        cache.evictAll()
        com.android.purebilibili.core.util.Logger.d(TAG, " Cache cleared")
    }
    
    /**
     * 获取当前缓存大小
     */
    fun size(): Int = cache.size()
    
    /**
     * 获取缓存统计信息（调试用）
     */
    fun getStats(): String {
        return "PlayUrlCache: size=${size()}, maxSize=$MAX_CACHE_SIZE, " +
               "hitCount=${cache.hitCount()}, missCount=${cache.missCount()}"
    }

    private fun findNewestValidCacheForVideo(bvid: String, cid: Long, accountScope: String): Pair<String, CachedPlayUrl>? {
        val prefix = generateVideoPrefix(bvid, cid, accountScope)
        var candidate: Pair<String, CachedPlayUrl>? = null
        val snapshot = cache.snapshot()

        snapshot.forEach { (key, entry) ->
            if (!key.startsWith(prefix)) return@forEach

            if (entry.isExpired()) {
                cache.remove(key)
                return@forEach
            }

            val currentCandidate = candidate
            if (currentCandidate == null || entry.timestamp > currentCandidate.second.timestamp) {
                candidate = key to entry
            }
        }

        return candidate
    }
}
