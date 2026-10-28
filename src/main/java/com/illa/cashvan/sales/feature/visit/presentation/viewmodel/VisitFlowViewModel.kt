package com.illa.cashvan.sales.feature.visit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.analytics.SalesAnalytics
import com.illa.cashvan.core.location.LocationData
import com.illa.cashvan.core.location.LocationService
import com.illa.cashvan.sales.domain.model.PresellOrderContext
import com.illa.cashvan.sales.domain.model.RouteMerchant
import com.illa.cashvan.sales.domain.route.GEOFENCE_RADIUS_METERS
import com.illa.cashvan.sales.domain.route.formatDistance
import com.illa.cashvan.sales.domain.route.getDistanceToMerchant
import com.illa.cashvan.sales.feature.fawry.domain.repository.FawryQuestionnaireRepository
import com.illa.cashvan.sales.feature.fawry.presentation.viewmodel.FawryGate
import com.illa.cashvan.sales.feature.visit.data.model.NoOrderReason
import com.illa.cashvan.sales.feature.visit.domain.repository.DuplicateVisitException
import com.illa.cashvan.sales.feature.visit.domain.repository.PresellVisitRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

sealed interface VisitSheet {
    data object None : VisitSheet
    data class RevisitWarning(val merchant: RouteMerchant, val unvisitedCount: Int) : VisitSheet
    data class VisitType(val merchant: RouteMerchant) : VisitSheet
    data class NoOrderReasons(
        val merchant: RouteMerchant,
        val location: LocationData,
        val reasons: List<NoOrderReason> = emptyList(),
        val isLoading: Boolean = true,
        val isSubmitting: Boolean = false
    ) : VisitSheet
}

data class VisitFlowUiState(
    val sheet: VisitSheet = VisitSheet.None,
    /** The merchant whose location is being checked; its card shows a spinner. */
    val checkingMerchantId: String? = null,
    val message: VisitMessage? = null
)

data class VisitMessage(val title: String, val description: String? = null, val isError: Boolean = true)

sealed interface VisitFlowEvent {
    data class OpenOrder(val context: PresellOrderContext) : VisitFlowEvent
    data object VisitLogged : VisitFlowEvent
}

/**
 * Starting a visit from the route, as the PWA dashboard does: revisit warning → with/without order →
 * 100 m geofence → order flow or no-order reason.
 */
