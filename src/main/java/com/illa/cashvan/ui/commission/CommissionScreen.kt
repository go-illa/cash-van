package com.illa.cashvan.ui.commission

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.illa.cashvan.R
import com.illa.cashvan.core.analytics.CashVanAnalyticsHelper
import com.illa.cashvan.feature.commission.domain.model.FulfillmentCommission
import com.illa.cashvan.feature.commission.presentation.viewmodel.CommissionViewModel
import com.illa.cashvan.ui.common.CashVanHeader
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.util.Locale
import kotlin.math.floor

private val PrimaryColor = Color(0xFF0D3773)
private val SuccessColor = Color(0xFF16A249)
private val WarningColor = Color(0xFFFF9800)
private val SecondaryTextColor = Color(0xFF65758B)

@Composable
fun CommissionScreen(
    viewModel: CommissionViewModel = koinViewModel(),
    analyticsHelper: CashVanAnalyticsHelper = koinInject()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val commissions = uiState.commissions

    LaunchedEffect(Unit) {
        viewModel.loadCommission()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        CashVanHeader()

        when {
            commissions == null && uiState.error == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryColor)
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.error.orEmpty(),
                            color = Color.Red,
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.zain_regular))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadCommission() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                        ) {
                            Text("إعادة المحاولة")
                        }
                    }
                }
            }

            commissions != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = "العمولة",
                        fontSize = 24.sp,
                        fontFamily = FontFamily(Font(R.font.zain_bold)),
                        color = Color.Black
                    )
                    Text(
                        text = "مستهدف الشهر الحالي",
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = SecondaryTextColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (commissions.isEmpty()) {
                        EmptyCommission()
                    } else {
                        commissions.forEach { commission ->
                            CommissionContent(
                                commission = commission,
                                showTargetName = commissions.size > 1,
                                onOpenCommission = {
                                    analyticsHelper.logEvent(
                                        "commission_opened",
                                        mapOf("payout_egp" to commission.payoutEgp.toString())
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommissionContent(
    commission: FulfillmentCommission,
    showTargetName: Boolean,
    onOpenCommission: () -> Unit
) {
    Column {
        if (showTargetName) {
            Text(
                text = commission.targetName,
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.zain_bold)),
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        TargetProgressCard(commission = commission)
        Spacer(modifier = Modifier.height(16.dp))
        if (commission.isEligible) {
            EligibleCommissionCard(
                commission = commission,
                onOpenCommission = onOpenCommission
            )
        } else {
            LockedCommissionCard(commission = commission)
        }
    }
}

@Composable
private fun TargetProgressCard(commission: FulfillmentCommission) {
    CommissionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${commission.achievedOrders} من ${commission.targetOrders} كرتونة",
                fontSize = 20.sp,
                fontFamily = FontFamily(Font(R.font.zain_bold)),
                color = Color.Black
            )
            Text(
                text = String.format(Locale.getDefault(), "%.1f%%", commission.achievementPercentage),
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(R.font.zain_bold)),
                color = PrimaryColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x1A0D3773))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = { (commission.achievementPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = if (commission.remainingOrders == 0) SuccessColor else PrimaryColor,
            trackColor = Color(0xFFE3F2FD),
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OrdersStat(
                modifier = Modifier.weight(1f),
                label = "المحقق",
                value = commission.achievedOrders,
                color = SuccessColor
            )
            OrdersStat(
                modifier = Modifier.weight(1f),
                label = "المتبقي",
                value = commission.remainingOrders,
                color = WarningColor
            )
            OrdersStat(
                modifier = Modifier.weight(1f),
                label = "المستهدف",
                value = commission.targetOrders,
                color = PrimaryColor
            )
        }
    }
}

@Composable
private fun OrdersStat(
    modifier: Modifier = Modifier,
    label: String,
    value: Int,
    color: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = FontFamily(Font(R.font.zain_regular)),
            color = SecondaryTextColor
        )
        Text(
            text = value.toString(),
            fontSize = 22.sp,
            fontFamily = FontFamily(Font(R.font.zain_bold)),
            color = color
        )
        Text(
            text = "كرتونة",
            fontSize = 12.sp,
            fontFamily = FontFamily(Font(R.font.zain_regular)),
            color = SecondaryTextColor
        )
    }
}

@Composable
private fun LockedCommissionCard(commission: FulfillmentCommission) {
    CommissionCard {
        CommissionStatusHeader(
            icon = Icons.Default.Lock,
            iconColor = SecondaryTextColor,
            title = "العمولة لسه مقفولة",
            subtitle = if (commission.remainingOrders > 0) {
                "فاضل ${commission.remainingOrders} كرتونة وتوصل للمستهدف وتفتح العمولة"
            } else {
                "وصلت لمستهدف الكراتين، لكن باقي شروط العمولة لسه متحققتش"
            }
        )

        if (commission.potentialPayoutEgp > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "العمولة اللي هتكسبها لما تفتح: ${formatEgp(commission.potentialPayoutEgp)} جنيه",
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = SecondaryTextColor,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EligibleCommissionCard(
    commission: FulfillmentCommission,
    onOpenCommission: () -> Unit
) {
    var isOpened by rememberSaveable(commission.targetName) { mutableStateOf(false) }
    val animatedPayout = remember { Animatable(0f) }

    // Opening the commission counts the amount up from zero
    LaunchedEffect(isOpened, commission.payoutEgp) {
        if (isOpened) {
            animatedPayout.snapTo(0f)
            animatedPayout.animateTo(
                targetValue = commission.payoutEgp.toFloat(),
                animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
            )
        }
    }

    CommissionCard {
        CommissionStatusHeader(
            icon = Icons.Default.LockOpen,
            iconColor = SuccessColor,
            title = "مبروك! انت مستحق العمولة",
            subtitle = "حققت المستهدف واتفتحت العمولة"
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isOpened) {
            val shownPayout = if (animatedPayout.isRunning) {
                floor(animatedPayout.value.toDouble())
            } else {
                commission.payoutEgp
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "عمولتك",
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.zain_regular)),
                    color = SecondaryTextColor
                )
                Text(
                    text = "${formatEgp(shownPayout)} جنيه",
                    fontSize = 36.sp,
                    fontFamily = FontFamily(Font(R.font.zain_bold)),
                    color = SuccessColor
                )
            }
        } else {
            Button(
                onClick = {
                    isOpened = true
                    onOpenCommission()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessColor)
            ) {
                Text(
                    text = "افتح العمولة",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.zain_bold))
                )
            }
        }
    }
}

