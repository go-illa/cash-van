package com.illa.cashvan.ui.merchant

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.R
import com.illa.cashvan.core.analytics.CashVanAnalyticsHelper
import com.illa.cashvan.core.location.LocationPermissionHandler
import com.illa.cashvan.core.location.LocationViewModel
import com.illa.cashvan.feature.merchant.data.model.Governorate
import com.illa.cashvan.feature.merchant.data.model.MerchantType
import com.illa.cashvan.feature.merchant.presentation.viewmodel.MerchantViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private val primaryColor = Color(0xFF0D3773)
private val borderColor = Color(0x23003366)
private val hintColor = Color(0xFFBDBDBD)
private val requiredStarColor = Color(0xFFE23636)
private val cardBorderColor = Color(0xFFEDEDED)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMerchantScreen(
    onBackClick: () -> Unit = {},
    onMerchantCreated: () -> Unit = {},
    merchantViewModel: MerchantViewModel = koinViewModel(),
    locationViewModel: LocationViewModel = koinViewModel(),
    analyticsHelper: CashVanAnalyticsHelper = koinInject()
) {
    var merchantName by remember { mutableStateOf("") }
    var signName by remember { mutableStateOf("") }
    var primaryPhone by remember { mutableStateOf("") }
    var secondaryPhone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedGovernorate by remember { mutableStateOf<Governorate?>(null) }
    var selectedMerchantType by remember { mutableStateOf<MerchantType?>(null) }
    var selectedPriceTier by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedVisitDays by remember { mutableStateOf(setOf<String>()) }

    val merchantUiState by merchantViewModel.uiState.collectAsStateWithLifecycle()
    val locationUiState by locationViewModel.uiState.collectAsStateWithLifecycle()

    val priceTiers = remember {
        listOf(
            "retail" to "قطاعي",
            "large_grocery" to "قطاعي كبير",
            "wholesale" to "جملة",
            "big_wholesale" to "جملة كبيرة"
        )
    }
    val visitDaysOptions = remember {
        listOf(
            "sunday" to "الأحد",
            "monday" to "الإثنين",
            "tuesday" to "الثلاثاء",
            "wednesday" to "الأربعاء",
            "thursday" to "الخميس",
            "friday" to "الجمعة",
            "saturday" to "السبت"
        )
    }

    LaunchedEffect(locationUiState.locationData) {
        locationUiState.locationData?.let { location ->
            merchantViewModel.loadReverseGeocode(
                latitude = location.latitude.toString(),
                longitude = location.longitude.toString()
            )
        }
    }

    LaunchedEffect(merchantUiState.reverseGeocodeAddress) {
        if (address.isEmpty() && !merchantUiState.reverseGeocodeAddress.isNullOrEmpty()) {
            address = merchantUiState.reverseGeocodeAddress.orEmpty()
        }
    }

    LaunchedEffect(merchantUiState.reverseGeocodeGovernorateName, merchantUiState.governorates) {
        val governorateName = merchantUiState.reverseGeocodeGovernorateName
        val governorates = merchantUiState.governorates
        if (selectedGovernorate == null && !governorateName.isNullOrEmpty() && governorates.isNotEmpty()) {
            selectedGovernorate = governorates.find {
                it.english_name?.equals(governorateName, ignoreCase = true) == true
            }
        }
    }

    LaunchedEffect(merchantUiState.isSuccess) {
        if (merchantUiState.isSuccess) {
            analyticsHelper.logEvent(
                "add_merchant",
                mapOf("merchant_name" to merchantName, "merchant_phone" to primaryPhone)
            )
            onMerchantCreated()
            merchantViewModel.resetState()
        }
    }

    val scrollState = rememberScrollState()

    LocationPermissionHandler(
        onPermissionGranted = { locationViewModel.onPermissionGranted() },
        onPermissionDenied = { locationViewModel.onPermissionDenied() }
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
                            if (!isPermissionGranted) requestPermission()
                            else locationViewModel.getCurrentLocation()
                            showLocationDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.primary))
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

        val isFormValid = merchantName.isNotBlank() &&
            signName.isNotBlank() &&
            primaryPhone.isNotBlank() &&
            address.isNotBlank() &&
            selectedGovernorate != null &&
            selectedMerchantType != null &&
            selectedPriceTier != null &&
            selectedVisitDays.isNotEmpty() &&
            locationUiState.locationData != null &&
            !merchantUiState.isLoading

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "إضافة تاجر جديد",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily(Font(R.font.zain_bold)),
                                color = Color.White
                            )
                            Text(
                                text = "املأ تفاصيل التاجر لإضافته إلى مسارك",
                                fontSize = 12.sp,
                                fontFamily = FontFamily(Font(R.font.zain_regular)),
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "العودة",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryColor)
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    merchantUiState.error?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            fontFamily = FontFamily(Font(R.font.zain_regular)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            textAlign = TextAlign.Start
                        )
                    }
                    Button(
                        onClick = {
                            locationUiState.locationData?.let { location ->
                                merchantViewModel.createMerchant(
                                    name = merchantName,
                                    signName = signName,
                                    phoneNumber = primaryPhone,
                                    secondaryPhoneNumber = secondaryPhone.takeIf { it.isNotBlank() },
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    planId = merchantViewModel.getFirstPlanId() ?: 0,
                                    merchantTypeId = selectedMerchantType?.id ?: "",
                                    detailedAddress = address,
                                    priceTier = selectedPriceTier?.first ?: "",
                                    visitDays = selectedVisitDays
                                )
                            }
                        },
                        enabled = isFormValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            disabledContainerColor = hintColor
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        if (merchantUiState.isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Text(
                                text = "إضافة التاجر",
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.zain_regular)),
                                color = Color(0xFFFAFAFA)
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MerchantSectionCard(title = "المعلومات الأساسية") {
                    MerchantFormField(
                        label = "اسم التاجر",
                        isRequired = true,
                        value = merchantName,
                        onValueChange = { merchantName = it },
                        placeholder = "أدخل اسم التاجر"
                    )
                    Spacer(Modifier.height(14.dp))
                    MerchantFormField(
                        label = "اسم اللافتة",
                        isRequired = true,
                        value = signName,
                        onValueChange = { signName = it },
                        placeholder = "أدخل اسم اللافتة"
                    )
                    Spacer(Modifier.height(14.dp))
                    MerchantDropdownField(
                        label = "نوع التاجر",
                        isRequired = true,
                        selectedLabel = selectedMerchantType?.description,
                        placeholder = "اختر نوع التاجر",
                        items = merchantUiState.merchantTypes,
                        itemLabel = { it.description },
                        onItemSelected = { selectedMerchantType = it }
                    )
                    Spacer(Modifier.height(14.dp))
                    MerchantDropdownField(
                        label = "فئة السعر",
                        isRequired = true,
                        selectedLabel = selectedPriceTier?.second,
                        placeholder = "اختر فئة السعر",
                        items = priceTiers,
                        itemLabel = { it.second },
                        onItemSelected = { selectedPriceTier = it }
                    )
                }

                MerchantSectionCard(title = "معلومات الاتصال") {
                    MerchantFormField(
                        label = "رقم الهاتف الأساسي",
                        isRequired = true,
                        value = primaryPhone,
                        onValueChange = { primaryPhone = it },
                        placeholder = "أدخل رقم الهاتف",
                        keyboardType = KeyboardType.Phone
                    )
                    Spacer(Modifier.height(14.dp))
                    MerchantFormField(
                        label = "رقم الهاتف الثانوي",
                        isRequired = false,
                        value = secondaryPhone,
                        onValueChange = { secondaryPhone = it },
                        placeholder = "أدخل رقم الهاتف الثانوي (اختياري)",
                        keyboardType = KeyboardType.Phone
                    )
                }

                MerchantSectionCard(title = "تفاصيل الموقع") {
                    MerchantFormField(
                        label = "العنوان التفصيلي",
                        isRequired = true,
                        value = address,
                        onValueChange = { address = it },
                        placeholder = "سيظهر العنوان المكتشف تلقائياً هنا. يمكنك تعديله."
                    )
                    Spacer(Modifier.height(14.dp))
                    MerchantDropdownField(
                        label = "المحافظة",
                        isRequired = true,
                        selectedLabel = selectedGovernorate?.arabic_name,
                        placeholder = "اختر المحافظة",
                        items = merchantUiState.governorates,
                        itemLabel = { it.arabic_name.orEmpty() },
                        onItemSelected = { selectedGovernorate = it }
                    )
                }

                MerchantSectionCard(title = "تفاصيل العمل") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = "أيام الزيارة",
                            fontSize = 14.sp,
                            color = Color.Black,
                            fontFamily = FontFamily(Font(R.font.zain_regular))
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "*",
                            color = requiredStarColor,
                            fontSize = 14.sp,
                            fontFamily = FontFamily(Font(R.font.zain_regular))
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    visitDaysOptions.forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedVisitDays = if (key in selectedVisitDays)
                                        selectedVisitDays - key
                                    else
                                        selectedVisitDays + key
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = key in selectedVisitDays,
                                onCheckedChange = { checked ->
                                    selectedVisitDays = if (checked)
                                        selectedVisitDays + key
                                    else
                                        selectedVisitDays - key
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = primaryColor,
                                    uncheckedColor = cardBorderColor
                                )
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.zain_regular)),
                                color = Color.Black
                            )
                        }
                    }
                }

            }
        }
    }
}

