// File: core/player/LoudnessAwareRenderersFactory.kt
package com.android.purebilibili.core.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink

/**
 * 在 Hi-Res 兼容渲染工厂之上注入响度均衡音频处理器。
 * 仅在「响度均衡」设置开启时由播放器构建路径选用。
 */
@OptIn(UnstableApi::class)
internal class LoudnessAwareRenderersFactory(
    context: Context
) : HiResCompatibleRenderersFactory(context) {

    private val loudnessProcessor = LoudnessNormalizationAudioProcessor()

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
        enableOffload: Boolean
    ): AudioSink {
        return DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            // 注入音频处理器后不使用 offload，否则 PCM 链路被旁路
            .setOffloadMode(DefaultAudioSink.OFFLOAD_MODE_DISABLED)
            .setAudioProcessors(arrayOf(loudnessProcessor as AudioProcessor))
            .build()
    }
}
