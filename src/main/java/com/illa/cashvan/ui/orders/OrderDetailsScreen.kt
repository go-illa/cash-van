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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.R
import com.illa.cashvan.feature.orders.data.model.Order
import com.illa.cashvan.feature.orders.presentation.mapper.toOrderSpecs
import com.illa.cashvan.feature.orders.presentation.mapper.toPaymentSummary
import com.illa.cashvan.feature.orders.presentation.mapper.toUIMerchant
import com.illa.cashvan.feature.orders.presentation.viewmodel.OrderViewModel
import com.illa.cashvan.feature.orders.presentation.viewmodel.ProductPriceInfo
import com.illa.cashvan.ui.common.CashVanHeader
import com.illa.cashvan.ui.common.ErrorSnackbar
import com.illa.cashvan.ui.common.SuccessSnackbar
import com.illa.cashvan.ui.orders.ui_components.CancelOrderBottomSheet
import com.illa.cashvan.ui.orders.ui_components.EditableOrderItem
import com.illa.cashvan.ui.orders.ui_components.EditableOrderItemCard
import com.illa.cashvan.ui.orders.ui_components.MerchantDetailsComponent
import com.illa.cashvan.ui.orders.ui_components.OrderConfirmationBottomSheet
import com.illa.cashvan.ui.orders.ui_components.OrderSpecsComponentCompact
import com.illa.cashvan.ui.orders.ui_components.PaymentSummaryCard
import org.koin.androidx.compose.koinViewModel

