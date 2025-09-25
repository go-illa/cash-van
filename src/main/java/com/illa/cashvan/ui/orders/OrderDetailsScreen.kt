package com.illa.cashvan.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.illa.cashvan.ui.common.CashVanHeader
import com.illa.cashvan.ui.orders.ui_components.MerchantDetailsComponent
import com.illa.cashvan.ui.orders.ui_components.OrderSpecsComponentCompact
import com.illa.cashvan.ui.orders.ui_components.PaymentSummaryCard
import com.illa.cashvan.ui.orders.ui_components.ProductDetails
import com.illa.cashvan.ui.orders.ui_components.ProductsDetailsComponent

@Composable
fun OrderDetailsScreen(
    modifier: Modifier = Modifier,
    orderId: String = "ORD-2024-001",
    onBackClick: () -> Unit = {},
    onConfirmOrder: () -> Unit = {},
) {
    // Get mock data for the order
    val orderData = MockData.getSampleCompleteOrder()
    val randomProduct = MockData.getRandomProduct()
    val productDetailsList = listOf(
        ProductDetails(
            productName = randomProduct.name,
            sku = "SKU-${randomProduct.id.padStart(3, '0')}",
            price = randomProduct.price * 3,
            unitPrice = randomProduct.price,
            quantity = 3
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        CashVanHeader()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SectionTitle(title = "تفاصيل الطلب")

            // Order Specs Card
            OrderSpecsComponentCompact(
                orderSpecs = orderData.orderSpecs.copy(orderNumber = orderId)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Section Title - Merchant Info
            SectionTitle(title = "معلومات التاجر")

            // Merchant Details (you'll need to create a compact version)
            MerchantDetailsComponent(
                merchant = orderData.merchant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Section Title - Order Items
            SectionTitle(title = "عناصر الطلب")

            // Product Details Cards
            productDetailsList.forEach { product ->
                ProductsDetailsComponent(
                    productDetails = product
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Payment Summary Card
            PaymentSummaryCard(
                paymentSummary = orderData.paymentSummary
            )

            Spacer(modifier = Modifier.height(80.dp)) // Space for button
        }

        // Bottom Button Container
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Button(
                onClick = onConfirmOrder,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0D3773)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "إطبع الفاتورة",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily(Font(R.font.zain_regular)),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily(Font(R.font.zain_regular)),
        color = Color(0xFF1F252E),
        modifier = modifier.padding(vertical = 4.dp)
    )
}



@Preview(showBackground = true, locale = "ar")
@Composable
fun OrderDetailsScreenPreview() {
    MaterialTheme {
        OrderDetailsScreen(
            orderId = "ORD-2024-003",
            onBackClick = { /* Handle back */ },
            onConfirmOrder = { /* Handle confirm */ }
        )
    }
}