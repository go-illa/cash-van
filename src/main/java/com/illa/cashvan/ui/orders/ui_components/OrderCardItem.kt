package com.illa.cashvan.ui.orders.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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

data class OrderItem(
    val id: String,
    val orderNumber: String,
    val merchantName: String,
    val phoneNumber: String,
    val totalAmount: Double,
    val itemsCount: Int,
    val date: String
)



@Composable
fun OrderCardItem(
    order: OrderItem,
    modifier: Modifier = Modifier,
    onOrderClick: (OrderItem) -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { onOrderClick(order) }
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

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x1A16A249))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckBox,
                                tint = Color(0xFF16A249),
                                contentDescription = ""
                            )
                            Text(
                                text = "اكتمل الطلب",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily(Font(R.font.zain_regular)),
                                color = Color(0xFF16A249)
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

            Column(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OrderItemRow(
                    quantity = "5 كرتونة",
                    itemName = "Coca Cola Classic 12pk"
                )

                OrderItemRow(
                    quantity = "3 كرتونة",
                    itemName = "Sprite 6pk Bottles"
                )

                OrderItemRow(
                    quantity = "5 كرتونة",
                    itemName = "Coca Cola Classic 12pk"
                )

                OrderItemRow(
                    quantity = "5 كرتونة",
                    itemName = "Coca Cola Classic 12pk"
                )

                OrderItemRow(
                    quantity = "3 كرتونة",
                    itemName = "Sprite 6pk Bottles"
                )
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
fun OrderCardItemPreview() {
    OrderCardItem(
        order = OrderItem(
            id = "1",
            orderNumber = "1001",
            merchantName = "Corner Store Market",
            phoneNumber = "+20 123 456 7890",
            totalAmount = 147.5,
            itemsCount = 5,
            date = "08:33 AM"
        )
    )
}

@Preview(showBackground = true, locale = "ar")
@Composable
fun OrderCardItemPendingPreview() {
    OrderCardItem(
        order = OrderItem(
            id = "2",
            orderNumber = "1002",
            merchantName = "سامي محمود",
            phoneNumber = "+20 987 654 3210",
            totalAmount = 875.00,
            itemsCount = 3,
            date = "2024-01-16"
        )
    )
}