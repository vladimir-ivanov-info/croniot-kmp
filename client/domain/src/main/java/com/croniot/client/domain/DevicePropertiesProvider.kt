package com.croniot.client.domain

interface DevicePropertiesProvider {
    fun getDeviceDetails(): Map<String, String>
}
