package com.illa.cashvan.core.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import java.net.URLEncoder

/**
 * Utility class for WhatsApp integration
 * Handles opening chats and sharing content via WhatsApp
 */
object WhatsAppHelper {

    private const val TAG = "WhatsAppHelper"
    private const val DEFAULT_COUNTRY_CODE = "20" // Egypt
    private const val WHATSAPP_PACKAGE = "com.whatsapp"
    private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"

    /**
     * Format phone number for WhatsApp wa.me URLs
     * Adds Egypt country code (20) if missing
     *
     * Examples:
     * - "01234567890" → "201234567890"
     * - "+201234567890" → "201234567890"
     * - "201234567890" → "201234567890"
     * - "0020 1234 5678" → "201234567890"
     */
    fun formatPhoneForWhatsApp(phoneNumber: String): String {
        // Remove all non-numeric characters except +
        val cleaned = phoneNumber.replace(Regex("[^0-9+]"), "")

        // Remove leading + if present
        val withoutPlus = cleaned.removePrefix("+")

        // Handle different formats
        return when {
            // Already has Egypt country code
            withoutPlus.startsWith(DEFAULT_COUNTRY_CODE) -> withoutPlus

            // Local format starting with 0 (e.g., 01234567890)
            withoutPlus.startsWith("0") -> DEFAULT_COUNTRY_CODE + withoutPlus.substring(1)

            // 10 digits without country code
            withoutPlus.length == 10 -> DEFAULT_COUNTRY_CODE + withoutPlus

            // Assume it's already in international format
            else -> withoutPlus
        }
    }

    /**
     * Check if WhatsApp or WhatsApp Business is installed
     */
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

    /**
     * Get the installed WhatsApp package name
     * Prefers regular WhatsApp over Business
     */
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

    /**
     * Open WhatsApp chat with a specific phone number
     * Uses WhatsApp's official wa.me URL scheme
     *
     * @param context Android context
     * @param phoneNumber Merchant's phone number (any format)
     * @param message Optional pre-filled message
     * @return Result.success if WhatsApp opened, Result.failure otherwise
     */
    fun openChat(context: Context, phoneNumber: String, message: String? = null): Result<Unit> {
        return try {
            val formattedPhone = formatPhoneForWhatsApp(phoneNumber)
            Log.d(TAG, "Opening WhatsApp chat for: $formattedPhone")

            // Build wa.me URL
            val url = if (message != null) {
                val encodedMessage = URLEncoder.encode(message, "UTF-8")
                "https://wa.me/$formattedPhone?text=$encodedMessage"
            } else {
                "https://wa.me/$formattedPhone"
            }

            Log.d(TAG, "WhatsApp URL: $url")

            // Create intent to open WhatsApp
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            Log.d(TAG, "WhatsApp chat opened successfully")
            Result.success(Unit)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "WhatsApp not installed", e)
            Result.failure(WhatsAppNotInstalledException())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open WhatsApp chat", e)
            Result.failure(e)
        }
    }

    /**
     * Share a file via WhatsApp
     * Opens WhatsApp with the file ready to send
     *
     * @param context Android context
     * @param fileUri URI of the file to share (must have proper permissions)
     * @param message Optional message to include with the file
     * @return Result.success if WhatsApp opened, Result.failure otherwise
     */
    fun shareFile(context: Context, fileUri: Uri, message: String? = null): Result<Unit> {
        return try {
            val whatsAppPackage = getInstalledWhatsAppPackage(context)
                ?: return Result.failure(WhatsAppNotInstalledException())

            Log.d(TAG, "Sharing file via $whatsAppPackage")

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
            Log.d(TAG, "File share intent sent successfully")
            Result.success(Unit)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "WhatsApp not installed", e)
            Result.failure(WhatsAppNotInstalledException())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share file via WhatsApp", e)
            Result.failure(e)
        }
    }

    /**
     * Send invoice to merchant via WhatsApp with file attached
     * Tries multiple methods to attach file directly to specific chat
     *
     * @param context Android context
     * @param fileUri URI of the PDF invoice
     * @param phoneNumber Merchant's phone number
     * @param invoiceCode Invoice code for the message
     * @return Result indicating success or failure
     */
    fun sendInvoiceToMerchant(
        context: Context,
        fileUri: Uri,
        phoneNumber: String,
        invoiceCode: String
    ): SendResult {
        val message = "فاتورة رقم: $invoiceCode"
        val formattedPhone = formatPhoneForWhatsApp(phoneNumber)

        Log.d(TAG, "Attempting to send file to WhatsApp chat: $formattedPhone")

        // Strategy 1: Try ACTION_SEND with whatsapp:// URI (most direct)
        try {
            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_TEXT, message)
                // Try to specify the phone number in the data URI
                data = Uri.parse("whatsapp://send?phone=$formattedPhone&text=$encodedMessage")
                setPackage(WHATSAPP_PACKAGE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            Log.d(TAG, "WhatsApp opened with ACTION_SEND + whatsapp:// data")
            return SendResult.ChatOpened(message = "تم فتح واتساب مع الفاتورة")
        } catch (e: ActivityNotFoundException) {
            Log.d(TAG, "Strategy 1 failed, trying WhatsApp Business")

            // Try WhatsApp Business
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
                Log.d(TAG, "WhatsApp Business opened with ACTION_SEND")
                return SendResult.ChatOpened(message = "تم فتح واتساب مع الفاتورة")
            } catch (e2: ActivityNotFoundException) {
                Log.d(TAG, "WhatsApp Business also failed, trying Strategy 2")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Strategy 1 error: ${e.message}")
        }

        // Strategy 2: Try with explicit phone in EXTRA
        try {
            val whatsAppPackage = getInstalledWhatsAppPackage(context)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_TEXT, message)
                putExtra("phone", formattedPhone) // Try adding phone as extra
                putExtra("jid", "$formattedPhone@s.whatsapp.net") // WhatsApp internal ID
                setPackage(whatsAppPackage)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            Log.d(TAG, "WhatsApp opened with phone extras")
            return SendResult.ChatOpened(message = "تم فتح واتساب مع الفاتورة - اختر التاجر")
        } catch (e: Exception) {
            Log.d(TAG, "Strategy 2 failed: ${e.message}")
        }

        // Strategy 3: Use standard share with WhatsApp package
        // This will show WhatsApp's internal contact picker
        // Note: WhatsApp API doesn't support pre-selecting a contact with file attachment
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
            Log.d(TAG, "WhatsApp opened with file ready to share")
            // Clear instruction for user - they need to search for merchant name
            return SendResult.ChatOpened(message = "اختر محادثة التاجر من القائمة")
        } catch (e: Exception) {
            Log.e(TAG, "All strategies failed", e)
            return when (e) {
                is ActivityNotFoundException -> SendResult.Failure("تطبيق واتساب غير مثبت")
                else -> SendResult.Failure("فشل في فتح واتساب: ${e.message}")
            }
        }
    }

    /**
     * Result of sending invoice operation
     */
    sealed class SendResult {
        data class ChatOpened(val message: String) : SendResult()
        data class Failure(val error: String) : SendResult()
    }

    /**
     * Exception thrown when WhatsApp is not installed
     */
    class WhatsAppNotInstalledException : Exception("WhatsApp is not installed")
}
