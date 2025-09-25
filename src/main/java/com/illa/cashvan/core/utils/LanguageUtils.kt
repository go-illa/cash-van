package com.illa.cashvan.core.utils

import java.util.Locale

fun getLanguage(): String {
    return Locale.getDefault().language
}