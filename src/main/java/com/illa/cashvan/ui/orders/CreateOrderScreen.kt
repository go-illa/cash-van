package com.illa.cashvan.ui.orders

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch
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
import com.illa.cashvan.feature.orders.presentation.viewmodel.CreateOrderViewModel
import com.illa.cashvan.ui.common.ErrorSnackbar
import com.illa.cashvan.ui.common.SuccessSnackbar
import com.illa.cashvan.ui.orders.ui_components.ProductSelectionComponent
import com.illa.cashvan.ui.orders.ui_components.SearchableDropdown
import com.illa.cashvan.ui.orders.ui_components.SelectedProductsList
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    viewModel: CreateOrderViewModel = koinViewModel(),
    merchantCreatedSignal: Int = 0,
    onAddMerchantClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onOrderCreated: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var orderSubmitted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.resetState()
    }

    LaunchedEffect(merchantCreatedSignal) {
        if (merchantCreatedSignal > 0) {
            viewModel.searchMerchants("")
            coroutineScope.launch {
                snackbarHostState.showSnackbar("تم إضافة التاجر بنجاح")
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                if (data.visuals.message.contains("بنجاح")) {
                    SuccessSnackbar(
                        message = data.visuals.message,
                        onDismiss = { snackbarHostState.currentSnackbarData?.dismiss() }
                    )
                } else {
                    ErrorSnackbar(
                        message = data.visuals.message,
                        onDismiss = { snackbarHostState.currentSnackbarData?.dismiss() }
                    )
                }
            }
        },
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
                val totalAmount = uiState.selectedProducts.entries.sumOf { (planProductId, quantity) ->
                    val priceInfo = uiState.productPrices[planProductId]
                    if (priceInfo != null) {
                        priceInfo.totalPrice
                    } else {
                        val product = uiState.allProducts.find { it.id == planProductId }
                        product?.product_price?.toDoubleOrNull()?.times(quantity) ?: 0.0
                    }
                }

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

                val hasInvalidQuantity = uiState.selectedProducts.any { (planProductId, quantity) ->
                    val product = uiState.allProducts.find { it.id == planProductId }
                    product == null || quantity > (product.cash_van_available_quantity ?: 0)
                }

                Button(
                    onClick = {
                        orderSubmitted = true
                        viewModel.createOrder()
                        onOrderCreated()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D3773),
                        disabledContainerColor = Color(0xFFE5E7EB)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !orderSubmitted &&
                              uiState.selectedMerchant != null &&
                              uiState.selectedProducts.isNotEmpty() &&
                              !hasInvalidQuantity
                ) {
                    Text(
                        text = "إتمام الطلب",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        if (uiState.currentPlan == null && uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF0D3773)
                )
            }
        } else if (uiState.currentPlan == null && uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.error ?: "خطأ في تحميل الخطة",
                        fontSize = 16.sp,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFFDC2626),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.retryLoadPlan() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D3773)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "إعادة المحاولة",
                            fontFamily = FontFamily(Font(R.font.zain_regular)),
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8F9FA))
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                uiState.currentPlan?.let { plan ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "الخطة الحالية",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily(Font(R.font.zain_regular)),
                            color = Color(0xFF6B7280)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        plan.formatted_code?.let {
                            Text(
                                text = it,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily(Font(R.font.zain_regular)),
                                color = Color(0xFF0D3773)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(horizontal = 23.dp, vertical = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "اختيار التاجر",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily(Font(R.font.zain_regular)),
                                color = Color.Black
                            )
                            Text(
                                text = " *",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily(Font(R.font.zain_regular)),
                                color = Color(0xFFE23636)
                            )
                        }

                        OutlinedButton(
                            onClick = onAddMerchantClick,
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(200.dp),
                            border = BorderStroke(1.dp, Color(0xFF0D3773)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF0D3773)
                            )
                        ) {
                            Text(
                                text = "+ أضافة تاجر",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily(Font(R.font.zain_regular)),
                                color = Color(0xFF0D3773)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(7.dp))

                    SearchableDropdown(
                        label = "",
                        placeholder = "أبحث بالاسم او رقم التيليفون",
                        searchQuery = uiState.merchantSearchQuery,
                        onSearchQueryChange = { viewModel.searchMerchants(it) },
                        items = uiState.merchants,
                        selectedItem = uiState.selectedMerchant,
                        onItemSelected = { viewModel.selectMerchant(it) },
                        itemText = { it.displayName },
                        isLoading = uiState.isSearchingMerchants,
                        enabled = !uiState.isLoading,
                        onExpanded = {
                            if (uiState.merchantSearchQuery.isEmpty()) {
                                viewModel.searchMerchants("")
                            }
                        },
                        onClear = { viewModel.clearMerchant() },
                        onLoadMore = { viewModel.loadMoreMerchants() },
                        isLoadingMore = uiState.isLoadingMoreMerchants,
                        analyticsEventName = "select_merchant"
                    )
                }

                ProductSelectionComponent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    searchQuery = uiState.productSearchQuery,
                    onSearchQueryChange = { viewModel.searchProducts(it) },
                    products = uiState.products,
                    onProductSelected = { product, quantity ->
                        viewModel.addProductToOrder(product.id, quantity)
                    },
                    isLoading = uiState.isSearchingProducts,
                    enabled = !uiState.isLoading,
                    onFetchPricePreview = { planProductId, quantity ->
                        viewModel.fetchPreviewPrice(planProductId, quantity)
                    },
                    previewPrice = uiState.previewProductPrice,
                    isLoadingPreviewPrice = uiState.isLoadingPreviewPrice,
                    merchantSelected = uiState.selectedMerchant != null,
                    onLoadMore = { viewModel.loadMoreProducts() },
                    isLoadingMore = uiState.isLoadingMoreProducts,
                    onOpenDropdown = { viewModel.refreshProducts() }
                )

                if (uiState.selectedProducts.isNotEmpty()) {
                    SelectedProductsList(
                        modifier = Modifier.fillMaxWidth(),
                        selectedProducts = uiState.selectedProducts,
                        products = uiState.allProducts,
                        onQuantityChange = { planProductId, quantity ->
                            viewModel.updateProductQuantity(planProductId, quantity)
                        },
                        onRemoveProduct = { planProductId ->
                            viewModel.removeProduct(planProductId)
                        },
                        productPrices = uiState.productPrices,
                        loadingPriceForProducts = uiState.loadingPriceForProducts
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "الخصم المؤجل (اختياري)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.rebateValue,
                        onValueChange = { viewModel.updateRebateValue(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "أدخل قيمة الخصم المؤجل",
                                fontFamily = FontFamily(Font(R.font.zain_regular)),
                                color = Color(0xFF9CA3AF)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0D3773),
                            unfocusedBorderColor = Color(0xFFE5E7EB)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
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
