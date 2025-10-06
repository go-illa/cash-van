package com.illa.cashvan.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

fun makePhoneCall(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:$phoneNumber")
    }
    context.startActivity(intent)
}
