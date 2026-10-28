package com.illa.cashvan.sales.ui.route

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PhoneInTalk
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.sales.domain.model.RouteMerchant
import com.illa.cashvan.sales.domain.model.VisitProgressStatus
import com.illa.cashvan.sales.domain.model.visitProgress
import com.illa.cashvan.sales.domain.route.RecentRemoteOrder
import com.illa.cashvan.sales.domain.route.RemoteOrderChannel
import com.illa.cashvan.sales.domain.route.formatDistance
import com.illa.cashvan.sales.domain.route.getRemoteOrderDayBucket
import com.illa.cashvan.sales.ui.components.SalesColors
import com.illa.cashvan.sales.ui.components.ZainBold
import com.illa.cashvan.sales.ui.components.ZainRegular

/**
 * Completed merchants are tinted green and faded, in-progress ones get an amber edge, unvisited ones stay
 * neutral, as in the PWA.
 */
@Composable
fun RouteMerchantCard(
    merchant: RouteMerchant,
    distanceMeters: Double?,
    remoteOrder: RecentRemoteOrder?,
    isCheckingLocation: Boolean,
    onClick: () -> Unit
) {
    val progress = visitProgress(merchant.visitCount)
    val (accent, container) = when (progress.status) {
        VisitProgressStatus.COMPLETED -> SalesColors.Success to SalesColors.Success.copy(alpha = 0.05f)
        VisitProgressStatus.IN_PROGRESS -> SalesColors.Progress to Color.White
        VisitProgressStatus.UNVISITED -> SalesColors.Border to Color.White
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (progress.status == VisitProgressStatus.COMPLETED) 0.85f else 1f)
            .clickable(enabled = !isCheckingLocation, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(if (progress.status == VisitProgressStatus.IN_PROGRESS) 1.5.dp else 1.dp, accent),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusIcon(progress.status)
                    Text(
                        text = merchant.displayName,
                        fontSize = 15.sp,
                        fontFamily = ZainBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    FawryBadge(merchant.fawryDeviceNumber, merchant.wantsFawryDevice)
                    RemoteOrderBadge(remoteOrder)
                }
                Row(
                    modifier = Modifier.padding(start = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = merchant.merchantCode,
                        fontSize = 11.sp,
                        color = SalesColors.Muted,
                        modifier = Modifier
                            .background(SalesColors.Background, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    if (merchant.routeName.isNotEmpty()) {
                        Text("•", color = SalesColors.Border, fontSize = 11.sp)
                        Text(merchant.routeName, fontSize = 12.sp, color = SalesColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(
                    modifier = Modifier.padding(start = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Pill(
                        text = if (merchant.visitCount == 0) "لم يُزور" else "${progress.count}/${progress.target} زيارات",
                        color = if (progress.status == VisitProgressStatus.UNVISITED) SalesColors.Muted else accent
                    )
                    formatLastVisit(merchant.lastVisitDate)?.let {
                        Text(it, fontSize = 12.sp, color = SalesColors.Muted, fontFamily = ZainRegular)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (distanceMeters != null) {
                    Text(
                        text = formatDistance(distanceMeters),
                        fontSize = 11.sp,
                        color = SalesColors.Muted,
                        modifier = Modifier
                            .background(SalesColors.Background, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (isCheckingLocation) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = SalesColors.Muted)
                } else {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = SalesColors.Muted)
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(status: VisitProgressStatus) {
    val (icon, tint) = when (status) {
        VisitProgressStatus.COMPLETED -> Icons.Outlined.CheckCircle to SalesColors.Success
        VisitProgressStatus.IN_PROGRESS -> Icons.Outlined.Schedule to SalesColors.Progress
        VisitProgressStatus.UNVISITED -> Icons.Outlined.RadioButtonUnchecked to SalesColors.Muted
    }
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontFamily = ZainBold,
        color = color,
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/**
 * Has a Fawry device: primary; agreed to get one: grey; declined: red; never asked: nothing (DIS-520).
 */
@Composable
fun FawryBadge(deviceNumber: String?, wantsDevice: Boolean?) {
    val hasDevice = deviceNumber != null
    if (!hasDevice && wantsDevice == null) return
    val color = when {
        hasDevice -> SalesColors.Primary
        wantsDevice == false -> SalesColors.Danger
        else -> SalesColors.Muted
    }
    Box(
        modifier = Modifier
            .size(24.dp)
            .border(1.5.dp, color, CircleShape)
            .background(color.copy(alpha = 0.1f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("ف", fontSize = 11.sp, fontFamily = ZainBold, color = color)
    }
}

/** A gold pill for a live telesales or retail-app order in the last 3 calendar days (DIS-991). */
@Composable
fun RemoteOrderBadge(order: RecentRemoteOrder?) {
    val bucket = getRemoteOrderDayBucket(order?.at) ?: return
    val day = when (bucket) {
        0 -> "اليوم"
        1 -> "أمس"
        2 -> "منذ يومين"
        else -> "منذ 3 أيام"
    }
    val (icon, label) = when (order?.channel) {
        RemoteOrderChannel.RETAIL -> Icons.Outlined.Smartphone to "طلب من التطبيق"
        else -> Icons.Outlined.PhoneInTalk to "طلب هاتفي"
    }
    Row(
        modifier = Modifier
            .background(SalesColors.Progress.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = SalesColors.Progress, modifier = Modifier.size(12.dp))
        Text("$label: $day", fontSize = 11.sp, fontFamily = ZainBold, color = SalesColors.Progress, maxLines = 1)
    }
}
