package com.croniot.client.data.util

import android.net.TrafficStats
import android.os.StrictMode
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.net.Socket
import javax.net.SocketFactory

class TaggingSocketFactoryTest {

    private val delegate: SocketFactory = mockk()
    private lateinit var factory: TaggingSocketFactory

    @BeforeEach
    fun setUp() {
        mockkStatic(TrafficStats::class)
        every { TrafficStats.tagSocket(any()) } returns Unit
        factory = TaggingSocketFactory(delegate)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(TrafficStats::class)
    }

    @Test
    fun `WHEN createSocket is called with no args THEN it tags and returns the delegate socket`() {
        val socket: Socket = mockk()
        every { delegate.createSocket() } returns socket

        val result = factory.createSocket()

        assertSame(socket, result)
        verify(exactly = 1) { TrafficStats.tagSocket(socket) }
    }

    @Test
    fun `WHEN createSocket is called with host and port THEN it delegates correctly`() {
        val socket: Socket = mockk()
        every { delegate.createSocket("host", 8080) } returns socket

        val result = factory.createSocket("host", 8080)

        assertSame(socket, result)
        verify(exactly = 1) { delegate.createSocket("host", 8080) }
    }

    @Test
    fun `WHEN createSocket is called with host port localHost and localPort THEN it delegates correctly`() {
        val socket: Socket = mockk()
        val localHost: InetAddress = mockk()
        every { delegate.createSocket("host", 8080, localHost, 9090) } returns socket

        val result = factory.createSocket("host", 8080, localHost, 9090)

        assertSame(socket, result)
        verify(exactly = 1) { delegate.createSocket("host", 8080, localHost, 9090) }
    }

    @Test
    fun `WHEN createSocket is called with an InetAddress and port THEN it delegates correctly`() {
        val socket: Socket = mockk()
        val address: InetAddress = mockk()
        every { delegate.createSocket(address, 8080) } returns socket

        val result = factory.createSocket(address, 8080)

        assertSame(socket, result)
        verify(exactly = 1) { delegate.createSocket(address, 8080) }
    }

    @Test
    fun `WHEN createSocket is called with address port localAddress and localPort THEN it delegates correctly`() {
        val socket: Socket = mockk()
        val address: InetAddress = mockk()
        val localAddress: InetAddress = mockk()
        every { delegate.createSocket(address, 8080, localAddress, 9090) } returns socket

        val result = factory.createSocket(address, 8080, localAddress, 9090)

        assertSame(socket, result)
        verify(exactly = 1) { TrafficStats.tagSocket(socket) }
    }

    @Test
    fun `WHEN createSocket is called multiple times THEN the delegate is only constructed once via lazy initialization`() {
        var callCount = 0
        val lazyFactory = TaggingSocketFactory(delegateProvider = { callCount++; delegate })
        val socket: Socket = mockk()
        every { delegate.createSocket() } returns socket

        lazyFactory.createSocket()
        lazyFactory.createSocket()

        assertSame(1, callCount)
    }

    @Test
    fun `WHEN the no-arg constructor is used THEN it resolves the default SocketFactory under an allow-thread-disk-reads policy`() {
        mockkStatic(StrictMode::class)
        val oldPolicy: StrictMode.ThreadPolicy = mockk()
        every { StrictMode.allowThreadDiskReads() } returns oldPolicy
        every { StrictMode.setThreadPolicy(oldPolicy) } returns Unit

        try {
            val defaultFactory = TaggingSocketFactory()

            assertNotNull(defaultFactory)
            verify(exactly = 1) { StrictMode.allowThreadDiskReads() }
            verify(exactly = 1) { StrictMode.setThreadPolicy(oldPolicy) }
        } finally {
            unmockkStatic(StrictMode::class)
        }
    }
}
