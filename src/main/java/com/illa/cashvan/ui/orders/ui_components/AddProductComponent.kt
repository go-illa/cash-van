package com.illa.cashvan.ui.orders.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.R
import com.illa.cashvan.core.analytics.CashVanAnalyticsHelper
import com.illa.cashvan.data.MockData
import org.koin.compose.koinInject

data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val unit: String = "قطعة"
)

@Composable
fun AddProductComponent(
    modifier: Modifier = Modifier,
    products: List<Product> = emptyList(),
    onAddToOrder: (Product, Int) -> Unit = { _, _ -> },
    analyticsHelper: CashVanAnalyticsHelper = koinInject()
) {
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var quantity by remember { mutableIntStateOf(1) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val totalPrice = selectedProduct?.let { it.price * quantity } ?: 0.0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "إضافة منتج للطلب",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color(0xFF1F252E)
            )

            Column {
                Box {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(width = 1.dp, color = Color(0x2400335D), shape = RoundedCornerShape(size = 10.dp))
                        .clickable { isDropdownExpanded = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 0.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedProduct?.name ?: "اختر منتج",
                                fontSize = 16.sp,
                                fontFamily = FontFamily(Font(R.font.zain_regular)),
                                color = Color.Black
                            )

                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                    ) {
                        products.forEach { product ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = product.name,
                                            fontSize = 16.sp,
                                            fontFamily = FontFamily(Font(R.font.zain_regular)),
                                            color = Color(0xFF1F252E)
                                        )
                                        Text(
                                            text = "${product.price} جنيه/${product.unit}",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily(Font(R.font.zain_regular)),
                                            color = Color(0xFF6B7280)
                                        )
                                    }
                                },
                                onClick = {
                                    analyticsHelper.logEvent(
                                        "select_product",
                                        mapOf("product_name" to product.name)
                                    )
                                    selectedProduct = product
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (selectedProduct != null) {
                Column {
                    Text(
                        text = "الكمية",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { if (quantity > 1) quantity-- },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = Color(0xFFF3F4F6),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "تقليل الكمية",
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            OutlinedTextField(
                                value = quantity.toString(),
                                onValueChange = { newValue ->
                                    val newQuantity = newValue.toIntOrNull()
                                    if (newQuantity != null && newQuantity > 0) {
                                        quantity = newQuantity
                                    }
                                },
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(56.dp),
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily(Font(R.font.zain_regular)),
                                    color = Color(0xFF1F252E),
                                    textAlign = TextAlign.Center
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF0D3773),
                                    unfocusedBorderColor = Color(0xFFE5E7EB),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )

                            IconButton(
                                onClick = { quantity++ },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = Color(0xFFF3F4F6),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "زيادة الكمية",
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Text(
                            text = selectedProduct?.unit?:"",
                            fontSize = 14.sp,
                            fontFamily = FontFamily(Font(R.font.zain_regular)),
                            color = Color(0xFF6B7280)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إجمالي السعر",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF9CA3AF)
                    )

                    Text(
                        text = "${String.format("%.2f", totalPrice)} جنيه",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF9CA3AF)
                    )
                }

                Button(
                    onClick = {
                        selectedProduct?.let { product ->
                            analyticsHelper.logEvent(
                                "add_to_order",
                                mapOf("skus" to product.id)
                            )
                            onAddToOrder(product, quantity)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(width = 1.dp, color = Color(0xFF0D3773), shape = RoundedCornerShape(size = 6.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "+ إضافة للطلب",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF0D3773)
                    )
                }
            }
        }
        }
    }

@Preview(showBackground = true, locale = "ar")
@Composable
fun AddProductComponentPreview() {
    val sampleProducts = MockData.getRandomProducts()

    MaterialTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .background(Color(0xFFF8F9FA))
        ) {
            AddProductComponent(
                products = sampleProducts,
                onAddToOrder = { product, quantity ->
                    // Handle add to order
                }
            )
        }
    }
}