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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
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
import com.illa.cashvan.feature.orders.presentation.viewmodel.ProductPriceInfo

@Composable
fun SelectedProductsList(
    modifier: Modifier = Modifier,
    selectedProducts: Map<String, Int>,
    products: List<PlanProduct>,
    onQuantityChange: (String, Int) -> Unit,
    onRemoveProduct: (String) -> Unit,
    productPrices: Map<String, ProductPriceInfo> = emptyMap(),
    loadingPriceForProducts: Set<String> = emptySet()
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
                    priceInfo = productPrices[planProductId],
                    isLoadingPrice = planProductId in loadingPriceForProducts,
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
    priceInfo: ProductPriceInfo?,
    isLoadingPrice: Boolean,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    var quantityText by remember(quantity) { mutableStateOf(quantity.toString()) }
    var showQuantityError by remember { mutableStateOf(false) }

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
                                if (newQuantity <= product.calculatedAvailableQuantity) {
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
                        .clickable(enabled = quantity < product.calculatedAvailableQuantity) {
                            if (quantity < product.calculatedAvailableQuantity) {
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
                        color = if (quantity < product.calculatedAvailableQuantity) Color(0xFF111827) else Color(0xFF9CA3AF)
                    )
                }
            }

            val displayTotal = priceInfo?.totalPrice
                ?: (product.product_price.toDoubleOrNull()?.times(quantity) ?: 0.0)
            Text(
                text = "${"%.2f".format(displayTotal)} جنيه",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color(0xFF0D3773)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFE5E7EB))
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (isLoadingPrice) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF1E40AF)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "جاري حساب السعر...",
                    fontSize = 12.sp,
                    fontFamily = FontFamily(Font(R.font.zain_regular)),
                    color = Color(0xFF6B7280)
                )
            }
        } else if (priceInfo != null) {
            PriceBreakdownSection(priceInfo = priceInfo)
        } else {
            PriceBreakdownRow(
                label = "السعر الأساسي",
                value = "${"%.2f".format(product.product_price.toDoubleOrNull() ?: 0.0)} جنيه"
            )
        }
    }
}

@Composable
private fun PriceBreakdownSection(priceInfo: ProductPriceInfo) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PriceBreakdownRow(
            label = "السعر الأساسي",
            value = "${"%.2f".format(priceInfo.basePrice)} جنيه"
        )

        if (priceInfo.discountAmount > 0) {
            PriceBreakdownRow(
                label = "الخصم",
                value = "${"%.2f".format(priceInfo.discountAmount)} جنيه",
                valueColor = Color(0xFF10B981)
            )
        }

        if (priceInfo.vatPercentage > 0) {
            PriceBreakdownRow(
                label = "نسبة الضريبة",
                value = "${priceInfo.vatPercentage.toInt()}%"
            )
        }

        if (priceInfo.vatAmount > 0) {
            PriceBreakdownRow(
                label = "ضريبة القيمة المضافة",
                value = "${"%.2f".format(priceInfo.vatAmount)} جنيه"
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFE5E7EB))
        )

        PriceBreakdownRow(
            label = "الإجمالي",
            value = "${"%.2f".format(priceInfo.totalPrice)} جنيه",
            isHighlight = true
        )
    }
}

@Composable
private fun PriceBreakdownRow(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    valueColor: Color = Color(0xFF1F252E)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = if (isHighlight) 15.sp else 13.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily(Font(R.font.zain_regular)),
            color = Color(0xFF6B7280)
        )
        Text(
            text = value,
            fontSize = if (isHighlight) 15.sp else 13.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily(Font(R.font.zain_regular)),
            color = if (isHighlight) Color(0xFF0D3773) else valueColor
        )
    }
}
