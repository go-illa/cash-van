package com.illa.cashvan.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class Error(
    val localizedMessage: String? = null,
    val errors: List<String>? = null,
    val code: String? = null,
    val details: String? = null
) {
    fun getMessage(): String {
        return when {
            !errors.isNullOrEmpty() -> errors[0]
            !localizedMessage.isNullOrBlank() -> localizedMessage
            else -> "Unknown error occurred"
        }
    }
}

@Serializable
data class ApiSuccess<T>(
    val data: T,
    val message: String? = null,
    val status: String = "success"
)

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val exception: Throwable, val message: String) : ApiResult<Nothing>()
    data object Loading : ApiResult<Nothing>()
}