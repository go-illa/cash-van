package com.illa.cashvan.ui.orders.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.R

data class EditableOrderItem(
    val planProductId: String,
    val productName: String,
    val sku: String,
    val quantity: Int,
    val maxQuantity: Int,
    val basePrice: Double,
    val finalPrice: Double,
    val discountAmount: Double,
    val vatAmount: Double,
    val vatPercentage: Double = 0.0,
    val totalPrice: Double,
    val isLoadingPrice: Boolean = false
)

@Composable
fun EditableOrderItemCard(
    item: EditableOrderItem,
    onQuantityChange: (String, Int) -> Unit,
    onRemoveItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
            // Header: Product name, SKU and Quantity Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Product Info
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.productName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF1F252E),
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "رمز التخزين: ${item.sku}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF9CA3AF),
                        textAlign = TextAlign.End
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))

                // Quantity Input
                QuantityInput(
                    quantity = item.quantity,
                    maxQuantity = item.maxQuantity,
                    onQuantityChange = { newQty ->
                        onQuantityChange(item.planProductId, newQty)
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFE5E7EB))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Price Details
            if (item.isLoadingPrice) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
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
            } else {
                PriceDetailsSection(
                    basePrice = item.basePrice,
                    discountAmount = item.discountAmount,
                    vatAmount = item.vatAmount,
                    vatPercentage = item.vatPercentage,
                    totalPrice = item.totalPrice
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Remove Button (aligned to end)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = Color(0xFFEF4444),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .height(36.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "إزالة",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "إزالة",
                            fontSize = 14.sp,
                            fontFamily = FontFamily(Font(R.font.zain_regular)),
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }

    // Show confirmation dialog
    if (showDeleteDialog) {
        DeleteProductConfirmationDialog(
            productName = item.productName,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                onRemoveItem(item.planProductId)
            }
        )
    }
}

@Composable
private fun QuantityInput(
    quantity: Int,
    maxQuantity: Int,
    onQuantityChange: (Int) -> Unit
) {
    var textValue by remember(quantity) { mutableStateOf(quantity.toString()) }
    var isError by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.Start
    ) {
        // Quantity Label
        Text(
            text = "الكمية",
            fontSize = 12.sp,
            fontFamily = FontFamily(Font(R.font.zain_regular)),
            color = Color(0xFF6B7280)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Quantity Input Field
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(44.dp)
                .border(
                    width = 1.5.dp,
                    color = if (isError) Color(0xFFEF4444) else Color(0xFF1E40AF),
                    shape = RoundedCornerShape(8.dp)
                )
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = textValue,
                onValueChange = { newValue ->
                    // Only allow numbers
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        // Parse and validate
                        val newQty = newValue.toIntOrNull()

                        // Check if exceeds maximum
                        if (newQty != null && newQty > maxQuantity) {
                            // Show error (red border) but don't update text
                            isError = true
                        } else if (newQty != null && newQty > 0) {
                            // Valid input (greater than 0) - update text and clear error
                            isError = false
                            textValue = newValue
                            onQuantityChange(newQty)
                        } else if (newValue.isEmpty()) {
                            // Allow clearing but don't call onChange with 0
                            isError = false
                            textValue = newValue
                        }
                        // If newQty is 0, don't update (ignore the input)
                    }
                },
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily(Font(R.font.zain_bold)),
                    color = Color(0xFF1E40AF),
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Max Quantity Hint
        Text(
            text = "الحد الأقصى: $maxQuantity",
            fontSize = 11.sp,
            fontFamily = FontFamily(Font(R.font.zain_regular)),
            color = Color(0xFF9CA3AF)
        )
    }
}

@Composable
private fun PriceDetailsSection(
    basePrice: Double,
    discountAmount: Double,
    vatAmount: Double,
    vatPercentage: Double,
    totalPrice: Double
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Base Price
        PriceRow(
            label = "السعر الأساسي",
            value = "${"%.2f".format(basePrice)} جنيه",
            isHighlight = false
        )

        // Discount
        if (discountAmount > 0) {
            PriceRow(
                label = "الخصم",
                value = "${"%.2f".format(discountAmount)} جنيه",
                valueColor = Color(0xFF10B981)
            )
        }

        // VAT Percentage
        if (vatPercentage > 0) {
            PriceRow(
                label = "نسبة الضريبة",
                value = "${vatPercentage.toInt()}%"
            )
        }

        // VAT Amount
        if (vatAmount > 0) {
            PriceRow(
                label = "ضريبة القيمة المضافة",
                value = "${"%.2f".format(vatAmount)} جنيه"
            )
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFE5E7EB))
        )

        // Total
        PriceRow(
            label = "الإجمالي",
            value = "${"%.2f".format(totalPrice)} جنيه",
            isHighlight = true
        )
    }
}

@Composable
private fun PriceRow(
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
            fontSize = if (isHighlight) 16.sp else 14.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily(Font(R.font.zain_regular)),
            color = Color(0xFF6B7280)
        )
        Text(
            text = value,
            fontSize = if (isHighlight) 16.sp else 14.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily(Font(R.font.zain_regular)),
            color = valueColor
        )
    }
}

@Composable
fun EditableOrderItemsList(
    items: List<EditableOrderItem>,
    onQuantityChange: (String, Int) -> Unit,
    onRemoveItem: (String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Items
        items.forEach { item ->
            EditableOrderItemCard(
                item = item,
                onQuantityChange = onQuantityChange,
                onRemoveItem = onRemoveItem
            )
        }
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
fun EditableOrderItemCardPreview() {
    val sampleItem = EditableOrderItem(
        planProductId = "1",
        productName = "علبة بيتي فور ممتاز 14 قطعة",
        sku = "4005032005",
        quantity = 20,
        maxQuantity = 50,
        basePrice = 61.6,
        finalPrice = 61.6,
        discountAmount = 0.0,
        vatAmount = 0.0,
        totalPrice = 1232.0,
        isLoadingPrice = false
    )

    MaterialTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .background(Color(0xFFF5F5F5))
        ) {
            EditableOrderItemCard(
                item = sampleItem,
                onQuantityChange = { _, _ -> },
                onRemoveItem = { }
            )
        }
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
fun EditableOrderItemWithDiscountPreview() {
    val sampleItem = EditableOrderItem(
        planProductId = "2",
        productName = "tote bag",
        sku = "T0000",
        quantity = 16,
        maxQuantity = 50,
        basePrice = 54.0,
        finalPrice = 49.25,
        discountAmount = 10.8,
        vatAmount = 6.05,
        totalPrice = 788.0,
        isLoadingPrice = false
    )

    MaterialTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .background(Color(0xFFF5F5F5))
        ) {
            EditableOrderItemCard(
                item = sampleItem,
                onQuantityChange = { _, _ -> },
                onRemoveItem = { }
            )
        }
    }
}
