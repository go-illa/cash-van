package com.illa.cashvan.sales.feature.nearby.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.app_preferences.domain.use_case.user.GetUserUseCase
import com.illa.cashvan.core.location.LocationData
import com.illa.cashvan.core.location.LocationService
import com.illa.cashvan.sales.data.CurrentAgentProvider
import com.illa.cashvan.sales.domain.nearby.DirectApprovalMessageParams
import com.illa.cashvan.sales.domain.nearby.MerchantRouteRow
import com.illa.cashvan.sales.domain.nearby.NearbyMerchant
import com.illa.cashvan.sales.domain.nearby.RawNearbyMerchant
import com.illa.cashvan.sales.domain.nearby.ReassignmentMessageParams
import com.illa.cashvan.sales.domain.nearby.ReassignmentRequestRow
import com.illa.cashvan.sales.domain.nearby.RouteNameRow
import com.illa.cashvan.sales.domain.nearby.assembleNearbyMerchants
import com.illa.cashvan.sales.domain.nearby.buildDirectApprovalMessage
import com.illa.cashvan.sales.domain.nearby.buildReassignmentMessage
import com.illa.cashvan.sales.domain.nearby.buildWhatsAppLaunch
import com.illa.cashvan.sales.domain.nearby.involvedRouteIds
import com.illa.cashvan.sales.domain.nearby.nearbyBoundingBox
import com.illa.cashvan.sales.domain.nearby.refineNearbyMerchants
import com.illa.cashvan.sales.feature.kpi.data.model.AssignedRouteRef
import com.illa.cashvan.sales.feature.kpi.domain.repository.SalesKpiRepository
import com.illa.cashvan.sales.feature.nearby.data.model.NewReassignmentRequest
import com.illa.cashvan.sales.feature.nearby.data.model.ReassignmentRequestRow as RequestDto
import com.illa.cashvan.sales.feature.nearby.domain.repository.NearbyRepository
import com.illa.cashvan.sales.feature.nearby.domain.repository.PendingRequestExistsException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import kotlin.math.abs

enum class NearbyDialogKind { ADD, TRANSFER }

data class NearbyDialog(
    val kind: NearbyDialogKind,
    val merchant: NearbyMerchant,
    val routeId: String?,
    val submitting: Boolean = false
)

data class NearbyUiState(
    val isLoading: Boolean = true,
    val locationFailed: Boolean = false,
    val loadFailed: Boolean = false,
    val merchants: List<NearbyMerchant> = emptyList(),
    val myRoutes: List<AssignedRouteRef> = emptyList(),
    val whatsAppNumber: String? = null,
    val dialog: NearbyDialog? = null,
    val history: List<RequestDto>? = null,
    val message: String? = null
)

sealed interface NearbyEvent {
    /** Open this WhatsApp link (`whatsapp://send?...`). */
    data class OpenWhatsApp(val link: String) : NearbyEvent
}

/**
 * The PWA's `NearbyMerchants`: merchants within 1 km that aren't on the agent's routes. Unassigned ones can be
 * added straight to a route; ones on another agent's route need a reassignment request sent over WhatsApp.
 */
