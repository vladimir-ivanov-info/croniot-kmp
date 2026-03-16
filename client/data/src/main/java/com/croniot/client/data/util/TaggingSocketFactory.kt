package com.croniot.client.data.util

import android.net.TrafficStats
import android.os.StrictMode
import java.net.InetAddress
import java.net.Socket
import javax.net.SocketFactory

class TaggingSocketFactory(private val delegateProvider: () -> SocketFactory) : SocketFactory() {

    constructor(delegate: SocketFactory = getDefaultFactory()) : this({ delegate })

    companion object {
        private fun getDefaultFactory(): SocketFactory {
            val oldPolicy = StrictMode.allowThreadDiskReads()
            return try {
                SocketFactory.getDefault()
            } finally {
                StrictMode.setThreadPolicy(oldPolicy)
            }
        }
    }

    private val delegate by lazy { delegateProvider() }

    private fun tagSocket(socket: Socket): Socket {
        TrafficStats.tagSocket(socket)
        return socket
    }

    override fun createSocket(): Socket = tagSocket(delegate.createSocket())

    override fun createSocket(host: String?, port: Int): Socket =
        tagSocket(delegate.createSocket(host, port))

    override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket =
        tagSocket(delegate.createSocket(host, port, localHost, localPort))

    override fun createSocket(host: InetAddress?, port: Int): Socket =
        tagSocket(delegate.createSocket(host, port))

    override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket =
        tagSocket(delegate.createSocket(address, port, localAddress, localPort))
}
