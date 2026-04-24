package com.croniot.client.data.source.remote.http.login

import Outcome
import com.croniot.client.domain.models.auth.AuthError
import croniot.messages.LoginDto
import croniot.models.LoginResultDto
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class LoginDataSourceImpl(
    private val api: LoginApi,
) : LoginDataSource {
    override suspend fun login(request: LoginDto): Outcome<LoginResultDto, AuthError> {
        return try {
            Outcome.Ok(api.login(request))
        } catch (e: ClientRequestException) {
            when (e.response.status.value) {
                401 -> Outcome.Err(AuthError.InvalidCredentials)
                else -> Outcome.Err(AuthError.Server(e.message))
            }
        } catch (e: ServerResponseException) {
            Outcome.Err(AuthError.Server(e.message))
        } catch (e: HttpRequestTimeoutException) {
            Outcome.Err(AuthError.NetworkTiemout)
        } catch (e: ConnectTimeoutException) {
            Outcome.Err(AuthError.NetworkTiemout)
        } catch (e: SocketTimeoutException) {
            Outcome.Err(AuthError.NetworkTiemout)
        } catch (e: IOException) {
            Outcome.Err(AuthError.Network)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Outcome.Err(AuthError.Unknown)
        }
    }
}
