package com.croniot.client.data.repositories

import com.croniot.client.data.source.remote.http.RegisterApi
import croniot.models.Result
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

class RegisterAccountRepositoryImplTest {

    private val registerApi: RegisterApi = mockk()
    private lateinit var repository: RegisterAccountRepositoryImpl

    @BeforeEach
    fun setUp() {
        repository = RegisterAccountRepositoryImpl(registerApi)
    }

    @Test
    fun `WHEN api returns success THEN registerAccount returns success result`() = runTest {
        coEvery { registerApi.registerAccount(any()) } returns Result(success = true, message = "")

        val result = repository.registerAccount("nickname", "user@example.com", "password123")

        assertTrue(result.success)
    }

    @Test
    fun `WHEN api returns failure THEN registerAccount returns failure result`() = runTest {
        coEvery { registerApi.registerAccount(any()) } returns Result(success = false, message = "Email taken")

        val result = repository.registerAccount("nickname", "user@example.com", "password123")

        assertFalse(result.success)
        assertEquals("Email taken", result.message)
    }

    @Test
    fun `WHEN api throws IOException THEN registerAccount maps it to failure result with network error message`() = runTest {
        coEvery { registerApi.registerAccount(any()) } throws IOException("connection refused")

        val result = repository.registerAccount("nickname", "user@example.com", "password123")

        assertFalse(result.success)
        assertEquals("connection refused", result.message)
    }

    @Test
    fun `WHEN api throws a generic exception THEN registerAccount maps it to failure result`() = runTest {
        coEvery { registerApi.registerAccount(any()) } throws IllegalStateException("boom")

        val result = repository.registerAccount("nickname", "user@example.com", "password123")

        assertFalse(result.success)
        assertEquals("boom", result.message)
    }

    @Test
    fun `WHEN IOException has null message THEN registerAccount uses default message`() = runTest {
        coEvery { registerApi.registerAccount(any()) } throws IOException()

        val result = repository.registerAccount("nickname", "user@example.com", "password123")

        assertFalse(result.success)
        assertEquals("Network error", result.message)
    }

    @Test
    fun `WHEN generic exception has null message THEN registerAccount uses default message`() = runTest {
        coEvery { registerApi.registerAccount(any()) } throws RuntimeException()

        val result = repository.registerAccount("nickname", "user@example.com", "password123")

        assertFalse(result.success)
        assertEquals("Unknown error", result.message)
    }
}