class VisitFlowViewModel(
    private val visitRepository: PresellVisitRepository,
    private val locationService: LocationService,
    fawryQuestionnaireRepository: FawryQuestionnaireRepository,
    private val analytics: SalesAnalytics
) : ViewModel() {

    /** Runs after the geofence passes, before the visit goes on (DIS-517). */
    val fawryGate = FawryGate(fawryQuestionnaireRepository, viewModelScope)

    private val _uiState = MutableStateFlow(VisitFlowUiState())
    val uiState: StateFlow<VisitFlowUiState> = _uiState.asStateFlow()

    private val _events = Channel<VisitFlowEvent>(Channel.BUFFERED)
    val events: Flow<VisitFlowEvent> = _events.receiveAsFlow()

    fun onMerchantOpened(merchant: RouteMerchant) {
        analytics.track(
            "merchant_detail_sheet_opened",
            "merchant_id" to merchant.merchantId,
            "merchant_name" to merchant.displayName,
            "is_visited_today" to (merchant.visitCount > 0)
        )
    }

    fun startVisit(merchant: RouteMerchant, routeMerchants: List<RouteMerchant>) {
        val unvisited = routeMerchants.count { it.visitCount == 0 }
        analytics.track("visit_setup_sheet_opened", "merchant_id" to merchant.merchantId, "unvisited_count" to unvisited)
        val sheet = if (merchant.visitCount > 0 && unvisited > 0) {
            VisitSheet.RevisitWarning(merchant, unvisited)
        } else {
            VisitSheet.VisitType(merchant)
        }
        _uiState.update { it.copy(sheet = sheet) }
    }

    fun confirmRevisit() {
        val sheet = _uiState.value.sheet as? VisitSheet.RevisitWarning ?: return
        _uiState.update { it.copy(sheet = VisitSheet.VisitType(sheet.merchant)) }
    }

    fun dismiss() {
        _uiState.update { it.copy(sheet = VisitSheet.None) }
    }

    /** Payment is chosen at checkout, after the products, so this goes straight to the order flow. */
    fun selectWithOrder() {
        val merchant = (_uiState.value.sheet as? VisitSheet.VisitType)?.merchant ?: return
        analytics.track("visit_type_selected", "merchant_id" to merchant.merchantId, "visit_type" to "with_order")
        viewModelScope.launch {
            val location = checkGeofence(merchant) ?: return@launch
            if (!fawryGate.run(merchant.merchantId)) return@launch
            _uiState.update { it.copy(sheet = VisitSheet.None) }
            _events.send(
                VisitFlowEvent.OpenOrder(
                    PresellOrderContext(
                        merchantId = merchant.merchantId,
                        merchantName = merchant.displayName,
                        priceTier = merchant.priceTier ?: DEFAULT_PRICE_TIER,
                        regionId = merchant.regionId,
                        routeId = merchant.routeId.ifEmpty { null },
                        routeName = merchant.routeName.ifEmpty { null },
                        isAdHoc = false,
                        visitLocation = location
                    )
                )
            )
        }
    }

    fun selectWithoutOrder() {
        val merchant = (_uiState.value.sheet as? VisitSheet.VisitType)?.merchant ?: return
        analytics.track("visit_type_selected", "merchant_id" to merchant.merchantId, "visit_type" to "without_order")
        viewModelScope.launch {
            val location = checkGeofence(merchant) ?: return@launch
            if (!fawryGate.run(merchant.merchantId)) return@launch
            _uiState.update { it.copy(sheet = VisitSheet.NoOrderReasons(merchant, location)) }
            val reasons = runCatching { visitRepository.getNoOrderReasons() }
            _uiState.update { state ->
                val sheet = state.sheet as? VisitSheet.NoOrderReasons ?: return@update state
                state.copy(
                    sheet = sheet.copy(reasons = reasons.getOrDefault(emptyList()), isLoading = false),
                    message = if (reasons.isFailure) VisitMessage("خطأ", "فشل تحميل أسباب عدم الطلب") else state.message
                )
            }
        }
    }

    /** Records the visit, then marks it without order with the chosen reason. */
    fun submitNoOrder(reasonId: String) {
        val sheet = _uiState.value.sheet as? VisitSheet.NoOrderReasons ?: return
        if (sheet.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(sheet = sheet.copy(isSubmitting = true)) }
            try {
                val reason = sheet.reasons.firstOrNull { it.id == reasonId }
                analytics.track("no_order_reason_selected", "reason_id" to reasonId, "reason_text_ar" to reason?.reason_ar, "reason_text_en" to reason?.reason_en)
                val visitId = visitRepository.createVisit(sheet.merchant.merchantId, sheet.location)
                visitRepository.completeVisitWithoutOrder(visitId, reasonId)
                analytics.track("visit_without_order_completed", "merchant_id" to sheet.merchant.merchantId, "reason_id" to reasonId)
                _uiState.update { it.copy(sheet = VisitSheet.None, message = VisitMessage("نجاح", "تم تسجيل الزيارة بنجاح", isError = false)) }
                _events.send(VisitFlowEvent.VisitLogged)
            } catch (e: DuplicateVisitException) {
                _uiState.update {
                    it.copy(
                        sheet = sheet.copy(isSubmitting = false),
                        message = VisitMessage("عذراً", "لقد قمت بزيارة هذا التاجر في آخر 5 دقائق. يرجى الانتظار قبل المحاولة مرة أخرى.")
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "No-order visit failed")
                _uiState.update {
                    it.copy(sheet = sheet.copy(isSubmitting = false), message = VisitMessage("خطأ", "مقدرناش نسجل الزيارة"))
                }
            }
        }
    }

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }

    /**
     * A fresh GPS fix within [GEOFENCE_RADIUS_METERS] of the merchant. Merchants without coordinates can be
     * visited from anywhere. Returns null, closing the sheet, when the check fails.
     */
    private suspend fun checkGeofence(merchant: RouteMerchant): LocationData? {
        _uiState.update { it.copy(checkingMerchantId = merchant.merchantId) }
        try {
            val location = locationService.getCurrentLocation().getOrNull()
            if (location == null) {
                fail(VisitMessage("خطأ في تحديد الموقع", "مقدرناش نحدد مكانك. ظبط الـ GPS وحاول تاني."))
                return null
            }
            val distance = getDistanceToMerchant(location.latitude, location.longitude, merchant.latitude, merchant.longitude)
            when {
                distance == null -> _uiState.update {
                    it.copy(message = VisitMessage("موقع التاجر مش متوفر، هتقدر تزوره عادي", "هنكمل من غير إحداثيات التاجر", isError = false))
                }
                distance > GEOFENCE_RADIUS_METERS -> {
                    analytics.track("geofence_check_failed", "merchant_id" to merchant.merchantId, "distance_meters" to distance, "reason" to "too_far")
                    fail(
                        VisitMessage(
                            "لازم تكون في مكان التاجر عشان تبدأ الزيارة",
                            "أنت بعيد عن المكان بـ ${formatDistance(distance)}. لازم تكون على بعد 100 متر على الأقل."
                        )
                    )
                    return null
                }
            }
            return location
        } finally {
            _uiState.update { it.copy(checkingMerchantId = null) }
        }
    }

    private fun fail(message: VisitMessage) {
        _uiState.update { it.copy(sheet = VisitSheet.None, message = message) }
    }

    private companion object {
        const val DEFAULT_PRICE_TIER = "retail"
    }
}
