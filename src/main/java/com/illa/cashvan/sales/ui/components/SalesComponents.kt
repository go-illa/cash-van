package com.illa.cashvan.sales.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.R

object SalesColors {
    val Primary = Color(0xFF0D3773)
    val Background = Color(0xFFF5F5F5)
    val Surface = Color.White
    val Border = Color(0xFFE1E7EF)
    val Muted = Color(0xFF65758B)
    val Danger = Color(0xFFE23636)
    val Success = Color(0xFF16A34A)
    val Progress = Color(0xFFD97706)
    val FieldBackground = Color(0xFFF8F9FA)
}

val ZainRegular = FontFamily(Font(R.font.zain_regular))
val ZainBold = FontFamily(Font(R.font.zain_bold))

@Composable
fun SalesTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SalesColors.Surface)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = SalesColors.Primary)
            }
        } else {
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 20.sp, fontFamily = ZainBold, color = Color.Black)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 13.sp, fontFamily = ZainRegular, color = SalesColors.Muted)
            }
        }
        actions()
    }
}

@Composable
fun FieldLabel(text: String, required: Boolean = false) {
    Row(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(text = text, fontSize = 16.sp, fontFamily = ZainRegular, color = Color.Black)
        if (required) {
            Text(text = " *", fontSize = 16.sp, color = SalesColors.Danger)
        }
    }
}

@Composable
fun SalesTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FieldLabel(label, required)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            placeholder = placeholder?.let { { Text(it, color = Color(0xFFBDBDBD)) } },
            leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null, tint = SalesColors.Muted) } },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(8.dp),
            colors = salesFieldColors()
        )
    }
}

@Composable
fun salesFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = SalesColors.FieldBackground,
    focusedContainerColor = SalesColors.FieldBackground,
    disabledContainerColor = SalesColors.FieldBackground,
    unfocusedBorderColor = SalesColors.Border,
    focusedBorderColor = Color(0xFF3B82F6)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SalesDropdownField(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    placeholder: String = "اختار"
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        FieldLabel(label, required)
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selected?.let(optionLabel).orEmpty(),
                onValueChange = {},
                readOnly = true,
                placeholder = { Text(placeholder, color = Color(0xFFBDBDBD)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = RoundedCornerShape(8.dp),
                colors = salesFieldColors()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option), fontFamily = ZainRegular, fontSize = 16.sp) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SalesPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    containerColor: Color = SalesColors.Primary
) {
    Button(
        onClick = { if (!loading) onClick() },
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor)
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, fontSize = 17.sp, fontFamily = ZainBold)
        }
    }
}

@Composable
fun SalesSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = SalesColors.Primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, fontSize = 16.sp, fontFamily = ZainBold, color = SalesColors.Primary)
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = SalesColors.Primary)
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = SalesColors.Muted, modifier = Modifier.size(48.dp))
        Text(text = title, fontSize = 18.sp, fontFamily = ZainBold, color = Color.Black, textAlign = TextAlign.Center)
        if (description != null) {
            Text(text = description, fontSize = 14.sp, fontFamily = ZainRegular, color = SalesColors.Muted, textAlign = TextAlign.Center)
        }
        action?.invoke()
    }
}
