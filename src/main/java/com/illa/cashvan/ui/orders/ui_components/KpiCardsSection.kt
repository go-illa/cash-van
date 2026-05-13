package com.illa.cashvan.ui.orders.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.illa.cashvan.R
import com.illa.cashvan.feature.kpi.data.model.AgentKpiResponse
import com.illa.cashvan.feature.kpi.presentation.viewmodel.KpiViewModel
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
fun KpiCardsSection(
    modifier: Modifier = Modifier,
    viewModel: KpiViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color(0xFF0D3773),
                    strokeWidth = 2.dp
                )
            }
            uiState.error != null -> {
                Text(
                    text = uiState.error ?: "",
                    fontSize = 12.sp,
                    color = Color(0xFF9E9E9E),
                    fontFamily = FontFamily(Font(R.font.zain_regular)),
                    textAlign = TextAlign.Center
                )
            }
            uiState.kpi != null -> {
                KpiRow(kpi = uiState.kpi!!)
            }
        }
    }
}

@Composable
private fun KpiRow(kpi: AgentKpiResponse) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KpiCard(
            modifier = Modifier.weight(1f),
            title = "نسبة البيع",
            percentage = kpi.strike_rate.percentage,
            primaryValue = String.format(Locale.getDefault(), "%.1f%%", kpi.strike_rate.percentage),
            secondaryValue = "${kpi.strike_rate.ratio} زيارات"
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            title = "معدل إنجاز الخطة",
            percentage = kpi.completion_rate.percentage,
            primaryValue = String.format(Locale.getDefault(), "%.1f%%", kpi.completion_rate.percentage),
            secondaryValue = kpi.completion_rate.ratio
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            title = "زيارات اليوم",
            percentage = kpi.today_visits.percentage,
            primaryValue = kpi.today_visits.ratio,
            secondaryValue = "هدف يومي"
        )
    }
}

@Composable
private fun KpiCard(
    modifier: Modifier = Modifier,
    title: String,
    percentage: Double,
    primaryValue: String,
    secondaryValue: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE8E8E8), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        KpiCircularIndicator(percentage = percentage)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily(Font(R.font.zain_regular)),
            color = Color(0xFF212121),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = primaryValue,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily(Font(R.font.zain_regular)),
            color = Color(0xFFE53935),
            textAlign = TextAlign.Center
        )
        Text(
            text = secondaryValue,
            fontSize = 10.sp,
            fontFamily = FontFamily(Font(R.font.zain_regular)),
            color = Color(0xFF00897B),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun KpiCircularIndicator(percentage: Double) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(68.dp)
    ) {
        CircularProgressIndicator(
            progress = { (percentage / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFE53935),
            trackColor = Color(0xFFE0E0E0),
            strokeWidth = 6.dp
        )
        Text(
            text = "${percentage.toInt()}%",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily(Font(R.font.zain_regular)),
            color = Color(0xFFE53935),
            textAlign = TextAlign.Center
        )
    }
}