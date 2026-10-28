package com.illa.cashvan.sales.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.illa.cashvan.sales.feature.profile.presentation.viewmodel.SalesProfileViewModel
import com.illa.cashvan.sales.ui.components.LoadingBox
import com.illa.cashvan.sales.ui.components.SalesColors
import com.illa.cashvan.sales.ui.components.SalesPrimaryButton
import com.illa.cashvan.sales.ui.components.SalesSecondaryButton
import com.illa.cashvan.sales.ui.components.SalesTopBar
import com.illa.cashvan.sales.ui.components.ZainBold
import com.illa.cashvan.sales.ui.components.ZainRegular
import com.illa.cashvan.sales.ui.route.Pill
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SalesProfileScreen(
    onLogout: () -> Unit,
    onSwitchModule: (() -> Unit)?,
    viewModel: SalesProfileViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SalesColors.Background)
    ) {
        SalesTopBar(title = "الملف الشخصي", onBack = null)
        if (state.isLoading) {
            LoadingBox()
            return@Column
        }
        val agent = state.agent
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(SalesColors.Primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = SalesColors.Primary, modifier = Modifier.size(40.dp))
                }
                Text(agent?.name.orEmpty(), fontSize = 20.sp, fontFamily = ZainBold)
                if (agent?.status == 1) Pill("نشط", SalesColors.Success) else Pill("غير نشط", SalesColors.Danger)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoRow(Icons.Outlined.Numbers, "كود المندوب", agent?.agent_code ?: "غير متاح")
                InfoRow(Icons.Outlined.Phone, "رقم الهاتف", agent?.phone_number ?: "غير متاح")
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Map, contentDescription = null, tint = SalesColors.Primary)
                    Text(" المسارات المخصصة", fontFamily = ZainBold, fontSize = 16.sp)
                }
                when {
                    state.routesLoading -> Text("جاري تحميل المسارات...", color = SalesColors.Muted, fontFamily = ZainRegular)
                    state.routes.isEmpty() -> Text("لا توجد مسارات مخصصة حالياً", color = SalesColors.Muted, fontFamily = ZainRegular)
                    else -> FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.routes.forEach { Pill(it.name ?: it.id, SalesColors.Primary) }
                    }
                }
            }
            if (onSwitchModule != null) {
                SalesSecondaryButton(text = "تبديل الوضع", onClick = onSwitchModule, icon = Icons.Default.SwapHoriz)
            }
            SalesPrimaryButton(
                text = "تسجيل الخروج",
                onClick = onLogout,
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                containerColor = SalesColors.Danger
            )
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = SalesColors.Muted, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(label, fontSize = 12.sp, color = SalesColors.Muted, fontFamily = ZainRegular)
            Text(value, fontSize = 16.sp, fontFamily = ZainBold)
        }
    }
}
