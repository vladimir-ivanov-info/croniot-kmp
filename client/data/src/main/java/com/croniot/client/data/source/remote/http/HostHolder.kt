package com.croniot.client.data.source.remote.http

class HostHolder(initial: String) {
    @Volatile
    var host: String = initial
}
