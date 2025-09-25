package com.illa.cashvan.core.network.util

import com.illa.cashvan.core.network.model.Error
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse

suspend fun HttpResponse.getLocalizedError(): Error {
    return try {
        body<Error>()
    } catch (e: Exception) {
        Error(
            localizedMessage = "Failed to parse error response",
            code = status.value.toString(),
            details = e.message
        )
    }
}