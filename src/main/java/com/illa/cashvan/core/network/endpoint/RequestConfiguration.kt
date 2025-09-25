package com.illa.cashvan.core.network.endpoint

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.formUrlEncode

enum class ParameterEncoding {
    QUERY, BODY, FORM
}

data class RequestConfiguration(
    val path: String,
    val method: HttpMethod = HttpMethod.Get,
    val headers: Map<String, String>? = null,
    val parameters: Parameters? = null,
    val parameterEncoding: ParameterEncoding? = null,
    val version: Int = 0
)

suspend fun HttpClient.request(config: RequestConfiguration): HttpResponse {
    val versionedPath = "v${config.version}/${config.path}"

    val path = if (config.parameterEncoding == ParameterEncoding.QUERY) {
        buildString {
            append(versionedPath)
            config.parameters?.let {
                append("?")
                append(it.formUrlEncode())
            }
        }
    } else {
        versionedPath
    }

    return request(path) {
        method = config.method
        config.headers?.forEach { header(it.key, it.value) }
        when (config.parameterEncoding) {
            ParameterEncoding.BODY -> {
                config.parameters?.let {
                    contentType(ContentType.Application.Json)
                    setBody(it)
                }
            }

            ParameterEncoding.FORM -> {
                config.parameters?.let {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(FormDataContent(it))
                }
            }

            else -> Unit // Do nothing for QUERY and PATH
        }
    }
}