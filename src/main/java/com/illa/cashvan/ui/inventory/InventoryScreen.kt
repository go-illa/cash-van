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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.illa.cashvan.feature.plans.data.model.PlanProduct
import com.illa.cashvan.feature.plans.presentation.viewmodel.InventoryViewModel
import com.illa.cashvan.ui.common.CashVanHeader
import com.illa.cashvan.ui.inventory.ui_components.InventoryCard
import com.illa.cashvan.ui.inventory.ui_components.InventoryItem
import org.koin.androidx.compose.koinViewModel

fun PlanProduct.toInventoryItem(): InventoryItem {
    val soldPercentage = if (assigned_quantity > 0) {
        (sold_quantity.toFloat() / assigned_quantity.toFloat()) * 100f
    } else {
        0f
    }
    val availableQuantity = assigned_quantity - sold_quantity

    return InventoryItem(
        id = id.toString(),
        name = "Product $product_id",
        code = "P$product_id",
        totalQuantity = assigned_quantity,
        availableQuantity = availableQuantity,
        soldQuantity = sold_quantity,
        progressPercentage = soldPercentage
    )
}

@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel = koinViewModel (),
    onAddOrderClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            CashVanHeader()

            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF0D3773)
                        )
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Error: ${uiState.error}",
                                color = Color.Red,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            FloatingActionButton(
                                onClick = { viewModel.refresh() },
                                containerColor = Color(0xFF0D3773)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
                else -> {
                    val inventoryItems = uiState.planProducts.map { it.toInventoryItem() }

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

