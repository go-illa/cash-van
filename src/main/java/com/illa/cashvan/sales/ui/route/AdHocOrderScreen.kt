package com.illa.cashvan.sales.ui.route

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.illa.cashvan.sales.domain.model.PresellOrderContext
import com.illa.cashvan.sales.feature.route.presentation.viewmodel.AdHocOrderViewModel
import com.illa.cashvan.sales.ui.components.EmptyState
import com.illa.cashvan.sales.ui.components.SalesColors
import com.illa.cashvan.sales.ui.components.SalesTopBar
import com.illa.cashvan.sales.ui.components.ZainBold
import com.illa.cashvan.sales.ui.components.ZainRegular
import com.illa.cashvan.sales.ui.components.salesFieldColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun AdHocOrderScreen(
    onBack: () -> Unit,
    onStartOrder: (PresellOrderContext) -> Unit,
    viewModel: AdHocOrderViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = SalesColors.Background,
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                SalesTopBar(title = "طلب خارج المسار", subtitle = "ابحث عن تاجر على مساراتك", onBack = onBack)
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    placeholder = { Text("اسم التاجر أو اللافتة أو الكود", color = Color(0xFFBDBDBD)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = SalesColors.Muted) },
                    shape = RoundedCornerShape(10.dp),
                    colors = salesFieldColors()
                )
                if (state.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = SalesColors.Primary)
            }
        }
    ) { padding ->
        when {
            state.error != null -> EmptyState(icon = Icons.Outlined.Search, title = state.error.orEmpty(), modifier = Modifier.padding(padding))
            state.query.isBlank() -> EmptyState(
                icon = Icons.Outlined.Search,
                title = "ابدأ اكتب اسم التاجر",
                modifier = Modifier.padding(padding)
            )
            !state.isLoading && state.results.isEmpty() -> EmptyState(
                icon = Icons.Outlined.Storefront,
                title = "مفيش تجار مطابقين",
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.results, key = { it.id }) { merchant ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(10.dp))
                            .clickable { viewModel.select(merchant)?.let(onStartOrder) }
                            .padding(14.dp)
                    ) {
                        Text(merchant.displayName, fontSize = 15.sp, fontFamily = ZainBold)
                        Text(
                            listOfNotNull(merchant.merchant_code, priceTierLabel(merchant.price_tier), merchant.location).joinToString(" • "),
                            fontSize = 12.sp,
                            color = SalesColors.Muted,
                            fontFamily = ZainRegular,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    state.pickingRouteFor?.let { merchant ->
        val routes = state.routesByMerchant[merchant.id].orEmpty()
        AlertDialog(
            onDismissRequest = viewModel::cancelRoutePick,
            title = { Text("اختر المسار", fontFamily = ZainBold) },
            text = {
                Column {
                    routes.forEach { route ->
                        TextButton(onClick = { viewModel.selectRoute(route)?.let(onStartOrder) }) {
                            Text(route.name ?: route.id, fontFamily = ZainBold)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::cancelRoutePick) { Text("إلغاء", fontFamily = ZainBold) } }
        )
    }
}
