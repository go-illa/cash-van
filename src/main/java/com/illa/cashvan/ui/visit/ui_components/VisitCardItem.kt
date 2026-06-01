package com.illa.cashvan.ui.visit.ui_components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.R
import com.illa.cashvan.feature.visit.data.model.VisitItem
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun VisitCardItem(
    visit: VisitItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val zain = FontFamily(Font(R.font.zain_regular))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = visit.merchant?.name ?: "تاجر غير محدد",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = zain,
                    color = Color(0xFF1F252E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatVisitDate(visit.visit_date),
                    fontSize = 13.sp,
                    fontFamily = zain,
                    color = Color(0xFF757575)
                )
            }

            if (!visit.location.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = visit.location,
                    fontSize = 13.sp,
                    fontFamily = zain,
                    color = Color(0xFF757575),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(8.dp))

            val reason = visit.no_order_reason?.reason_ar ?: "—"
            Text(
                text = reason,
                fontSize = 13.sp,
                fontFamily = zain,
                color = Color(0xFF0D3773),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatVisitDate(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return "—"
    return try {
        val inputFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
        val outputFmt = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        val date = inputFmt.parse(dateStr)
        outputFmt.format(date ?: return dateStr)
    } catch (e: Exception) {
        try { dateStr.substring(5, 16).replace("T", " ") } catch (_: Exception) { dateStr }
    }
}
