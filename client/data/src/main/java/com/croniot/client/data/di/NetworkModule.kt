package com.croniot.client.data.di

import com.croniot.client.core.config.Constants
import com.croniot.client.core.config.ServerConfig
import com.croniot.client.data.source.local.TokenStore
import com.croniot.client.data.source.remote.http.HostHolder
import com.croniot.client.data.source.remote.http.RegisterApi
import com.croniot.client.data.source.remote.http.TaskApi
import com.croniot.client.data.source.remote.http.login.LoginApi
import com.croniot.client.domain.models.auth.AuthTokens
import croniot.models.RefreshTokenRequestDto
import croniot.models.RefreshTokenResultDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.CertificatePinner
import org.koin.dsl.module

object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    val networkModule = module {

        single { HostHolder(initial = ServerConfig.SERVER_IP_REMOTE) }

        single<HttpClient> {
            val tokenStore = get<TokenStore>()
            val hostHolder = get<HostHolder>()

            HttpClient(OkHttp) {
                engine {
                    config {
                        certificatePinner(
                            CertificatePinner.Builder()
                                .add(ServerConfig.SERVER_IP_LOCAL, ServerConfig.CERT_PIN_SHA256)
                                .add(ServerConfig.SERVER_IP_REMOTE, ServerConfig.CERT_PIN_SHA256)
                                .build()
                        )
                    }
                }

                expectSuccess = true

                install(ContentNegotiation) { json(json) }

                install(Auth) {
                    bearer {
                        loadTokens {
                            tokenStore.getTokens()?.let {
                                BearerTokens(it.accessToken, it.refreshToken)
                            }
                        }
                        refreshTokens {
                            val refresh = oldTokens?.refreshToken
                                ?: return@refreshTokens null
                            try {
                                val result: RefreshTokenResultDto = client.post(Constants.ENDPOINT_TOKEN_REFRESH) {
                                    markAsRefreshTokenRequest()
                                    contentType(ContentType.Application.Json)
                                    setBody(RefreshTokenRequestDto(refreshToken = refresh))
                                }.body()

                                val newAccess = result.token
                                val newRefresh = result.refreshToken
                                val newExpires = result.accessTokenExpiresAtEpochSeconds
                                if (!result.result.success || newAccess == null || newRefresh == null || newExpires == null) {
                                    tokenStore.clearTokens()
                                    null
                                } else {
                                    tokenStore.saveTokens(
                                        AuthTokens(
                                            accessToken = newAccess,
                                            refreshToken = newRefresh,
                                            expiresAtEpochSeconds = newExpires,
                                        )
                                    )
                                    BearerTokens(newAccess, newRefresh)
                                }
                            } catch (e: Exception) {
                                tokenStore.clearTokens()
                                null
                            }
                        }
                        sendWithoutRequest { request ->
                            val path = request.url.pathSegments.joinToString("/", prefix = "/")
                            !(path.endsWith(Constants.ENDPOINT_LOGIN) ||
                                path.endsWith(Constants.ENDPOINT_TOKEN_REFRESH) ||
                                path.endsWith(Constants.ENDPOINT_REGISTER_ACCOUNT))
                        }
                    }
                }

                install(DefaultRequest) {
                    url {
                        protocol = URLProtocol.HTTPS
                        host = hostHolder.host
                        port = ServerConfig.SERVER_PORT
                    }
                }
            }
        }

        single { LoginApi(http = get()) }
        single { TaskApi(http = get()) }
        single { RegisterApi(http = get()) }
    }
}
