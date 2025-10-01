package com.illa.cashvan.ui.orders.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.R
import com.illa.cashvan.core.analytics.CashVanAnalyticsHelper
import com.illa.cashvan.core.location.LocationViewModel
import com.illa.cashvan.feature.merchant.presentation.viewmodel.MerchantViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMerchantBottomSheet(
    onDismiss: () -> Unit = {},
    onMerchantCreated: () -> Unit = {},
    merchantViewModel: MerchantViewModel = koinViewModel(),
    locationViewModel: LocationViewModel = koinViewModel(),
    analyticsHelper: CashVanAnalyticsHelper = koinInject()
) {
    var merchantName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }

    val merchantUiState by merchantViewModel.uiState.collectAsState()
    val locationUiState by locationViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        locationViewModel.getCurrentLocation()
    }

    LaunchedEffect(merchantUiState.isSuccess) {
        if (merchantUiState.isSuccess) {
            onMerchantCreated()
            merchantViewModel.resetState()
        }
    }

    val displayCoordinates = locationUiState.locationData?.let {
        "${it.latitude}, ${it.longitude}"
    } ?: "Fetching location..."

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "اضافة تاجر",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color.Black
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = merchantName,
            onValueChange = { merchantName = it },
            label = {
                Text(
                    text = "اضافة اسم التاجر",
                    color = Color.Black,
                    fontSize = 14.sp
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Gray,
                unfocusedBorderColor = Color.LightGray,
                focusedLabelColor = Color.Gray,
                unfocusedLabelColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = {
                Text(
                    text = "اضافة رقم الهاتف",
                    color = Color.Black,
                    fontSize = 14.sp
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Gray,
                unfocusedBorderColor = Color.LightGray,
                focusedLabelColor = Color.Gray,
                unfocusedLabelColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location",
                tint = colorResource(R.color.primary),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = "هنحط مكانك تلقائيا",
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.zain_light)),
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Text(
                    text = displayCoordinates,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        merchantUiState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Button(
            onClick = {
                locationUiState.locationData?.let { location ->
                    analyticsHelper.logEvent(
                        "add_merchant",
                        mapOf(
                            "merchant_name" to merchantName,
                            "merchant_phone" to phoneNumber
                        )
                    )
                    merchantViewModel.createMerchant(
                        name = merchantName,
                        phoneNumber = phoneNumber,
                        latitude = location.latitude.toString(),
                        longitude = location.longitude.toString(),
                        planId = merchantViewModel.getFirstPlanId() ?: "2"
                    )
                }
            },
            enabled = merchantName.isNotBlank() && phoneNumber.isNotBlank() &&
                     locationUiState.locationData != null && !merchantUiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.primary)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (merchantUiState.isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = "اضافة التاجر",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily(Font(R.font.zain_bold)),
                    color = Color.White
                )
            }
        }
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
fun AddMerchantBottomSheetPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color(0x40000000)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White
            ) {
                AddMerchantBottomSheet()
            }
        }
    }
}