package com.croniot.testing.fakes

import com.croniot.client.domain.DevicePropertiesProvider

class FakeDevicePropertiesProvider(
    private val properties: Map<String, String> = mapOf(
        "model" to "FakePixel",
        "os" to "Android",
    ),
) : DevicePropertiesProvider {
    override fun getDeviceDetails(): Map<String, String> = properties
}
