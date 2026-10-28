package com.illa.cashvan.sales.ui.route

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.sales.feature.visit.presentation.viewmodel.VisitSheet
import com.illa.cashvan.sales.ui.components.SalesColors
import com.illa.cashvan.sales.ui.components.SalesPrimaryButton
import com.illa.cashvan.sales.ui.components.SalesSecondaryButton
import com.illa.cashvan.sales.ui.components.ZainBold
import com.illa.cashvan.sales.ui.components.ZainRegular

/** The PWA's `VisitSetupSheet` and `NoOrderReasonSheet`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitFlowSheets(
    sheet: VisitSheet,
    isCheckingLocation: Boolean,
    onConfirmRevisit: () -> Unit,
    onWithOrder: () -> Unit,
    onWithoutOrder: () -> Unit,
    onReasonSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (sheet == VisitSheet.None) return
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SalesColors.Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            when (sheet) {
                is VisitSheet.RevisitWarning -> RevisitWarning(
                    merchantName = sheet.merchant.displayName,
                    unvisitedCount = sheet.unvisitedCount,
                    onConfirm = onConfirmRevisit,
                    onCancel = onDismiss
                )
                is VisitSheet.VisitType -> VisitTypeChooser(
                    merchantName = sheet.merchant.displayName,
                    isCheckingLocation = isCheckingLocation,
                    onWithOrder = onWithOrder,
                    onWithoutOrder = onWithoutOrder
                )
                is VisitSheet.NoOrderReasons -> NoOrderReasons(sheet, onReasonSelected)
                VisitSheet.None -> Unit
            }
        }
    }
}

@Composable
private fun RevisitWarning(
    merchantName: String,
    unvisitedCount: Int,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Text("تأكيد إعادة الزيارة", fontSize = 22.sp, fontFamily = ZainBold)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFFFEF3C7), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = SalesColors.Progress, modifier = Modifier.size(32.dp))
        }
        Text(
            "أنت زورت $merchantName الشهر ده. لسه فيه $unvisitedCount تجار متزورتهمش. تكمل؟",
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            fontFamily = ZainRegular,
            color = SalesColors.Muted
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SalesSecondaryButton("إلغاء", onCancel, Modifier.weight(1f))
        SalesPrimaryButton("متابعة", onConfirm, Modifier.weight(1f))
    }
}

@Composable
private fun VisitTypeChooser(
    merchantName: String,
    isCheckingLocation: Boolean,
    onWithOrder: () -> Unit,
    onWithoutOrder: () -> Unit
) {
    var selected by remember { mutableStateOf<Boolean?>(null) }
    Text("اختار نوع الزيارة", fontSize = 22.sp, fontFamily = ZainBold)
    Text(merchantName, fontSize = 14.sp, color = SalesColors.Muted, fontFamily = ZainRegular)
    Spacer(Modifier.height(16.dp))
    VisitTypeOption(
        icon = Icons.Outlined.ShoppingCart,
        title = "زيارة بطلب",
        description = "اختار المنتجات وسجل الطلب",
        loading = isCheckingLocation && selected == true,
        enabled = !isCheckingLocation,
        onClick = {
            selected = true
            onWithOrder()
        }
    )
    Spacer(Modifier.height(12.dp))
    VisitTypeOption(
        icon = Icons.Outlined.Assignment,
        title = "زيارة بدون طلب",
        description = "سجل الزيارة وسبب عدم الطلب",
        loading = isCheckingLocation && selected == false,
        enabled = !isCheckingLocation,
        onClick = {
            selected = false
            onWithoutOrder()
        }
    )
}

@Composable
private fun VisitTypeOption(
    icon: ImageVector,
    title: String,
    description: String,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(2.dp, SalesColors.Border), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(SalesColors.Primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = SalesColors.Primary)
            } else {
                Icon(icon, contentDescription = null, tint = SalesColors.Primary)
            }
        }
        Column {
            Text(title, fontSize = 17.sp, fontFamily = ZainBold)
            Text(description, fontSize = 13.sp, color = SalesColors.Muted, fontFamily = ZainRegular)
        }
    }
}

@Composable
private fun NoOrderReasons(sheet: VisitSheet.NoOrderReasons, onReasonSelected: (String) -> Unit) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    Text("ليه مفيش طلب النهارده؟", fontSize = 22.sp, fontFamily = ZainBold)
    Text("اختار السبب", fontSize = 14.sp, color = SalesColors.Muted, fontFamily = ZainRegular)
    Spacer(Modifier.height(12.dp))
    if (sheet.isLoading) {
        Text("جاري تحميل الأسباب...", fontSize = 14.sp, color = SalesColors.Muted)
    } else {
        Column(
            modifier = Modifier
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sheet.reasons.forEach { reason ->
                val isSelected = selectedId == reason.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) SalesColors.Primary else SalesColors.Border),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedId = reason.id }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(reason.reason_ar ?: reason.reason_en.orEmpty(), fontSize = 15.sp, fontFamily = ZainBold, modifier = Modifier.weight(1f))
                    if (isSelected) Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = SalesColors.Primary)
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    SalesPrimaryButton(
        text = "تأكيد الزيارة",
        onClick = { selectedId?.let(onReasonSelected) },
        enabled = selectedId != null,
        loading = sheet.isSubmitting
    )
}
