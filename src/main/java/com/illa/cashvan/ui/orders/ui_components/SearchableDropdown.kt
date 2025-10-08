package com.illa.cashvan.ui.orders.ui_components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.R
import com.illa.cashvan.core.analytics.CashVanAnalyticsHelper
import org.koin.compose.koinInject

@Composable
fun <T> SearchableDropdown(
    modifier: Modifier = Modifier,
    label: String,
    placeholder: String = "",
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    items: List<T>,
    selectedItem: T? = null,
    onItemSelected: (T) -> Unit,
    itemText: (T) -> String,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    onExpanded: () -> Unit = {},
    onClear: () -> Unit = {},
    analyticsEventName: String? = null,
    analyticsHelper: CashVanAnalyticsHelper = koinInject()
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Label with optional action button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color(0xFF374151)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Search Field
        Box {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    onSearchQueryChange(it)
                    if (!expanded) expanded = true
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = placeholder,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF9CA3AF)
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp),
                                color = Color(0xFF0D3773),
                                strokeWidth = 2.dp
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                onSearchQueryChange("")
                                onClear()
                                expanded = false
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "مسح",
                                    tint = Color(0xFF9CA3AF)
                                )
                            }
                        }
                        IconButton(onClick = {
                            expanded = !expanded
                            if (expanded) {
                                onExpanded()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = if (expanded) "إخفاء" else "عرض",
                                tint = Color(0xFF0D3773)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0D3773),
                    unfocusedBorderColor = Color(0xFFD1D5DB),
                    focusedTextColor = Color(0xFF111827),
                    unfocusedTextColor = Color(0xFF111827)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = enabled,
                singleLine = true
            )
        }

        // Dropdown List
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .padding(top = 4.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(12.dp))
            ) {
                if (items.isEmpty() && !isLoading) {
                    Text(
                        text = "لا توجد نتائج",
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.zain_regular)),
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn {
                        items(items) { item ->
                            val isSelected = selectedItem != null && itemText(selectedItem) == itemText(item)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        analyticsEventName?.let { eventName ->
                                            analyticsHelper.logEvent(
                                                eventName,
                                                mapOf("item_name" to itemText(item))
                                            )
                                        }
                                        onItemSelected(item)
                                        expanded = false
                                    }
                                    .background(
                                        if (isSelected) Color(0xFFEFF6FF) else Color.Transparent
                                    )
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = itemText(item),
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily(Font(R.font.zain_regular)),
                                    color = if (isSelected) Color(0xFF0D3773) else Color(0xFF111827),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Selected Item Display
        selectedItem?.let {
            Text(
                text = "المختار: ${itemText(it)}",
                fontSize = 12.sp,
                fontFamily = FontFamily(Font(R.font.zain_regular)),
                color = Color(0xFF0D3773),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}