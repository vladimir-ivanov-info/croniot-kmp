package com.croniot.client.data.source.remote.http.login

import Outcome
import com.croniot.client.core.models.auth.AuthError
import croniot.messages.LoginDto
import croniot.models.LoginResultDto
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class LoginDataSourceImpl(
    private val api: LoginApi,
) : LoginDataSource {
    override suspend fun login(request: LoginDto): Outcome<LoginResultDto, AuthError> {
        return try {
            val resp = api.login(request)
            if (!resp.isSuccessful) throw HttpException(resp)
            val body = resp.body()
                ?: return Outcome.Err(AuthError.Server("Empty response body"))
            Outcome.Ok(body)
        } catch (e: ConnectException) {
            Outcome.Err(AuthError.Network)
        } catch (e: UnknownHostException) {
            Outcome.Err(AuthError.Network)
        } catch (e: SocketTimeoutException) {
            Outcome.Err(AuthError.Network)
        } catch (e: HttpException) {
            when (e.code()) {
                401 -> Outcome.Err(AuthError.InvalidCredentials)
                else -> Outcome.Err(AuthError.Server(e.message()))
            }
        } catch (e: Exception) {
            Outcome.Err(AuthError.Unknown)
        }
    }
}
