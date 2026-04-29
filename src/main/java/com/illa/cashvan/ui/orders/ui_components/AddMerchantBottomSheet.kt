package com.illa.cashvan.ui.orders.ui_components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Refresh
import com.illa.cashvan.core.location.LocationPermissionHandler
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

    val merchantUiState by merchantViewModel.uiState.collectAsStateWithLifecycle()
    val locationUiState by locationViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(merchantUiState.isSuccess) {
        if (merchantUiState.isSuccess) {
            onMerchantCreated()
            merchantViewModel.resetState()
        }
    }

    val scrollState = rememberScrollState()

    LocationPermissionHandler(
        onPermissionGranted = {
            locationViewModel.onPermissionGranted()
        },
        onPermissionDenied = {
            locationViewModel.onPermissionDenied()
        }
    ) { requestPermission, isPermissionGranted, _ ->
        val context = LocalContext.current
        var showLocationDialog by remember { mutableStateOf(false) }

        LaunchedEffect(isPermissionGranted, locationUiState.error) {
            if (!isPermissionGranted || locationUiState.error != null) {
                showLocationDialog = true
            }
        }

        if (showLocationDialog && (!isPermissionGranted || locationUiState.error != null)) {
            AlertDialog(
                onDismissRequest = { showLocationDialog = false },
                title = {
                    Text(
                        text = "تحديد الموقع مطلوب",
                        fontFamily = FontFamily(Font(R.font.zain_bold)),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = if (!isPermissionGranted)
                            "يجب السماح بالوصول للموقع لإضافة التاجر. يرجى تفعيل صلاحية الموقع."
                        else
                            "تعذر تحديد موقعك. تأكد من تفعيل خدمة الموقع في جهازك وحاول مرة أخرى.",
                        fontFamily = FontFamily(Font(R.font.zain_regular))
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (!isPermissionGranted) {
                                requestPermission()
                            } else {
                                locationViewModel.getCurrentLocation()
                            }
                            showLocationDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.primary)
                        )
                    ) {
                        Text(
                            text = if (!isPermissionGranted) "السماح" else "إعادة المحاولة",
                            fontFamily = FontFamily(Font(R.font.zain_regular)),
                            color = Color.White
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            val intent = if (isPermissionGranted && locationUiState.error != null) {
                                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            } else {
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            }
                            context.startActivity(intent)
                            showLocationDialog = false
                        }
                    ) {
                        Text(
                            text = "فتح الإعدادات",
                            fontFamily = FontFamily(Font(R.font.zain_regular)),
                            color = colorResource(R.color.primary)
                        )
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
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
                    tint = if (locationUiState.locationData != null) colorResource(R.color.primary) else Color.Red,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "هنحط مكانك تلقائيا",
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.zain_light)),
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    when {
                        !isPermissionGranted -> {
                            Text(
                                text = "يجب السماح بالوصول للموقع",
                                fontSize = 12.sp,
                                color = Color.Red
                            )
                        }
                        locationUiState.isLoading -> {
                            Text(
                                text = "جاري تحديد الموقع...",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        locationUiState.error != null -> {
                            Text(
                                text = locationUiState.error.orEmpty(),
                                fontSize = 12.sp,
                                color = Color.Red
                            )
                        }
                        locationUiState.locationData != null -> {
                            Text(
                                text = "${locationUiState.locationData?.latitude}, ${locationUiState.locationData?.longitude}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                if (!isPermissionGranted) {
                    Button(
                        onClick = { requestPermission() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.primary)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "السماح",
                            fontSize = 12.sp,
                            fontFamily = FontFamily(Font(R.font.zain_regular)),
                            color = Color.White
                        )
                    }
                } else if (locationUiState.error != null && !locationUiState.isLoading) {
                    IconButton(
                        onClick = { locationViewModel.getCurrentLocation() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = colorResource(R.color.primary)
                        )
                    }
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
                            signName = merchantName,
                            phoneNumber = phoneNumber,
                            secondaryPhoneNumber = null,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            planId = merchantViewModel.getFirstPlanId() ?: 0,
                            merchantTypeId = "",
                            detailedAddress = "",
                            priceTier = "retail",
                            visitDays = emptySet()
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

            Spacer(modifier = Modifier.height(16.dp))
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