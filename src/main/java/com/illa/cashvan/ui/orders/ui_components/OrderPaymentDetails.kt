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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.R

data class PaymentSummary(
    val subtotal: Double,
    val taxAmount: Double,
    val taxPercentage: Double,
    val discountAmount: Double,
    val cashDiscountAmount: Double,
    val total: Double
)

@Composable
fun PaymentSummaryCard(
    modifier: Modifier = Modifier,
    paymentSummary: PaymentSummary
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Text(
                text = "ملخص الطلب",
                fontSize = 18.sp,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color(0xFF1F252E),
                modifier = Modifier.align(Alignment.Start)
            )

            // Summary Items
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Subtotal
                SummaryRow(
                    label = "المجموع الفرعي",
                    value = "${String.format("%.2f", paymentSummary.subtotal)} جنيه"
                )

                // Tax
                if (paymentSummary.taxAmount > 0) {
                    val taxLabel = if (paymentSummary.taxPercentage > 0) {
                        "الضريبة (${paymentSummary.taxPercentage.toInt()}%)"
                    } else {
                        "الضريبة"
                    }
                    SummaryRow(
                        label = taxLabel,
                        value = "${String.format("%.2f", paymentSummary.taxAmount)} جنيه"
                    )
                }

                // Discount (not including cash discount)
                if (paymentSummary.discountAmount > 0) {
                    SummaryRow(
                        label = "إجمالي الخصومات",
                        value = "${String.format("%.2f", paymentSummary.discountAmount)} جنيه"
                    )
                }

                // Cash Discount
                if (paymentSummary.cashDiscountAmount > 0) {
                    SummaryRow(
                        label = "الخصم النقدي",
                        value = "${String.format("%.2f", paymentSummary.cashDiscountAmount)} جنيه"
                    )
                }
            }

            // Divider
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
                    fontFamily = FontFamily(Font(R.font.zain_regular)),
                    color = Color(0xFF1F252E)
                )

                Text(
                    text = "${String.format("%.2f", paymentSummary.total)} جنيه",
                    fontSize = 20.sp,
                    fontFamily = FontFamily(Font(R.font.zain_regular)),
                    color = Color(0xFF1F252E)
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontFamily = FontFamily(Font(R.font.zain_regular)),
            color = Color(0xFF1F252E)
        )

        Text(
            text = value,
            fontSize = 14.sp,
            fontFamily = FontFamily(Font(R.font.zain_regular)),
            color = Color(0xFF1F252E)
        )
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
fun PaymentSummaryCardPreview() {
    val samplePaymentSummary = PaymentSummary(
        subtotal = 762.00,
        taxAmount = 76.20,
        taxPercentage = 10.0,
        discountAmount = 76.20,
        cashDiscountAmount = 0.0,
        total = 838.20
    )

    MaterialTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .background(Color(0xFFF5F5F5)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PaymentSummaryCard(
                paymentSummary = samplePaymentSummary
            )
        }
    }
}