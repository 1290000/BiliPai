package com.android.purebilibili.feature.audio.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.android.purebilibili.core.ui.AppIcon
import com.android.purebilibili.core.ui.AppIconButton
import com.android.purebilibili.core.ui.AppSurface
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.AppText
import com.android.purebilibili.feature.audio.player.MusicQueueItemUi
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 3D 实体 CD 盒切歌转盘 / 唱片架 (Cover Flow)。
 *
 * 核心视觉与交互特色：
 * 1. 纯原生 Compose 3D 透视：借助 graphicsLayer 中的 cameraDistance、rotationY、scale 与 translationX，
 *    实现高帧率硬件加速的 Y 轴 3D 偏转流动效果。
 * 2. 实体 CD 盒拟材质感：亚克力双层高光边框、左侧侧脊厚度高光（Spine）、斜向高光折射。
 * 3. 镜面地面倒影（Floor Reflection）：下方倒影垂直翻转并加渐变虚化消融，呈现桌面摆放沉浸感。
 * 4. 悬浮胶囊控制条：复刻底部一体化药丸切歌与播放控制 bar。
 */
@Composable
internal fun Music3DCoverFlow(
    queue: List<MusicQueueItemUi>,
    currentIndex: Int,
    isPlaying: Boolean,
    onItemClick: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    modifier: Modifier = Modifier,
    isLiked: Boolean = false,
    onLikeClick: (() -> Unit)? = null,
    cardSizeDp: Int = 160
) {
    if (queue.isEmpty()) return

    val validCurrentIndex = currentIndex.coerceIn(0, queue.size - 1)
    val pagerState = rememberPagerState(
        initialPage = validCurrentIndex,
        pageCount = { queue.size }
    )
    val coroutineScope = rememberCoroutineScope()

    // 监听当前曲目切换，联动 3D 唱片架滚动到居中位置
    LaunchedEffect(currentIndex) {
        if (currentIndex in queue.indices && pagerState.currentPage != currentIndex) {
            pagerState.animateScrollToPage(currentIndex)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 3D 唱片架滚动舞台
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 96.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height((cardSizeDp + 48).dp)
        ) { page ->
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
            val item = queue[page]
            val isPlayingThis = page == currentIndex

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // 1. 设置透视摄像机距离（景深感）
                        cameraDistance = 10 * density

                        // 2. Y 轴 3D 旋转角度（偏转并限制最大角度，模拟实体唱片架折角）
                        rotationY = (pageOffset * -36f).coerceIn(-60f, 60f)

                        // 3. 缩放景深层次
                        val scale = (1f - (abs(pageOffset) * 0.16f)).coerceIn(0.72f, 1f)
                        scaleX = scale
                        scaleY = scale

                        // 4. 重叠排列（让两侧 CD 壳产生自然的堆叠遮挡感）
                        translationX = pageOffset * -20.dp.toPx()

                        // 5. 层次渐隐
                        alpha = (1f - (abs(pageOffset) * 0.22f)).coerceIn(0.45f, 1f)
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(page)
                        }
                        onItemClick(page)
                    }
                ) {
                    // 实体 CD 盒主体
                    Box(
                        modifier = Modifier
                            .size(cardSizeDp.dp)
                            .shadow(
                                elevation = if (abs(pageOffset) < 0.5f) 18.dp else 6.dp,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.42f),
                                        Color.White.copy(alpha = 0.08f)
                                    )
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        // 唱片封面
                        AsyncImage(
                            model = item.coverUrl,
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // 实体 CD 盒左侧侧脊厚度高光（Jewel Case Spine）
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(3.5.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.60f),
                                            Color.White.copy(alpha = 0.18f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        // 亚克力塑料斜向光斑漫反射（Acrylic Sheen）
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        0.0f to Color.White.copy(alpha = 0.16f),
                                        0.28f to Color.White.copy(alpha = 0.04f),
                                        0.55f to Color.Transparent
                                    )
                                )
                        )

                        // 正在播放中的发光角标
                        if (isPlayingThis) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(24.dp)
                                    .background(MusicAccentColor.copy(alpha = 0.90f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                AppIcon(
                                    imageVector = if (isPlaying) Icons.Outlined.MusicNote else Icons.Filled.Pause,
                                    contentDescription = if (isPlaying) "正在播放" else "已暂停",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // 地面镜面反射 (Floor Mirror Reflection)
                    Box(
                        modifier = Modifier
                            .size(width = cardSizeDp.dp, height = 32.dp)
                            .graphicsLayer {
                                scaleY = -1f // 倒影垂直反转
                            }
                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    ) {
                        AsyncImage(
                            model = item.coverUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(cardSizeDp.dp)
                                .blur(2.dp),
                            contentScale = ContentScale.Crop,
                            alpha = 0.32f
                        )
                        // 渐变遮罩：让倒影向下迅速消隐进桌面背景
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.88f)
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 底部悬浮胶囊控制条（悬停观赏、优雅切歌）
        val focusedItem = queue.getOrNull(pagerState.currentPage) ?: queue[validCurrentIndex]
        AppSurface(
            shape = RoundedCornerShape(24.dp),
            color = AppSurfaceTokens.cardContainer(),
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 4.dp)
                .height(48.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 歌名 - 艺术家
                AppText(
                    text = "${focusedItem.title}  ·  ${focusedItem.artist.ifBlank { "未知艺术家" }}",
                    color = MusicContentColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(6.dp))

                // 点赞按钮
                onLikeClick?.let { like ->
                    AppIconButton(onClick = like, modifier = Modifier.size(36.dp)) {
                        AppIcon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isLiked) "取消点赞" else "点赞",
                            tint = if (isLiked) MaterialTheme.colorScheme.error else MusicContentColor.copy(alpha = 0.80f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 上一首
                AppIconButton(
                    onClick = onPrevious ?: {},
                    enabled = onPrevious != null,
                    modifier = Modifier.size(36.dp)
                ) {
                    AppIcon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "上一首",
                        tint = MusicContentColor.copy(alpha = if (onPrevious != null) 0.92f else 0.28f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 播放 / 暂停
                AppIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(36.dp)
                ) {
                    AppIcon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = MusicAccentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 下一首
                AppIconButton(
                    onClick = onNext ?: {},
                    enabled = onNext != null,
                    modifier = Modifier.size(36.dp)
                ) {
                    AppIcon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "下一首",
                        tint = MusicContentColor.copy(alpha = if (onNext != null) 0.92f else 0.28f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
