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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.R
import com.illa.cashvan.feature.orders.data.model.PlanProduct

@Composable
fun ProductSelectionComponent(
    modifier: Modifier = Modifier,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    products: List<PlanProduct>,
    onProductSelected: (PlanProduct, Int) -> Unit,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    var selectedProduct by remember { mutableStateOf<PlanProduct?>(null) }
    var quantity by remember { mutableIntStateOf(1) }

    Column(modifier = modifier) {
        Text(
            text = "إضافة منتج",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily(Font(R.font.zain_regular)),
            color = Color(0xFF111827),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Product Searchable Dropdown
        SearchableDropdown(
            label = "المنتج",
            placeholder = "ابحث بكود المنتج أو الاسم",
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            items = products,
            selectedItem = selectedProduct,
            onItemSelected = {
                selectedProduct = it
            },
            itemText = { "${it.product.name} (${it.product.fd_sku_code})" },
            isLoading = isLoading,
            enabled = enabled,
            onExpanded = {
                if (searchQuery.isEmpty()) {
                    onSearchQueryChange("")
                }
            }
        )

        // Product Details & Quantity
        selectedProduct?.let { product ->
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "السعر:",
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = "${product.product_price} جنيه",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF111827)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "المتاح:",
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = "${product.available_quantity} وحدة",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = if (product.available_quantity > 0) Color(0xFF059669) else Color(0xFFDC2626)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quantity Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الكمية",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily(Font(R.font.zain_regular)),
                    color = Color(0xFF374151)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp))
                            .clickable(enabled = quantity > 1) {
                                if (quantity > 1) quantity--
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "-",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily(Font(R.font.zain_regular)),
                            color = if (quantity > 1) Color(0xFF111827) else Color(0xFF9CA3AF)
                        )
                    }

                    Text(
                        text = quantity.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF111827),
                        modifier = Modifier.width(40.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp))
                            .clickable(enabled = quantity < product.available_quantity) {
                                if (quantity < product.available_quantity) quantity++
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily(Font(R.font.zain_regular)),
                            color = if (quantity < product.available_quantity) Color(0xFF111827) else Color(0xFF9CA3AF)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add to Order Button
            Button(
                onClick = {
                    onProductSelected(product, quantity)
                    selectedProduct = null
                    quantity = 1
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0D3773)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = enabled && product.available_quantity > 0
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "إضافة",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "إضافة للطلب",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily(Font(R.font.zain_regular)),
                    color = Color.White
                )
            }
        }
    }
}