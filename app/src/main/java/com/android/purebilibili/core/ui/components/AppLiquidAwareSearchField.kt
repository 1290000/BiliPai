package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Compatibility entry point for search screens; the visible field is the theme's native component. */
@Composable
fun AppLiquidAwareSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索",
    onSearch: () -> Unit = {},
    onClear: () -> Unit = { onQueryChange("") },
    autoFocusEnabled: Boolean = false,
    focusRequester: FocusRequester? = null,
    interactionSource: MutableInteractionSource? = null,
    leadingIconHorizontalOffset: Dp = 0.dp,
) {
    AppSearchField(
        query = query,
        onQueryChange = onQueryChange,
        modifier = modifier,
        placeholder = placeholder,
        onSearch = onSearch,
        onClear = onClear,
        autoFocusEnabled = autoFocusEnabled,
        focusRequester = focusRequester,
        interactionSource = interactionSource,
        leadingIconHorizontalOffset = leadingIconHorizontalOffset,
    )
}
