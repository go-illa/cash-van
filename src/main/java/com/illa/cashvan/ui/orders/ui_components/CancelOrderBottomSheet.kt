package com.illa.cashvan.ui.orders.ui_components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CancelOrderBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onConfirm: (reason: String, note: String) -> Unit,
    orderNumber: String
) {
    var selectedReason by remember { mutableStateOf<String?>(null) }
    var cancellationNote by remember { mutableStateOf("") }

    val reasons = listOf(
        "شك في سلامة العبوة",
        "لا يوجد مساحة للتخزين",
        "رفض الاستلام",
        "عطل بالسيستم"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            // Title
            Text(
                text = "إلغاء الاوردر",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color(0xFF212121),
                modifier = Modifier.fillMaxWidth()
            )

            // Sad box icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background circle
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            color = Color(0xFFFFE5E5),
                            shape = RoundedCornerShape(60.dp)
                        )
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_cancel_sheet), // Add this drawable
                    contentDescription = "Cancel order",
                    modifier = Modifier.size(80.dp),
                    tint = Color(0xFFE57373)
                )
            }



            Spacer(modifier = Modifier.height(24.dp))

            // Reason selection label
            Text(
                text = "اختار السبب*",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color(0xFF212121),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Reason chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                reasons.forEach { reason ->
                    FilterChip(
                        selected = selectedReason == reason,
                        onClick = { selectedReason = reason },
                        label = {
                            Text(
                                text = reason,
                                fontFamily = FontFamily(Font(R.font.zain_regular)),
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE3F2FD),
                            selectedLabelColor = Color(0xFF0D3773),
                            containerColor = Color.White,
                            labelColor = Color(0xFF212121)
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selectedReason == reason) Color(0xFF0D3773) else Color(0xFFE0E0E0)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Notes field label
            Text(
                text = "اضافة أي ملاحظات؟*",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color(0xFF212121),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Notes text field
            OutlinedTextField(
                value = cancellationNote,
                onValueChange = { cancellationNote = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                placeholder = {
                    Text(
                        text = "اكتب ملاحظاتك هنا.....",
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFFBDBDBD)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE0E0E0),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedContainerColor = Color(0xFFFFFFFF),
                    unfocusedContainerColor = Color(0xFFFFFFFF)
                ),
                textStyle = TextStyle(
                    fontFamily = FontFamily(Font(R.font.zain_regular)),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Right
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Info message
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0x1A0D3773),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF0D3773),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "من فضلك اختار السبب،  اكتب أي ملاحظات قبل ما تكمل.",
                    fontSize = 12.sp,
                    fontFamily = FontFamily(Font(R.font.zain_regular)),
                    color = Color(0xFF0D3773),
                    modifier = Modifier,
                    textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // No, I want to cancel button (dismiss)
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF212121)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Text(
                        text = "لا، أريد إلغاء الاوردار",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily(Font(R.font.zain_regular))
                    )
                }

                // Confirm cancel button
                Button(
                    onClick = {
                        selectedReason?.let { reason ->
                            onConfirm(reason, cancellationNote)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedReason != null) Color(0xFFDC3545) else Color(0xFFE0E0E0),
                        disabledContainerColor = Color(0xFFE0E0E0),
                        disabledContentColor = Color(0xFF9E9E9E)
                    ),
                    enabled = selectedReason != null
                ) {
                    Text(
                        text = "تأكيد إلغاء الاوردار",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily(Font(R.font.zain_regular))
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}