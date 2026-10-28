package com.illa.cashvan.sales.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.AddBusiness
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illa.cashvan.R

/**
 * The "المزيد" sheet from the PWA's bottom bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesMoreSheet(
    onDismiss: () -> Unit,
    onProfileClick: () -> Unit,
    onCreateMerchant: () -> Unit,
    onOrderHistory: () -> Unit,
    onReconciliation: () -> Unit,
    reconciliationEnabled: Boolean,
    onCoverageMap: () -> Unit,
    onSwitchModule: (() -> Unit)?,
    onLogout: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "المزيد",
                fontSize = 22.sp,
                fontFamily = FontFamily(Font(R.font.zain_bold)),
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF8F9FA))
            ) {
                MoreItem(icon = Icons.Outlined.Person, label = "الملف الشخصي", onClick = onProfileClick)
                HorizontalDivider(color = Color(0xFFE1E7EF))
                MoreItem(icon = Icons.Outlined.AddBusiness, label = "إنشاء تاجر", onClick = onCreateMerchant)
                HorizontalDivider(color = Color(0xFFE1E7EF))
                MoreItem(icon = Icons.Outlined.History, label = "سجل الطلبات", onClick = onOrderHistory)
                HorizontalDivider(color = Color(0xFFE1E7EF))
                MoreItem(icon = Icons.Outlined.FactCheck, label = "تقفيل الحساب اليومي", onClick = onReconciliation, enabled = reconciliationEnabled)
                HorizontalDivider(color = Color(0xFFE1E7EF))
                MoreItem(icon = Icons.Outlined.Map, label = "خريطة التغطية", onClick = onCoverageMap)
                if (onSwitchModule != null) {
                    HorizontalDivider(color = Color(0xFFE1E7EF))
                    MoreItem(icon = Icons.Default.SwapHoriz, label = "تبديل الوضع", onClick = onSwitchModule)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE23636).copy(alpha = 0.1f))
                    .clickable(onClick = onLogout),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = Color(0xFFE23636),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "تسجيل الخروج",
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.zain_bold)),
                    color = Color(0xFFE23636)
                )
            }
        }
    }
}

@Composable
private fun MoreItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF0D3773),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            fontSize = 16.sp,
            fontFamily = FontFamily(Font(R.font.zain_bold)),
            color = Color.Black
        )
    }
}
