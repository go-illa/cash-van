package com.illa.cashvan.sales.ui.kpi

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.illa.cashvan.sales.domain.kpi.AgentKpiCardData
import com.illa.cashvan.sales.domain.kpi.CommissionProgressView
import com.illa.cashvan.sales.domain.kpi.KPI_CARD_SEE_MORE_LABEL
import com.illa.cashvan.sales.domain.kpi.KpiDestination
import com.illa.cashvan.sales.domain.kpi.kpiCardDestination
import com.illa.cashvan.sales.domain.kpi.kpiStatusTone
import com.illa.cashvan.sales.domain.kpi.resolveKpiCardSubtitle
import com.illa.cashvan.sales.domain.kpi.translateKpiKey
import com.illa.cashvan.sales.feature.kpi.presentation.viewmodel.COMMISSION_REFRESH_MS
import com.illa.cashvan.sales.feature.kpi.presentation.viewmodel.SalesKpiViewModel
import com.illa.cashvan.sales.ui.components.LoadingBox
import com.illa.cashvan.sales.ui.components.SalesColors
import com.illa.cashvan.sales.ui.components.SalesTopBar
import com.illa.cashvan.sales.ui.components.ZainBold
import com.illa.cashvan.sales.ui.components.ZainRegular
import com.illa.cashvan.sales.ui.components.playVictoryFanfare
import kotlinx.coroutines.delay

/** "مؤشرات الأداء": commission progress, the three KPI rings, and links to the report and leaderboard. */
@Composable
fun KpiHubScreen(
    viewModel: SalesKpiViewModel,
    onOpen: (KpiDestination) -> Unit,
    onReport: () -> Unit,
    onLeaderboard: () -> Unit
) {
    val state by viewModel.hub.collectAsStateWithLifecycle()
    LaunchedEffect(state.celebrateUnlock) {
        if (state.celebrateUnlock) {
            playVictoryFanfare()
            viewModel.unlockCelebrated()
        }
    }
    // As the PWA: refetch stale numbers on every visit and return to the app, and keep the commission live.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshHubIfStale()
            while (true) {
                delay(COMMISSION_REFRESH_MS)
                viewModel.refreshCommission()
            }
        }
    }
    Column(modifier = Modifier
        .fillMaxSize()
        .background(SalesColors.Background)) {
        SalesTopBar(title = "مؤشرات الأداء", subtitle = "ملخص أدائك الحالي", onBack = null)
        when {
            state.isLoading -> LoadingBox()
            state.error != null -> Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .border(1.dp, SalesColors.Danger.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.error.orEmpty(), color = SalesColors.Danger, fontFamily = ZainRegular)
                TextButton(onClick = viewModel::retryHub) { Text("حاول تاني", fontFamily = ZainBold) }
            }
            else -> Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.commission?.let { view ->
                    LaunchedEffect(view.title, view.unlocked) { viewModel.trackCommissionViewed() }
                    CommissionCard(view)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.cards.forEach { card ->
                        KpiRingCard(card, Modifier.weight(1f)) { onOpen(kpiCardDestination(card.key)) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HubLinkButton("تقرير الأداء", Icons.Outlined.Assessment, SalesColors.Primary, Modifier.weight(1f), onReport)
                    HubLinkButton("المتصدرون", Icons.Outlined.EmojiEvents, Color(0xFFB45309), Modifier.weight(1f), onLeaderboard)
                }
            }
        }
    }
}

/** The PWA's side-by-side outline buttons under the rings. */
@Composable
private fun HubLinkButton(text: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = color.copy(alpha = 0.06f), contentColor = color)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontFamily = ZainBold, fontSize = 14.sp)
    }
}

/** The PWA's `KPICard`: ring, label, value and subtitle in the status colour, "شاهد المزيد". */
@Composable
private fun KpiRingCard(card: AgentKpiCardData, modifier: Modifier, onClick: () -> Unit) {
    val tone = toneColor(kpiStatusTone(card.percentage))
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, SalesColors.Primary.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PercentRing(card.percentage, stroke = 4.5.dp, labelColor = tone)
        Text(
            translateKpiKey(card.labelKey),
            fontSize = 14.sp,
            fontFamily = ZainBold,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(card.displayValue, fontSize = 18.sp, fontFamily = ZainBold, color = tone)
            Text(
                resolveKpiCardSubtitle(card),
                fontSize = 10.sp,
                color = SalesColors.Muted,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(KPI_CARD_SEE_MORE_LABEL, fontSize = 11.sp, color = SalesColors.Primary, fontFamily = ZainBold)
            Icon(Icons.Outlined.ChevronLeft, contentDescription = null, tint = SalesColors.Primary, modifier = Modifier.size(14.dp))
        }
    }
}

