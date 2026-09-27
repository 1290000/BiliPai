package com.android.purebilibili.feature.audio.screen

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import com.android.purebilibili.data.repository.ExternalPlaylistRepository

internal data class ExternalPlaylistNativeDialogState(
    val inputText: String,
    val fetchError: String?,
    val fetching: Boolean,
    val playlist: ExternalPlaylistRepository.ExternalPlaylistMeta?,
    val matching: Boolean,
    val matchCompleted: Int,
    val matchTotal: Int,
    val matchingTrackTitle: String,
    val matchResults: List<ExternalPlaylistRepository.MatchOutcome>,
    val editingIndex: Int?,
    val manualKeyword: String,
    val manualSearching: Boolean,
    val manualResults: List<ExternalPlaylistRepository.MatchedVideo>,
)

internal data class ExternalPlaylistNativeDialogActions(
    val onDismiss: () -> Unit,
    val onInputChange: (String) -> Unit,
    val onFetch: () -> Unit,
    val onStartMatching: () -> Unit,
    val onStopMatching: () -> Unit,
    val onSave: () -> Unit,
    val onToggleEdit: (Int) -> Unit,
    val onKeywordChange: (String) -> Unit,
    val onManualSearch: () -> Unit,
    val onPickVideo: (Int, ExternalPlaylistRepository.MatchedVideo) -> Unit,
)

@Composable
internal fun ExternalPlaylistNativeDialogContent(
    state: ExternalPlaylistNativeDialogState,
    actions: ExternalPlaylistNativeDialogActions,
    surfaceColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    errorColor: Color,
) {
    AndroidView(
        modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.88f),
        factory = { context -> ExternalPlaylistNativeDialogView(context) },
        update = { view ->
            view.render(
                state = state,
                actions = actions,
                surfaceColor = surfaceColor.toArgb(),
                textColor = textColor.toArgb(),
                secondaryTextColor = secondaryTextColor.toArgb(),
                accentColor = accentColor.toArgb(),
                errorColor = errorColor.toArgb(),
            )
        },
    )
}