@Composable
fun OrderDetailsScreen(
    modifier: Modifier = Modifier,
    orderId: String,
    onBackClick: () -> Unit = {},
    onConfirmOrder: () -> Unit = {},
    orderViewModel: OrderViewModel = koinViewModel()
) {
    val orderDetailsState by orderViewModel.orderDetailsUiState.collectAsState()

    LaunchedEffect(orderId) {
        orderViewModel.loadOrderById(orderId)
    }

    when {
        orderDetailsState.isLoading -> {
            LoadingContent(modifier = modifier)
        }
        orderDetailsState.error != null -> {
            ErrorContent(
                modifier = modifier,
                error = orderDetailsState.error ?: "حدث خطأ غير متوقع"
            )
        }
        orderDetailsState.order != null -> {
            OrderDetailsContent(
                modifier = modifier,
                order = orderDetailsState.order!!,
                onBackClick = onBackClick,
                orderViewModel = orderViewModel
            )
        }
        else -> {
            ErrorContent(
                modifier = modifier,
                error = "لم يتم العثور على الطلب"
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = Color(0xFF0D3773)
            )
            Text(
                text = "جاري تحميل تفاصيل الطلب...",
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color(0xFF1F252E),
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun ErrorContent(modifier: Modifier = Modifier, error: String) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "خطأ في تحميل تفاصيل الطلب",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color(0xFFDC2626)
            )
            Text(
                text = error,
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderDetailsContent(
    modifier: Modifier = Modifier,
    order: Order,
    onBackClick: () -> Unit,
    orderViewModel: OrderViewModel = koinViewModel()
) {
    val uiState by orderViewModel.uiState.collectAsState()
    val orderDetailsState by orderViewModel.orderDetailsUiState.collectAsState()
    val orderSpecs = order.toOrderSpecs()
    val merchant = order.toUIMerchant()
    val paymentSummary = order.toPaymentSummary()

    var showCancelBottomSheet by remember { mutableStateOf(false) }
    var showConfirmationBottomSheet by remember { mutableStateOf(false) }
    val cancelSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val confirmationSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            CashVanHeader()

            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                SectionTitle(title = "تفاصيل الطلب")

                OrderSpecsComponentCompact(
                    orderSpecs = orderSpecs
                )

                Spacer(modifier = Modifier.height(4.dp))

                SectionTitle(title = "معلومات التاجر")


                MerchantDetailsComponent(
                    merchant = merchant,
                    latitude = order.merchant?.latitude,
                    longitude = order.merchant?.longitude
                )

                Spacer(modifier = Modifier.height(4.dp))

                SectionTitle(title = "عناصر الطلب")

                // Show products with detailed price breakdown
                order.order_plan_products?.forEach { orderPlanProduct ->
                    val planProductId = orderPlanProduct.plan_product_id ?: return@forEach

                    // Skip deleted products
                    if (planProductId in orderDetailsState.deletedProductIds) {
                        return@forEach
                    }

                    // Get current quantity
                    val currentQuantity = if (orderDetailsState.isEditMode) {
                        orderDetailsState.editedQuantities[planProductId] ?: orderPlanProduct.sold_quantity
                    } else {
                        orderPlanProduct.sold_quantity
                    }

                    // Get price info from state or calculate from order data
                    val priceInfo = orderDetailsState.productPrices[planProductId] ?: run {
                        // Try to use total_price_details first (new API structure)
                        val totalPriceDetails = orderPlanProduct.total_price_details
                        if (totalPriceDetails != null) {
                            val basePrice = totalPriceDetails.unit?.base_price ?: 0.0
                            val finalPricePerUnit = totalPriceDetails.unit?.final_price ?: 0.0
                            val vatAmount = totalPriceDetails.unit?.vat_amount ?: 0.0
                            val discountAmount = totalPriceDetails.unit?.discount_amount ?: 0.0
                            val totalPrice = totalPriceDetails.total?.final_price ?: (finalPricePerUnit * currentQuantity)

                            ProductPriceInfo(
                                basePrice = basePrice,
                                finalPrice = finalPricePerUnit,
                                discountAmount = discountAmount,
                                vatAmount = vatAmount,
                                totalPrice = totalPrice
                            )
                        } else {
                            // Fallback to old structure
                            val priceDetails = orderPlanProduct.plan_product_price?.price_details
                            val basePrice = orderPlanProduct.plan_product_price?.base_price?.toDoubleOrNull() ?: 0.0
                            val finalPricePerUnit = priceDetails?.final_price ?: 0.0
                            val vatAmount = priceDetails?.vat_amount ?: 0.0
                            val discountAmount = priceDetails?.discount_amount ?: 0.0

                            ProductPriceInfo(
                                basePrice = basePrice,
                                finalPrice = finalPricePerUnit,
                                discountAmount = discountAmount,
                                vatAmount = vatAmount,
                                totalPrice = finalPricePerUnit * currentQuantity
                            )
                        }
                    }

                    // Get VAT percentage from total_price_details or fallback to plan_product_price
                    val vatPercentage = orderPlanProduct.total_price_details?.vat_percentage
                        ?: orderPlanProduct.plan_product_price?.vat_percentage ?: 0.0

                    val item = EditableOrderItem(
                        planProductId = planProductId,
                        productName = orderPlanProduct.product?.name ?: "",
                        sku = orderPlanProduct.product?.sku ?: "",
                        quantity = currentQuantity,
                        maxQuantity = orderPlanProduct.initial_sold_quantity,
                        basePrice = priceInfo.basePrice,
                        finalPrice = priceInfo.finalPrice,
                        discountAmount = priceInfo.discountAmount,
                        vatAmount = priceInfo.vatAmount,
                        vatPercentage = vatPercentage,
                        totalPrice = priceInfo.totalPrice,
                        isLoadingPrice = planProductId in orderDetailsState.loadingPriceForProducts
                    )

                    if (orderDetailsState.isEditMode && order.order_type == "pre_sell") {
                        EditableOrderItemCard(
                            item = item,
                            onQuantityChange = { id, qty ->
                                orderViewModel.updateProductQuantity(id, qty)
                            },
                            onRemoveItem = { id ->
                                orderViewModel.deleteProductImmediately(id)
                            }
                        )
                    } else {
                        ReadOnlyProductCard(item = item)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                PaymentSummaryCard(
                    paymentSummary = paymentSummary
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Fixed action buttons at bottom - shown only for ongoing pre_sell orders
            if (order.status == "ongoing" && order.order_type == "pre_sell") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(2.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (orderDetailsState.isEditMode) {
                            // Edit mode buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Cancel edit button
                                OutlinedButton(
                                    onClick = { orderViewModel.exitEditMode() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF6B7280)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        Color(0xFF6B7280)
                                    )
                                ) {
                                    Text(
                                        text = "إلغاء",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily(Font(R.font.zain_regular))
                                    )
                                }

                                // Save button
                                Button(
                                    onClick = {
                                        orderViewModel.saveEditedOrder()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0D3773)
                                    )
                                ) {
                                    Text(
                                        text = "حفظ",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily(Font(R.font.zain_regular))
                                    )
                                }
                            }
                        } else {
                            // Normal mode buttons
                            // Edit button
                            Button(
                                onClick = { orderViewModel.enterEditMode() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6B7280)
                                )
                            ) {
                                Text(
                                    text = "تعديل الأوردر",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily(Font(R.font.zain_regular))
                                )
                            }

                            // Cancel button
                            OutlinedButton(
                                onClick = { showCancelBottomSheet = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFDC3545)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color(0xFFDC3545)
                                )
                            ) {
                                Text(
                                    text = "إلغاء الأوردر",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily(Font(R.font.zain_regular))
                                )
                            }

                            // Submit button
                            Button(
                                onClick = {
                                    orderViewModel.submitOrder(order) {
                                        showConfirmationBottomSheet = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0D3773)
                                )
                            ) {
                                Text(
                                    text = "تسليم الاوردر",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily(Font(R.font.zain_regular))
                                )
                            }
                        }
                    }
                }
            }

            // Print Invoice button - shown for fulfilled or partially fulfilled orders
            if (order.status == "fulfilled" || order.status == "partially_fulfilled") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(2.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Print Invoice button
                        Button(
                            onClick = {
                                orderViewModel.printInvoice(order.id)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0D3773)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "طباعة الفاتورة",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "إطبع الفاتورة",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily(Font(R.font.zain_regular))
                            )
                        }

                        // Send Invoice via WhatsApp button
                        OutlinedButton(
                            onClick = {
                                orderViewModel.sendInvoiceViaWhatsApp(order.id)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF25D366)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color(0xFF25D366)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "إرسال الفاتورة للتاجر",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ارسال الفاتورة للتاجر",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily(Font(R.font.zain_regular))
                            )
                        }
                    }
                }
            }
        }

        // Cancel order bottom sheet
        if (showCancelBottomSheet) {
            CancelOrderBottomSheet(
                sheetState = cancelSheetState,
                onDismiss = {
                    showCancelBottomSheet = false
                },
                onConfirm = { reason, note ->
                    orderViewModel.cancelOrder(
                        orderId = order.id,
                        reason = reason,
                        note = note
                    ) {
                        showCancelBottomSheet = false
                        onBackClick()
                    }
                },
                orderNumber = order.formatted_code
            )
        }

        // Order confirmation bottom sheet
        if (showConfirmationBottomSheet) {
            OrderConfirmationBottomSheet(
                sheetState = confirmationSheetState,
                onDismiss = {
                    showConfirmationBottomSheet = false
                    onBackClick()
                },
                onBackToHome = {
                    showConfirmationBottomSheet = false
                    onBackClick()
                }
            )
        }

        // Show print status snackbar with auto-dismiss
        if (uiState.printStatus != null) {
            LaunchedEffect(uiState.printStatus) {
                kotlinx.coroutines.delay(3000) // Auto-dismiss after 3 seconds
                orderViewModel.clearPrintStatus()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (uiState.printStatus?.contains("بنجاح") == true) {
                    SuccessSnackbar(
                        message = uiState.printStatus ?: "",
                        onDismiss = { orderViewModel.clearPrintStatus() }
                    )
                } else {
                    ErrorSnackbar(
                        message = uiState.printStatus ?: "",
                        onDismiss = { orderViewModel.clearPrintStatus() }
                    )
                }
            }
        }

        // Show success message snackbar with auto-dismiss
        if (orderDetailsState.successMessage != null) {
            LaunchedEffect(orderDetailsState.successMessage) {
                kotlinx.coroutines.delay(3000) // Auto-dismiss after 3 seconds
                orderViewModel.clearSuccessMessage()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                SuccessSnackbar(
                    message = orderDetailsState.successMessage ?: "",
                    onDismiss = { orderViewModel.clearSuccessMessage() }
                )
            }
        }

        // Show error message snackbar with auto-dismiss
        if (orderDetailsState.error != null) {
            LaunchedEffect(orderDetailsState.error) {
                kotlinx.coroutines.delay(3000) // Auto-dismiss after 3 seconds
                orderViewModel.clearOrderDetailsError()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                ErrorSnackbar(
                    message = orderDetailsState.error ?: "",
                    onDismiss = { orderViewModel.clearOrderDetailsError() }
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily(Font(R.font.zain_regular)),
        color = Color(0xFF1F252E),
        modifier = modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun ReadOnlyProductCard(item: EditableOrderItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Product Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Product Info
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = item.productName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF1F252E)
                    )
                    Text(
                        text = "رمز التخزين: ${item.sku}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF9CA3AF)
                    )
                }
                // Quantity Badge
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFFE5E7EB),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "الكمية: ${item.quantity}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF1F252E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Price Details
            PriceDetailsReadOnly(
                basePrice = item.basePrice,
                discountAmount = item.discountAmount,
                vatAmount = item.vatAmount,
                vatPercentage = item.vatPercentage,
                totalPrice = item.totalPrice
            )
        }
    }
}