class NearbyMerchantsViewModel(
    private val repository: NearbyRepository,
    private val kpiRepository: SalesKpiRepository,
    private val locationService: LocationService,
    private val currentAgentProvider: CurrentAgentProvider,
    private val getUserUseCase: GetUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NearbyUiState())
    val uiState: StateFlow<NearbyUiState> = _uiState.asStateFlow()

    private val _events = Channel<NearbyEvent>(Channel.BUFFERED)
    val events: Flow<NearbyEvent> = _events.receiveAsFlow()

    /** The first GPS fix; the list doesn't follow the agent afterwards. */
    private var pageLocation: LocationData? = null
    /** `flow` column on requests: which mode the agent came from. */
    var flow: String = "presell"

    init {
        viewModelScope.launch {
            val routes = runCatching { kpiRepository.getAssignedRoutes() }.getOrDefault(emptyList())
            val number = runCatching { repository.getWhatsAppNumber() }.getOrNull()
            _uiState.update { it.copy(myRoutes = routes, whatsAppNumber = number) }
            load()
        }
    }

    fun retryLocation() {
        pageLocation = null
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        _uiState.update { it.copy(isLoading = true, locationFailed = false, loadFailed = false) }
        val location = pageLocation ?: locationService.getCurrentLocation().getOrNull()?.also { pageLocation = it }
        if (location == null) {
            _uiState.update { it.copy(isLoading = false, locationFailed = true) }
            return
        }
        try {
            val box = nearbyBoundingBox(location.latitude, location.longitude)
            val raw = repository.getMerchantsInBox(box.minLatitude, box.maxLatitude, box.minLongitude, box.maxLongitude).map {
                RawNearbyMerchant(it.id, it.name.orEmpty(), it.sign_name, it.merchant_code, it.latitude?.toString(), it.longitude?.toString(), it.location, it.phone_number)
            }
            val within = refineNearbyMerchants(raw, location.latitude, location.longitude)
            val ids = within.map { it.merchant.id }
            val merchantRoutes = repository.getMerchantRoutes(ids).map { MerchantRouteRow(it.merchant_id, it.route_id) }
            val routes = repository.getRouteNames(involvedRouteIds(merchantRoutes)).map { RouteNameRow(it.id, it.name.orEmpty()) }
            val requests = repository.getRequestsFor(ids).mapNotNull { row ->
                row.merchant_id?.let { ReassignmentRequestRow(row.id, it, row.status.orEmpty(), row.decline_note, row.requested_at, row.request_type) }
            }
            val myRouteIds = _uiState.value.myRoutes.map { it.id }
            val merchants = assembleNearbyMerchants(within, merchantRoutes, routes, requests, myRouteIds)
            _uiState.update { it.copy(isLoading = false, merchants = merchants) }
        } catch (e: Exception) {
            Timber.e(e, "Nearby merchants failed")
            _uiState.update { it.copy(isLoading = false, loadFailed = true) }
        }
    }

    fun openAdd(merchant: NearbyMerchant) {
        viewModelScope.launch { openDialog(NearbyDialogKind.ADD, merchant) }
    }

    fun openTransfer(merchant: NearbyMerchant) {
        val state = _uiState.value
        val blocker = when {
            state.myRoutes.isEmpty() -> "مفيش مسار مُكلَّف ليك. كلّم المسؤول."
            merchant.pendingRequestId != null -> "فيه طلب قائم بالفعل لهذا التاجر."
            state.whatsAppNumber == null -> "رقم واتساب غير مُعدّ. كلّم الإدارة."
            else -> null
        }
        if (blocker != null) {
            _uiState.update { it.copy(message = blocker) }
            return
        }
        viewModelScope.launch { openDialog(NearbyDialogKind.TRANSFER, merchant) }
    }

    /** Preselects the only route, or the one whose merchants are nearest this merchant. */
    private suspend fun openDialog(kind: NearbyDialogKind, merchant: NearbyMerchant) {
        val routes = _uiState.value.myRoutes
        val routeId = when {
            routes.size <= 1 -> routes.firstOrNull()?.id
            merchant.latitude == null || merchant.longitude == null -> null
            else -> runCatching { repository.getRouteMerchantLocations(routes.map { it.id }) }.getOrDefault(emptyList())
                .mapNotNull { row ->
                    val lat = row.merchants?.latitude ?: return@mapNotNull null
                    val lng = row.merchants.longitude ?: return@mapNotNull null
                    row.route_id to abs(lat - merchant.latitude) + abs(lng - merchant.longitude)
                }
                .minByOrNull { it.second }?.first
        }
        _uiState.update { it.copy(dialog = NearbyDialog(kind, merchant, routeId)) }
    }

    fun selectRoute(routeId: String) {
        _uiState.update { state -> state.copy(dialog = state.dialog?.copy(routeId = routeId)) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialog = null) }
    }

    fun confirmDialog() {
        val dialog = _uiState.value.dialog ?: return
        val routeId = dialog.routeId ?: return
        if (dialog.submitting) return
        val route = _uiState.value.myRoutes.firstOrNull { it.id == routeId }
        viewModelScope.launch {
            _uiState.update { it.copy(dialog = dialog.copy(submitting = true)) }
            val agent = currentAgentProvider.require()
            val user = getUserUseCase().firstOrNull()
            val merchant = dialog.merchant
            val merchantName = merchant.signName?.takeIf { it.isNotBlank() } ?: merchant.name
            try {
                val message = when (dialog.kind) {
                    NearbyDialogKind.ADD -> {
                        repository.linkMerchantToRoute(merchant.id, routeId)
                        repository.createRequest(
                            NewReassignmentRequest(
                                merchant_id = merchant.id, merchant_name = merchantName, merchant_code = merchant.merchantCode,
                                agent_id = agent.agentId, agent_name = user?.name, agent_phone = user?.phone_number,
                                to_route_id = routeId, to_route_name = route?.name,
                                request_type = "direct", status = "approved", resolved_at = Instant.now().toString(), flow = flow
                            )
                        )
                        _uiState.update { it.copy(message = "اتضاف «$merchantName» لمسارك") }
                        buildDirectApprovalMessage(
                            DirectApprovalMessageParams(user?.name.orEmpty(), user?.phone_number.orEmpty(), merchantName, merchant.merchantCode, route?.name.orEmpty())
                        )
                    }
                    NearbyDialogKind.TRANSFER -> {
                        // The merchant moves from its current route; the PWA records the agent's own first route here.
                        val fromRouteId = merchant.routeIds.firstOrNull()
                        val fromRouteName = merchant.routeNames.firstOrNull() ?: "مسار غير معروف"
                        repository.createRequest(
                            NewReassignmentRequest(
                                merchant_id = merchant.id, merchant_name = merchantName, merchant_code = merchant.merchantCode,
                                agent_id = agent.agentId, agent_name = user?.name, agent_phone = user?.phone_number,
                                from_route_id = fromRouteId, from_route_name = fromRouteName,
                                to_route_id = routeId, to_route_name = route?.name,
                                request_type = "whatsapp", status = "pending", flow = flow
                            )
                        )
                        _uiState.update { it.copy(message = "اتبعت طلب النقل") }
                        buildReassignmentMessage(
                            ReassignmentMessageParams(user?.name.orEmpty(), user?.phone_number.orEmpty(), merchantName, merchant.merchantCode, fromRouteName, route?.name.orEmpty())
                        )
                    }
                }
                _uiState.update { it.copy(dialog = null) }
                // WhatsApp opens only after the request is recorded; for a direct add it's just a notification.
                buildWhatsAppLaunch(message, _uiState.value.whatsAppNumber)?.let { _events.send(NearbyEvent.OpenWhatsApp(it.link)) }
                load()
            } catch (e: PendingRequestExistsException) {
                _uiState.update { it.copy(dialog = null, message = "فيه طلب قائم بالفعل لهذا التاجر.") }
            } catch (e: Exception) {
                Timber.e(e, "Nearby action failed")
                _uiState.update {
                    it.copy(
                        dialog = dialog.copy(submitting = false),
                        message = if (dialog.kind == NearbyDialogKind.ADD) "تعذّرت إضافة التاجر للمسار. حاول تاني." else "تعذّر إرسال الطلب. حاول تاني."
                    )
                }
            }
        }
    }

    fun openHistory() {
        viewModelScope.launch {
            val history = runCatching { repository.getMyRequests() }.getOrDefault(emptyList())
            _uiState.update { it.copy(history = history) }
        }
    }

    fun closeHistory() {
        _uiState.update { it.copy(history = null) }
    }

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }
}
