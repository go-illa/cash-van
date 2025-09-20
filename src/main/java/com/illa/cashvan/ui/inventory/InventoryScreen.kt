package com.illa.cashvan.ui.inventory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.illa.cashvan.ui.common.CashVanHeader
import com.illa.cashvan.ui.inventory.ui_components.InventoryCard
import com.illa.cashvan.ui.inventory.ui_components.InventoryItem

@Composable
fun InventoryScreen(
    onAddOrderClick: () -> Unit = {},
) {
    val inventoryItems = listOf(
        InventoryItem(
            id = "1",
            name = "Coca Cola Classic 12pk",
            code = "CC001",
            totalQuantity = 24,
            availableQuantity = 18,
            soldQuantity = 6,
            progressPercentage = 75f
        ),
        InventoryItem(
            id = "2",
            name = "Coca Cola Classic 12pk",
            code = "CC001",
            totalQuantity = 24,
            availableQuantity = 18,
            soldQuantity = 6,
            progressPercentage = 75f
        )
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            CashVanHeader(
                userName = "azab"
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
            ) {
                items(inventoryItems.size) { index ->
                    InventoryCard(
                        item = inventoryItems[index],
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
fun InventoryScreenPreview() {
    InventoryScreen()
}