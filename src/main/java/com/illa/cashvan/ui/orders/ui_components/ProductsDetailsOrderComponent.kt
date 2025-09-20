package com.illa.cashvan.ui.orders.ui_components


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.R
import com.illa.cashvan.data.MockData

data class ProductDetails(
    val productName: String,
    val sku: String,
    val price: Double,
    val unitPrice: Double,
    val quantity: Int
)

@Composable
fun ProductsDetailsComponent(
    modifier: Modifier = Modifier,
    productDetails: ProductDetails
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = productDetails.productName,
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF1F252E)
                    )
                    Text(
                        text = "رقم التخزين : ${productDetails.sku}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF9CA3AF)
                    )
                }
                Text(
                    text = "جنيه ${String.format("%.2f", productDetails.price)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily(Font(R.font.zain_regular)),
                    color = Color(0xFF1F252E)
                )
            }


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الكمية : ${productDetails.quantity}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily(Font(R.font.zain_regular)),
                    color = Color(0xFF9CA3AF)
                )

                Text(
                    text = "سعر الوحدة ${String.format("%.2f", productDetails.unitPrice)} جنيه",
                    fontSize = 12.sp,
                    fontFamily = FontFamily(Font(R.font.zain_regular)),
                    color = Color(0xFF9CA3AF)
                )
            }
        }
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
fun ProductDetailsComponentPreview() {
    val randomProduct = MockData.getRandomProduct()
    val sampleProductDetails = ProductDetails(
        productName = randomProduct.name,
        sku = "SKU-${randomProduct.id.padStart(3, '0')}",
        price = randomProduct.price * 5,
        unitPrice = randomProduct.price,
        quantity = 5
    )

    MaterialTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .background(Color(0xFFF5F5F5)),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProductsDetailsComponent(
                productDetails = sampleProductDetails
            )
        }
    }
}