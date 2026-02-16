package com.illa.cashvan.feature.orders.presentation.viewmodel

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.getCurrentDateApiFormat
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.orders.data.model.Order
import com.illa.cashvan.feature.orders.data.model.UpdateOrderData
import com.illa.cashvan.feature.orders.data.model.UpdateOrderRequest
import com.illa.cashvan.feature.orders.domain.usecase.GetOrdersUseCase
import com.illa.cashvan.feature.orders.domain.usecase.GetOrderByIdUseCase
import com.illa.cashvan.feature.orders.domain.usecase.UpdateOrderUseCase
import com.illa.cashvan.feature.orders.domain.usecase.GetInvoiceContentUseCase
import com.illa.cashvan.feature.orders.domain.usecase.GetProductTotalPriceUseCase
import com.illa.cashvan.feature.printer.CpclInvoiceFormatter
import com.illa.cashvan.feature.printer.HoneywellPrinterManager
import com.illa.cashvan.core.utils.WhatsAppHelper
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class OrderType(val value: String, val displayName: String) {
    CASH_VAN("cash_van", "أوردارات كاش"),
    PRE_SELL("pre_sell", "أوردرات تسليم")
}

data class OrderUiState(
    val isLoading: Boolean = false,
    val orders: List<Order> = emptyList(),
    val error: String? = null,
    val selectedTab: OrderType = OrderType.PRE_SELL,
    val isPrinting: Boolean = false,
    val printStatus: String? = null
)

data class ProductPriceInfo(
    val basePrice: Double,
    val finalPrice: Double,
    val discountAmount: Double,
    val vatAmount: Double,
    val totalPrice: Double
)

data class OrderDetailsUiState(
    val isLoading: Boolean = false,
    val order: Order? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val isEditMode: Boolean = false,
    val editedQuantities: Map<String, Int> = emptyMap(), // planProductId -> quantity
    val deletedProductIds: Set<String> = emptySet(),
    val isUpdatingPrice: Boolean = false,
    val productPrices: Map<String, ProductPriceInfo> = emptyMap(), // planProductId -> price info
    val loadingPriceForProducts: Set<String> = emptySet() // planProductIds currently loading prices
)

