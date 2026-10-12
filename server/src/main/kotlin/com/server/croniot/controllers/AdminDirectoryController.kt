package com.server.croniot.controllers

import com.server.croniot.admin.AdminDirectoryDao
import com.server.croniot.application.DomainException
import croniot.models.errors.DomainError
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import javax.inject.Inject

// Admin-only device and owner pages:
//   GET /api/admin/devices               every device with owner and status
//   GET /api/admin/devices/{uuid}        one device's page
//   GET /api/admin/accounts/{uuid}       one owner and their devices
class AdminDirectoryController @Inject constructor(
    private val dao: AdminDirectoryDao,
) {
    suspend fun listDevices(call: ApplicationCall) {
        call.respond(dao.listDevices())
    }

    suspend fun getDevice(call: ApplicationCall) {
        val uuid = call.parameters["uuid"] ?: throw DomainException(DomainError.Validation("uuid", "Missing uuid"))
        val detail = dao.deviceDetail(uuid) ?: throw DomainException(DomainError.NotFound("Device $uuid"))
        call.respond(detail)
    }

    suspend fun getAccount(call: ApplicationCall) {
        val uuid = call.parameters["uuid"] ?: throw DomainException(DomainError.Validation("uuid", "Missing uuid"))
        val detail = dao.accountDetail(uuid) ?: throw DomainException(DomainError.NotFound("Account $uuid"))
        call.respond(detail)
    }
}
