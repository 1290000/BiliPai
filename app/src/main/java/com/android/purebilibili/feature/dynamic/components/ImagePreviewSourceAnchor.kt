package com.android.purebilibili.feature.dynamic.components

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * 记录缩略图在窗口中的坐标，供 [ImagePreviewDialog] 做「落位/回位」morph。
 *
 * 与 DrawGrid 内联的 `onGloballyPositioned + boundsInWindow` 捕获是同一模式，
 * 这里收敛成助手，供没有网格回调的入口（头像、封面、订阅文章图等）复用。
 */
fun rememberImagePreviewSourceRect(): MutableState<Rect?> {
    return remember { mutableStateOf(null) }
}

fun Modifier.imagePreviewSourceBounds(target: MutableState<Rect?>): Modifier =
    onGloballyPositioned { coordinates ->
        target.value = coordinates.boundsInWindow()
    }
