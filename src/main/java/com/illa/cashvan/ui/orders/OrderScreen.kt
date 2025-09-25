package com.illa.cashvan.ui.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import com.illa.cashvan.data.MockData
import com.illa.cashvan.ui.common.CashVanHeader
import com.illa.cashvan.ui.orders.ui_components.OrderCardItem
import com.illa.cashvan.ui.orders.ui_components.OrderItem

@Composable
fun OrderScreen(
    onAddOrderClick: () -> Unit = {},
    onOrderClick: (OrderItem) -> Unit = {}
) {
    val sampleOrders = MockData.orderItems

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            CashVanHeader()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "طلبات اليوم",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color(0xFF1F252E),
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sampleOrders) { order ->
                    OrderCardItem(
                        order = order,
                        onOrderClick = onOrderClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAddOrderClick,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .size(64.dp),
            containerColor = Color(0xFF0D3773),
            shape = CircleShape,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "إضافة طلب جديد",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
fun OrderScreenPreview() {
    OrderScreen()
}

@Preview(showBackground = true, locale = "ar")
@Composable
fun OrderScreenEmptyPreview() {
    OrderScreen()
}