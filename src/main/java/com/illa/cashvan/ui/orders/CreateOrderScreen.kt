package com.illa.cashvan.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.illa.cashvan.ui.orders.ui_components.AddMerchantBottomSheet
import com.illa.cashvan.ui.orders.ui_components.AddProductComponent
import com.illa.cashvan.ui.orders.ui_components.ChooseMerchantComponent
import com.illa.cashvan.ui.orders.ui_components.Merchant
import com.illa.cashvan.ui.orders.ui_components.OrderElement
import com.illa.cashvan.ui.orders.ui_components.OrderElementsList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    onBackClick: () -> Unit = {},
    onCreateOrder: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var selectedMerchant by remember { mutableStateOf<Merchant?>(null) }
    val orderElements = remember { mutableStateListOf<OrderElement>() }
    var totalAmount by remember { mutableDoubleStateOf(0.0) }
    var showAddMerchantBottomSheet by remember { mutableStateOf(false) }

    val sampleMerchants = MockData.merchants
    val sampleProducts = MockData.products

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = "إنشاء طلب جديد",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily(Font(R.font.zain_regular)),
                            color = Color.White,
                            textAlign = TextAlign.Start
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "العودة",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D3773)
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إجمالي الطلب",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF6B7280)
                    )

                    Text(
                        text = "${String.format("%.2f", totalAmount)} جنيه",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF0D3773)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onCreateOrder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D3773),
                        disabledContainerColor = Color(0xFFE5E7EB)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = selectedMerchant != null && orderElements.isNotEmpty()
                ) {
                    Text(
                        text = "تأكيد الطلب",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ChooseMerchantComponent(
                selectedMerchant = selectedMerchant,
                merchants = sampleMerchants,
                onMerchantSelected = { merchant ->
                    selectedMerchant = merchant
                },
                onAddMerchantClick = {
                    showAddMerchantBottomSheet = true
                }
            )

            AddProductComponent(
                products = sampleProducts,
                onAddToOrder = { product, quantity ->
                    val existingElementIndex = orderElements.indexOfFirst { it.id == product.id }
                    if (existingElementIndex != -1) {
                        val existingElement = orderElements[existingElementIndex]
                        orderElements[existingElementIndex] = existingElement.copy(
                            quantity = existingElement.quantity + quantity,
                            price = (existingElement.quantity + quantity) * product.price
                        )
                    } else {
                        orderElements.add(
                            OrderElement(
                                id = product.id,
                                name = product.name,
                                description = "${product.price} جنيه/${product.unit}",
                                price = product.price * quantity,
                                quantity = quantity,
                                unit = "جنيه"
                            )
                        )
                    }
                    totalAmount = orderElements.sumOf { it.price }
                }
            )

            if (orderElements.isNotEmpty()) {
                OrderElementsList(
                    orderElements = orderElements,
                    onQuantityIncrease = { element ->
                        val index = orderElements.indexOfFirst { it.id == element.id }
                        if (index != -1) {
                            val unitPrice = element.price / element.quantity
                            orderElements[index] = element.copy(
                                quantity = element.quantity + 1,
                                price = (element.quantity + 1) * unitPrice
                            )
                            totalAmount = orderElements.sumOf { it.price }
                        }
                    },
                    onQuantityDecrease = { element ->
                        val index = orderElements.indexOfFirst { it.id == element.id }
                        if (index != -1 && element.quantity > 1) {
                            val unitPrice = element.price / element.quantity
                            orderElements[index] = element.copy(
                                quantity = element.quantity - 1,
                                price = (element.quantity - 1) * unitPrice
                            )
                            totalAmount = orderElements.sumOf { it.price }
                        } else if (index != -1 && element.quantity == 1) {
                            orderElements.removeAt(index)
                            totalAmount = orderElements.sumOf { it.price }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Bottom Sheet Overlay
    if (showAddMerchantBottomSheet) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80000000))
                .padding(bottom = 0.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AddMerchantBottomSheet(
                onDismiss = { showAddMerchantBottomSheet = false },
                onMerchantCreated = {
                    showAddMerchantBottomSheet = false
                }
            )
        }
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
fun CreateOrderScreenPreview() {
    MaterialTheme {
        CreateOrderScreen()
    }
}