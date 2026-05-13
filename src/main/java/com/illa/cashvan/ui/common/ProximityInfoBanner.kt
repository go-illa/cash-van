package com.illa.cashvan.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.R

enum class ProximityBannerVariant { INITIAL_HINT, EMPTY_RESULTS }

@Composable
fun ProximityInfoBanner(
    variant: ProximityBannerVariant,
    modifier: Modifier = Modifier
) {
    val message = when (variant) {
        ProximityBannerVariant.INITIAL_HINT ->
            stringResource(R.string.merchant_proximity_initial_hint)
        ProximityBannerVariant.EMPTY_RESULTS ->
            stringResource(R.string.merchant_proximity_empty_results)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF0D3773).copy(alpha = 0.30f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = Color(0xFF0D3773),
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Text(
            text = message,
            fontSize = 14.sp,
            fontFamily = FontFamily(Font(R.font.zain_regular)),
            color = Color(0xFF374151),
            lineHeight = 20.sp
        )
    }
}

@Preview(showBackground = true, locale = "ar", backgroundColor = 0xFFF3F4F6)
@Composable
private fun ProximityInfoBannerInitialPreview() {
    ProximityInfoBanner(
        variant = ProximityBannerVariant.INITIAL_HINT,
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(showBackground = true, locale = "ar", backgroundColor = 0xFFF3F4F6)
@Composable
private fun ProximityInfoBannerEmptyPreview() {
    ProximityInfoBanner(
        variant = ProximityBannerVariant.EMPTY_RESULTS,
        modifier = Modifier.padding(16.dp)
    )
}
