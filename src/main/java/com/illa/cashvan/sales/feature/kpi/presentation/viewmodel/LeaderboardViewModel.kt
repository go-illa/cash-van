package com.illa.cashvan.sales.feature.kpi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.sales.domain.kpi.KpiAgentSummaryRow
import com.illa.cashvan.sales.domain.kpi.LeaderboardTab
import com.illa.cashvan.sales.domain.kpi.currentCairoPeriod
import com.illa.cashvan.sales.domain.kpi.sortLeaderboard
import com.illa.cashvan.sales.feature.kpi.data.mapper.toDomain
import com.illa.cashvan.sales.feature.kpi.domain.repository.SalesKpiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class LeaderboardUiState(
    val isLoading: Boolean = true,
    val tab: LeaderboardTab = LeaderboardTab.SALES,
    val rows: List<KpiAgentSummaryRow> = emptyList(),
    val myAgentId: String? = null
) {
    val sorted: List<KpiAgentSummaryRow> get() = sortLeaderboard(rows, tab)
}

/** "المتصدرون": every active sales agent this Cairo month, ranked by sales, coverage or visits. */
class LeaderboardViewModel(
    private val repository: SalesKpiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val period = currentCairoPeriod()
            val rows = runCatching { repository.getLeaderboardSummary(period.month, period.year).map { it.toDomain() } }
                .onFailure { Timber.e(it, "Leaderboard failed") }
                .getOrDefault(emptyList())
            val me = runCatching { repository.currentAgentId() }.getOrNull()
            _uiState.update { it.copy(isLoading = false, rows = rows, myAgentId = me) }
        }
    }

    fun selectTab(tab: LeaderboardTab) {
        _uiState.update { it.copy(tab = tab) }
    }
}
