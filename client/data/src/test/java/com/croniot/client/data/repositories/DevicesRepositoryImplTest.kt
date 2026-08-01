package com.croniot.client.data.repositories

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DevicesRepositoryImplTest {

    @Test
    fun `WHEN getDevices is called THEN it returns empty list`() {
        val repository = DevicesRepositoryImpl()

        val result = repository.getDevices("account-1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `WHEN getDevices is called with any accountUuid THEN it returns empty list`() {
        val repository = DevicesRepositoryImpl()

        val result = repository.getDevices("any-other-account")

        assertTrue(result.isEmpty())
    }
}
