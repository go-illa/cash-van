package com.illa.cashvan.sales.feature.merchant.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.analytics.SalesAnalytics
import com.illa.cashvan.core.app_preferences.domain.use_case.user.GetUserUseCase
import com.illa.cashvan.core.location.LocationService
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.sales.data.CurrentAgentProvider
import com.illa.cashvan.sales.domain.merchant.GOVERNORATES
import com.illa.cashvan.sales.domain.merchant.GovernorateRegion
import com.illa.cashvan.sales.domain.merchant.findGovernorateRegion
import com.illa.cashvan.sales.feature.merchant.data.model.AgentRouteOption
import com.illa.cashvan.sales.feature.merchant.data.model.NewMerchantForm
import com.illa.cashvan.sales.feature.merchant.data.model.PresellMerchantType
import com.illa.cashvan.sales.feature.merchant.domain.repository.MerchantCreationException
import com.illa.cashvan.sales.feature.merchant.domain.repository.MerchantCreationException.Reason
import com.illa.cashvan.sales.feature.merchant.domain.repository.PresellMerchantRepository
import com.illa.cashvan.shared.feature.merchant.domain.usecase.GetReverseGeocodeUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class CreatePresellMerchantUiState(
    val step: Int = 1,
    val form: PresellMerchantFormState = PresellMerchantFormState(),
    val merchantTypes: List<PresellMerchantType> = emptyList(),
    val routes: List<AgentRouteOption> = emptyList(),
    val regions: List<GovernorateRegion> = emptyList(),
    val isLoading: Boolean = true,
    val isLocating: Boolean = false,
    val isSubmitting: Boolean = false,
    val message: String? = null,
    /** Set once the merchant is saved; the screen shows it and closes. */
    val createdMessage: String? = null
)

