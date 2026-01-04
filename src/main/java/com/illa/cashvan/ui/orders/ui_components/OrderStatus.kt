package com.illa.cashvan.ui.orders.ui_components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents different order statuses with their Arabic labels and colors
 */
enum class OrderStatus(
    val apiValue: String,
    val arabicLabel: String,
    val color: Color,
    val backgroundColor: Color,
    val icon: ImageVector
) {
    FULFILLED(
        apiValue = "fulfilled",
        arabicLabel = "اكتمل الطلب",
        color = Color(0xFF16A249),
        backgroundColor = Color(0x1A16A249),
        icon = Icons.Default.CheckBox
    ),
    ONGOING(
        apiValue = "ongoing",
        arabicLabel = "شغال",
        color = Color(0xFFFF9800),
        backgroundColor = Color(0x1AFF9800),
        icon = Icons.Default.HourglassEmpty
    ),
    CANCELED(
        apiValue = "canceled",
        arabicLabel = "ملغي",
        color = Color(0xFFDC3545),
        backgroundColor = Color(0x1ADC3545),
        icon = Icons.Default.Cancel
    ),
    PARTIALLY_FULFILLED(
        apiValue = "partially_fulfilled",
        arabicLabel = "اكتمل جزئيا",
        color = Color(0xFF2196F3),
        backgroundColor = Color(0x1A2196F3),
        icon = Icons.Default.CheckCircle
    );

    companion object {
        /**
         * Get OrderStatus from API status string value
         * Returns ONGOING as default if status is unknown or null
         * Handles both "fulfilled" and "completed" as the same status
         */
        fun fromApiValue(status: String?): OrderStatus {
            return when (status?.lowercase()) {
                "fulfilled", "completed" -> FULFILLED
                "ongoing" -> ONGOING
                "canceled" -> CANCELED
                "partially_fulfilled" -> PARTIALLY_FULFILLED
                else -> ONGOING // Default to ongoing for unknown statuses
            }
        }
    }
}
