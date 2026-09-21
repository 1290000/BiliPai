package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LikedVideosRepository {
    data class Page(
        val items: List<VideoItem>,
        val total: Int,
    )

    suspend fun getLikedVideos(
        mid: Long,
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<Page> = withContext(Dispatchers.IO) {
        runCatching {
            val response = NetworkModule.api.getLikedVideos(mid, page, pageSize)
            check(response.code == 0) {
                response.message.ifBlank { "获取点赞视频失败：${response.code}" }
            }
            val data = response.data
            val detailedItems = data?.list.orEmpty().map { it.toVideoItem() }
            val aggregateItems = data?.item.orEmpty().map { item ->
                VideoItem(
                    id = item.aid,
                    aid = item.aid,
                    bvid = item.bvid.ifBlank { item.param },
                    cid = item.firstCid,
                    title = item.title,
                    pic = item.cover,
                    owner = com.android.purebilibili.data.model.response.Owner(name = item.author),
                    stat = com.android.purebilibili.data.model.response.Stat(
                        view = item.play,
                        danmaku = item.danmaku,
                        reply = item.reply,
                    ),
                    duration = item.duration,
                    pubdate = item.ctime,
                    tname = item.tname,
                )
            }
            Page(
                items = detailedItems.ifEmpty { aggregateItems },
                total = data?.count ?: 0,
            )
        }
    }
}
