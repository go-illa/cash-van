package com.illa.cashvan.sales.ui.order

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.illa.cashvan.sales.feature.order.presentation.viewmodel.FawryInvoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private const val MAX_INVOICE_BYTES = 20 * 1024 * 1024
private const val MAX_IMAGE_EDGE = 1600

internal sealed interface InvoiceRead {
    data class Ok(val invoice: FawryInvoice) : InvoiceRead
    data class Rejected(val message: String) : InvoiceRead
}

/** Same limits as the PWA: images or PDF up to 20 MB; images are downscaled before upload. */
internal suspend fun readInvoice(context: Context, uri: Uri): InvoiceRead = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        val type = resolver.getType(uri) ?: "application/octet-stream"
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: "invoice"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
        when {
            bytes.isEmpty() -> InvoiceRead.Rejected("الملف فارغ، يرجى اختيار ملف آخر")
            bytes.size > MAX_INVOICE_BYTES -> InvoiceRead.Rejected("حجم الملف يتجاوز 20 ميجابايت، يرجى اختيار ملف أصغر")
            type == "application/pdf" -> InvoiceRead.Ok(FawryInvoice(dataUrl(type, bytes), type, name))
            type.startsWith("image/") -> {
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: return@runCatching InvoiceRead.Rejected("نوع الملف غير مدعوم، يُسمح فقط بالصور (JPEG, PNG, WEBP, HEIC) أو PDF")
                val scale = minOf(1f, MAX_IMAGE_EDGE.toFloat() / maxOf(bitmap.width, bitmap.height))
                val scaled = if (scale < 1f) {
                    Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
                } else bitmap
                val output = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, output)
                InvoiceRead.Ok(FawryInvoice(dataUrl("image/jpeg", output.toByteArray()), "image/jpeg", name))
            }
            else -> InvoiceRead.Rejected("نوع الملف غير مدعوم، يُسمح فقط بالصور (JPEG, PNG, WEBP, HEIC) أو PDF")
        }
    }.getOrElse { InvoiceRead.Rejected("حدث خطأ أثناء معالجة الملف، يرجى المحاولة مرة أخرى") }
}

private fun dataUrl(type: String, bytes: ByteArray): String =
    "data:$type;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
