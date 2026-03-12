package de.czoeller.openhardwaremonitor.client

import java.net.URI
import java.net.http.HttpRequest
import kotlin.time.Duration
import kotlin.time.toJavaDuration

internal class OpenHardwareMonitorRequestFactory(
    private val timeout: Duration
) {
    fun createGetRequest(uri: URI): HttpRequest {
        return HttpRequest.newBuilder(uri)
            .timeout(timeout.toJavaDuration())
            .GET()
            .build()
    }
}