private class ExternalPlaylistNativeDialogView(context: Context) : ScrollView(context) {
    private val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(14), dp(20), dp(20))
    }
    private val header = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val title = textView(22f, Typeface.BOLD)
    private val closeButton = Button(context).apply { text = "×"; textSize = 25f; minWidth = dp(48) }
    private val inputSection = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val matchingSection = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val description = textView(14f)
    private val input = EditText(context).apply {
        singleLine = true
        hint = "粘贴歌单链接或歌单 id"
        textSize = 16f
        setPadding(dp(14), dp(12), dp(14), dp(12))
    }
    private val fetchError = textView(13f)
    private val fetchButton = Button(context)
    private val playlistName = textView(18f, Typeface.BOLD)
    private val playlistSummary = textView(14f)
    private val matchButton = Button(context)
    private val saveButton = Button(context).apply { text = "保存歌单" }
    private val progress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
    private val matchStatus = textView(13f)
    private val rows = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val manualSection = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE
    }
    private val manualRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val manualInput = EditText(context).apply {
        singleLine = true
        hint = "搜索 B 站视频"
        textSize = 14f
        setPadding(dp(10), dp(8), dp(10), dp(8))
    }
    private val manualSearchButton = Button(context).apply { text = "搜索" }
    private val manualResults = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val footerNote = textView(12f)
    private var lastRowsState: Any? = null
    private var lastManualResults: Any? = null
    private var updatingText = false

    init {
        isFillViewport = true
        clipToPadding = false
        addView(root, android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        ))
        setBackground(rounded(0xFF171114.toInt(), dp(24)))
        root.addView(header, linearParams(width = ViewGroup.LayoutParams.MATCH_PARENT))
        header.addView(title, LinearLayout.LayoutParams(0, dp(52), 1f))
        header.addView(closeButton, linearParams(dp(48), dp(48)))
        root.addView(inputSection, linearParams(width = ViewGroup.LayoutParams.MATCH_PARENT))
        root.addView(matchingSection, linearParams(width = ViewGroup.LayoutParams.MATCH_PARENT))

        inputSection.addView(description, linearParams(width = ViewGroup.LayoutParams.MATCH_PARENT).apply {
            bottomMargin = dp(12)
        })
        inputSection.addView(input, linearParams(width = ViewGroup.LayoutParams.MATCH_PARENT, height = dp(56)).apply {
            bottomMargin = dp(8)
        })
        inputSection.addView(fetchError, linearParams(width = ViewGroup.LayoutParams.MATCH_PARENT))
        inputSection.addView(fetchButton, linearParams(width = ViewGroup.LayoutParams.MATCH_PARENT, height = dp(48)).apply {
            topMargin = dp(12)
        })

        val metaRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        matchingSection.addView(metaRow, linearParams(width = ViewGroup.LayoutParams.MATCH_PARENT))
        val metaLabels = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        metaRow.addView(metaLabels, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        metaLabels.addView(playlistName, linearParams(width = ViewGroup.LayoutParams.MATCH_PARENT))
        metaLabels.addView(playlistSummary, linearParams(width = ViewGroup.LayoutParams.MATCH_PARENT))
        metaRow.addView(matchButton, linearParams(height = dp(48)))
        metaRow.addView(saveButton, linearParams(height = dp(48)))
        matchingSection.addView(progress, linearParams(width = ViewGroup.LayoutParams.MATCH_PARENT, height = dp(4)).apply {
            topMargin = dp(10)
        })
        matchingSection.addView(matchStatus, linearParams(width = ViewGroup.LayoutParams.MATCH_PARENT).apply {
            topMargin = dp(4)
            bottomMargin = dp(8)
        })
        matchingSection.addView(rows, linearParams(width = ViewGroup.LayoutParams.MATCH_PARENT))
        matchingSection.addView(manualSection, linearParams(width = ViewGroup.LayoutParams.MATCH_PARENT))
        manualRow.addView(manualInput, LinearLayout.LayoutParams(0, dp(48), 1f))
        manualRow.addView(manualSearchButton, linearParams(height = dp(48)))
        manualSection.addView(manualRow, linearParams(width = ViewGroup.LayoutParams.MATCH_PARENT))
        manualSection.addView(manualResults, linearParams(width = ViewGroup.LayoutParams.MATCH_PARENT))
        matchingSection.addView(footerNote, linearParams(width = ViewGroup.LayoutParams.MATCH_PARENT).apply {
            topMargin = dp(10)
        })

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!updatingText) currentActions?.onInputChange(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        manualInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!updatingText) currentActions?.onKeywordChange(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private var currentActions: ExternalPlaylistNativeDialogActions? = null

    fun render(
        state: ExternalPlaylistNativeDialogState,
        actions: ExternalPlaylistNativeDialogActions,
        surfaceColor: Int,
        textColor: Int,
        secondaryTextColor: Int,
        accentColor: Int,
        errorColor: Int,
    ) {
        currentActions = actions
        setBackground(rounded(surfaceColor, dp(24)))
        title.text = "导入外部歌单"
        title.setTextColor(textColor)
        closeButton.setTextColor(textColor)
        closeButton.setOnClickListener { actions.onDismiss() }
        description.text = "支持网易云音乐和 QQ 音乐歌单。复制分享链接粘贴到下面（歌单需为公开状态）。"
        description.setTextColor(secondaryTextColor)
        styleInput(input, textColor, secondaryTextColor)
        styleInput(manualInput, textColor, secondaryTextColor)
        styleButton(fetchButton, accentColor)
        styleButton(matchButton, accentColor)
        styleButton(manualSearchButton, accentColor)
        styleButton(saveButton, accentColor)

        updatingText = true
        if (input.text.toString() != state.inputText) input.setText(state.inputText)
        if (manualInput.text.toString() != state.manualKeyword) manualInput.setText(state.manualKeyword)
        updatingText = false

        val meta = state.playlist
        inputSection.visibility = if (meta == null) View.VISIBLE else View.GONE
        matchingSection.visibility = if (meta == null) View.GONE else View.VISIBLE
        fetchError.text = state.fetchError.orEmpty()
        fetchError.setTextColor(errorColor)
        fetchButton.text = if (state.fetching) "正在获取…" else "获取歌单信息"
        fetchButton.isEnabled = !state.fetching
        fetchButton.setOnClickListener { actions.onFetch() }

        if (meta != null) {
            playlistName.text = meta.name
            playlistName.setTextColor(textColor)
            playlistSummary.text = "${meta.author} · ${meta.tracks.size} 首"
            playlistSummary.setTextColor(secondaryTextColor)
            val done = !state.matching && state.matchResults.isNotEmpty() && state.matchCompleted >= state.matchTotal
            matchButton.visibility = if (done) View.GONE else View.VISIBLE
            matchButton.text = when {
                state.matching -> "停止"
                state.matchResults.isEmpty() -> "开始匹配"
                else -> "继续匹配"
            }
            matchButton.setOnClickListener {
                if (state.matching) actions.onStopMatching() else actions.onStartMatching()
            }
            saveButton.visibility = if (done) View.VISIBLE else View.GONE
            saveButton.isEnabled = state.matchResults.any { it.video != null }
            saveButton.setOnClickListener { actions.onSave() }
            progress.visibility = if (state.matchTotal > 0 && !done) View.VISIBLE else View.GONE
            progress.max = state.matchTotal.coerceAtLeast(1)
            progress.progress = state.matchCompleted.coerceIn(0, progress.max)
            matchStatus.text = when {
                state.matching -> "正在匹配：${state.matchingTrackTitle} (${state.matchCompleted}/${state.matchTotal})"
                done -> "匹配 ${state.matchResults.count { it.video != null }}/${state.matchResults.size} 首"
                state.matchTotal > 0 -> "已匹配 ${state.matchCompleted}/${state.matchTotal} 首"
                else -> "歌单共 ${meta.tracks.size} 首"
            }
            matchStatus.setTextColor(secondaryTextColor)
            val outcomes = state.matchResults.ifEmpty {
                meta.tracks.map { ExternalPlaylistRepository.MatchOutcome(it, null) }
            }
            val rowsKey = Triple(
                outcomes.map { it.track.title to it.video?.bvid },
                state.editingIndex,
                state.matchResults.isNotEmpty(),
            )
            if (lastRowsState != rowsKey) {
                renderRows(
                    outcomes = outcomes,
                    editingIndex = state.editingIndex,
                    canEdit = state.matchResults.isNotEmpty(),
                    actions = actions,
                    textColor = textColor,
                    secondaryColor = secondaryTextColor,
                    errorColor = errorColor,
                )
                lastRowsState = rowsKey
            }
            manualSection.visibility = if (state.editingIndex != null) View.VISIBLE else View.GONE
            manualSearchButton.text = if (state.manualSearching) "搜索中…" else "搜索"
            manualSearchButton.isEnabled = !state.manualSearching
            manualSearchButton.setOnClickListener { actions.onManualSearch() }
            val manualKey = state.manualResults.map { it.bvid }
            if (lastManualResults != manualKey) {
                renderManualResults(state.editingIndex, state.manualResults, actions, textColor, secondaryTextColor)
                lastManualResults = manualKey
            }
            footerNote.text = "未匹配的曲目将被跳过，保存后可稍后再添加。"
            footerNote.setTextColor(secondaryTextColor)
        }
    }

    private fun renderRows(
        outcomes: List<ExternalPlaylistRepository.MatchOutcome>,
        editingIndex: Int?,
        canEdit: Boolean,
        actions: ExternalPlaylistNativeDialogActions,
        textColor: Int,
        secondaryColor: Int,
        errorColor: Int,
    ) {
        rows.removeAllViews()
        outcomes.forEachIndexed { index, outcome ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(0x33222222, dp(12))
                setPadding(dp(10), dp(8), dp(8), dp(8))
            }
            val labels = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            val titleView = textView(14f, Typeface.BOLD).apply {
                text = "${index + 1}. ${outcome.track.title}"
                setTextColor(textColor)
                maxLines = 1
            }
            val subtitleView = textView(12f).apply {
                text = outcome.video?.let { "${it.author} · ${it.title}" }
                    ?: outcome.track.artists.joinToString("/").ifBlank { "未匹配" }
                setTextColor(if (outcome.video != null) secondaryColor else errorColor)
                maxLines = 1
            }
            labels.addView(titleView, linearParams(width = ViewGroup.LayoutParams.MATCH_PARENT))
            labels.addView(subtitleView, linearParams(width = ViewGroup.LayoutParams.MATCH_PARENT))
            row.addView(labels, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val edit = Button(context).apply {
                text = if (editingIndex == index) "收起" else "修正"
                visibility = if (canEdit) View.VISIBLE else View.GONE
                setOnClickListener { actions.onToggleEdit(index) }
            }
            row.addView(edit, linearParams(height = dp(44)))
            rows.addView(row, linearParams(width = ViewGroup.LayoutParams.MATCH_PARENT).apply {
                topMargin = dp(4)
                bottomMargin = dp(4)
            })
        }
    }

    private fun renderManualResults(
        editingIndex: Int?,
        results: List<ExternalPlaylistRepository.MatchedVideo>,
        actions: ExternalPlaylistNativeDialogActions,
        textColor: Int,
        secondaryColor: Int,
    ) {
        manualResults.removeAllViews()
        if (editingIndex == null) return
        results.forEach { video ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), dp(8), dp(10), dp(8))
                background = rounded(0x33222222, dp(8))
                setOnClickListener { actions.onPickVideo(editingIndex, video) }
            }
            row.addView(textView(14f, Typeface.BOLD).apply {
                text = video.title
                setTextColor(textColor)
                maxLines = 1
            })
            row.addView(textView(12f).apply {
                text = video.author
                setTextColor(secondaryColor)
                maxLines = 1
            })
            manualResults.addView(row, linearParams(width = ViewGroup.LayoutParams.MATCH_PARENT).apply {
                topMargin = dp(3)
            })
        }
    }

    private fun styleInput(view: EditText, textColor: Int, hintColor: Int) {
        view.setTextColor(textColor)
        view.setHintTextColor(hintColor)
        view.background = rounded(0x00000000, dp(8), strokeColor = 0xFF80777B.toInt())
    }

    private fun styleButton(view: Button, color: Int) {
        view.backgroundTintList = ColorStateList.valueOf(color)
        view.setTextColor(if (android.graphics.Color.luminance(color) > 0.56f) 0xFF171114.toInt() else android.graphics.Color.WHITE)
        view.isAllCaps = false
    }

    private fun textView(sizeSp: Float, style: Int = Typeface.NORMAL) = TextView(context).apply {
        textSize = sizeSp
        typeface = Typeface.create("sans-serif", style)
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun rounded(color: Int, radius: Int, strokeColor: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
        strokeColor?.let { setStroke(dp(1), it) }
    }

    private fun linearParams(
        width: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
        height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    ) =
        LinearLayout.LayoutParams(width, height)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