@Composable
private fun MerchantSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.zain_bold)),
                color = Color.Black,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun MerchantFormField(
    label: String,
    isRequired: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color.Black,
                fontFamily = FontFamily(Font(R.font.zain_regular))
            )
            if (isRequired) {
                Spacer(Modifier.width(2.dp))
                Text("*", color = requiredStarColor, fontSize = 14.sp, fontFamily = FontFamily(Font(R.font.zain_regular)))
            }
        }
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = hintColor,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                    fontFamily = FontFamily(Font(R.font.zain_regular))
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = LocalTextStyle.current.copy(
                textAlign = TextAlign.Start,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                fontSize = 14.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Gray,
                unfocusedBorderColor = borderColor,
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> MerchantDropdownField(
    label: String,
    isRequired: Boolean,
    selectedLabel: String?,
    placeholder: String,
    items: List<T>,
    itemLabel: (T) -> String,
    onItemSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color.Black,
                fontFamily = FontFamily(Font(R.font.zain_regular))
            )
            if (isRequired) {
                Spacer(Modifier.width(2.dp))
                Text("*", color = requiredStarColor, fontSize = 14.sp, fontFamily = FontFamily(Font(R.font.zain_regular)))
            }
        }
        Spacer(Modifier.height(7.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedLabel ?: placeholder,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(10.dp),
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Start,
                    color = if (selectedLabel != null) Color.Black else hintColor,
                    fontFamily = FontFamily(Font(R.font.zain_regular)),
                    fontSize = 14.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Gray,
                    unfocusedBorderColor = borderColor,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = itemLabel(item),
                                fontFamily = FontFamily(Font(R.font.zain_regular)),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        onClick = {
                            onItemSelected(item)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