@Composable
private fun PriceDetailsReadOnly(
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
        // Base Price Per Unit
        PriceRowReadOnly(
            label = "سعر الوحدة",
            value = "${"%.2f".format(basePrice)} جنيه"
        )

        // Discount (per unit if > 0)
        if (discountAmount > 0) {
            PriceRowReadOnly(
                label = "الخصم",
                value = "${"%.2f".format(discountAmount)} جنيه",
                valueColor = Color(0xFF10B981)
            )
        }

        // VAT Percentage (if > 0)
        if (vatPercentage > 0) {
            PriceRowReadOnly(
                label = "نسبة الضريبة",
                value = "${vatPercentage.toInt()}%"
            )
        }

        // VAT Amount (per unit if > 0)
        if (vatAmount > 0) {
            PriceRowReadOnly(
                label = "الضريبة",
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

        // Total (final price after applying discounts and VAT, multiplied by quantity)
        PriceRowReadOnly(
            label = "الاجمالي",
            value = "${"%.2f".format(totalPrice)} جنيه",
            isHighlight = true
        )
    }
}

@Composable
private fun PriceRowReadOnly(
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

@Preview(showBackground = true, locale = "ar")
@Composable
fun OrderDetailsContentPreview() {
    val sampleOrder = Order(
        id = "4",
        created_at = "2025-09-28T21:52:48.528Z",
        updated_at = "2025-09-28T21:52:48.528Z",
        plan_id = "1",
        formatted_code = "ORD-000000004",
        creator_id = "1",
        creator_type = "SalesAgent",
        total_sold_quantity = 3,
        total_income = "149.97",
        status = "fulfilled",
        order_type = "pre_sell",
        order_plan_products = listOf(
            com.illa.cashvan.feature.orders.data.model.OrderPlanProduct(
                id = "4",
                created_at = "2025-09-28T21:52:48.533Z",
                updated_at = "2025-09-28T21:52:48.533Z",
                sold_quantity = 3,
                plan_product_id = "2",
                order_id = "4",
                total_income = "149.97",
                product = com.illa.cashvan.feature.orders.data.model.OrderProduct(
                    id = "2",
                    created_at = "2025-09-28T21:52:47.890Z",
                    updated_at = "2025-09-28T21:52:47.890Z",
                    sku = "PROD002",
                    frontdoor_code = "FD002",
                    price = "49.99",
                    name = "Bluetooth Speaker",
                    description = "Portable Bluetooth speaker with waterproof design and superior sound quality."
                )
            )
        ),
        merchant = com.illa.cashvan.feature.orders.data.model.Merchant(
            id = "2",
            created_at = "2025-09-28T21:52:48.202Z",
            updated_at = "2025-09-28T21:52:48.202Z",
            name = "Electronics Hub",
            address = "456 Pyramids Road, Giza",
            google_link = null,
            phone_number = "+201555000002",
            latitude = null,
            longitude = null,
            governorate_id = "2",
            creator_id = "1",
            creator_type = "Supervisor",
            plan_id = "1"
        )
    )

    MaterialTheme {
        OrderDetailsContent(
            order = sampleOrder,
            onBackClick = { /* Handle back */ }
        )
    }
}