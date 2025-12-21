package com.illa.cashvan.ui.orders.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.R
import com.illa.cashvan.feature.orders.data.model.PlanProduct

@Composable
fun SelectedProductsList(
    modifier: Modifier = Modifier,
    selectedProducts: Map<String, Int>,
    products: List<PlanProduct>,
    onQuantityChange: (String, Int) -> Unit,
    onRemoveProduct: (String) -> Unit
) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "المنتجات المضافة",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily(Font(R.font.zain_regular)),
            color = Color(0xFF111827),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        selectedProducts.forEach { (planProductId, quantity) ->
            val product = products.find { it.id == planProductId }
            product?.let {
                SelectedProductItem(
                    product = it,
                    quantity = quantity,
                    onQuantityChange = { newQuantity ->
                        onQuantityChange(planProductId, newQuantity)
                    },
                    onRemove = { onRemoveProduct(planProductId) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SelectedProductItem(
    product: PlanProduct,
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    var quantityText by remember(quantity) { mutableStateOf(quantity.toString()) }
    var showQuantityError by remember { mutableStateOf(false) }
    val totalPrice = (product.product_price.toDoubleOrNull() ?: 0.0) * quantity

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FAFB), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.product.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily(Font(R.font.zain_regular)),
                    color = Color(0xFF111827)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.product.frontdoor_code,
                    fontSize = 12.sp,
                    fontFamily = FontFamily(Font(R.font.zain_regular)),
                    color = Color(0xFF6B7280)
                )
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quantity Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp))
                        .clickable(enabled = quantity > 1) {
                            if (quantity > 1) {
                                val newQuantity = quantity - 1
                                quantityText = newQuantity.toString()
                                onQuantityChange(newQuantity)
                                showQuantityError = false
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "-",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = if (quantity > 1) Color(0xFF111827) else Color(0xFF9CA3AF)
                    )
                }

                Box {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { newText ->
                            quantityText = newText
                            val newQuantity = newText.toIntOrNull()
                            if (newQuantity != null && newQuantity > 0) {
                                if (newQuantity <= product.available_quantity) {
                                    onQuantityChange(newQuantity)
                                    showQuantityError = false
                                } else {
                                    showQuantityError = true
                                }
                            } else {
                                showQuantityError = false
                            }
                        },
                        modifier = Modifier
                            .width(70.dp)
                            .height(48.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily(Font(R.font.zain_regular)),
                            color = Color(0xFF111827),
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = showQuantityError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0D3773),
                            unfocusedBorderColor = Color(0xFFD1D5DB),
                            errorBorderColor = Color(0xFFDC2626),
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF111827),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    if (showQuantityError) {
                        Text(
                            text = "تم الوصول لحد الاقصي",
                            fontSize = 9.sp,
                            fontFamily = FontFamily(Font(R.font.zain_regular)),
                            color = Color(0xFFDC2626),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = (-14).dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp))
                        .clickable(enabled = quantity < product.available_quantity) {
                            if (quantity < product.available_quantity) {
                                val newQuantity = quantity + 1
                                quantityText = newQuantity.toString()
                                onQuantityChange(newQuantity)
                                showQuantityError = false
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = if (quantity < product.available_quantity) Color(0xFF111827) else Color(0xFF9CA3AF)
                    )
                }
            }

            // Total Price
            Text(
                text = "${String.format("%.2f", totalPrice)} جنيه",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color(0xFF0D3773)
            )
        }
    }
}