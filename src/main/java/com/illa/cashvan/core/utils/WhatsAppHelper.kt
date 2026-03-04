package com.illa.cashvan.core.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

object WhatsAppHelper {

    private const val DEFAULT_COUNTRY_CODE = "20"
    private const val WHATSAPP_PACKAGE = "com.whatsapp"
    private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"

    fun formatPhoneForWhatsApp(phoneNumber: String): String {
        val cleaned = phoneNumber.replace(Regex("[^0-9+]"), "")

        val withoutPlus = cleaned.removePrefix("+")

        return when {
            withoutPlus.startsWith(DEFAULT_COUNTRY_CODE) -> withoutPlus
            withoutPlus.startsWith("0") -> DEFAULT_COUNTRY_CODE + withoutPlus.substring(1)
            withoutPlus.length == 10 -> DEFAULT_COUNTRY_CODE + withoutPlus
            else -> withoutPlus
        }
    }

    fun isWhatsAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(WHATSAPP_PACKAGE, 0)
            true
        } catch (e: Exception) {
            try {
                context.packageManager.getPackageInfo(WHATSAPP_BUSINESS_PACKAGE, 0)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    fun getInstalledWhatsAppPackage(context: Context): String? {
        return try {
            context.packageManager.getPackageInfo(WHATSAPP_PACKAGE, 0)
            WHATSAPP_PACKAGE
        } catch (e: Exception) {
            try {
                context.packageManager.getPackageInfo(WHATSAPP_BUSINESS_PACKAGE, 0)
                WHATSAPP_BUSINESS_PACKAGE
            } catch (e2: Exception) {
                null
            }
        }
    }

    fun openChat(context: Context, phoneNumber: String, message: String? = null): Result<Unit> {
        return try {
            val formattedPhone = formatPhoneForWhatsApp(phoneNumber)

            val url = if (message != null) {
                val encodedMessage = URLEncoder.encode(message, "UTF-8")
                "https://wa.me/$formattedPhone?text=$encodedMessage"
            } else {
                "https://wa.me/$formattedPhone"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: ActivityNotFoundException) {
            Result.failure(WhatsAppNotInstalledException())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareFile(context: Context, fileUri: Uri, message: String? = null): Result<Unit> {
        return try {
            val whatsAppPackage = getInstalledWhatsAppPackage(context)
                ?: return Result.failure(WhatsAppNotInstalledException())

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                if (message != null) {
                    putExtra(Intent.EXTRA_TEXT, message)
                }
                setPackage(whatsAppPackage)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: ActivityNotFoundException) {
            Result.failure(WhatsAppNotInstalledException())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun sendInvoiceToMerchant(
        context: Context,
        fileUri: Uri,
        phoneNumber: String,
        invoiceCode: String
    ): SendResult {
        val message = "فاتورة رقم: $invoiceCode"
        val formattedPhone = formatPhoneForWhatsApp(phoneNumber)

        try {
            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_TEXT, message)
                data = Uri.parse("whatsapp://send?phone=$formattedPhone&text=$encodedMessage")
                setPackage(WHATSAPP_PACKAGE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            return SendResult.ChatOpened(message = "تم فتح واتساب مع الفاتورة")
        } catch (e: ActivityNotFoundException) {
            try {
                val encodedMessage = URLEncoder.encode(message, "UTF-8")
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_TEXT, message)
                    data = Uri.parse("whatsapp://send?phone=$formattedPhone&text=$encodedMessage")
                    setPackage(WHATSAPP_BUSINESS_PACKAGE)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(intent)
                return SendResult.ChatOpened(message = "تم فتح واتساب مع الفاتورة")
            } catch (e2: ActivityNotFoundException) {
            }
        } catch (e: Exception) {
        }

        try {
            val whatsAppPackage = getInstalledWhatsAppPackage(context)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_TEXT, message)
                putExtra("phone", formattedPhone)
                putExtra("jid", "$formattedPhone@s.whatsapp.net")
                setPackage(whatsAppPackage)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            return SendResult.ChatOpened(message = "تم فتح واتساب مع الفاتورة - اختر التاجر")
        } catch (e: Exception) {
        }

        try {
            val whatsAppPackage = getInstalledWhatsAppPackage(context)
                ?: return SendResult.Failure("تطبيق واتساب غير مثبت")

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_TEXT, message)
                setPackage(whatsAppPackage)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            return SendResult.ChatOpened(message = "اختر محادثة التاجر من القائمة")
        } catch (e: Exception) {
            return when (e) {
                is ActivityNotFoundException -> SendResult.Failure("تطبيق واتساب غير مثبت")
                else -> SendResult.Failure("فشل في فتح واتساب: ${e.message}")
            }
        }
    }

    sealed class SendResult {
        data class ChatOpened(val message: String) : SendResult()
        data class Failure(val error: String) : SendResult()
    }

    class WhatsAppNotInstalledException : Exception("WhatsApp is not installed")
}