/** The PWA's `CommissionProgressCard`: purple while locked, gold once the threshold is reached. */
@Composable
private fun CommissionCard(view: CommissionProgressView) {
    val gradient = if (view.unlocked) {
        Brush.linearGradient(listOf(Color(0xFFFBBF24), Color(0xFFF97316), Color(0xFFFACC15)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF9333EA), Color(0xFFC026D3)))
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(view.title, fontSize = 16.sp, fontFamily = ZainBold, color = Color.White)
                Text(view.subtitle, fontSize = 12.sp, fontFamily = ZainBold, color = Color.White.copy(alpha = 0.6f))
            }
            Text(
                view.achievementLabel,
                fontSize = 14.sp,
                fontFamily = ZainBold,
                color = Color.White,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                modifier = Modifier.width(96.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (view.unlocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = if (view.unlocked) Color(0xFFFEF9C3) else Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                view.remainingToUnlockText?.let {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("فاضل", fontSize = 11.sp, fontFamily = ZainBold, color = Color.White.copy(alpha = 0.85f))
                        Text(it, fontSize = 18.sp, fontFamily = ZainBold, color = Color(0xFFFDE047))
                    }
                }
                view.unlockedCaption?.let {
                    Text(it, fontSize = 11.sp, fontFamily = ZainBold, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center)
                }
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                // The payout sits in a glass box whose border states the outcome; locked, it is frosted and slashed.
                Box(
                    modifier = Modifier
                        .widthIn(min = 96.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (view.isEarning) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(16.dp)
                        )
                        .border(
                            2.dp,
                            if (view.isEarning) Color(0xFF4ADE80).copy(alpha = 0.8f) else Color(0xFFF87171).copy(alpha = 0.8f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        view.heroPayoutText,
                        fontSize = 48.sp,
                        lineHeight = 52.sp,
                        fontFamily = ZainBold,
                        color = if (view.isEarning) Color(0xFFFACC15) else Color(0xFFFCD34D).copy(alpha = 0.9f),
                        modifier = if (view.isEarning) Modifier else Modifier.blur(10.dp)
                    )
                    if (!view.isEarning) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .drawBehind {
                                    rotate(-18f) {
                                        drawLine(
                                            color = Color(0xFFF87171).copy(alpha = 0.8f),
                                            start = Offset(-size.width * 0.1f, center.y),
                                            end = Offset(size.width * 1.1f, center.y),
                                            strokeWidth = 3.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                        )
                    }
                }
            }
        }
        CommissionBar(view)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            listOf(
                Triple("المحقق", view.achievedText, Color(0xFF16A34A)),
                Triple("المتبقي", view.remainingToTargetText, Color(0xFFD97706)),
                Triple("المستهدف", view.targetText, Color(0xFF0F172A))
            ).forEach { (label, value, color) ->
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, fontSize = 12.sp, fontFamily = ZainBold, color = Color(0xFF6B7280))
                    Text(value, fontSize = 20.sp, fontFamily = ZainBold, color = color, maxLines = 1)
                }
            }
        }
    }
}

/** Progress bar that fills up from 0, with the 🗝 key marker at the unlock threshold while locked. */
@Composable
private fun CommissionBar(view: CommissionProgressView) {
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { ready = true }
    val fill by animateFloatAsState(
        targetValue = if (ready) (view.barPercent / 100).toFloat().coerceIn(0f, 1f) else 0f,
        animationSpec = tween(700),
        label = "commissionBar"
    )
    val marker = view.thresholdMarkerPct
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        Column {
            if (marker != null) Spacer(Modifier.height(26.dp))
            LinearProgressIndicator(
                progress = { fill },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(50)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f),
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }
        if (marker != null) {
            val x = maxWidth * (marker / 100).toFloat()
            Box(
                modifier = Modifier
                    .offset(x = x - 1.dp, y = 22.dp)
                    .width(2.dp)
                    .height(18.dp)
                    .background(Color.White.copy(alpha = 0.7f))
            )
            Row(
                modifier = Modifier
                    .offset(x = x - 32.dp)
                    .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Outlined.Key, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Text(view.thresholdLabel, fontSize = 13.sp, fontFamily = ZainBold, color = Color.White)
            }
        }
    }
}
