package com.illa.cashvan.ui.modulepicker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.illa.cashvan.R
import com.illa.cashvan.core.session.AgentRole
import com.illa.cashvan.core.user.presentation.viewmodel.UserViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Shown after sign-in to agents with both the sales and fulfillment roles.
 */
@Composable
fun ModulePickerScreen(
    onModuleSelected: (AgentRole) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    userViewModel: UserViewModel = koinViewModel()
) {
    val userState by userViewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "أهلاً، ${userState.userName}",
            fontSize = 28.sp,
            fontFamily = FontFamily(Font(R.font.zain_bold)),
            color = Color(0xFF0D3773)
        )
        Text(
            text = "اختار هتشتغل على أنهي وضع. تقدر تغيّره بعدين من الملف الشخصي.",
            fontSize = 16.sp,
            fontFamily = FontFamily(Font(R.font.zain_regular)),
            color = Color(0xFF878787)
        )

        Spacer(modifier = Modifier.height(32.dp))

        ModuleCard(
            icon = Icons.Outlined.Storefront,
            title = "المبيعات",
            description = "بري سيل، كاش فان، والمؤشرات",
            onClick = { onModuleSelected(AgentRole.SALES) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ModuleCard(
            icon = Icons.Outlined.LocalShipping,
            title = "التوصيل",
            description = "أوردرات البري سيل، المخزون، والزيارات",
            onClick = { onModuleSelected(AgentRole.FULFILLMENT) }
        )

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            onClick = onLogout,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = null,
                tint = Color(0xFFE23636),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "تسجيل الخروج",
                color = Color(0xFFE23636),
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.zain_regular))
            )
        }
    }
}

@Composable
private fun ModuleCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0D3773).copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF0D3773),
                    modifier = Modifier.size(32.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily(Font(R.font.zain_bold)),
                    color = Color.Black
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.zain_regular)),
                    color = Color(0xFF65758B)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF65758B)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, locale = "ar")
@Composable
fun ModulePickerScreenPreview() {
    ModulePickerScreen(onModuleSelected = {}, onLogout = {})
}
