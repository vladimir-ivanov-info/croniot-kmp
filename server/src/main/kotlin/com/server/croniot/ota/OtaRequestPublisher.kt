package com.server.croniot.ota

import com.server.croniot.mqtt.MqttController
import javax.inject.Inject

// Seam over MqttController (an object with a live broker client) so the
// routes can be tested without one.
interface OtaRequestPublisher {
    // Retained; an empty payload clears the request on the broker.
    suspend fun publish(deviceUuid: String, manifestJson: String)
}

class MqttOtaRequestPublisher @Inject constructor() : OtaRequestPublisher {
    override suspend fun publish(deviceUuid: String, manifestJson: String) {
        MqttController.publishOtaRequest(deviceUuid, manifestJson)
    }
}
