package com.illa.cashvan.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale.ENGLISH

fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd", ENGLISH)
    return sdf.format(Date(System.currentTimeMillis()))
}