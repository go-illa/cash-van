package com.illa.cashvan.feature.visit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.visit.data.model.VisitItem
import com.illa.cashvan.feature.visit.domain.usecase.GetVisitByIdUseCase
import com.illa.cashvan.feature.visit.domain.usecase.GetVisitsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VisitListUiState(
    val isLoading: Boolean = false,
    val visits: List<VisitItem> = emptyList(),
    val error: String? = null,
    val page: Int = 1,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false
)

data class VisitDetailUiState(
    val isLoading: Boolean = false,
    val visit: VisitItem? = null,
    val error: String? = null
)

class VisitListViewModel(
    private val getVisitsUseCase: GetVisitsUseCase,
    private val getVisitByIdUseCase: GetVisitByIdUseCase
) : ViewModel() {

    private val _listState = MutableStateFlow(VisitListUiState())
    val listState: StateFlow<VisitListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(VisitDetailUiState())
    val detailState: StateFlow<VisitDetailUiState> = _detailState.asStateFlow()

    fun loadVisits() {
        viewModelScope.launch {
            _listState.value = _listState.value.copy(isLoading = true, error = null, page = 1)
            when (val result = getVisitsUseCase(page = 1)) {
                is ApiResult.Success -> {
                    val meta = result.data.meta
                    _listState.value = _listState.value.copy(
                        isLoading = false,
                        visits = result.data.data,
                        page = 1,
                        hasMore = (meta?.page ?: 1) < (meta?.last ?: 1)
                    )
                }
                is ApiResult.Error -> {
                    _listState.value = _listState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is ApiResult.Loading -> Unit
            }
        }
    }

    fun loadMore() {
        val state = _listState.value
        if (state.isLoadingMore || !state.hasMore || state.isLoading) return
        viewModelScope.launch {
            val nextPage = state.page + 1
            _listState.value = state.copy(isLoadingMore = true)
            when (val result = getVisitsUseCase(page = nextPage)) {
                is ApiResult.Success -> {
                    val meta = result.data.meta
                    _listState.value = _listState.value.copy(
                        isLoadingMore = false,
                        visits = _listState.value.visits + result.data.data,
                        page = nextPage,
                        hasMore = (meta?.page ?: nextPage) < (meta?.last ?: nextPage)
                    )
                }
                is ApiResult.Error -> {
                    _listState.value = _listState.value.copy(isLoadingMore = false)
                }
                is ApiResult.Loading -> Unit
            }
        }
    }

    fun loadVisitDetail(id: String) {
        viewModelScope.launch {
            _detailState.value = VisitDetailUiState(isLoading = true)
            when (val result = getVisitByIdUseCase(id)) {
                is ApiResult.Success -> {
                    _detailState.value = VisitDetailUiState(visit = result.data)
                }
                is ApiResult.Error -> {
                    _detailState.value = VisitDetailUiState(error = result.message)
                }
                is ApiResult.Loading -> Unit
            }
        }
    }
}
