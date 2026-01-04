package com.illa.cashvan.ui.orders.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.R
import com.illa.cashvan.core.analytics.CashVanAnalyticsHelper
import org.koin.compose.koinInject

data class OrderProductItem(
    val name: String,
    val quantity: Int
)

data class OrderItem(
    val id: String,
    val orderNumber: String,
    val merchantName: String,
    val phoneNumber: String,
    val totalAmount: Double,
    val itemsCount: Int,
    val date: String,
    val products: List<OrderProductItem> = emptyList(),
    val status: String? = null,
    val orderType: String? = null
)



@Composable
fun OrderCardItem(
    order: OrderItem,
    modifier: Modifier = Modifier,
    onOrderClick: (OrderItem) -> Unit = {},
    onCancelClick: (OrderItem) -> Unit = {},
    onSubmitClick: (OrderItem) -> Unit = {},
    analyticsHelper: CashVanAnalyticsHelper = koinInject()
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = {
            analyticsHelper.logEvent("order_clicked")
            onOrderClick(order)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = order.merchantName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF212121),
                        textAlign = TextAlign.End
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dynamic status badge
                    val orderStatus = OrderStatus.fromApiValue(order.status)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(orderStatus.backgroundColor)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = orderStatus.icon,
                                tint = orderStatus.color,
                                contentDescription = ""
                            )
                            Text(
                                text = orderStatus.arabicLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily(Font(R.font.zain_regular)),
                                color = orderStatus.color
                            )

                        }
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${order.totalAmount} جنيه",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF4CAF50)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = order.date,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF757575)
                    )
                }
                }
            }

            if (order.products.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    order.products.forEach { product ->
                        OrderItemRow(
                            quantity = "${product.quantity} قطعة",
                            itemName = product.name
                        )
                    }
                }
            }

            // Show buttons only for pending presell orders
            if (order.status == "ongoing" && order.orderType == "pre_sell") {
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel button
                    OutlinedButton(
                        onClick = {
                            analyticsHelper.logEvent("order_cancel_clicked")
                            onCancelClick(order)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFDC3545)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFFDC3545)
                        )
                    ) {
                        Text(
                            text = "إلغاء الأوردر",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily(Font(R.font.zain_regular))
                        )
                    }

                    // Submit button
                    Button(
                        onClick = {
                            analyticsHelper.logEvent("order_submit_clicked")
                            onSubmitClick(order)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0D3773)
                        )
                    ) {
                        Text(
                            text = "تسليم الاوردر",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily(Font(R.font.zain_regular))
                        )
                    }
                }
            }
        }
    }

@Composable
fun OrderItemRow(
    quantity: String,
    itemName: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .background(color = Color(0x80F1F5F9), shape = RoundedCornerShape(size = 4.dp))
        ,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Icon(
                imageVector = Icons.Default.CardGiftcard,
                tint = Color(0xFF1F252E),
                contentDescription = ""
            )
            Text(
                text = itemName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color(0xFF1F252E)
            )
        }
        Text(
            text = quantity,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily(Font(R.font.zain_regular)),
            color = Color(0xFF0D3773)
        )
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
fun OrderCardItemFulfilledPreview() {
    OrderCardItem(
        order = OrderItem(
            id = "1",
            orderNumber = "1001",
            merchantName = "Corner Store Market",
            phoneNumber = "+20 123 456 7890",
            totalAmount = 147.5,
            itemsCount = 5,
            date = "08:33 AM",
            status = "fulfilled"
        )
    )
}

@Preview(showBackground = true, locale = "ar")
@Composable
fun OrderCardItemOngoingPreview() {
    OrderCardItem(
        order = OrderItem(
            id = "2",
            orderNumber = "1002",
            merchantName = "سامي محمود",
            phoneNumber = "+20 987 654 3210",
            totalAmount = 875.00,
            itemsCount = 3,
            date = "2024-01-16",
            status = "ongoing",
            orderType = "pre_sell"
        )
    )
}

@Preview(showBackground = true, locale = "ar")
@Composable
fun OrderCardItemCanceledPreview() {
    OrderCardItem(
        order = OrderItem(
            id = "3",
            orderNumber = "1003",
            merchantName = "محمد علي",
            phoneNumber = "+20 111 222 3333",
            totalAmount = 250.00,
            itemsCount = 2,
            date = "10:15 AM",
            status = "canceled"
        )
    )
}

@Preview(showBackground = true, locale = "ar")
@Composable
fun OrderCardItemPartiallyFulfilledPreview() {
    OrderCardItem(
        order = OrderItem(
            id = "4",
            orderNumber = "1004",
            merchantName = "Ahmed Store",
            phoneNumber = "+20 555 666 7777",
            totalAmount = 500.00,
            itemsCount = 4,
            date = "11:45 AM",
            status = "partially_fulfilled"
        )
    )
}