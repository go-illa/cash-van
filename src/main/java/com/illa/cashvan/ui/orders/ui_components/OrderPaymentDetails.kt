package com.illa.cashvan.ui.orders.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.R
import com.illa.cashvan.data.MockData

data class PaymentSummary(
    val total: Double
)

@Composable
fun PaymentSummaryCard(
    modifier: Modifier = Modifier,
    paymentSummary: PaymentSummary
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Text(
                text = "ملخص الطلب",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color(0xFF1F252E),
                modifier = Modifier.align(Alignment.Start)
            )
            HorizontalDivider(
                color = Color(0xFFE1E7EF),
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth()
            )
            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المجموع",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily(Font(R.font.zain_regular)),
                    color = Color(0xFF1F252E)
                )

                Text(
                    text = " ${String.format("%.2f", paymentSummary.total)} جنيه",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily(Font(R.font.zain_regular)),
                    color = Color(0xFF1F252E)
                )
            }
        }
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
fun PaymentSummaryCardPreview() {
    val samplePaymentSummary = MockData.getRandomPaymentSummary()

    MaterialTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .background(Color.White),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PaymentSummaryCard(
                paymentSummary = samplePaymentSummary
            )
        }
    }
}