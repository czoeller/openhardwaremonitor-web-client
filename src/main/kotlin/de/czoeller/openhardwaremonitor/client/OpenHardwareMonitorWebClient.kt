package de.czoeller.openhardwaremonitor.client

import java.io.IOException

interface OpenHardwareMonitorWebClient {
    @Throws(IOException::class, InterruptedException::class)
    fun fetchSnapshot(): OpenHardwareMonitorSnapshot
}
