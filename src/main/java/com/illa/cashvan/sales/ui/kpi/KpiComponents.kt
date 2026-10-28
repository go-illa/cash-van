package com.illa.cashvan.sales.ui.kpi

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.sales.domain.kpi.ARABIC_MONTHS
import com.illa.cashvan.sales.domain.kpi.KpiPeriod
import com.illa.cashvan.sales.domain.kpi.KpiStatusTone
import com.illa.cashvan.sales.domain.kpi.arabicMonthLabel
import com.illa.cashvan.sales.domain.kpi.currentCairoPeriod
import com.illa.cashvan.sales.domain.kpi.kpiRingFraction
import com.illa.cashvan.sales.domain.kpi.kpiRingLabel
import com.illa.cashvan.sales.domain.kpi.kpiStatusTone
import com.illa.cashvan.sales.ui.components.SalesColors
import com.illa.cashvan.sales.ui.components.ZainBold
import com.illa.cashvan.sales.ui.components.ZainRegular

fun toneColor(tone: KpiStatusTone): Color = when (tone) {
    KpiStatusTone.DANGER -> SalesColors.Danger
    KpiStatusTone.PROGRESS -> SalesColors.Progress
    KpiStatusTone.SUCCESS -> SalesColors.Success
}

/** A ring that animates to [fraction] (0..1) with a centre caption, like the PWA's SVG gauges. */
@Composable
fun KpiRing(
    fraction: Float,
    color: Color,
    size: Dp,
    stroke: Dp,
    center: @Composable () -> Unit
) {
    var target by remember { mutableStateOf(0f) }
    LaunchedEffect(fraction) { target = fraction.coerceIn(0f, 1f) }
    val animated by animateFloatAsState(target, animationSpec = tween(700), label = "ring")
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(progress = { 1f }, modifier = Modifier.size(size), color = SalesColors.Border, strokeWidth = stroke)
        CircularProgressIndicator(progress = { animated }, modifier = Modifier.size(size), color = color, strokeWidth = stroke, trackColor = Color.Transparent)
        Column(horizontalAlignment = Alignment.CenterHorizontally) { center() }
    }
}

@Composable
fun PercentRing(
    percentage: Double,
    size: Dp = 56.dp,
    stroke: Dp = 5.dp,
    caption: String? = null,
    labelColor: Color = Color.Unspecified
) {
    KpiRing(
        fraction = kpiRingFraction(percentage).toFloat(),
        color = toneColor(kpiStatusTone(percentage)),
        size = size,
        stroke = stroke
    ) {
        Text(kpiRingLabel(percentage), fontSize = if (size > 100.dp) 26.sp else 13.sp, fontFamily = ZainBold, color = labelColor)
        caption?.let { Text(it, fontSize = 11.sp, color = SalesColors.Muted, fontFamily = ZainRegular) }
    }
}

@Composable
fun KpiCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) { content() }
}

@Composable
fun LabeledBar(label: String, valueText: String, fraction: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(label, fontSize = 13.sp, fontFamily = ZainRegular, modifier = Modifier.weight(1f))
            Text(valueText, fontSize = 13.sp, fontFamily = ZainBold)
        }
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = SalesColors.Border
        )
    }
}

/** Month chip: current month in primary, any other in amber. Future months aren't offered. */
@Composable
fun MonthPicker(selected: KpiPeriod, onSelected: (KpiPeriod) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val current = currentCairoPeriod()
    val months = remember(current) {
        (0 until 24).map { offset ->
            val index = current.year * 12 + (current.month - 1) - offset
            KpiPeriod(month = index % 12 + 1, year = index / 12)
        }
    }
    val isCurrent = selected == current
    Box {
        TextButton(onClick = { open = true }) {
            Text(
                arabicMonthLabel(selected.month, selected.year),
                fontFamily = ZainBold,
                color = if (isCurrent) SalesColors.Primary else SalesColors.Progress
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            months.forEach { period ->
                DropdownMenuItem(
                    text = { Text("${ARABIC_MONTHS[period.month - 1]} ${period.year}", fontFamily = ZainRegular) },
                    onClick = {
                        open = false
                        onSelected(period)
                    }
                )
            }
        }
    }
}

@Composable
fun StatLine(label: String, value: String, valueColor: Color = Color.Black, caption: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontFamily = ZainRegular)
            caption?.let { Text(it, fontSize = 11.sp, color = SalesColors.Muted) }
        }
        Text(value, fontSize = 15.sp, fontFamily = ZainBold, color = valueColor)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, fontSize = 17.sp, fontFamily = ZainBold, modifier = Modifier.padding(top = 4.dp))
}
