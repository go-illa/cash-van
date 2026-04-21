package com.illa.cashvan.feature.orders.presentation.viewmodel

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
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
import com.illa.cashvan.feature.orders.domain.usecase.GetCashVanProductTotalPriceUseCase
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
    val totalPrice: Double,
    val vatPercentage: Double = 0.0
)

data class OrderDetailsUiState(
    val isLoading: Boolean = false,
    val order: Order? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val isEditMode: Boolean = false,
    val editedQuantities: Map<String, Int> = emptyMap(),
    val deletedProductIds: Set<String> = emptySet(),
    val isUpdatingPrice: Boolean = false,
    val productPrices: Map<String, ProductPriceInfo> = emptyMap(),
    val loadingPriceForProducts: Set<String> = emptySet(),
    val rebateValue: String = ""
)

class OrderViewModel(
    private val context: Context,
    private val getOrdersUseCase: GetOrdersUseCase,
    private val getOrderByIdUseCase: GetOrderByIdUseCase,
    private val updateOrderUseCase: UpdateOrderUseCase,
    private val getInvoiceContentUseCase: GetInvoiceContentUseCase,
    private val getProductTotalPriceUseCase: GetProductTotalPriceUseCase,
    private val getCashVanProductTotalPriceUseCase: GetCashVanProductTotalPriceUseCase
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

    fun loadOrders(orderType: OrderType = _uiState.value.selectedTab) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

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
            _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
                isLoading = true,
                error = null,
                isEditMode = false,
                editedQuantities = emptyMap(),
                deletedProductIds = emptySet(),
                rebateValue = ""
            )

            when (val result = getOrderByIdUseCase(orderId)) {
                is ApiResult.Success -> {
                    _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
                        isLoading = false,
                        order = result.data
                    )
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
                    loadOrders()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun updateRebateValue(value: String) {
        if (value.isEmpty() || value.toDoubleOrNull() != null) {
            _orderDetailsUiState.value = _orderDetailsUiState.value.copy(rebateValue = value)
        }
    }

    fun submitOrder(order: Order, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val orderItems = order.order_plan_products?.map { planProduct ->
                com.illa.cashvan.feature.orders.data.model.SubmitOrderItem(
                    plan_product_id = planProduct.plan_product_id ?: "",
                    sold_quantity = planProduct.sold_quantity
                )
            } ?: emptyList()

            val request = UpdateOrderRequest(
                order = UpdateOrderData(
                    status = "submitted",
                    rebate_value = _orderDetailsUiState.value.rebateValue.toDoubleOrNull(),
                    order_items = orderItems
                )
            )

            when (val result = updateOrderUseCase(order.id, request)) {
                is ApiResult.Success -> {
                    onSuccess()
                    printInvoice(order.id)
                    loadOrders()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun printInvoice(orderId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isPrinting = true,
                    printStatus = "جاري تحميل الفاتورة..."
                )

                val orderResult = getOrderByIdUseCase(orderId)

                if (orderResult !is ApiResult.Success) {
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        printStatus = "فشل في تحميل الفاتورة"
                    )
                    return@launch
                }

                val invoiceAttachment = orderResult.data.invoice_attachment
                if (invoiceAttachment == null) {
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        printStatus = "لا توجد فاتورة متاحة لهذا الطلب"
                    )
                    return@launch
                }

                val invoiceUrl = invoiceAttachment.url

                _uiState.value = _uiState.value.copy(printStatus = "جاري تنزيل الفاتورة...")

                val invoiceResult = getInvoiceContentUseCase(invoiceUrl)

                if (invoiceResult !is ApiResult.Success) {
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        printStatus = "فشل في تنزيل الفاتورة"
                    )
                    return@launch
                }

                val invoiceText = invoiceResult.data

                _uiState.value = _uiState.value.copy(printStatus = "جاري تجهيز الفاتورة...")

                val cpclFormattedBytes = CpclInvoiceFormatter.formatInvoiceAsCpclBytes(invoiceText)

                _uiState.value = _uiState.value.copy(printStatus = "جاري طباعة الفاتورة...")

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

                _uiState.value = _uiState.value.copy(isPrinting = false)
                if (printResult.isSuccess) {
                    setPrintStatusWithAutoClear("تم طباعة الفاتورة بنجاح!")
                } else {
                    setPrintStatusWithAutoClear("فشل في الطباعة: ${printResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isPrinting = false)
                setPrintStatusWithAutoClear("خطأ: ${e.message}")
            }
        }
    }

    fun clearPrintStatus() {
        _uiState.value = _uiState.value.copy(printStatus = null)
    }

    private fun setPrintStatusWithAutoClear(status: String, delayMs: Long = 3000) {
        _uiState.value = _uiState.value.copy(printStatus = status)
        viewModelScope.launch {
            kotlinx.coroutines.delay(delayMs)
            _uiState.value = _uiState.value.copy(printStatus = null)
        }
    }

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

        calculateProductPrice(planProductId, newQuantity)
    }

    private fun calculateProductPrice(planProductId: String, quantity: Int) {
        viewModelScope.launch {
            val currentOrder = _orderDetailsUiState.value.order ?: return@launch

            val orderPlanProduct = currentOrder.order_plan_products?.find {
                it.plan_product_id == planProductId
            } ?: return@launch

            val productId = orderPlanProduct.plan_product_id ?: return@launch
            val planId = currentOrder.plan_id ?: return@launch

            val loadingProducts = _orderDetailsUiState.value.loadingPriceForProducts.toMutableSet()
            loadingProducts.add(planProductId)
            _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
                loadingPriceForProducts = loadingProducts
            )

            val result = if (currentOrder.order_type == "cash_van") {
                val priceId = orderPlanProduct.plan_product_price?.id ?: return@launch
                getCashVanProductTotalPriceUseCase(
                    planId = planId,
                    productId = productId,
                    merchantId = priceId,
                    quantity = quantity
                )
            } else {
                getProductTotalPriceUseCase(
                    planId = planId,
                    productId = productId,
                    orderId = currentOrder.id,
                    quantity = quantity
                )
            }

            when (result) {
                is ApiResult.Success -> {
                    val priceResponse = result.data

                    val priceInfo = if (priceResponse.unit != null && priceResponse.total != null) {
                        ProductPriceInfo(
                            basePrice = priceResponse.unit.base_price ?: 0.0,
                            finalPrice = priceResponse.unit.final_price ?: 0.0,
                            discountAmount = priceResponse.unit.discount_amount ?: 0.0,
                            vatAmount = priceResponse.unit.vat_amount ?: 0.0,
                            totalPrice = priceResponse.total.final_price ?: 0.0,
                            vatPercentage = priceResponse.vat_percentage ?: 0.0
                        )
                    } else {
                        ProductPriceInfo(
                            basePrice = priceResponse.base_price ?: 0.0,
                            finalPrice = priceResponse.final_price ?: 0.0,
                            discountAmount = priceResponse.total_discount ?: 0.0,
                            vatAmount = priceResponse.total_vat ?: 0.0,
                            totalPrice = priceResponse.total_price ?: 0.0,
                            vatPercentage = priceResponse.vat_percentage ?: 0.0
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
                }
                is ApiResult.Loading -> {}
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
                ProductPriceInfo(
                    basePrice = totalPriceDetails.unit?.base_price ?: 0.0,
                    finalPrice = totalPriceDetails.unit?.final_price ?: 0.0,
                    discountAmount = totalPriceDetails.unit?.discount_amount ?: 0.0,
                    vatAmount = totalPriceDetails.unit?.vat_amount ?: 0.0,
                    totalPrice = totalPriceDetails.total?.final_price ?: 0.0,
                    vatPercentage = totalPriceDetails.vat_percentage ?: 0.0
                )
            } else {
                val priceDetails = orderPlanProduct.plan_product_price?.price_details
                ProductPriceInfo(
                    basePrice = orderPlanProduct.plan_product_price?.base_price?.toDoubleOrNull() ?: 0.0,
                    finalPrice = priceDetails?.final_price ?: 0.0,
                    discountAmount = priceDetails?.discount_amount ?: 0.0,
                    vatAmount = priceDetails?.vat_amount ?: 0.0,
                    totalPrice = (priceDetails?.final_price ?: 0.0) * quantity,
                    vatPercentage = orderPlanProduct.plan_product_price?.vat_percentage ?: 0.0
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

            _orderDetailsUiState.value = _orderDetailsUiState.value.copy(
                isLoading = true,
                error = null
            )

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
                    loadOrderById(currentOrder.id)
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
                else -> {}
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

            val orderItems = editedQuantities
                .filter { (planProductId, _) -> planProductId !in deletedProductIds }
                .filter { (_, quantity) -> quantity > 0 }
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
                    loadOrderById(currentOrder.id)
                    exitEditMode()
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
                is ApiResult.Loading -> {}
            }
        }
    }

    fun sendInvoiceViaWhatsApp(orderId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isPrinting = true,
                    printStatus = "جاري تحميل الفاتورة..."
                )

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

                _uiState.value = _uiState.value.copy(printStatus = "جاري تنزيل الفاتورة...")

                var pdfFile = downloadPdfFile(invoiceUrl, order.formatted_code)

                if (pdfFile == null) {
                    _uiState.value = _uiState.value.copy(printStatus = "جاري تحويل الفاتورة إلى PDF...")

                    val invoiceResult = getInvoiceContentUseCase(invoiceUrl)
                    if (invoiceResult !is ApiResult.Success) {
                        _uiState.value = _uiState.value.copy(
                            isPrinting = false,
                            printStatus = "فشل في تحميل محتوى الفاتورة"
                        )
                        return@launch
                    }

                    val invoiceText = invoiceResult.data

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

                openWhatsAppWithFile(pdfFile, merchantPhone, order.formatted_code)

                _uiState.value = _uiState.value.copy(isPrinting = false)
                setPrintStatusWithAutoClear("تم فتح واتساب لإرسال الفاتورة")

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isPrinting = false)
                setPrintStatusWithAutoClear("خطأ: ${e.message}")
            }
        }
    }

    private suspend fun downloadPdfFile(url: String, orderCode: String): File? {
        return try {
            val client = HttpClient(Android)
            val response: HttpResponse = client.get(url)

            val bytes = response.readBytes()
            client.close()

            if (bytes.size < 4 || !isPdfFile(bytes)) {
                return null
            }

            val cacheDir = context.cacheDir
            val pdfFile = File(cacheDir, "invoice_${orderCode}.pdf")
            pdfFile.writeBytes(bytes)

            pdfFile
        } catch (e: Exception) {
            null
        }
    }

    private fun isPdfFile(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
               bytes[0] == 0x25.toByte() &&
               bytes[1] == 0x50.toByte() &&
               bytes[2] == 0x44.toByte() &&
               bytes[3] == 0x46.toByte()
    }

    private fun generatePdfFromText(text: String, orderCode: String): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val margin = 40f
            val lineHeight = 18f
            val fontSize = 11f

            val paint = Paint().apply {
                textSize = fontSize
                isAntiAlias = true
                color = android.graphics.Color.BLACK
                typeface = android.graphics.Typeface.MONOSPACE
            }

            val lines = text.lines()
            val linesPerPage = ((pageHeight - 2 * margin) / lineHeight).toInt()

            var pageNumber = 1
            var lineIndex = 0

            while (lineIndex < lines.size) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                var yPosition = margin

                val endLine = minOf(lineIndex + linesPerPage, lines.size)
                for (i in lineIndex until endLine) {
                    var line = lines[i]

                    if (line.isBlank()) {
                        yPosition += lineHeight
                        continue
                    }

                    val trimmedLine = line.trim()
                    val leadingSpaces = line.takeWhile { it == ' ' }.length
                    val trailingSpaces = line.takeLastWhile { it == ' ' }.length

                    line = trimmedLine

                    val textWidth = paint.measureText(line)
                    val contentWidth = pageWidth - 2 * margin

                    val xPosition = when {
                        leadingSpaces > 5 && trailingSpaces > 5 -> {
                            margin + (contentWidth - textWidth) / 2
                        }
                        line.firstOrNull()?.let { isArabic(it) } == true -> {
                            pageWidth - margin - textWidth
                        }
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

            val cacheDir = context.cacheDir
            val pdfFile = File(cacheDir, "invoice_${orderCode}.pdf")
            pdfFile.outputStream().use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()

            pdfFile
        } catch (e: Exception) {
            null
        }
    }

    private fun isArabic(char: Char): Boolean {
        val codePoint = char.code
        return codePoint in 0x0600..0x06FF ||
               codePoint in 0x0750..0x077F ||
               codePoint in 0xFB50..0xFDFF ||
               codePoint in 0xFE70..0xFEFF
    }

    private fun openWhatsAppWithFile(file: File, phoneNumber: String, orderCode: String) {
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            when (val result = WhatsAppHelper.sendInvoiceToMerchant(
                context,
                contentUri,
                phoneNumber,
                orderCode
            )) {
                is WhatsAppHelper.SendResult.ChatOpened -> {
                    setPrintStatusWithAutoClear(result.message)
                }
                is WhatsAppHelper.SendResult.Failure -> {
                    setPrintStatusWithAutoClear(result.error)
                }
            }
        } catch (e: Exception) {
            setPrintStatusWithAutoClear("فشل في فتح واتساب: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        printerManager.disconnect()
    }
}
