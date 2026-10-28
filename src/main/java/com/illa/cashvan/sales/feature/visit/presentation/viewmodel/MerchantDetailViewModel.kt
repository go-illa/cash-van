package com.illa.cashvan.sales.feature.visit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.sales.feature.visit.data.model.VisitHistoryItem
import com.illa.cashvan.sales.feature.visit.domain.repository.PresellVisitRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class MerchantHistoryState(
    val merchantId: String? = null,
    val isLoading: Boolean = false,
    val visits: List<VisitHistoryItem> = emptyList(),
    val failed: Boolean = false
)

class MerchantDetailViewModel(
    private val visitRepository: PresellVisitRepository
) : ViewModel() {

    private val _history = MutableStateFlow(MerchantHistoryState())
    val history: StateFlow<MerchantHistoryState> = _history.asStateFlow()

    private var loadJob: Job? = null

    fun load(merchantId: String) {
        if (_history.value.merchantId == merchantId && !_history.value.failed) return
        loadJob?.cancel()
        _history.value = MerchantHistoryState(merchantId = merchantId, isLoading = true)
        loadJob = viewModelScope.launch {
            _history.value = try {
                MerchantHistoryState(merchantId, visits = visitRepository.getVisitHistory(merchantId))
            } catch (e: Exception) {
                Timber.e(e, "Visit history failed")
                MerchantHistoryState(merchantId, failed = true)
            }
        }
    }

    /** Called after a visit is logged, so the next open shows it. */
    fun invalidate() {
        _history.value = MerchantHistoryState()
    }
}
