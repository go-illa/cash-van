package com.illa.cashvan.data

import com.illa.cashvan.ui.orders.ui_components.Merchant
import com.illa.cashvan.ui.orders.ui_components.OrderElement
import com.illa.cashvan.ui.orders.ui_components.OrderItem
import com.illa.cashvan.ui.orders.ui_components.OrderSpecs
import com.illa.cashvan.ui.orders.ui_components.PaymentSummary
import com.illa.cashvan.ui.orders.ui_components.Product

object MockData {

    // Mock Merchants
    val merchants = listOf(
        Merchant(
            id = "1",
            name = "كورنر ماركت بلس",
            phoneNumber = "+201234567890",
            address = "شارع النصر، القاهرة الجديدة، القاهرة"
        ),
        Merchant(
            id = "2",
            name = "سوبر ماركت الأمين",
            phoneNumber = "+201987654321",
            address = "شارع الجمهورية، الجيزة"
        ),
        Merchant(
            id = "3",
            name = "بقالة أبو أحمد",
            phoneNumber = "+201555123456",
            address = "ميدان التحرير، وسط القاهرة"
        ),
        Merchant(
            id = "4",
            name = "متجر النور التجاري",
            phoneNumber = "+201122334455",
            address = "شارع المعز، القاهرة القديمة"
        ),
        Merchant(
            id = "5",
            name = "مول الأسرة",
            phoneNumber = "+201333222111",
            address = "كورنيش النيل، المعادي"
        )
    )

    // Mock Products
    val products = listOf(
        Product(id = "1", name = "شاي ليبتون أحمر", price = 25.50, unit = "علبة"),
        Product(id = "2", name = "قهوة نسكافيه جولد", price = 45.00, unit = "علبة"),
        Product(id = "3", name = "سكر أبيض فاخر", price = 12.75, unit = "كيلو"),
        Product(id = "4", name = "أرز مصري درجة أولى", price = 18.00, unit = "كيلو"),
        Product(id = "5", name = "زيت دوار الشمس", price = 35.00, unit = "لتر"),
        Product(id = "6", name = "مكرونة إيطالية", price = 8.50, unit = "علبة"),
        Product(id = "7", name = "صوص طماطم", price = 6.75, unit = "علبة"),
        Product(id = "8", name = "جبنة رومي", price = 85.00, unit = "كيلو"),
        Product(id = "9", name = "لبن طازج", price = 15.00, unit = "لتر"),
        Product(id = "10", name = "خبز عيش بلدي", price = 1.50, unit = "رغيف"),
        Product(id = "11", name = "بيض بلدي", price = 3.00, unit = "بيضة"),
        Product(id = "12", name = "دجاج مجمد", price = 45.00, unit = "كيلو"),
        Product(id = "13", name = "لحمة بقري", price = 180.00, unit = "كيلو"),
        Product(id = "14", name = "سمك بلطي", price = 25.00, unit = "كيلو"),
        Product(id = "15", name = "بطاطس", price = 5.00, unit = "كيلو")
    )

    // Mock Order Elements
    val orderElements = listOf(
        OrderElement(
            id = "1",
            name = "شاي ليبتون أحمر",
            description = "25.50 جنيه/علبة",
            price = 51.00,
            quantity = 2,
            unit = "جنيه"
        ),
        OrderElement(
            id = "2",
            name = "قهوة نسكافيه جولد",
            description = "45.00 جنيه/علبة",
            price = 135.00,
            quantity = 3,
            unit = "جنيه"
        ),
        OrderElement(
            id = "3",
            name = "سكر أبيض فاخر",
            description = "12.75 جنيه/كيلو",
            price = 25.50,
            quantity = 2,
            unit = "جنيه"
        ),
        OrderElement(
            id = "4",
            name = "أرز مصري درجة أولى",
            description = "18.00 جنيه/كيلو",
            price = 90.00,
            quantity = 5,
            unit = "جنيه"
        ),
        OrderElement(
            id = "5",
            name = "زيت دوار الشمس",
            description = "35.00 جنيه/لتر",
            price = 70.00,
            quantity = 2,
            unit = "جنيه"
        )
    )

    // Mock Order Items
    val orderItems = listOf(
        OrderItem(
            id = "1",
            orderNumber = "ORD-2024-001",
            merchantName = "كورنر ماركت بلس",
            phoneNumber = "+201234567890",
            totalAmount = 1250.50,
            itemsCount = 5,
            date = "2024-01-15"
        ),
        OrderItem(
            id = "2",
            orderNumber = "ORD-2024-002",
            merchantName = "سوبر ماركت الأمين",
            phoneNumber = "+201987654321",
            totalAmount = 875.00,
            itemsCount = 3,
            date = "2024-01-16"
        ),
        OrderItem(
            id = "3",
            orderNumber = "ORD-2024-003",
            merchantName = "بقالة أبو أحمد",
            phoneNumber = "+201555123456",
            totalAmount = 2100.75,
            itemsCount = 8,
            date = "2024-01-14"
        ),
        OrderItem(
            id = "4",
            orderNumber = "ORD-2024-004",
            merchantName = "متجر النور التجاري",
            phoneNumber = "+201122334455",
            totalAmount = 650.25,
            itemsCount = 2,
            date = "2024-01-13"
        ),
        OrderItem(
            id = "5",
            orderNumber = "ORD-2024-005",
            merchantName = "مول الأسرة",
            phoneNumber = "+201333222111",
            totalAmount = 1800.00,
            itemsCount = 6,
            date = "2024-01-17"
        )
    )

    // Mock Order Specs
    val orderSpecs = listOf(
        OrderSpecs(
            orderNumber = "ORD-2024-001",
            orderDate = "2024-01-26",
            orderTime = "14:30"
        ),
        OrderSpecs(
            orderNumber = "ORD-2024-002",
            orderDate = "2024-01-25",
            orderTime = "10:15"
        ),
        OrderSpecs(
            orderNumber = "ORD-2024-003",
            orderDate = "2024-01-24",
            orderTime = "16:45"
        ),
        OrderSpecs(
            orderNumber = "ORD-2024-004",
            orderDate = "2024-01-23",
            orderTime = "09:30"
        ),
        OrderSpecs(
            orderNumber = "ORD-2024-005",
            orderDate = "2024-01-22",
            orderTime = "13:20"
        )
    )

    // Mock Payment Summary
    val paymentSummaries = listOf(
        PaymentSummary(
            total = 838.20
        ),
    )

    // Helper functions to get random data
    fun getRandomMerchant() = merchants.random()
    fun getRandomProduct() = products.random()
    fun getRandomOrderElement() = orderElements.random()
    fun getRandomOrderItem() = orderItems.random()
    fun getRandomOrderSpecs() = orderSpecs.random()
    fun getRandomPaymentSummary() = paymentSummaries.random()

    // Get subsets of data
    fun getRandomMerchants(count: Int = 3) = merchants.shuffled().take(count)
    fun getRandomProducts(count: Int = 5) = products.shuffled().take(count)
    fun getRandomOrderElements(count: Int = 3) = orderElements.shuffled().take(count)
    fun getRandomOrderItems(count: Int = 5) = orderItems.shuffled().take(count)

    // Sample complete order data
    fun getSampleCompleteOrder() = CompleteOrderData(
        merchant = merchants.first(),
        orderElements = orderElements.take(3),
        orderSpecs = orderSpecs.first(),
        paymentSummary = paymentSummaries.first()
    )
}

data class CompleteOrderData(
    val merchant: Merchant,
    val orderElements: List<OrderElement>,
    val orderSpecs: OrderSpecs,
    val paymentSummary: PaymentSummary
)