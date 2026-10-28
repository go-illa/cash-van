package com.illa.cashvan.sales.feature.fawry.presentation.viewmodel

import com.illa.cashvan.sales.feature.fawry.domain.repository.DuplicateFawryDeviceException
import com.illa.cashvan.sales.feature.fawry.domain.repository.FawryQuestionnaireRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

enum class FawryStep { HAS_DEVICE, DEVICE_NUMBER, WANTS_DEVICE }

data class FawryGateState(
    val step: FawryStep = FawryStep.HAS_DEVICE,
    val saving: Boolean = false,
    val error: String? = null
)

/**
 * The Fawry device questionnaire (DIS-517) as a step in a visit flow. Run it only after the agent's location
 * has been validated, so it can't be answered while spoofing location.
 *
 * Owned by a ViewModel, which exposes [state] for the dialog and forwards its actions.
 */
class FawryGate(
    private val repository: FawryQuestionnaireRepository,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow<FawryGateState?>(null)
    /** Null while no questionnaire is showing. */
    val state: StateFlow<FawryGateState?> = _state.asStateFlow()

    private var merchantId: String? = null
    private var result: CompletableDeferred<Boolean>? = null

    /** True when the visit can go on: no questionnaire was needed, or it was answered. False if cancelled. */
    suspend fun run(merchantId: String): Boolean {
        if (!repository.needsQuestionnaire(merchantId)) return true
        this.merchantId = merchantId
        val deferred = CompletableDeferred<Boolean>()
        result = deferred
        _state.value = FawryGateState()
        return deferred.await()
    }

    fun answerHasDevice(hasDevice: Boolean) {
        _state.update { it?.copy(step = if (hasDevice) FawryStep.DEVICE_NUMBER else FawryStep.WANTS_DEVICE, error = null) }
    }

    fun back() {
        _state.update { it?.copy(step = FawryStep.HAS_DEVICE, error = null) }
    }

    fun saveDeviceNumber(deviceNumber: String) {
        val digits = deviceNumber.trim()
        if (digits.isEmpty() || !digits.all(Char::isDigit)) {
            _state.update { it?.copy(error = "أدخل رقم جهاز فوري صحيح (أرقام فقط)") }
            return
        }
        save(
            failureMessage = "تعذر حفظ رقم الجهاز، يرجى المحاولة مرة أخرى",
            duplicateMessage = "رقم الجهاز مستخدم بالفعل مع تاجر آخر، يرجى التأكد من الرقم"
        ) { repository.saveDeviceNumber(it, digits) }
    }

    fun answerWantsDevice(wantsDevice: Boolean) {
        save(failureMessage = "تعذر حفظ الإجابة، يرجى المحاولة مرة أخرى") { repository.saveWantsDevice(it, wantsDevice) }
    }

    fun cancel() {
        finish(false)
    }

    private fun save(
        failureMessage: String,
        duplicateMessage: String = failureMessage,
        block: suspend (String) -> Unit
    ) {
        val id = merchantId ?: return
        if (_state.value?.saving == true) return
        _state.update { it?.copy(saving = true, error = null) }
        scope.launch {
            try {
                block(id)
                finish(true)
            } catch (e: DuplicateFawryDeviceException) {
                _state.update { it?.copy(saving = false, error = duplicateMessage) }
            } catch (e: Exception) {
                Timber.e(e, "Fawry questionnaire save failed")
                _state.update { it?.copy(saving = false, error = failureMessage) }
            }
        }
    }

    private fun finish(answered: Boolean) {
        _state.value = null
        merchantId = null
        result?.complete(answered)
        result = null
    }
}
