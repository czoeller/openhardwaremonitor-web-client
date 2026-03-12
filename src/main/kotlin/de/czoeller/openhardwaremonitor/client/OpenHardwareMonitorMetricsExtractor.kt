package de.czoeller.openhardwaremonitor.client

import kotlin.jvm.JvmStatic
import kotlin.math.roundToInt

object OpenHardwareMonitorMetricsExtractor {
    @JvmStatic
    fun extract(snapshot: OpenHardwareMonitorSnapshot): OpenHardwareMonitorMetrics {
        val sensors = snapshot.sensors()
        return OpenHardwareMonitorMetrics(
            cpuLoadPercent = findCpuLoadPercent(sensors),
            memoryLoadPercent = findMemoryLoadPercent(sensors),
            cpuTempC = findCpuTempC(sensors),
            gpuLoadPercent = findGpuLoadPercent(sensors),
            gpuTempC = findGpuTempC(sensors)
        )
    }

    private fun findCpuLoadPercent(sensors: List<OpenHardwareMonitorSensor>): Int? {
        val preferred = sensors.firstOrNull { sensor ->
            sensor.isPercent() &&
                sensor.matchesAnyPathKeyword("cpu", "intel", "amd", "ryzen") &&
                sensor.name.contains("total", ignoreCase = true)
        }
        return preferred?.numericValue?.roundToInt()
    }

    private fun findMemoryLoadPercent(sensors: List<OpenHardwareMonitorSensor>): Int? {
        val preferred = sensors.firstOrNull { sensor ->
            sensor.isPercent() &&
                sensor.matchesAnyPathKeyword("memory", "ram") &&
                sensor.name.contains("memory", ignoreCase = true)
        }
        return preferred?.numericValue?.roundToInt()
    }

    private fun findCpuTempC(sensors: List<OpenHardwareMonitorSensor>): Int? {
        val candidates = sensors.filter { sensor ->
            sensor.isTemperature() &&
                sensor.matchesAnyPathKeyword("cpu", "intel", "amd", "ryzen") &&
                sensor.name.containsAnyIgnoreCase("package", "tctl", "tdie", "ccd", "core", "cpu")
        }
        return candidates.maxOfOrNull { it.numericValue ?: Double.MIN_VALUE }?.roundToInt()
    }

    private fun findGpuLoadPercent(sensors: List<OpenHardwareMonitorSensor>): Int? {
        val preferred = sensors.firstOrNull { sensor ->
            sensor.isPercent() &&
                sensor.matchesAnyPathKeyword("gpu", "nvidia", "geforce", "radeon", "graphics", "intel arc") &&
                sensor.name.containsAnyIgnoreCase("core", "3d", "gpu")
        }
        return preferred?.numericValue?.roundToInt()
    }

    private fun findGpuTempC(sensors: List<OpenHardwareMonitorSensor>): Int? {
        val candidates = sensors.filter { sensor ->
            sensor.isTemperature() &&
                sensor.matchesAnyPathKeyword("gpu", "nvidia", "geforce", "radeon", "graphics", "intel arc") &&
                sensor.name.containsAnyIgnoreCase("core", "hot spot", "gpu")
        }
        return candidates.maxOfOrNull { it.numericValue ?: Double.MIN_VALUE }?.roundToInt()
    }

    private fun OpenHardwareMonitorSensor.isPercent(): Boolean = unit == "%"

    private fun OpenHardwareMonitorSensor.isTemperature(): Boolean {
        return unit?.contains("C", ignoreCase = true) == true
    }

    private fun OpenHardwareMonitorSensor.matchesAnyPathKeyword(vararg keywords: String): Boolean {
        val haystack = (path + name).joinToString(" ").lowercase()
        return keywords.any { haystack.contains(it.lowercase()) }
    }

    private fun String.containsAnyIgnoreCase(vararg needles: String): Boolean {
        return needles.any { contains(it, ignoreCase = true) }
    }
}
