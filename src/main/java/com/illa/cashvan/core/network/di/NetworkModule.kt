package com.illa.cashvan.core.network.di

import android.util.Log
import com.illa.cashvan.BuildConfig
import com.illa.cashvan.core.app_preferences.domain.use_case.app_cache.ClearAppDataUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.token.GetRefreshTokenUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.token.GetTokenUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.token.SaveAccessTokenUseCase
import com.illa.cashvan.core.app_preferences.domain.use_case.token.SaveRefreshTokenUseCase
import com.illa.cashvan.core.network.model.AppClientRequestException
import com.illa.cashvan.core.network.model.AppRedirectResponseException
import com.illa.cashvan.core.network.model.AppServerResponseException
import com.illa.cashvan.core.network.util.getLocalizedError
import com.illa.cashvan.core.session.SessionManager
import com.illa.cashvan.core.utils.getLanguage
import com.illa.cashvan.di.SharedConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val networkModule = module {
    single { SharedConfig() }
    single { provideHttpClient(get(), get(), get(), get(), get(), get(), get()) }
    single(named("supabase")) { provideSupabaseHttpClient() }
    single(named("plain")) { providePlainHttpClient() }
}

fun provideSupabaseHttpClient(): HttpClient = HttpClient(Android) {
    install(HttpTimeout) {
        connectTimeoutMillis = 20.seconds.inWholeMilliseconds
        socketTimeoutMillis = 30.seconds.inWholeMilliseconds
    }
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; isLenient = true })
    }
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                val tag = "SupabaseClient"
                var offset = 0
                while (offset < message.length) {
                    val end = minOf(offset + 3000, message.length)
                    Log.d(tag, message.substring(offset, end))
                    offset = end
                }
            }
        }
        level = LogLevel.ALL
    }
    defaultRequest {
        url("https://hlzgbivgdcesiaymbijn.supabase.co/functions/v1/")
        header(HttpHeaders.Authorization, "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        header("apikey", BuildConfig.SUPABASE_ANON_KEY)
    }
}

fun providePlainHttpClient(): HttpClient = HttpClient(Android) {
    install(HttpTimeout) {
        connectTimeoutMillis = 30_000
        socketTimeoutMillis = 30_000
    }
}

fun provideHttpClient(
    sharedConfig: SharedConfig,
    getTokenUseCase: GetTokenUseCase,
    getRefreshTokenUseCase: GetRefreshTokenUseCase,
    saveTokenUseCase: SaveAccessTokenUseCase,
    saveRefreshTokenUseCase: SaveRefreshTokenUseCase,
    clearAppDataUseCase: ClearAppDataUseCase,
    sessionManager: SessionManager
): HttpClient {
    return HttpClient(Android) {
        expectSuccess = true

        install(HttpTimeout) {
            connectTimeoutMillis = 20.seconds.inWholeMilliseconds
            socketTimeoutMillis = 5.minutes.inWholeMilliseconds
        }

        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                }
            )
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    val tag = "HttpClient"
                    var offset = 0
                    while (offset < message.length) {
                        val end = minOf(offset + 3000, message.length)
                        Log.d(tag, message.substring(offset, end))
                        offset = end
                    }
                }
            }
            level = LogLevel.ALL
        }

        defaultRequest {
            url(sharedConfig.baseUrl)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            val token = runBlocking(Dispatchers.IO) { getTokenUseCase().firstOrNull() }
            header(HttpHeaders.AcceptLanguage, getLanguage())
            header("X-App-Version", BuildConfig.VERSION_CODE.toString())
            if (!token.isNullOrBlank()) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }

        HttpResponseValidator {
            handleResponseExceptionWithRequest { exception, request ->
                throw when (exception) {
                    is ClientRequestException -> {
                        var customError: com.illa.cashvan.core.network.model.Error? = null
                        when (exception.response.status) {
                            HttpStatusCode.Unauthorized -> {
                                val isRefreshEndpoint = request.url.encodedPath.contains("auth/refresh")

                                if (!isRefreshEndpoint) {
                                    val refreshToken = runBlocking(Dispatchers.IO) {
                                        getRefreshTokenUseCase().firstOrNull()
                                    }

                                    if (!refreshToken.isNullOrBlank()) {
                                        try {
                                            val refreshResponse = runBlocking(Dispatchers.IO) {
                                                val refreshClient = HttpClient(Android) {
                                                    install(ContentNegotiation) {
                                                        json(Json { ignoreUnknownKeys = true })
                                                    }
                                                }

                                                val response = refreshClient.request("${sharedConfig.baseUrl}v1/auth/refresh") {
                                                    method = io.ktor.http.HttpMethod.Post
                                                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                                                    setBody(mapOf("tokens" to mapOf("refresh_token" to refreshToken)))
                                                }.body<Map<String, String>>()

                                                refreshClient.close()
                                                response
                                            }

                                            val newAccessToken = refreshResponse["access_token"]
                                            val newRefreshToken = refreshResponse["refresh_token"]

                                            if (newAccessToken != null && newRefreshToken != null) {
                                                runBlocking(Dispatchers.IO) {
                                                    saveTokenUseCase(newAccessToken)
                                                    saveRefreshTokenUseCase(newRefreshToken)
                                                }
                                            } else {
                                                runBlocking(Dispatchers.IO) { clearAppDataUseCase() }
                                                sessionManager.triggerForceLogout()
                                            }
                                        } catch (e: Exception) {
                                            runBlocking(Dispatchers.IO) { clearAppDataUseCase() }
                                            sessionManager.triggerForceLogout()
                                        }
                                    } else {
                                        runBlocking(Dispatchers.IO) { clearAppDataUseCase() }
                                        sessionManager.triggerForceLogout()
                                    }
                                } else {
                                    runBlocking(Dispatchers.IO) { clearAppDataUseCase() }
                                    sessionManager.triggerForceLogout()
                                }
                            }
                            HttpStatusCode.Forbidden -> {
                                sessionManager.triggerInactiveAgent()
                                customError = com.illa.cashvan.core.network.model.Error(
                                    localizedMessage = "انت حسابك مش مربوط بحساب بري-سيل كلم المشرف بتاعك"
                                )
                            }
                        }
                        AppClientRequestException(
                            exception.response,
                            customError ?: exception.response.getLocalizedError()
                        )
                    }

                    is RedirectResponseException -> {
                        AppRedirectResponseException(
                            exception.response,
                            exception.response.getLocalizedError()
                        )
                    }

                    is ServerResponseException -> {
                        AppServerResponseException(
                            exception.response,
                            exception.response.getLocalizedError()
                        )
                    }

                    else -> exception
                }
            }
        }
    }
}
