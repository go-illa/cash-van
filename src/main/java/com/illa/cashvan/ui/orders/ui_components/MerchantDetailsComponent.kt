package com.illa.cashvan.ui.orders.ui_components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.R
import com.illa.cashvan.core.analytics.CashVanAnalyticsHelper
import com.illa.cashvan.data.MockData
import org.koin.compose.koinInject

@Composable
fun MerchantDetailsComponent(
    modifier: Modifier = Modifier,
    merchant: Merchant,
    showLocation: Boolean = true,
    latitude: String? = null,
    longitude: String? = null,
    analyticsHelper: CashVanAnalyticsHelper = koinInject()
) {
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "تفاصيل التاجر",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color(0xFF1F252E)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = merchant.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF1F252E)
                    )

                    if (merchant.code.isNotEmpty()) {
                        MerchantDetailItem(
                            icon = Icons.Default.Badge,
                            text = merchant.code,
                            iconTint = Color(0xFF6B7280)
                        )
                    }

                    val hasCoordinates = !latitude.isNullOrEmpty() && !longitude.isNullOrEmpty()
                    val hasAddress = merchant.address.isNotEmpty()

                    if (showLocation && (hasAddress || hasCoordinates)) {
                        val displayText = if (hasAddress) {
                            merchant.address
                        } else {
                            "عرض الموقع على الخريطة"
                        }

                        MerchantDetailItem(
                            icon = Icons.Default.LocationOn,
                            text = displayText,
                            iconTint = Color(0xFF10B981),
                            onClick = {
                                analyticsHelper.logEvent(
                                    "open_merchant_location",
                                    mapOf(
                                        "merchant_name" to merchant.name,
                                        "latitude" to (latitude ?: ""),
                                        "longitude" to (longitude ?: "")
                                    )
                                )
                                val uri = if (hasCoordinates) {
                                    // Use coordinates if available
                                    Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(merchant.name)})")
                                } else {
                                    // Fallback to address search
                                    Uri.parse("geo:0,0?q=${Uri.encode(merchant.address)}")
                                }
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                intent.setPackage("com.google.android.apps.maps")
                                if (intent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(intent)
                                } else {
                                    // Fallback to browser if Google Maps is not installed
                                    val browserUri = if (hasCoordinates) {
                                        Uri.parse("https://www.google.com/maps?q=$latitude,$longitude")
                                    } else {
                                        Uri.parse("https://www.google.com/maps/search/${Uri.encode(merchant.address)}")
                                    }
                                    context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                                }
                            }
                        )
                    }

                    MerchantDetailItem(
                        icon = Icons.Default.Phone,
                        text = merchant.phoneNumber,
                        iconTint = Color(0xFF0D3773),
                        onClick = {
                            analyticsHelper.logEvent(
                                "contact_merchnant",
                                mapOf("merchant_phone" to merchant.phoneNumber)
                            )
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun MerchantDetailItem(
    icon: ImageVector,
    text: String,
    iconTint: Color = Color(0xFF6B7280),
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp)
        )

        Text(
            text = text,
            fontSize = 14.sp,
            fontFamily = FontFamily(Font(R.font.zain_regular)),
            color = Color(0xFF6B7280)
        )
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
fun MerchantDetailsComponentPreview() {
    val sampleMerchant = MockData.getRandomMerchant()

    MaterialTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .background(Color(0xFFF8F9FA)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MerchantDetailsComponent(
                merchant = sampleMerchant
            )
        }
    }
}