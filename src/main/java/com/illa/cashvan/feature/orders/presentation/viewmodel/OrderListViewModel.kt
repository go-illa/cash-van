package com.illa.cashvan.feature.orders.presentation.viewmodel

import android.content.Context
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.illa.cashvan.core.getCurrentDateApiFormat
import com.illa.cashvan.core.network.model.ApiResult
import com.illa.cashvan.feature.orders.data.model.Order
import com.illa.cashvan.feature.orders.data.model.SubmitOrderItem
import com.illa.cashvan.feature.orders.data.model.UpdateOrderData
import com.illa.cashvan.feature.orders.data.model.UpdateOrderRequest
import com.illa.cashvan.feature.orders.domain.usecase.GetInvoiceContentUseCase
import com.illa.cashvan.feature.orders.domain.usecase.GetOrderByIdUseCase
import com.illa.cashvan.feature.orders.domain.usecase.GetOrdersUseCase
import com.illa.cashvan.feature.orders.domain.usecase.UpdateOrderUseCase
import com.illa.cashvan.feature.orders.util.PdfGenerator
import com.illa.cashvan.feature.printer.CpclInvoiceFormatter
import com.illa.cashvan.feature.printer.HoneywellPrinterManager
import com.illa.cashvan.core.utils.WhatsAppHelper
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class OrderListViewModel(
    private val context: Context,
    private val getOrdersUseCase: GetOrdersUseCase,
    private val getOrderByIdUseCase: GetOrderByIdUseCase,
    private val updateOrderUseCase: UpdateOrderUseCase,
    private val getInvoiceContentUseCase: GetInvoiceContentUseCase,
    private val plainHttpClient: HttpClient,
    private val printerManager: HoneywellPrinterManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()

    init {
        loadOrders(OrderType.PRE_SELL)
    }

    fun loadOrders(orderType: OrderType = _uiState.value.selectedTab) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val dateToUse = getCurrentDateApiFormat()
            when (val result = getOrdersUseCase(dateToUse, orderType.value)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    orders = result.data.data
                )
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
                is ApiResult.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
            }
        }
    }

    fun selectTab(orderType: OrderType) {
        if (_uiState.value.selectedTab != orderType) {
            _uiState.value = _uiState.value.copy(selectedTab = orderType)
            loadOrders(orderType)
        }
    }

    fun refresh() {
        loadOrders()
    }

    fun cancelOrder(
        orderId: String,
        reason: String,
        note: String,
        subReason: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val request = UpdateOrderRequest(
                order = UpdateOrderData(
                    status = "canceled",
                    cancellation_reason = reason,
                    cancellation_note = note,
                    cancellation_sub_reason = subReason
                )
            )
            when (val result = updateOrderUseCase(orderId, request)) {
                is ApiResult.Success -> {
                    onSuccess()
                    loadOrders()
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(cancellationError = result.message)
                is ApiResult.Loading -> {}
            }
        }
    }

    fun clearCancellationError() {
        _uiState.value = _uiState.value.copy(cancellationError = null)
    }

    fun submitOrder(order: Order, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val orderItems = order.order_plan_products?.map { planProduct ->
                SubmitOrderItem(
                    plan_product_id = planProduct.plan_product_id ?: "",
                    sold_quantity = planProduct.sold_quantity
                )
            } ?: emptyList()

            val request = UpdateOrderRequest(
                order = UpdateOrderData(
                    status = "submitted",
                    order_items = orderItems
                )
            )

            when (val result = updateOrderUseCase(order.id, request)) {
                is ApiResult.Success -> {
                    onSuccess()
                    printInvoice(order.id)
                    loadOrders()
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
                is ApiResult.Loading -> {}
            }
        }
    }

    fun printInvoice(orderId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isPrinting = true, printStatus = "جاري تحميل الفاتورة...")

                val orderResult = getOrderByIdUseCase(orderId)
                if (orderResult !is ApiResult.Success) {
                    _uiState.value = _uiState.value.copy(isPrinting = false, printStatus = "فشل في تحميل الفاتورة")
                    return@launch
                }

                val invoiceAttachment = orderResult.data.invoice_attachment
                if (invoiceAttachment == null) {
                    _uiState.value = _uiState.value.copy(isPrinting = false, printStatus = "لا توجد فاتورة متاحة لهذا الطلب")
                    return@launch
                }

                _uiState.value = _uiState.value.copy(printStatus = "جاري تنزيل الفاتورة...")
                val invoiceResult = getInvoiceContentUseCase(invoiceAttachment.url)
                if (invoiceResult !is ApiResult.Success) {
                    _uiState.value = _uiState.value.copy(isPrinting = false, printStatus = "فشل في تنزيل الفاتورة")
                    return@launch
                }

                _uiState.value = _uiState.value.copy(printStatus = "جاري تجهيز الفاتورة...")
                val cpclFormattedBytes = CpclInvoiceFormatter.formatInvoiceAsCpclBytes(invoiceResult.data)

                _uiState.value = _uiState.value.copy(printStatus = "جاري طباعة الفاتورة...")
                val connectResult = if (!printerManager.isConnected()) printerManager.connect()
                                    else Result.success("Already connected")

                val printResult = if (connectResult.isSuccess) printerManager.printBytes(cpclFormattedBytes)
                                  else connectResult

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

    fun sendInvoiceViaWhatsApp(orderId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isPrinting = true, printStatus = "جاري تحميل الفاتورة...")

                val orderResult = getOrderByIdUseCase(orderId)
                if (orderResult !is ApiResult.Success) {
                    _uiState.value = _uiState.value.copy(isPrinting = false, printStatus = "فشل في تحميل الفاتورة")
                    return@launch
                }

                val order = orderResult.data
                val invoiceAttachment = order.invoice_attachment
                if (invoiceAttachment == null) {
                    _uiState.value = _uiState.value.copy(isPrinting = false, printStatus = "لا توجد فاتورة متاحة لهذا الطلب")
                    return@launch
                }

                val merchantPhone = order.merchant?.phone_number
                if (merchantPhone.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(isPrinting = false, printStatus = "رقم هاتف التاجر غير متوفر")
                    return@launch
                }

                _uiState.value = _uiState.value.copy(printStatus = "جاري تنزيل الفاتورة...")
                var pdfFile = downloadPdfFile(invoiceAttachment.url, order.formatted_code)

                if (pdfFile == null) {
                    _uiState.value = _uiState.value.copy(printStatus = "جاري تحويل الفاتورة إلى PDF...")
                    val invoiceResult = getInvoiceContentUseCase(invoiceAttachment.url)
                    if (invoiceResult !is ApiResult.Success) {
                        _uiState.value = _uiState.value.copy(isPrinting = false, printStatus = "فشل في تحميل محتوى الفاتورة")
                        return@launch
                    }
                    pdfFile = PdfGenerator.generateFromText(invoiceResult.data, order.formatted_code, context)
                    if (pdfFile == null) {
                        _uiState.value = _uiState.value.copy(isPrinting = false, printStatus = "فشل في إنشاء ملف PDF")
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

    private suspend fun downloadPdfFile(url: String, orderCode: String): File? {
        return try {
            val response = plainHttpClient.get(url)
            val bytes = response.readBytes()
            if (!PdfGenerator.isPdfBytes(bytes)) return null
            val pdfFile = File(context.cacheDir, "invoice_$orderCode.pdf")
            pdfFile.writeBytes(bytes)
            pdfFile
        } catch (e: Exception) {
            null
        }
    }

    private fun openWhatsAppWithFile(file: File, phoneNumber: String, orderCode: String) {
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            when (val result = WhatsAppHelper.sendInvoiceToMerchant(context, contentUri, phoneNumber, orderCode)) {
                is WhatsAppHelper.SendResult.ChatOpened -> setPrintStatusWithAutoClear(result.message)
                is WhatsAppHelper.SendResult.Failure -> setPrintStatusWithAutoClear(result.error)
            }
        } catch (e: Exception) {
            setPrintStatusWithAutoClear("فشل في فتح واتساب: ${e.message}")
        }
    }
}
