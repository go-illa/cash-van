package com.illa.cashvan.sales.feature.cashvan.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.location.LocationData
import com.illa.cashvan.core.location.LocationService
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.sales.domain.model.RouteMerchant
import com.illa.cashvan.sales.domain.route.calculateDistance
import com.illa.cashvan.sales.domain.route.formatDistance
import com.illa.cashvan.sales.feature.cashvan.data.model.CashVanMerchantContext
import com.illa.cashvan.sales.feature.cashvan.data.model.FawryBadgeRow
import com.illa.cashvan.sales.feature.cashvan.data.model.NearbyMerchant
import com.illa.cashvan.sales.feature.cashvan.domain.repository.CashVanRepository
import com.illa.cashvan.sales.feature.fawry.domain.repository.FawryQuestionnaireRepository
import com.illa.cashvan.sales.feature.fawry.presentation.viewmodel.FawryGate
import com.illa.cashvan.sales.feature.route.domain.repository.RouteRepository
import com.illa.cashvan.shared.feature.visit.data.model.CreateVisitRequest
import com.illa.cashvan.shared.feature.visit.data.model.NoOrderReason
import com.illa.cashvan.shared.feature.visit.data.model.VisitData
import com.illa.cashvan.shared.feature.visit.domain.usecase.CreateVisitUseCase
import com.illa.cashvan.shared.feature.visit.domain.usecase.GetNoOrderReasonsUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant

enum class MerchantListMode { ALL, NEARBY }

/** A merchant row in either list, with what the card needs. */
data class CashVanMerchantItem(
    val id: String,
    val name: String,
    val code: String?,
    val phone: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val visitCount: Int?,
    val targetCompleted: Boolean,
    val fawryDeviceNumber: String?,
    val wantsFawryDevice: Boolean?,
    val distanceMeters: Double?
)

sealed interface CashVanVisitSheet {
    data object None : CashVanVisitSheet
    data class VisitType(val merchant: CashVanMerchantItem) : CashVanVisitSheet
    data class Reasons(
        val merchant: CashVanMerchantItem,
        val location: LocationData,
        val reasons: List<NoOrderReason> = emptyList(),
        val isLoading: Boolean = true,
        val isSubmitting: Boolean = false
    ) : CashVanVisitSheet
}

data class CashVanMerchantsUiState(
    val mode: MerchantListMode = MerchantListMode.ALL,
    val query: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val routeMerchants: List<RouteMerchant> = emptyList(),
    val nearby: List<NearbyMerchant> = emptyList(),
    val fawryBadges: Map<String, FawryBadgeRow> = emptyMap(),
    val location: LocationData? = null,
    val checkingMerchantId: String? = null,
    val sheet: CashVanVisitSheet = CashVanVisitSheet.None,
    val message: String? = null
) {
    val items: List<CashVanMerchantItem>
        get() {
            val term = query.trim().lowercase()
            val progressById = routeMerchants.associateBy { it.merchantId }
            val items = when (mode) {
                MerchantListMode.ALL -> routeMerchants.map { merchant ->
                    CashVanMerchantItem(
                        id = merchant.merchantId,
                        name = merchant.displayName,
                        code = merchant.merchantCode,
                        phone = merchant.phoneNumber,
                        address = merchant.detailedAddress ?: merchant.location,
                        latitude = merchant.latitude,
                        longitude = merchant.longitude,
                        visitCount = merchant.visitCount,
                        targetCompleted = merchant.targetCompleted,
                        fawryDeviceNumber = merchant.fawryDeviceNumber,
                        wantsFawryDevice = merchant.wantsFawryDevice,
                        distanceMeters = distance(merchant.latitude, merchant.longitude)
                    )
                }
                MerchantListMode.NEARBY -> nearby.map { merchant ->
                    val badge = fawryBadges[merchant.id]
                    val progress = progressById[merchant.id]
                    CashVanMerchantItem(
                        id = merchant.id,
                        name = merchant.displayName,
                        code = merchant.code,
                        phone = merchant.phone_number,
                        address = listOfNotNull(merchant.address, merchant.governorate?.name).joinToString(" • ").ifEmpty { null },
                        latitude = merchant.lat,
                        longitude = merchant.lng,
                        visitCount = progress?.visitCount,
                        targetCompleted = progress?.targetCompleted == true,
                        fawryDeviceNumber = badge?.fawry_device_number,
                        wantsFawryDevice = badge?.wants_fawry_device,
                        distanceMeters = distance(merchant.lat, merchant.lng)
                    )
                }
            }
            val filtered = if (term.isEmpty()) items else items.filter {
                it.name.lowercase().contains(term) || it.code.orEmpty().lowercase().contains(term)
            }
            return filtered.sortedBy { it.distanceMeters ?: Double.POSITIVE_INFINITY }
        }

    private fun distance(lat: Double?, lng: Double?): Double? {
        val here = location ?: return null
        if (lat == null || lng == null) return null
        return calculateDistance(here.latitude, here.longitude, lat, lng)
    }
}

