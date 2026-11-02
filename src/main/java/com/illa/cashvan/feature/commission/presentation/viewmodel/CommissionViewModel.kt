package com.illa.cashvan.feature.commission.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.app_preferences.domain.use_case.user.GetUserUseCase
import com.illa.cashvan.feature.commission.domain.model.FulfillmentCommission
import com.illa.cashvan.feature.commission.domain.usecase.GetFulfillmentCommissionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class CommissionUiState(
    val commissions: List<FulfillmentCommission>? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class CommissionViewModel(
    private val getFulfillmentCommissionUseCase: GetFulfillmentCommissionUseCase,
    private val getUserUseCase: GetUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommissionUiState())
    val uiState: StateFlow<CommissionUiState> = _uiState.asStateFlow()

    fun loadCommission() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val agentName = getUserUseCase().firstOrNull()?.name
            if (agentName.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "تعذر تحديد هوية المندوب")
                return@launch
            }
            try {
                val commissions = getFulfillmentCommissionUseCase(agentName)
                _uiState.value = _uiState.value.copy(isLoading = false, commissions = commissions)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "حدث خطأ في تحميل العمولة")
            }
        }
    }
}
