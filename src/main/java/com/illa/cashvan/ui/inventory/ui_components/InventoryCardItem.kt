package com.illa.cashvan.ui.inventory.ui_components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.R


data class InventoryItem(
    val id: String,
    val name: String,
    val code: String,
    val totalQuantity: Int,
    val availableQuantity: Int,
    val soldQuantity: Int,
    val progressPercentage: Float
)

@Composable
fun InventoryCard(
    item: InventoryItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(width = 2.dp, color = Color(0xFFE1E7EF), shape = RoundedCornerShape(size = 8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),

        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)

        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {

                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = item.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF1F252E),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "${stringResource(R.string.product_code)}: ${item.code}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF1F252E),
                        textAlign = TextAlign.End
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddBox,
                        contentDescription = null,
                        tint = Color(0xFF0D3773),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.cartoon_label),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D3773)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column {
                Text(
                    text = "${item.progressPercentage.toInt()}%",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                progress = { item.progressPercentage / 100f },
                modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF0D3773),
                trackColor = Color(0xFFE3F2FD),
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatisticItem(
                    label = stringResource(R.string.total_label),
                    value = item.totalQuantity.toString(),
                    valueColor = Color(0xFF1F252E)
                )

                StatisticItem(
                    label = stringResource(R.string.available_label),
                    value = item.availableQuantity.toString(),
                    valueColor = Color(0xFF16A249)
                )

                StatisticItem(
                    label = stringResource(R.string.sold_label),
                    value = item.soldQuantity.toString(),
                    valueColor = Color(0xFFEE7C2B)
                )
            }
        }
    }
}

@Composable
fun StatisticItem(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF65758B),
            fontFamily = FontFamily(Font(R.font.zain_regular)),
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            textAlign = TextAlign.Center
        )
    }
}
@Preview(showBackground = true, locale = "ar")
@Composable
fun InventoryCardArabicPreview() {
    InventoryCard(
        item = InventoryItem(
            id = "1",
            name = "Coca Cola Classic 12pk",
            code = "CC001",
            totalQuantity = 24,
            availableQuantity = 18,
            soldQuantity = 6,
            progressPercentage = 75f
        )
    )
}
