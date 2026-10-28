package com.illa.cashvan.sales.ui.fawry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.sales.feature.fawry.presentation.viewmodel.FawryGate
import com.illa.cashvan.sales.feature.fawry.presentation.viewmodel.FawryGateState
import com.illa.cashvan.sales.feature.fawry.presentation.viewmodel.FawryStep
import com.illa.cashvan.sales.ui.components.SalesColors
import com.illa.cashvan.sales.ui.components.ZainBold
import com.illa.cashvan.sales.ui.components.ZainRegular
import com.illa.cashvan.sales.ui.components.salesFieldColors

@Composable
fun FawryQuestionnaireDialog(state: FawryGateState?, gate: FawryGate) {
    if (state == null) return
    var deviceNumber by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!state.saving) gate.cancel() },
        title = {
            Text(
                when (state.step) {
                    FawryStep.HAS_DEVICE -> "جهاز فوري"
                    FawryStep.DEVICE_NUMBER -> "رقم جهاز فوري"
                    FawryStep.WANTS_DEVICE -> "جهاز فوري"
                },
                fontFamily = ZainBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    when (state.step) {
                        FawryStep.HAS_DEVICE -> "هل يوجد جهاز فوري لدى التاجر؟"
                        FawryStep.DEVICE_NUMBER -> "أدخل رقم جهاز فوري الخاص بالتاجر"
                        FawryStep.WANTS_DEVICE -> "هل يرغب التاجر في الحصول على جهاز فوري؟"
                    },
                    fontFamily = ZainRegular,
                    fontSize = 16.sp
                )
                if (state.step == FawryStep.DEVICE_NUMBER) {
                    OutlinedTextField(
                        value = deviceNumber,
                        onValueChange = { value -> deviceNumber = value.filter(Char::isDigit) },
                        label = { Text("رقم الجهاز") },
                        placeholder = { Text("مثال: 123456789") },
                        singleLine = true,
                        enabled = !state.saving,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = salesFieldColors()
                    )
                }
                state.error?.let { Text(it, color = SalesColors.Danger, fontSize = 13.sp, fontFamily = ZainRegular) }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                when (state.step) {
                    FawryStep.HAS_DEVICE -> {
                        TextButton(onClick = { gate.answerHasDevice(false) }) { Text("لا", fontFamily = ZainBold) }
                        TextButton(onClick = { gate.answerHasDevice(true) }) { Text("نعم", fontFamily = ZainBold) }
                    }
                    FawryStep.DEVICE_NUMBER -> {
                        TextButton(onClick = gate::back, enabled = !state.saving) { Text("رجوع", fontFamily = ZainBold) }
                        TextButton(onClick = { gate.saveDeviceNumber(deviceNumber) }, enabled = !state.saving) {
                            Text(if (state.saving) "جاري الحفظ..." else "حفظ ومتابعة", fontFamily = ZainBold)
                        }
                    }
                    FawryStep.WANTS_DEVICE -> {
                        TextButton(onClick = gate::back, enabled = !state.saving) { Text("رجوع", fontFamily = ZainBold) }
                        TextButton(onClick = { gate.answerWantsDevice(false) }, enabled = !state.saving) { Text("لا", fontFamily = ZainBold) }
                        TextButton(onClick = { gate.answerWantsDevice(true) }, enabled = !state.saving) { Text("نعم", fontFamily = ZainBold) }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = gate::cancel, enabled = !state.saving) {
                Text("إلغاء", fontFamily = ZainBold, color = SalesColors.Muted)
            }
        }
    )
}
