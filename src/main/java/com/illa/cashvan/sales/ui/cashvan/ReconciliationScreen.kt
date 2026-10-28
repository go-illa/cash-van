package com.illa.cashvan.sales.ui.cashvan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.illa.cashvan.core.app_preferences.domain.use_case.user.GetUserUseCase
import com.illa.cashvan.core.utils.shareCacheFile
import com.illa.cashvan.sales.data.CurrentAgentProvider
import com.illa.cashvan.sales.domain.format.formatEGP
import com.illa.cashvan.sales.domain.reconciliation.PlanProductBreakdownRow
import com.illa.cashvan.sales.feature.cashvan.presentation.viewmodel.ReconciliationViewModel
import com.illa.cashvan.sales.ui.components.EmptyState
import com.illa.cashvan.sales.ui.components.LoadingBox
import com.illa.cashvan.sales.ui.components.SalesColors
import com.illa.cashvan.sales.ui.components.SalesPrimaryButton
import com.illa.cashvan.sales.ui.components.SalesTopBar
import com.illa.cashvan.sales.ui.components.ZainBold
import com.illa.cashvan.sales.ui.components.ZainRegular
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import timber.log.Timber

/** "تقفيل الحساب اليومي": the open plan's money and stock, with a PDF to share. */
@Composable
fun ReconciliationScreen(
    onBack: () -> Unit,
    viewModel: ReconciliationViewModel = koinViewModel(),
    getUserUseCase: GetUserUseCase = koinInject(),
    currentAgentProvider: CurrentAgentProvider = koinInject()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val host = remember { SnackbarHostState() }
    var exporting by remember { mutableStateOf(false) }

    val exportPdf: () -> Unit = {
        scope.launch {
            exporting = true
            try {
                val report = viewModel.freshReport() ?: state.report ?: return@launch
                val agent = ReconciliationAgent(
                    name = getUserUseCase().firstOrNull()?.name.orEmpty(),
                    code = runCatching { currentAgentProvider.require().agentCode }.getOrNull().orEmpty()
                )
                val file = withContext(Dispatchers.IO) { writeReconciliationPdf(context, report, agent) }
                shareCacheFile(context, file, "application/pdf", "مشاركة التقرير")
            } catch (e: Exception) {
                Timber.e(e, "Reconciliation PDF failed")
                host.showSnackbar("تعذر إنشاء تقرير PDF")
            } finally {
                exporting = false
            }
        }
    }

    Scaffold(
        containerColor = SalesColors.Background,
        snackbarHost = { SnackbarHost(host) },
        topBar = { SalesTopBar(title = "تقفيل الحساب اليومي", onBack = onBack) },
        bottomBar = {
            if (state.report != null) {
                Box(modifier = Modifier
                    .background(Color.White)
                    .padding(16.dp)) {
                    SalesPrimaryButton(
                        text = "تحميل تقرير PDF",
                        onClick = exportPdf,
                        loading = exporting,
                        icon = Icons.Outlined.PictureAsPdf
                    )
                }
            }
        }
    ) { padding ->
        val report = state.report
        when {
            state.isLoading -> LoadingBox(Modifier.padding(padding))
            state.failed -> EmptyState(
                icon = Icons.Outlined.Inventory2,
                title = "تعذر تحميل التقفيل",
                modifier = Modifier.padding(padding),
                action = { TextButton(onClick = viewModel::load) { Text("حاول تاني", fontFamily = ZainBold) } }
            )
            report == null -> EmptyState(icon = Icons.Outlined.Inventory2, title = "لا يوجد خطة كاش فان نشطة", modifier = Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("التقفيل", fontSize = 17.sp, fontFamily = ZainBold)
                        val totals = report.totals
                        MoneyLine("نقدي", totals.cash)
                        if (totals.credit > 0) MoneyLine("آجل", totals.credit)
                        if (totals.fawry > 0) MoneyLine("فوري", totals.fawry)
                        MoneyLine("الإجمالي", totals.cash + totals.credit + totals.fawry, bold = true)
                    }
                }
                item { Text("تفاصيل المخزون", fontSize = 17.sp, fontFamily = ZainBold) }
                if (report.groups.isEmpty()) {
                    item { EmptyState(icon = Icons.Outlined.Inventory2, title = "لا توجد منتجات في الخطة") }
                }
                report.groups.forEach { group ->
                    item(key = "group-${group.principalName}") {
                        Text(group.principalName, fontSize = 14.sp, fontFamily = ZainBold, color = SalesColors.Primary)
                    }
                    items(group.rows, key = { it.planProductId }) { row -> StockCard(row) }
                }
            }
        }
    }
}

@Composable
private fun MoneyLine(label: String, value: Double, bold: Boolean = false) {
    Row {
        Text(label, fontFamily = if (bold) ZainBold else ZainRegular, modifier = Modifier.weight(1f))
        Text(formatEGP(value, 2), fontFamily = ZainBold, color = if (bold) SalesColors.Primary else Color.Black)
    }
}

@Composable
private fun StockCard(row: PlanProductBreakdownRow) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(row.productName, fontFamily = ZainBold, fontSize = 14.sp)
        Text(row.sku, fontSize = 11.sp, color = SalesColors.Muted)
        QuantityLine("الإجمالي", row.assignedQuantity, row.soldQuantity, row.remainingQuantity, "مخصص")
        if (row.hasChannelBreakdown && row.loadedCashVan > 0) {
            QuantityLine("كاش فان", row.loadedCashVan, row.soldCashVan, row.availableCashVan, "الحمولة")
        }
        if (row.hasChannelBreakdown && row.loadedPreSell > 0) {
            QuantityLine("بري سيل", row.loadedPreSell, row.soldPreSell, row.availablePreSell, "الحمولة")
        }
        Row {
            Text("قيمة المبيعات", fontSize = 12.sp, color = SalesColors.Muted, modifier = Modifier.weight(1f))
            Text(formatEGP(row.totalValue, 2), fontSize = 13.sp, fontFamily = ZainBold, color = SalesColors.Primary)
        }
    }
}

@Composable
private fun QuantityLine(title: String, loaded: Double, sold: Double, remaining: Double, loadedLabel: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 12.sp, fontFamily = ZainBold, modifier = Modifier.weight(1f))
        Text(
            "$loadedLabel ${loaded.toInt()} • مباع ${sold.toInt()} • متبقي ${remaining.toInt()}",
            fontSize = 12.sp,
            color = SalesColors.Muted,
            fontFamily = ZainRegular
        )
    }
}
