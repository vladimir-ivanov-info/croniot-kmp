// package com.croniot.android.features.registeraccount.domain.controller
//
// import com.croniot.android.features.registeraccount.domain.usecase.RegisterAccountUseCase
// import com.croniot.client.data.repositories.LocalDataRepository
// import croniot.models.Result
// import java.util.UUID
//
// class RegisterAccountUseCase(
//    private val registerAccountUseCase: RegisterAccountUseCase,
//    private val localDataRepository: LocalDataRepository
// ) {
//    suspend fun registerAccount(nickname: String, email: String, password: String): Result {
//        val accountUuid = UUID.randomUUID().toString()
//
//        val result = registerAccountUseCase(accountUuid, nickname, email, password)
//        if (result.success) {
//            localDataRepository.saveEmail(email)
//            localDataRepository.savePassword(password)
//        }
//        return result
//    }
// }