sealed interface CashVanMerchantsEvent {
    data class StartOrder(val context: CashVanMerchantContext) : CashVanMerchantsEvent
    data object VisitLogged : CashVanMerchantsEvent
}

/**
 * The PWA's `CashvanMerchantSelector`: route merchants or nearby ones, then for the tapped merchant the
 * 200 m proximity gate, the Fawry questionnaire, and "create order" or "visit only".
 */
class CashVanMerchantsViewModel(
    private val routeRepository: RouteRepository,
    private val cashVanRepository: CashVanRepository,
    private val locationService: LocationService,
    private val getNoOrderReasonsUseCase: GetNoOrderReasonsUseCase,
    private val createVisitUseCase: CreateVisitUseCase,
    fawryQuestionnaireRepository: FawryQuestionnaireRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CashVanMerchantsUiState())
    val uiState: StateFlow<CashVanMerchantsUiState> = _uiState.asStateFlow()

    private val _events = Channel<CashVanMerchantsEvent>(Channel.BUFFERED)
    val events: Flow<CashVanMerchantsEvent> = _events.receiveAsFlow()

    val fawryGate = FawryGate(fawryQuestionnaireRepository, viewModelScope)

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val location = locationService.getCurrentLocation().getOrNull()
            _uiState.update { it.copy(location = location) }
            try {
                val merchants = routeRepository.getRouteMerchants()
                _uiState.update { it.copy(routeMerchants = merchants, isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "Cash van route merchants failed")
                _uiState.update { it.copy(isLoading = false, error = "حدث خطأ أثناء تحميل التجار") }
            }
            if (_uiState.value.mode == MerchantListMode.NEARBY) loadNearby()
        }
    }

    fun setMode(mode: MerchantListMode) {
        if (_uiState.value.mode == mode) return
        _uiState.update { it.copy(mode = mode) }
        if (mode == MerchantListMode.NEARBY && _uiState.value.nearby.isEmpty()) {
            viewModelScope.launch { loadNearby() }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    private suspend fun loadNearby() {
        val location = _uiState.value.location ?: locationService.getCurrentLocation().getOrNull()
        if (location == null) {
            _uiState.update { it.copy(error = "يتعذر تحديد موقعك، جرب البحث بالاسم") }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null, location = location) }
        try {
            val nearby = cashVanRepository.getNearbyMerchants(location.latitude, location.longitude)
            val badges = cashVanRepository.getFawryBadges(nearby.map { it.id })
            _uiState.update { it.copy(nearby = nearby, fawryBadges = badges, isLoading = false) }
        } catch (e: Exception) {
            Timber.e(e, "Nearby merchants failed")
            _uiState.update { it.copy(isLoading = false, error = "حدث خطأ أثناء تحميل التجار") }
        }
    }

    /** 200 m from the merchant (skipped without coordinates), then the Fawry questionnaire, then the visit type. */
    fun selectMerchant(merchant: CashVanMerchantItem) {
        if (_uiState.value.checkingMerchantId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(checkingMerchantId = merchant.id) }
            try {
                if (merchant.latitude != null && merchant.longitude != null) {
                    val here = locationService.getCurrentLocation().getOrNull()
                    if (here == null) {
                        showMessage("تعذر تحديد موقعك. يرجى تفعيل GPS والمحاولة مجددًا.")
                        return@launch
                    }
                    _uiState.update { it.copy(location = here) }
                    val meters = calculateDistance(here.latitude, here.longitude, merchant.latitude, merchant.longitude)
                    if (meters > PROXIMITY_METERS) {
                        showMessage("أنت على بُعد ${formatDistance(meters)} من هذا التاجر — استخدم زر الخريطة للتنقل أولاً")
                        return@launch
                    }
                }
                if (!fawryGate.run(merchant.id)) return@launch
                _uiState.update { it.copy(sheet = CashVanVisitSheet.VisitType(merchant)) }
            } finally {
                _uiState.update { it.copy(checkingMerchantId = null) }
            }
        }
    }

    fun createOrder() {
        val merchant = (_uiState.value.sheet as? CashVanVisitSheet.VisitType)?.merchant ?: return
        _uiState.update { it.copy(sheet = CashVanVisitSheet.None) }
        viewModelScope.launch { _events.send(CashVanMerchantsEvent.StartOrder(CashVanMerchantContext(merchant.id, merchant.name))) }
    }

    /** "زيارة فقط": no visit exists until the agent confirms a reason. */
    fun visitOnly() {
        val merchant = (_uiState.value.sheet as? CashVanVisitSheet.VisitType)?.merchant ?: return
        viewModelScope.launch {
            val location = locationService.getCurrentLocation().getOrNull() ?: _uiState.value.location
            if (location == null) {
                showMessage("لم نتمكن من الحصول على موقعك. حاول مرة أخرى.")
                return@launch
            }
            _uiState.update { it.copy(sheet = CashVanVisitSheet.Reasons(merchant, location)) }
            val reasons = (getNoOrderReasonsUseCase() as? ApiResult.Success)?.data?.no_order_reasons
                ?.filter { it.is_active }?.sortedBy { it.display_order }
            _uiState.update { state ->
                val sheet = state.sheet as? CashVanVisitSheet.Reasons ?: return@update state
                state.copy(
                    sheet = sheet.copy(reasons = reasons.orEmpty(), isLoading = false),
                    message = if (reasons == null) "فشل تحميل أسباب عدم الطلب" else state.message
                )
            }
        }
    }

    fun confirmVisit(reasonId: String) {
        val sheet = _uiState.value.sheet as? CashVanVisitSheet.Reasons ?: return
        if (sheet.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(sheet = sheet.copy(isSubmitting = true)) }
            val result = createVisitUseCase(
                CreateVisitRequest(
                    VisitData(
                        merchant_id = sheet.merchant.id,
                        visit_type = "without_order",
                        visit_date = Instant.now().toString(),
                        latitude = sheet.location.latitude,
                        longitude = sheet.location.longitude,
                        visit_quality = 1,
                        no_order_reason_id = reasonId
                    )
                )
            )
            if (result is ApiResult.Success) {
                _uiState.update { it.copy(sheet = CashVanVisitSheet.None, message = "تم تسجيل الزيارة بنجاح") }
                _events.send(CashVanMerchantsEvent.VisitLogged)
            } else {
                _uiState.update { it.copy(sheet = sheet.copy(isSubmitting = false), message = "حدث خطأ أثناء تسجيل الزيارة") }
            }
        }
    }

    fun dismissSheet() {
        _uiState.update { it.copy(sheet = CashVanVisitSheet.None) }
    }

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    private companion object {
        const val PROXIMITY_METERS = 200.0
    }
}
