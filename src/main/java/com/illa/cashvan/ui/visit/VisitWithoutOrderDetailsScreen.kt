package com.illa.cashvan.ui.visit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.illa.cashvan.R
import com.illa.cashvan.feature.visit.data.model.VisitItem
import com.illa.cashvan.feature.visit.presentation.viewmodel.VisitListViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitWithoutOrderDetailsScreen(
    visitId: String,
    onBackClick: () -> Unit = {},
    viewModel: VisitListViewModel = koinViewModel()
) {
    val state by viewModel.detailState.collectAsStateWithLifecycle()
    val zain = FontFamily(Font(R.font.zain_regular))

    LaunchedEffect(visitId) {
        viewModel.loadVisitDetail(visitId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "تفاصيل الزيارة",
                        fontFamily = zain,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D3773)
                )
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF0D3773))
                }
            }

            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.error.orEmpty(),
                        color = Color.Red,
                        fontFamily = zain,
                        textAlign = TextAlign.Center
                    )
                }
            }

            state.visit != null -> {
                VisitDetailsContent(
                    visit = state.visit!!,
                    modifier = Modifier.padding(padding),
                    zain = zain
                )
            }
        }
    }
}

@Composable
private fun VisitDetailsContent(
    visit: VisitItem,
    modifier: Modifier = Modifier,
    zain: FontFamily
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DetailCard(title = "معلومات الزيارة", zain = zain) {
            DetailRow("نوع الزيارة", "بدون طلب", zain)
            DetailRow("تاريخ الزيارة", formatFullDate(visit.visit_date), zain)
        }

        if (!visit.merchant?.name.isNullOrBlank() || !visit.merchant?.phone_number.isNullOrBlank()) {
            DetailCard(title = "التاجر", zain = zain) {
                if (!visit.merchant?.name.isNullOrBlank()) {
                    DetailRow("الاسم", visit.merchant!!.name!!, zain)
                }
                if (!visit.merchant?.phone_number.isNullOrBlank()) {
                    DetailRow("رقم الهاتف", visit.merchant!!.phone_number!!, zain)
                }
            }
        }

        if (!visit.no_order_reason?.reason_ar.isNullOrBlank()) {
            DetailCard(title = "سبب عدم الطلب", zain = zain) {
                Text(
                    text = visit.no_order_reason!!.reason_ar!!,
                    fontSize = 15.sp,
                    fontFamily = zain,
                    color = Color(0xFF1F252E)
                )
            }
        }

        if (!visit.sales_agent?.name.isNullOrBlank()) {
            DetailCard(title = "مندوب المبيعات", zain = zain) {
                DetailRow("الاسم", visit.sales_agent!!.name!!, zain)
            }
        }

        DetailCard(title = "بيانات إضافية", zain = zain) {
            DetailRow("تاريخ الإنشاء", formatFullDate(visit.created_at), zain)
            DetailRow("آخر تحديث", formatFullDate(visit.updated_at), zain)
        }
    }
}

@Composable
private fun DetailCard(
    title: String,
    zain: FontFamily,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = zain,
                color = Color(0xFF0D3773)
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, zain: FontFamily) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontFamily = zain,
            color = Color(0xFF757575),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontFamily = zain,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1F252E),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.5f)
        )
    }
}

private fun formatFullDate(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return "—"
    return try {
        val inputFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        val outputFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val date = inputFmt.parse(dateStr)
        outputFmt.format(date ?: return dateStr)
    } catch (e: Exception) {
        try { dateStr.substring(0, 16).replace("T", " ") } catch (_: Exception) { dateStr }
    }
}

