package com.illa.cashvan.feature.kpi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.app_preferences.domain.use_case.user.GetUserUseCase
import com.illa.cashvan.feature.kpi.data.model.AgentKpiResponse
import com.illa.cashvan.feature.kpi.domain.usecase.GetAgentKpiUseCase
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class KpiUiState(
    val kpi: AgentKpiResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class KpiViewModel(
    private val getAgentKpiUseCase: GetAgentKpiUseCase,
    private val getUserUseCase: GetUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(KpiUiState())
    val uiState: StateFlow<KpiUiState> = _uiState.asStateFlow()

    private var lastFetchTime: Long = 0L
    private val cacheDurationMs = 60_000L

    init {
        loadKpi()
    }

    fun loadKpi(forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && _uiState.value.kpi != null && (now - lastFetchTime) < cacheDurationMs) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val user = getUserUseCase().firstOrNull()
            if (user == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "تعذر تحديد هوية المندوب")
                return@launch
            }
            try {
                val response = getAgentKpiUseCase(user.id)
                lastFetchTime = System.currentTimeMillis()
                _uiState.value = _uiState.value.copy(isLoading = false, kpi = response)
            } catch (e: ClientRequestException) {
                val message = when (e.response.status) {
                    HttpStatusCode.NotFound -> "لم يتم العثور على المندوب، تواصل مع المشرف"
                    HttpStatusCode.Conflict -> "حسابك غير مرتبط بعد، تواصل بالإدارة"
                    HttpStatusCode.Forbidden -> "انت حسابك مش مربوط بحساب بري-سيل كلم المشرف بتاعك"
                    else -> e.message ?: "حدث خطأ في تحميل مؤشرات الأداء"
                }
                _uiState.value = _uiState.value.copy(isLoading = false, error = message)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "حدث خطأ في تحميل مؤشرات الأداء")
            }
        }
    }
}