@Composable
private fun CommissionStatusHeader(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 18.sp,
                fontFamily = FontFamily(Font(R.font.zain_bold)),
                color = Color.Black
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = SecondaryTextColor
            )
        }
    }
}

@Composable
private fun EmptyCommission() {
    CommissionCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Payments,
                contentDescription = null,
                tint = SecondaryTextColor,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "مفيش مستهدف عمولة متحدد ليك الشهر ده",
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.zain_bold)),
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = "أول ما المستهدف يتحدد هيظهر هنا",
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = SecondaryTextColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CommissionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            content()
        }
    }
}

private fun formatEgp(amount: Double): String {
    val pattern = if (amount % 1.0 == 0.0) "%,.0f" else "%,.2f"
    return String.format(Locale.getDefault(), pattern, amount)
}

private val previewLockedCommission = FulfillmentCommission(
    targetName = "Fulfillment agents target 1",
    targetOrders = 75,
    achievedOrders = 55,
    achievementPercentage = 73.3,
    remainingOrders = 20,
    isEligible = false,
    payoutEgp = 0.0,
    potentialPayoutEgp = 1000.0
)

@Preview(showBackground = true, locale = "ar")
@Composable
fun LockedCommissionPreview() {
    MaterialTheme {
        CommissionContent(
            commission = previewLockedCommission,
            showTargetName = false,
            onOpenCommission = {}
        )
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
fun EligibleCommissionPreview() {
    MaterialTheme {
        CommissionContent(
            commission = previewLockedCommission.copy(
                achievedOrders = 92,
                achievementPercentage = 122.7,
                remainingOrders = 0,
                isEligible = true,
                payoutEgp = 1000.0
            ),
            showTargetName = false,
            onOpenCommission = {}
        )
    }
}
