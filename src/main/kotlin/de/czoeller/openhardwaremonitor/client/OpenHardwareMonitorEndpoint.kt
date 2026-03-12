package de.czoeller.openhardwaremonitor.client

import java.net.URI

internal object OpenHardwareMonitorEndpoint {
    fun normalize(endpoint: String): URI {
        val trimmed = endpoint.trim().trimEnd('/')
        require(trimmed.isNotEmpty()) { "endpoint must not be blank" }
        val full = if (trimmed.endsWith("/data.json", ignoreCase = true)) trimmed else "$trimmed/data.json"
        return URI.create(full)
    }
}
