package com.illa.cashvan.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.illa.cashvan.ui.common.CashVanHeader
import com.illa.cashvan.ui.orders.ui_components.MerchantDetailsComponent
import com.illa.cashvan.ui.orders.ui_components.OrderSpecsComponentCompact
import com.illa.cashvan.ui.orders.ui_components.PaymentSummaryCard
import com.illa.cashvan.ui.orders.ui_components.ProductsDetailsComponent
import com.illa.cashvan.feature.orders.data.model.Order
import com.illa.cashvan.feature.orders.presentation.mapper.*
import com.illa.cashvan.feature.orders.presentation.viewmodel.OrderViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun OrderDetailsScreen(
    modifier: Modifier = Modifier,
    orderId: String,
    onBackClick: () -> Unit = {},
    onConfirmOrder: () -> Unit = {},
    orderViewModel: OrderViewModel = koinViewModel()
) {
    val orderDetailsState by orderViewModel.orderDetailsUiState.collectAsState()

    LaunchedEffect(orderId) {
        orderViewModel.loadOrderById(orderId)
    }

    when {
        orderDetailsState.isLoading -> {
            LoadingContent(modifier = modifier)
        }
        orderDetailsState.error != null -> {
            ErrorContent(
                modifier = modifier,
                error = orderDetailsState.error ?: "حدث خطأ غير متوقع"
            )
        }
        orderDetailsState.order != null -> {
            OrderDetailsContent(
                modifier = modifier,
                order = orderDetailsState.order!!,
                onBackClick = onBackClick,
                onConfirmOrder = onConfirmOrder
            )
        }
        else -> {
            ErrorContent(
                modifier = modifier,
                error = "لم يتم العثور على الطلب"
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = Color(0xFF0D3773)
            )
            Text(
                text = "جاري تحميل تفاصيل الطلب...",
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color(0xFF1F252E),
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun ErrorContent(modifier: Modifier = Modifier, error: String) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "خطأ في تحميل تفاصيل الطلب",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color(0xFFDC2626)
            )
            Text(
                text = error,
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun OrderDetailsContent(
    modifier: Modifier = Modifier,
    order: Order,
    onBackClick: () -> Unit,
    onConfirmOrder: () -> Unit
) {
    val orderSpecs = order.toOrderSpecs()
    val merchant = order.toUIMerchant()
    val paymentSummary = order.toPaymentSummary()
    val productDetailsList = order.toProductDetailsList()

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

            OrderSpecsComponentCompact(
                orderSpecs = orderSpecs
            )

            Spacer(modifier = Modifier.height(4.dp))

            SectionTitle(title = "معلومات التاجر")

            MerchantDetailsComponent(
                merchant = merchant
            )

            Spacer(modifier = Modifier.height(4.dp))

            SectionTitle(title = "عناصر الطلب")

            productDetailsList.forEach { product ->
                ProductsDetailsComponent(
                    productDetails = product
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            PaymentSummaryCard(
                paymentSummary = paymentSummary
            )

            Spacer(modifier = Modifier.height(80.dp)) // Space for button
        }

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
fun OrderDetailsContentPreview() {
    val sampleOrder = Order(
        id = 4,
        created_at = "2025-09-28T21:52:48.528Z",
        updated_at = "2025-09-28T21:52:48.528Z",
        plan_id = 1,
        formatted_code = "ORD-000000004",
        creator_id = 1,
        creator_type = "SalesAgent",
        total_sold_quantity = 3,
        total_income = "149.97",
        order_plan_products = listOf(
            com.illa.cashvan.feature.orders.data.model.OrderPlanProduct(
                id = 4,
                created_at = "2025-09-28T21:52:48.533Z",
                updated_at = "2025-09-28T21:52:48.533Z",
                sold_quantity = 3,
                plan_product_id = 2,
                order_id = 4,
                total_income = "149.97",
                product = com.illa.cashvan.feature.orders.data.model.OrderProduct(
                    id = 2,
                    created_at = "2025-09-28T21:52:47.890Z",
                    updated_at = "2025-09-28T21:52:47.890Z",
                    sku_code = "PROD002",
                    fd_sku_code = "FD002",
                    price = "49.99",
                    name = "Bluetooth Speaker",
                    description = "Portable Bluetooth speaker with waterproof design and superior sound quality."
                )
            )
        ),
        merchant = com.illa.cashvan.feature.orders.data.model.Merchant(
            id = 2,
            created_at = "2025-09-28T21:52:48.202Z",
            updated_at = "2025-09-28T21:52:48.202Z",
            name = "Electronics Hub",
            address = "456 Pyramids Road, Giza",
            google_link = null,
            phone_number = "+201555000002",
            latitude = null,
            longitude = null,
            governorate_id = 2,
            creator_id = 1,
            creator_type = "Supervisor",
            plan_id = 1
        )
    )

    MaterialTheme {
        OrderDetailsContent(
            order = sampleOrder,
            onBackClick = { /* Handle back */ },
            onConfirmOrder = { /* Handle confirm */ }
        )
    }
}