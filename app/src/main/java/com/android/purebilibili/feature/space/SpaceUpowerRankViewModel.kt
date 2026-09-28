package com.android.purebilibili.feature.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.SpaceUpowerRankItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UP主充电排行页（对齐 PiliPlus member_upower_rank）。 */
class SpaceUpowerRankViewModel(
    private val upMid: Long,
    upName: String,
    initialCount: Long = 0L,
) : ViewModel() {

    data class LevelTab(
        val privilegeType: Int,
        val name: String,
        val memberTotal: Int,
    )

    data class UiState(
        val upName: String = "",
        val totalCount: Long = 0L,
        val isLoading: Boolean = true,
        val error: String? = null,
        val items: List<SpaceUpowerRankItem> = emptyList(),
        val levelTabs: List<LevelTab> = emptyList(),
        // null 表示「全部」
        val selectedPrivilegeType: Int? = null,
    )

    private val spaceApi = NetworkModule.spaceApi
    private val _uiState = MutableStateFlow(UiState(upName = upName, totalCount = initialCount))
    val uiState = _uiState.asStateFlow()

    private var hasResolvedTabs = false

    init {
        load()
    }

    fun selectLevel(privilegeType: Int?) {
        if (_uiState.value.selectedPrivilegeType == privilegeType) return
        _uiState.value = _uiState.value.copy(
            selectedPrivilegeType = privilegeType,
            items = emptyList(),
            isLoading = true,
            error = null,
        )
        load()
    }

    fun retry() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val selectedType = _uiState.value.selectedPrivilegeType
            try {
                val params = buildMap {
                    put("up_mid", upMid.toString())
                    put("pn", "1")
                    put("ps", "100")
                    selectedType?.let { put("privilege_type", it.toString()) }
                    put("mobi_app", "web")
                    put("web_location", "333.1196")
                }
                val response = spaceApi.getUpowerRank(params)
                val data = response.data
                if (response.code != 0 || data == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.message.ifBlank { "获取充电排行失败" },
                    )
                    return@launch
                }
                val levelTabs = if (!hasResolvedTabs && selectedType == null && data.levelInfo.size > 1) {
                    hasResolvedTabs = true
                    data.levelInfo.map { LevelTab(it.privilegeType, it.name, it.memberTotal) }
                } else {
                    _uiState.value.levelTabs
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null,
                    items = data.rankInfo,
                    levelTabs = levelTabs,
                    totalCount = data.levelInfo.sumOf { it.memberTotal.toLong() }
                        .takeIf { it > 0L }
                        ?: _uiState.value.totalCount,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("SpaceUpowerRank", "load failed: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "网络请求失败",
                )
            }
        }
    }
}
