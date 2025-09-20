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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.R

data class OrderElement(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val quantity: Int,
    val unit: String = "جنيه"
)

@Composable
fun OrderElementItem(
    modifier: Modifier = Modifier,
    orderElement: OrderElement,
    onQuantityIncrease: (OrderElement) -> Unit = {},
    onQuantityDecrease: (OrderElement) -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = orderElement.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color(0xFF1F252E)
            )

            Text(
                text = orderElement.description,
                fontSize = 12.sp,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color(0xFF6B7280)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = { onQuantityDecrease(orderElement) },
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = Color(0xFF1E40AF),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "تقليل الكمية",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }

            Text(
                text = orderElement.quantity.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color(0xFF1F252E),
                modifier = Modifier.width(24.dp),
                textAlign = TextAlign.Center
            )

            IconButton(
                onClick = { onQuantityIncrease(orderElement) },
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = Color(0xFF1E40AF),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "زيادة الكمية",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Text(
            text = "${String.format("%.2f", orderElement.price)} ${orderElement.unit}",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily(Font(R.font.zain_regular)),
            color = Color(0xFF1F252E),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun OrderElementsList(
    modifier: Modifier = Modifier,
    orderElements: List<OrderElement>,
    onQuantityIncrease: (OrderElement) -> Unit = {},
    onQuantityDecrease: (OrderElement) -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                .padding(16.dp)
        ) {
            Text(
                text = "عناصر الطلب",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color(0xFF1F252E),
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            orderElements.forEachIndexed { index, element ->
                OrderElementItem(
                    orderElement = element,
                    onQuantityIncrease = onQuantityIncrease,
                    onQuantityDecrease = onQuantityDecrease
                )

                if (index < orderElements.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFE5E7EB))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
fun OrderElementsListPreview() {
    val sampleElements = listOf(
        OrderElement(
            id = "1",
            name = "Coca Cola Classic 12pk",
            description = "كل كرتونة 8.99 جنيه • CC001",
            price = 62.93,
            quantity = 0
        ),
        OrderElement(
            id = "2",
            name = "Coca Cola Classic 12pk",
            description = "كل كرتونة 8.99 جنيه • CC001",
            price = 62.93,
            quantity = 0
        )
    )

    MaterialTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .background(Color(0xFFF8F9FA))
        ) {
            OrderElementsList(
                orderElements = sampleElements
            )
        }
    }
}