package com.illa.cashvan.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.illa.cashvan.R
import com.illa.cashvan.ui.home.ui_components.EmptyOrdersComponent
import com.illa.cashvan.ui.orders.OrderScreen

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onCreateOrderClick: () -> Unit = {},
    onOrderDetailsClick: (String) -> Unit = {}
) {
    EmptyOrdersComponent()
//    OrderScreen(
//        onAddOrderClick = onCreateOrderClick,
//        onOrderClick = { order ->
//            onOrderDetailsClick(order.orderNumber)
//        },
//        userName = stringResource(R.string.default_user_name)
//    )
}

@Preview(showBackground = true, showSystemUi = true, locale = "ar")
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}