package com.croniot.client.data.repositories

import com.croniot.client.core.models.auth.AuthSession
import com.croniot.client.data.source.local.LocalDatasource
import com.croniot.client.domain.repositories.SessionRepository

class SessionRepositoryImpl(
    private val localDataSource: LocalDatasource,
) : SessionRepository {

    override suspend fun save(session: AuthSession) {
        localDataSource.saveEmail(session.email)
        localDataSource.saveToken(session.token)
    }

    override suspend fun clearAllExceptDeviceUuid() {
        localDataSource.clearAllCacheExceptDeviceUuid()
    }

    /*override suspend fun clearSession(){
        localDataSource.saveEmail("")
        localDataSource.saveToken("")
        localDataSource.saveCurrentAccount(null) //TODO
    }*/
}
