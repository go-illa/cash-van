package com.illa.cashvan.ui.orders.ui_components


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.illa.cashvan.R
import com.illa.cashvan.core.analytics.CashVanAnalyticsHelper
import com.illa.cashvan.data.MockData
import org.koin.compose.koinInject

data class Merchant(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val address: String = ""
)

@Composable
fun MerchantDropdownField(
    modifier: Modifier = Modifier,
    selectedMerchant: Merchant?,
    merchants: List<Merchant>,
    onMerchantSelected: (Merchant) -> Unit = {},
    placeholder: String = "ابحث بالاسم او رقم الهاتف",
    analyticsHelper: CashVanAnalyticsHelper = koinInject()
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(
                    width = 1.dp,
                    color = Color(0xFFE0E0E0),
                    shape = RoundedCornerShape(8.dp)
                )
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedMerchant?.name ?: placeholder,
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.zain_regular)),
                    color = if (selectedMerchant != null) Color.Black else Color(0xFF9E9E9E),
                    textAlign = TextAlign.End
                )

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Dropdown",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            merchants.forEach { merchant ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = merchant.name,
                            fontSize = 14.sp,
                            fontFamily = FontFamily(Font(R.font.zain_regular)),
                            color = Color.Black
                        )
                    },
                    onClick = {
                        analyticsHelper.logEvent(
                            "select_merchant",
                            mapOf("merchant_name" to merchant.name)
                        )
                        onMerchantSelected(merchant)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ChooseMerchantComponent(
    modifier: Modifier = Modifier,
    selectedMerchant: Merchant?,
    merchants: List<Merchant>,
    onMerchantSelected: (Merchant) -> Unit = {},
    onAddMerchantClick: () -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth()
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
                onClick = onAddMerchantClick,
                modifier = Modifier
                    .height(40.dp)
                    .border(width = 2.dp, color = Color(0xFF0D3773),RoundedCornerShape(size = 20.dp)),
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

        MerchantDropdownField(
            selectedMerchant = selectedMerchant,
            merchants = merchants,
            onMerchantSelected = onMerchantSelected
        )

    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
fun ChooseMerchantComponentPreview() {
    val sampleMerchants = MockData.getRandomMerchants()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .background(Color(0xFFF8F9FA))
    ) {
        ChooseMerchantComponent(
            selectedMerchant = null,
            merchants = sampleMerchants,
            onMerchantSelected = {},
            onAddMerchantClick = {}
        )
    }
}