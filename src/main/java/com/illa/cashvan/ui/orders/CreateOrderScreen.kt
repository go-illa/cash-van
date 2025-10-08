package com.illa.cashvan.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.illa.cashvan.ui.orders.ui_components.AddMerchantBottomSheet
import com.illa.cashvan.ui.orders.ui_components.ProductSelectionComponent
import com.illa.cashvan.ui.orders.ui_components.SearchableDropdown
import com.illa.cashvan.ui.orders.ui_components.SelectedProductsList
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    viewModel: CreateOrderViewModel = koinViewModel(),
    onBackClick: () -> Unit = {},
    onOrderCreated: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showAddMerchantSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    LaunchedEffect(Unit) {
        viewModel.resetState()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.orderCreated) {
        if (uiState.orderCreated) {
            snackbarHostState.showSnackbar("تم إنشاء الطلب بنجاح")
            viewModel.resetOrderCreated()
            onOrderCreated()
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
                    val product = uiState.products.find { it.id == planProductId }
                    product?.product_price?.toDoubleOrNull()?.times(quantity) ?: 0.0
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
                    val product = uiState.products.find { it.id == planProductId }
                    product == null || quantity > product.available_quantity
                }

                Button(
                    onClick = { viewModel.createOrder() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D3773),
                        disabledContainerColor = Color(0xFFE5E7EB)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = uiState.selectedMerchant != null &&
                              uiState.selectedProducts.isNotEmpty() &&
                              !uiState.isLoading &&
                              !hasInvalidQuantity
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
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
                // Plan Info
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
                        Text(
                            text = plan.formatted_code,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily(Font(R.font.zain_regular)),
                            color = Color(0xFF0D3773)
                        )
                    }
                }

                // Merchant Selection
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "اختيار التاجر",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily(Font(R.font.zain_regular)),
                            color = Color.Black
                        )

                        Button(
                            onClick = { showAddMerchantSheet = true },
                            modifier = Modifier
                                .height(40.dp)
                                .border(width = 2.dp, color = Color(0xFF0D3773), RoundedCornerShape(size = 20.dp)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF0D3773),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "أضافة تاجر",
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.zain_regular)),
                                color = Color(0xFF0D3773)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SearchableDropdown(
                        label = "",
                        placeholder = "ابحث بالاسم او التليفون",
                        searchQuery = uiState.merchantSearchQuery,
                        onSearchQueryChange = { viewModel.searchMerchants(it) },
                        items = uiState.merchants,
                        selectedItem = uiState.selectedMerchant,
                        onItemSelected = { viewModel.selectMerchant(it) },
                        itemText = { it.name },
                        isLoading = uiState.isSearchingMerchants,
                        enabled = !uiState.isLoading,
                        onExpanded = {
                            if (uiState.merchantSearchQuery.isEmpty()) {
                                viewModel.searchMerchants("")
                            }
                        },
                        onClear = { viewModel.clearMerchant() },
                        analyticsEventName = "select_merchant"
                    )
                }

                // Product Selection
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
                    enabled = !uiState.isLoading
                )

                // Selected Products List
                if (uiState.selectedProducts.isNotEmpty()) {
                    SelectedProductsList(
                        modifier = Modifier.fillMaxWidth(),
                        selectedProducts = uiState.selectedProducts,
                        products = uiState.products,
                        onQuantityChange = { planProductId, quantity ->
                            viewModel.updateProductQuantity(planProductId, quantity)
                        },
                        onRemoveProduct = { planProductId ->
                            viewModel.removeProduct(planProductId)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // Add Merchant Bottom Sheet
        if (showAddMerchantSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddMerchantSheet = false },
                sheetState = bottomSheetState
            ) {
                AddMerchantBottomSheet(
                    onDismiss = { showAddMerchantSheet = false },
                    onMerchantCreated = {
                        showAddMerchantSheet = false
                        viewModel.searchMerchants("")
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("تم إضافة التاجر بنجاح")
                        }
                    }
                )
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