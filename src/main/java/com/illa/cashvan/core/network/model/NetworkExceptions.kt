package com.illa.cashvan.core.network.model

import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse

class AppClientRequestException(
    response: HttpResponse,
    val error: Error
) : ResponseException(response, error.getMessage()) {
    override val message = error.getMessage()
}

class AppRedirectResponseException(
    response: HttpResponse,
    val error: Error
) : ResponseException(response, error.getMessage()) {
    override val message = error.getMessage()
}

class AppServerResponseException(
    response: HttpResponse,
    val error: Error
) : ResponseException(response, error.getMessage()) {
    override val message = error.getMessage()
}