class OrderViewModel(
    private val context: Context,
    private val getOrdersUseCase: GetOrdersUseCase,
    private val getOrderByIdUseCase: GetOrderByIdUseCase,
    private val updateOrderUseCase: UpdateOrderUseCase,
    private val getInvoiceContentUseCase: GetInvoiceContentUseCase,
    private val getProductTotalPriceUseCase: GetProductTotalPriceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()

    private val _orderDetailsUiState = MutableStateFlow(OrderDetailsUiState())
    val orderDetailsUiState: StateFlow<OrderDetailsUiState> = _orderDetailsUiState.asStateFlow()

    private val printerManager: HoneywellPrinterManager by lazy {
        HoneywellPrinterManager(context)
    }

    init {
        loadOrders(orderType = OrderType.PRE_SELL)
    }

    fun loadOrders(date: String? = null, orderType: OrderType = _uiState.value.selectedTab) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Always use today's date
            val dateToUse = getCurrentDateApiFormat()

            when (val result = getOrdersUseCase(dateToUse, orderType.value)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        orders = result.data.data
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is ApiResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun selectTab(orderType: OrderType) {
        if (_uiState.value.selectedTab != orderType) {
            _uiState.value = _uiState.value.copy(selectedTab = orderType)
            loadOrders(orderType = orderType)
        }
    }

    fun refresh() {
        loadOrders()
    }

    fun loadOrderById(orderId: String) {
        viewModelScope.launch {
            // Reset edit mode and clear any edited data when loading a new order
            _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
                isLoading = true,
                error = null,
                isEditMode = false,
                editedQuantities = emptyMap(),
                deletedProductIds = emptySet()
            )

            when (val result = getOrderByIdUseCase(orderId)) {
                is ApiResult.Success -> {
                    _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
                        isLoading = false,
                        order = result.data
                    )
                    // Initialize prices from the order data
                    initializeProductPrices()
                }
                is ApiResult.Error -> {
                    _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is ApiResult.Loading -> {
                    _orderDetailsUiState.value = _orderDetailsUiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun cancelOrder(orderId: String, reason: String, note: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val request = UpdateOrderRequest(
                order = UpdateOrderData(
                    status = "canceled",
                    cancellation_reason = reason,
                    cancellation_note = note
                )
            )

            when (val result = updateOrderUseCase(orderId, request)) {
                is ApiResult.Success -> {
                    onSuccess()
                    // Refresh the orders list
                    loadOrders()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is ApiResult.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    fun submitOrder(order: Order, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            // Map order plan products to submit order items
            val orderItems = order.order_plan_products?.map { planProduct ->
                com.illa.cashvan.feature.orders.data.model.SubmitOrderItem(
                    plan_product_id = planProduct.plan_product_id ?: "",
                    sold_quantity = planProduct.sold_quantity
                )
            } ?: emptyList()

            // Submit order with status "submitted" and order items
            val request = UpdateOrderRequest(
                order = UpdateOrderData(
                    status = "submitted",
                    order_items = orderItems
                )
            )

            when (val result = updateOrderUseCase(order.id, request)) {
                is ApiResult.Success -> {
                    onSuccess()
                    // Print invoice after successful submission
                    printInvoice(order.id)
                    // Refresh the orders list
                    loadOrders()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is ApiResult.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    fun printInvoice(orderId: String) {
        viewModelScope.launch {
            try {
                Log.d("OrderViewModel", "========================================")
                Log.d("OrderViewModel", "Starting print process for order $orderId")
                Log.d("OrderViewModel", "========================================")

                _uiState.value = _uiState.value.copy(
                    isPrinting = true,
                    printStatus = "جاري تحميل الفاتورة..."
                )

                // Fetch the order with invoice_attachment
                Log.d("OrderViewModel", "Fetching order details with invoice attachment")
                val orderResult = getOrderByIdUseCase(orderId)

                if (orderResult !is ApiResult.Success) {
                    Log.e("OrderViewModel", "Failed to fetch order details: ${(orderResult as? ApiResult.Error)?.message}")
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        printStatus = "فشل في تحميل الفاتورة"
                    )
                    return@launch
                }

                val invoiceAttachment = orderResult.data.invoice_attachment
                if (invoiceAttachment == null) {
                    Log.e("OrderViewModel", "No invoice attachment found for order $orderId")
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        printStatus = "لا توجد فاتورة متاحة لهذا الطلب"
                    )
                    return@launch
                }

                val invoiceUrl = invoiceAttachment.url
                Log.d("OrderViewModel", "Invoice URL: $invoiceUrl")
                Log.d("OrderViewModel", "Invoice filename: ${invoiceAttachment.filename}")
                Log.d("OrderViewModel", "Invoice content type: ${invoiceAttachment.content_type}")

                _uiState.value = _uiState.value.copy(printStatus = "جاري تنزيل الفاتورة...")

                // Download the invoice content directly from S3 URL
                Log.d("OrderViewModel", "Calling getInvoiceContentUseCase with URL: $invoiceUrl")
                val invoiceResult = getInvoiceContentUseCase(invoiceUrl)

                if (invoiceResult !is ApiResult.Success) {
                    Log.e("OrderViewModel", "Failed to download invoice: ${(invoiceResult as? ApiResult.Error)?.message}")
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        printStatus = "فشل في تنزيل الفاتورة"
                    )
                    return@launch
                }

                val invoiceText = invoiceResult.data
                Log.d("OrderViewModel", "Invoice downloaded successfully")
                Log.d("OrderViewModel", "Loaded invoice, length: ${invoiceText.length} characters")
                Log.d("OrderViewModel", "Invoice preview: ${invoiceText.take(100)}")

                _uiState.value = _uiState.value.copy(printStatus = "جاري تجهيز الفاتورة...")

                // Format as CPCL ByteArray with proper encoding (ASCII for commands, UTF-8 for text)
                val cpclFormattedBytes = CpclInvoiceFormatter.formatInvoiceAsCpclBytes(invoiceText)
                Log.d("OrderViewModel", "Formatted invoice as CPCL ByteArray, size: ${cpclFormattedBytes.size} bytes")

                _uiState.value = _uiState.value.copy(printStatus = "جاري طباعة الفاتورة...")

                // Send CPCL formatted invoice to printer using printBytes (sends raw bytes directly)
                Log.d("OrderViewModel", "Sending CPCL formatted invoice to printer...")

                // Connect if needed
                val connectResult = if (!printerManager.isConnected()) {
                    printerManager.connect()
                } else {
                    Result.success("Already connected")
                }

                // Use printBytes() to send raw bytes directly (no re-encoding)
                val printResult = if (connectResult.isSuccess) {
                    printerManager.printBytes(cpclFormattedBytes)
                } else {
                    connectResult
                }

                _uiState.value = _uiState.value.copy(isPrinting = false)
                if (printResult.isSuccess) {
                    Log.d("OrderViewModel", "Invoice printed successfully!")
                    setPrintStatusWithAutoClear("تم طباعة الفاتورة بنجاح!")
                } else {
                    Log.e("OrderViewModel", "Print failed: ${printResult.exceptionOrNull()?.message}")
                    setPrintStatusWithAutoClear("فشل في الطباعة: ${printResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Exception during print process", e)
                _uiState.value = _uiState.value.copy(isPrinting = false)
                setPrintStatusWithAutoClear("خطأ: ${e.message}")
            }
        }
    }

    fun clearPrintStatus() {
        _uiState.value = _uiState.value.copy(printStatus = null)
    }

    /**
     * Set print status with auto-clear after delay
     * This is managed by ViewModel lifecycle to avoid UI lifecycle issues
     */
    private fun setPrintStatusWithAutoClear(status: String, delayMs: Long = 3000) {
        _uiState.value = _uiState.value.copy(printStatus = status)
        viewModelScope.launch {
            kotlinx.coroutines.delay(delayMs)
            _uiState.value = _uiState.value.copy(printStatus = null)
        }
    }

    // Edit mode functions
    fun enterEditMode() {
        val currentOrder = _orderDetailsUiState.value.order ?: return
        val initialQuantities = currentOrder.order_plan_products?.associate {
            (it.plan_product_id ?: "") to it.sold_quantity
        } ?: emptyMap()

        _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
            isEditMode = true,
            editedQuantities = initialQuantities,
            deletedProductIds = emptySet()
        )
    }

    fun exitEditMode() {
        _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
            isEditMode = false,
            editedQuantities = emptyMap(),
            deletedProductIds = emptySet()
        )
    }

    fun updateProductQuantity(planProductId: String, newQuantity: Int) {
        val currentQuantities = _orderDetailsUiState.value.editedQuantities.toMutableMap()
        currentQuantities[planProductId] = newQuantity
        _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
            editedQuantities = currentQuantities
        )

        // Calculate new price for this product
        calculateProductPrice(planProductId, newQuantity)
    }

    private fun calculateProductPrice(planProductId: String, quantity: Int) {
        viewModelScope.launch {
            val currentOrder = _orderDetailsUiState.value.order ?: return@launch

            // Find the order plan product
            val orderPlanProduct = currentOrder.order_plan_products?.find {
                it.plan_product_id == planProductId
            } ?: return@launch

            val productId = orderPlanProduct.plan_product_id ?: return@launch
            val planId = currentOrder.plan_id ?: return@launch

            // Mark this product as loading price
            val loadingProducts = _orderDetailsUiState.value.loadingPriceForProducts.toMutableSet()
            loadingProducts.add(planProductId)
            _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
                loadingPriceForProducts = loadingProducts
            )

            when (val result = getProductTotalPriceUseCase(
                planId = planId,
                productId = productId,
                orderId = currentOrder.id,
                quantity = quantity
            )) {
                is ApiResult.Success -> {
                    val priceResponse = result.data

                    // Try to use new structure (total_price_details) first
                    val priceInfo = if (priceResponse.unit != null && priceResponse.total != null) {
                        ProductPriceInfo(
                            basePrice = priceResponse.unit.base_price ?: 0.0,
                            finalPrice = priceResponse.unit.final_price ?: 0.0,
                            discountAmount = priceResponse.unit.discount_amount ?: 0.0,
                            vatAmount = priceResponse.unit.vat_amount ?: 0.0,
                            totalPrice = priceResponse.total.final_price ?: 0.0
                        )
                    } else {
                        // Fallback to old structure
                        ProductPriceInfo(
                            basePrice = priceResponse.base_price ?: 0.0,
                            finalPrice = priceResponse.final_price ?: 0.0,
                            discountAmount = priceResponse.total_discount ?: 0.0,
                            vatAmount = priceResponse.total_vat ?: 0.0,
                            totalPrice = priceResponse.total_price ?: 0.0
                        )
                    }

                    val updatedPrices = _orderDetailsUiState.value.productPrices.toMutableMap()
                    updatedPrices[planProductId] = priceInfo

                    val updatedLoadingProducts = _orderDetailsUiState.value.loadingPriceForProducts.toMutableSet()
                    updatedLoadingProducts.remove(planProductId)

                    _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
                        productPrices = updatedPrices,
                        loadingPriceForProducts = updatedLoadingProducts
                    )
                }
                is ApiResult.Error -> {
                    val updatedLoadingProducts = _orderDetailsUiState.value.loadingPriceForProducts.toMutableSet()
                    updatedLoadingProducts.remove(planProductId)
                    _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
                        loadingPriceForProducts = updatedLoadingProducts
                    )
                    Log.e("OrderViewModel", "Error calculating price: ${result.message}")
                }
                is ApiResult.Loading -> {
                    // Already handled above
                }
            }
        }
    }

    fun initializeProductPrices() {
        val currentOrder = _orderDetailsUiState.value.order ?: return

        val initialPrices = currentOrder.order_plan_products?.associate { orderPlanProduct ->
            val planProductId = orderPlanProduct.plan_product_id ?: ""
            val totalPriceDetails = orderPlanProduct.total_price_details
            val quantity = orderPlanProduct.sold_quantity

            val priceInfo = if (totalPriceDetails != null) {
                // Use new total_price_details structure
                ProductPriceInfo(
                    basePrice = totalPriceDetails.unit?.base_price ?: 0.0,
                    finalPrice = totalPriceDetails.unit?.final_price ?: 0.0,
                    discountAmount = totalPriceDetails.unit?.discount_amount ?: 0.0,
                    vatAmount = totalPriceDetails.unit?.vat_amount ?: 0.0,
                    totalPrice = totalPriceDetails.total?.final_price ?: 0.0
                )
            } else {
                // Fallback to old structure
                val priceDetails = orderPlanProduct.plan_product_price?.price_details
                ProductPriceInfo(
                    basePrice = orderPlanProduct.plan_product_price?.base_price?.toDoubleOrNull() ?: 0.0,
                    finalPrice = priceDetails?.final_price ?: 0.0,
                    discountAmount = priceDetails?.discount_amount ?: 0.0,
                    vatAmount = priceDetails?.vat_amount ?: 0.0,
                    totalPrice = (priceDetails?.final_price ?: 0.0) * quantity
                )
            }

            planProductId to priceInfo
        }?.toMap() ?: emptyMap()

        _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
            productPrices = initialPrices
        )
    }


    fun deleteProductImmediately(planProductId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val currentOrder = _orderDetailsUiState.value.order ?: return@launch

            // Set loading state
            _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
                isLoading = true,
                error = null
            )

            // Create request with _destroy flag
            val request = UpdateOrderRequest(
                order = UpdateOrderData(
                    order_items = listOf(
                        com.illa.cashvan.feature.orders.data.model.SubmitOrderItem(
                            plan_product_id = planProductId,
                            sold_quantity = 0,
                            _destroy = true
                        )
                    )
                )
            )

            when (val result = updateOrderUseCase(currentOrder.id, request)) {
                is ApiResult.Success -> {
                    // Refresh order data to get updated product list
                    loadOrderById(currentOrder.id)
                    // Set success message
                    _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
                        successMessage = "تم حذف المنتج بنجاح"
                    )
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                    onError(result.message)
                }
                else -> {

                }
            }
        }
    }

    fun clearSuccessMessage() {
        _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
            successMessage = null
        )
    }

    fun clearOrderDetailsError() {
        _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
            error = null
        )
    }

    fun saveEditedOrder(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val currentOrder = _orderDetailsUiState.value.order ?: return@launch
            val editedQuantities = _orderDetailsUiState.value.editedQuantities
            val deletedProductIds = _orderDetailsUiState.value.deletedProductIds

            // Build order items from edited quantities, excluding deleted products
            val orderItems = editedQuantities
                .filter { (planProductId, _) -> planProductId !in deletedProductIds }
                .filter { (_, quantity) -> quantity > 0 } // Only include products with quantity > 0
                .map { (planProductId, quantity) ->
                    com.illa.cashvan.feature.orders.data.model.SubmitOrderItem(
                        plan_product_id = planProductId,
                        sold_quantity = quantity
                    )
                }

            val request = UpdateOrderRequest(
                order = UpdateOrderData(
                    order_items = orderItems
                )
            )

            when (val result = updateOrderUseCase(currentOrder.id, request)) {
                is ApiResult.Success -> {
                    // Refresh order data
                    loadOrderById(currentOrder.id)
                    // Exit edit mode
                    exitEditMode()
                    // Set success message
                    _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
                        successMessage = "تم حفظ التعديلات بنجاح"
                    )
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
                        error = result.message
                    )
                }
                is ApiResult.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    /**
     * Send invoice PDF to merchant via WhatsApp
     */
    fun sendInvoiceViaWhatsApp(orderId: String) {
        viewModelScope.launch {
            try {
                Log.d("OrderViewModel", "Starting send invoice via WhatsApp for order $orderId")

                _uiState.value = _uiState.value.copy(
                    isPrinting = true,
                    printStatus = "جاري تحميل الفاتورة..."
                )

                // Fetch the order with invoice_attachment
                val orderResult = getOrderByIdUseCase(orderId)

                if (orderResult !is ApiResult.Success) {
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        printStatus = "فشل في تحميل الفاتورة"
                    )
                    return@launch
                }

                val order = orderResult.data
                val invoiceAttachment = order.invoice_attachment
                if (invoiceAttachment == null) {
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        printStatus = "لا توجد فاتورة متاحة لهذا الطلب"
                    )
                    return@launch
                }

                val merchantPhone = order.merchant?.phone_number
                if (merchantPhone.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        printStatus = "رقم هاتف التاجر غير متوفر"
                    )
                    return@launch
                }

                val invoiceUrl = invoiceAttachment.url
                Log.d("OrderViewModel", "Invoice URL: $invoiceUrl")
                Log.d("OrderViewModel", "Invoice content_type: ${invoiceAttachment.content_type}")
                Log.d("OrderViewModel", "Invoice filename: ${invoiceAttachment.filename}")
                Log.d("OrderViewModel", "Merchant phone: $merchantPhone")

                _uiState.value = _uiState.value.copy(printStatus = "جاري تنزيل الفاتورة...")

                // Try to download as PDF first
                var pdfFile = downloadPdfFile(invoiceUrl, order.formatted_code)

                if (pdfFile == null) {
                    // If it's not a PDF, download as text and generate PDF
                    Log.d("OrderViewModel", "Not a PDF file, downloading as text and generating PDF")
                    _uiState.value = _uiState.value.copy(printStatus = "جاري تحويل الفاتورة إلى PDF...")

                    // Download invoice content as text
                    val invoiceResult = getInvoiceContentUseCase(invoiceUrl)
                    if (invoiceResult !is ApiResult.Success) {
                        _uiState.value = _uiState.value.copy(
                            isPrinting = false,
                            printStatus = "فشل في تحميل محتوى الفاتورة"
                        )
                        return@launch
                    }

                    val invoiceText = invoiceResult.data
                    Log.d("OrderViewModel", "Downloaded invoice text, length: ${invoiceText.length}")

                    // Generate PDF from text
                    pdfFile = generatePdfFromText(invoiceText, order.formatted_code)
                    if (pdfFile == null) {
                        _uiState.value = _uiState.value.copy(
                            isPrinting = false,
                            printStatus = "فشل في إنشاء ملف PDF"
                        )
                        return@launch
                    }
                }

                _uiState.value = _uiState.value.copy(printStatus = "جاري فتح واتساب...")

                // Open WhatsApp to send the file
                openWhatsAppWithFile(pdfFile, merchantPhone, order.formatted_code)

                _uiState.value = _uiState.value.copy(isPrinting = false)
                setPrintStatusWithAutoClear("تم فتح واتساب لإرسال الفاتورة")

            } catch (e: Exception) {
                Log.e("OrderViewModel", "Exception during send via WhatsApp", e)
                _uiState.value = _uiState.value.copy(isPrinting = false)
                setPrintStatusWithAutoClear("خطأ: ${e.message}")
            }
        }
    }

    private suspend fun downloadPdfFile(url: String, orderCode: String): File? {
        return try {
            val client = HttpClient(Android)
            val response: HttpResponse = client.get(url)

            // Check content type
            val contentType = response.headers["Content-Type"]
            Log.d("OrderViewModel", "Response Content-Type: $contentType")

            val bytes = response.readBytes()
            client.close()

            // Verify it's actually a PDF by checking the first few bytes (PDF magic number)
            if (bytes.size < 4 || !isPdfFile(bytes)) {
                Log.e("OrderViewModel", "Downloaded file is not a PDF. First bytes: ${bytes.take(10).joinToString()}")
                return null
            }

            // Save to cache directory
            val cacheDir = context.cacheDir
            val pdfFile = File(cacheDir, "invoice_${orderCode}.pdf")
            pdfFile.writeBytes(bytes)

            Log.d("OrderViewModel", "PDF file saved successfully: ${pdfFile.absolutePath}, size: ${bytes.size} bytes")
            pdfFile
        } catch (e: Exception) {
            Log.e("OrderViewModel", "Failed to download PDF", e)
            null
        }
    }

    private fun isPdfFile(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
               bytes[0] == 0x25.toByte() && // %
               bytes[1] == 0x50.toByte() && // P
               bytes[2] == 0x44.toByte() && // D
               bytes[3] == 0x46.toByte()    // F
    }

    private fun generatePdfFromText(text: String, orderCode: String): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 width in points (8.27 inches)
            val pageHeight = 842 // A4 height in points (11.69 inches)
            val margin = 40f
            val lineHeight = 18f
            val fontSize = 11f

            // Configure paint for text
            val paint = Paint().apply {
                textSize = fontSize
                isAntiAlias = true
                color = android.graphics.Color.BLACK
                // Use monospace font for receipt formatting
                typeface = android.graphics.Typeface.MONOSPACE
            }

            // Split text into lines
            val lines = text.lines()
            val linesPerPage = ((pageHeight - 2 * margin) / lineHeight).toInt()

            var pageNumber = 1
            var lineIndex = 0

            while (lineIndex < lines.size) {
                // Create a new page
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                var yPosition = margin

                // Draw lines on this page
                val endLine = minOf(lineIndex + linesPerPage, lines.size)
                for (i in lineIndex until endLine) {
                    var line = lines[i]

                    // Skip empty lines but preserve spacing
                    if (line.isBlank()) {
                        yPosition += lineHeight
                        continue
                    }

                    // Detect line alignment based on content
                    val trimmedLine = line.trim()
                    val leadingSpaces = line.takeWhile { it == ' ' }.length
                    val trailingSpaces = line.takeLastWhile { it == ' ' }.length

                    // Use trimmed line for drawing
                    line = trimmedLine

                    val textWidth = paint.measureText(line)
                    val contentWidth = pageWidth - 2 * margin

                    // Determine alignment
                    val xPosition = when {
                        // Centered text (has significant spaces on both sides or is a title-like line)
                        leadingSpaces > 5 && trailingSpaces > 5 -> {
                            margin + (contentWidth - textWidth) / 2
                        }
                        // Arabic/RTL text - align right
                        line.firstOrNull()?.let { isArabic(it) } == true -> {
                            pageWidth - margin - textWidth
                        }
                        // Default - align left
                        else -> {
                            margin
                        }
                    }

                    canvas.drawText(line, xPosition, yPosition, paint)
                    yPosition += lineHeight
                }

                pdfDocument.finishPage(page)
                lineIndex = endLine
                pageNumber++
            }

            // Save to file
            val cacheDir = context.cacheDir
            val pdfFile = File(cacheDir, "invoice_${orderCode}.pdf")
            pdfFile.outputStream().use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()

            Log.d("OrderViewModel", "PDF generated successfully: ${pdfFile.absolutePath}, pages: ${pageNumber - 1}")
            pdfFile
        } catch (e: Exception) {
            Log.e("OrderViewModel", "Failed to generate PDF from text", e)
            null
        }
    }

    private fun isArabic(char: Char): Boolean {
        // Check if character is in Arabic Unicode range
        val codePoint = char.code
        return codePoint in 0x0600..0x06FF || // Arabic
               codePoint in 0x0750..0x077F || // Arabic Supplement
               codePoint in 0xFB50..0xFDFF || // Arabic Presentation Forms-A
               codePoint in 0xFE70..0xFEFF    // Arabic Presentation Forms-B
    }

    private fun openWhatsAppWithFile(file: File, phoneNumber: String, orderCode: String) {
        try {
            // Create content URI using FileProvider
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val message = "فاتورة رقم: $orderCode"

            // Send invoice to merchant via WhatsApp
            Log.d("OrderViewModel", "Sending invoice to merchant via WhatsApp")
            when (val result = WhatsAppHelper.sendInvoiceToMerchant(
                context,
                contentUri,
                phoneNumber,
                orderCode
            )) {
                is WhatsAppHelper.SendResult.ChatOpened -> {
                    Log.d("OrderViewModel", "WhatsApp opened with invoice")
                    setPrintStatusWithAutoClear(result.message)
                }
                is WhatsAppHelper.SendResult.Failure -> {
                    setPrintStatusWithAutoClear(result.error)
                }
            }
        } catch (e: Exception) {
            Log.e("OrderViewModel", "Failed to open WhatsApp", e)
            setPrintStatusWithAutoClear("فشل في فتح واتساب: ${e.message}")
        }
    }

    private fun shareFileToOpenChat(contentUri: Uri, message: String) {
        // Share the file via WhatsApp (will open share dialog on top of the already open chat)
        val result = WhatsAppHelper.shareFile(context, contentUri, message)
        when {
            result.isSuccess -> {
                Log.d("OrderViewModel", "File share initiated successfully")
            }
            result.isFailure -> {
                Log.e("OrderViewModel", "Failed to share file", result.exceptionOrNull())
            }
        }
    }

    /**
     * Test print function with sample invoice data
     * For testing printer while backend engineer creates invoice API
     */
    fun testPrintInvoice() {
        viewModelScope.launch {
            try {
                Log.d("OrderViewModel", "========================================")
                Log.d("OrderViewModel", "Starting TEST print with sample invoice")
                Log.d("OrderViewModel", "========================================")

                _uiState.value = _uiState.value.copy(
                    isPrinting = true,
                    printStatus = "Testing printer with sample invoice..."
                )

                // Sample invoice text (same as from S3)
                // Logo made narrower to fit 80mm paper
                val testInvoiceText = """
████████████████  ████ ████  ████     ████
██████████  ████  ████ ████  ████    ██████
████████    ████  ████ ████  ████   ████████
██████      ████  ████ ████  ████  ██████████
█████       ████  ████ ████  ████  ████  ████
██████    ██████  ████ ████  ████  ████  ████
██████      ████  ████ ████  ████  ████  ████
████  █     ████  ████ █████ █████ ██████████
█████ ███   ████  ████ █████ █████ ████  ████
████████████████  ████ █████ █████ ████  ████


                           إلى لخدمات النقل الذكية
                               رقم السجل: 4763
                       رقم التسجيل الضريبي: 562-062-645
                           فاتورة مبيعات ضريبة نسخة

                          تاريخ الاصدار: 2026-01-09
            اORD-b20ed851-102a-483a-962a-21140f5c2f0b :رقم الفاتورة
                                رمز الخطة: 13

                          اSmart Gadgets Shop :العميل
                         اMER-2026-7326397 :رمز العميل
                        ا321 Nasr City, Cairo :العنوان

                           اSeif Fayez :اسم المندوب
                              اSA005 :كود المندوب

الوصف | كمية |    سعر  | النهائي | الإجمالي
 129.99 | 129.99  |  129.99 |  1   | Wireless Earbuds Pro   


عدد الاصناف: 1.00
اEGP 129.99 :(المجموع الفرعي (دون الاضافات)
اEGP 0.00 :قيمة الضريبة والخصم

اEGP 129.99 :الإجمالي

           سلمت البضاعة بحالة جيدة - الشركة غير مسئولة عن أي توالف
                             شكرا لتعاملكم معنا!
                """.trimIndent()

                Log.d("OrderViewModel", "Test invoice text length: ${testInvoiceText.length} characters")

                _uiState.value = _uiState.value.copy(printStatus = "Formatting test invoice...")

                // Format as CPCL ByteArray with proper encoding
                val cpclFormattedBytes = CpclInvoiceFormatter.formatInvoiceAsCpclBytes(testInvoiceText)
                Log.d("OrderViewModel", "Formatted test invoice as CPCL ByteArray, size: ${cpclFormattedBytes.size} bytes")

                _uiState.value = _uiState.value.copy(printStatus = "Printing test invoice...")

                // Connect if needed
                val connectResult = if (!printerManager.isConnected()) {
                    printerManager.connect()
                } else {
                    Result.success("Already connected")
                }

                val printResult = if (connectResult.isSuccess) {
                    printerManager.printBytes(cpclFormattedBytes)
                } else {
                    connectResult
                }

                if (printResult.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        printStatus = "Test invoice printed successfully!"
                    )
                    Log.d("OrderViewModel", "Test invoice printed successfully!")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        printStatus = "Print failed: ${printResult.exceptionOrNull()?.message}"
                    )
                    Log.e("OrderViewModel", "Test print failed: ${printResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
                    printStatus = "Error: ${e.message}"
                )
                Log.e("OrderViewModel", "Exception during test print", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        printerManager.disconnect()
    }
}