class CreatePresellMerchantViewModel(
    private val merchantRepository: PresellMerchantRepository,
    private val locationService: LocationService,
    private val getReverseGeocodeUseCase: GetReverseGeocodeUseCase,
    private val getUserUseCase: GetUserUseCase,
    private val analytics: SalesAnalytics,
    private val currentAgentProvider: CurrentAgentProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePresellMerchantUiState())
    val uiState: StateFlow<CreatePresellMerchantUiState> = _uiState.asStateFlow()

    init {
        analytics.track("merchant_creation_started", "source" to SOURCE)
        loadOptions()
    }

    private fun loadOptions() {
        viewModelScope.launch {
            val types = async { runCatching { merchantRepository.getMerchantTypes() } }
            val routes = async { runCatching { merchantRepository.getAgentRoutes() } }
            val regions = async { runCatching { merchantRepository.getRegions() } }
            val routeList = routes.await().getOrDefault(emptyList())
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    merchantTypes = types.await().getOrElse { emptyList() },
                    routes = routeList,
                    regions = regions.await().getOrElse { emptyList() },
                    form = state.form.copy(routeId = state.form.routeId ?: routeList.singleOrNull()?.route_id),
                    message = when {
                        types.await().isFailure -> "فشل تحميل أنواع التجار"
                        regions.await().isFailure -> "فشل تحميل بيانات المناطق. يرجى المحاولة لاحقاً."
                        else -> null
                    }
                )
            }
        }
    }

    fun updateForm(transform: (PresellMerchantFormState) -> PresellMerchantFormState) {
        _uiState.update { it.copy(form = transform(it.form)) }
    }

    fun selectGovernorate(name: String) {
        _uiState.update { state ->
            val region = findGovernorateRegion(state.regions, name)
            state.copy(form = state.form.copy(governorate = name, regionId = region?.id.orEmpty()))
        }
    }

    fun toggleVisitDay(day: String) {
        updateForm { form ->
            form.copy(visitDays = if (day in form.visitDays) form.visitDays - day else form.visitDays + day)
        }
    }

    fun nextStep() {
        val problem = validateMerchantStep(_uiState.value.form)
        if (problem != null) {
            showMessage(problem)
            return
        }
        _uiState.update { it.copy(step = 2) }
        if (_uiState.value.form.latitude == null) detectLocation()
    }

    fun previousStep() {
        _uiState.update { it.copy(step = 1) }
    }

    /** Fills coordinates, address and governorate from GPS and the Rails reverse geocoder. */
    fun detectLocation() {
        if (_uiState.value.isLocating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLocating = true) }
            val location = locationService.getCurrentLocation().getOrNull()
            if (location == null) {
                _uiState.update { it.copy(isLocating = false, message = "فشل تحديد الموقع. يرجى التحقق من إعدادات الموقع") }
                return@launch
            }
            // The geocoder is a Rails endpoint; presell-only agents pick the governorate themselves.
            val railsAccess = runCatching { currentAgentProvider.require().hasRailsAccess }.getOrDefault(false)
            val geocode = if (railsAccess) {
                getReverseGeocodeUseCase.invoke(location.latitude.toString(), location.longitude.toString())
            } else {
                null
            }
            _uiState.update { state ->
                val address = (geocode as? ApiResult.Success)?.data
                val region = findGovernorateRegion(state.regions, address?.components?.governorate)
                // Show the form's own spelling of the governorate when it matches one.
                val governorate = region?.let { matched ->
                    GOVERNORATES.firstOrNull { findGovernorateRegion(listOf(matched), it) != null }
                } ?: address?.components?.governorate?.trim()
                state.copy(
                    isLocating = false,
                    form = state.form.copy(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        detailedAddress = state.form.detailedAddress.ifBlank { address?.detailed_address ?: address?.formatted_address.orEmpty() },
                        governorate = governorate ?: state.form.governorate,
                        regionId = region?.id ?: state.form.regionId
                    ),
                    message = if (address != null) "تم تحديد الموقع والمحافظة والخط بنجاح" else null
                )
            }
        }
    }

    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val problem = validateMerchantStep(state.form)
            ?: validateLocationStep(state.form, state.routes.size, state.isLocating)
        if (problem != null) {
            showMessage(problem)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val form = state.form
            try {
                val agentName = getUserUseCase().firstOrNull()?.name
                merchantRepository.createMerchant(
                    NewMerchantForm(
                        merchantName = form.merchantName.trim(),
                        signName = form.signName.trim(),
                        merchantTypeId = form.merchantTypeId,
                        phoneNumber = form.phoneNumber.trim(),
                        secondaryPhone = form.secondaryPhone.trim().ifEmpty { null },
                        governorate = form.governorate,
                        regionId = form.regionId,
                        detailedAddress = form.detailedAddress.trim(),
                        priceTier = form.priceTier,
                        latitude = form.latitude,
                        longitude = form.longitude,
                        visitDays = WEEKDAYS.map { it.first }.filter { it in form.visitDays },
                        routeId = form.routeId
                    ),
                    agentName
                )
                analytics.track(
                    "merchant_created",
                    "merchant_name" to form.merchantName,
                    "merchant_type" to (state.merchantTypes.firstOrNull { it.id == form.merchantTypeId }?.name ?: "unknown"),
                    "phone_number" to form.phoneNumber,
                    "governorate" to form.governorate,
                    "price_tier" to form.priceTier,
                    "requested_price_tier" to form.priceTier.takeIf { it != "retail" },
                    "visit_days_count" to form.visitDays.size,
                    "has_secondary_phone" to form.secondaryPhone.isNotBlank()
                )
                val tierLabel = PRICE_TIERS.firstOrNull { it.first == form.priceTier }?.second
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        createdMessage = if (form.priceTier == "retail") {
                            "تم إضافة ${form.merchantName} بنجاح بسعر القطاعي"
                        } else {
                            "تم إضافة ${form.merchantName} بنجاح! طلب ترقية لسعر $tierLabel قيد المراجعة"
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Create merchant failed")
                analytics.track("merchant_creation_failed", "error_message" to (e.message ?: e::class.simpleName))
                val message = when ((e as? MerchantCreationException)?.reason) {
                    Reason.NO_ROUTE -> "لم يتم تعيين مسار لك لسه"
                    Reason.ROUTE_REQUIRED -> "يرجى اختيار المسار"
                    Reason.VISIT_DAYS_FAILED -> "حدث خطأ عند حفظ أيام الزيارة. يرجى المحاولة مرة أخرى."
                    Reason.ROUTE_LINK_FAILED -> "فشل إنشاء التاجر. يرجى المحاولة مرة أخرى."
                    null -> "حدث خطأ عند إنشاء التاجر"
                }
                _uiState.update { it.copy(isSubmitting = false, message = message) }
            }
        }
    }

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    private companion object {
        const val SOURCE = "android_native"
    }
}
