package io.github.czoeller.openhardwaremonitor.client

import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.math.roundToInt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface OpenHardwareMonitorClient {
    @Throws(IOException::class, InterruptedException::class)
    fun fetchSnapshot(): OpenHardwareMonitorSnapshot
}

class HttpOpenHardwareMonitorClient @JvmOverloads constructor(
    endpoint: String,
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) : OpenHardwareMonitorClient {
    private val endpointUri = normalizeEndpoint(endpoint)

    override fun fetchSnapshot(): OpenHardwareMonitorSnapshot {
        val request = HttpRequest.newBuilder(endpointUri)
            .timeout(Duration.ofSeconds(2))
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IOException("Unexpected OpenHardwareMonitor response: HTTP ${response.statusCode()}")
        }

        val payload = json.decodeFromString(OpenHardwareMonitorNodeDto.serializer(), response.body())
        return OpenHardwareMonitorSnapshot(payload.toModel())
    }

    private fun normalizeEndpoint(endpoint: String): URI {
        val trimmed = endpoint.trim().trimEnd('/')
        require(trimmed.isNotEmpty()) { "endpoint must not be blank" }
        val full = if (trimmed.endsWith("/data.json", ignoreCase = true)) trimmed else "$trimmed/data.json"
        return URI.create(full)
    }
}

data class OpenHardwareMonitorSnapshot(
    val root: OpenHardwareMonitorNode
) {
    fun sensors(): List<OpenHardwareMonitorSensor> = root.flatten()

    fun metrics(): OpenHardwareMonitorMetrics = OpenHardwareMonitorMetricsExtractor.extract(this)
}

data class OpenHardwareMonitorNode(
    val id: Int? = null,
    val text: String,
    val min: String? = null,
    val value: String? = null,
    val max: String? = null,
    val imageUrl: String? = null,
    val children: List<OpenHardwareMonitorNode> = emptyList()
) {
    fun flatten(path: List<String> = emptyList()): List<OpenHardwareMonitorSensor> {
        val nextPath = if (text.isBlank()) path else path + text
        val sensors = mutableListOf<OpenHardwareMonitorSensor>()
        val sensor = OpenHardwareMonitorSensor.fromNodeOrNull(path = path, node = this)
        if (sensor != null) {
            sensors += sensor
        }
        children.forEach { child ->
            sensors += child.flatten(nextPath)
        }
        return sensors
    }
}

data class OpenHardwareMonitorSensor(
    val id: Int? = null,
    val path: List<String>,
    val name: String,
    val rawValue: String,
    val numericValue: Double? = null,
    val unit: String? = null,
    val min: Double? = null,
    val max: Double? = null
) {
    companion object {
        private val numberPattern = Regex("""^\s*(-?\d+(?:[.,]\d+)?)\s*(.*)\s*$""")

        internal fun fromNodeOrNull(path: List<String>, node: OpenHardwareMonitorNode): OpenHardwareMonitorSensor? {
            val rawValue = node.value.orEmpty()
            val parsedValue = parseNumber(rawValue) ?: return null
            return OpenHardwareMonitorSensor(
                id = node.id,
                path = path,
                name = node.text,
                rawValue = rawValue,
                numericValue = parsedValue.first,
                unit = parsedValue.second,
                min = parseNumber(node.min)?.first,
                max = parseNumber(node.max)?.first
            )
        }

        private fun parseNumber(raw: String?): Pair<Double, String?>? {
            val value = raw?.trim().orEmpty()
            if (value.isEmpty()) {
                return null
            }
            val match = numberPattern.matchEntire(value) ?: return null
            val numeric = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
            val unit = match.groupValues[2].trim().ifBlank { null }
            return numeric to unit
        }
    }
}

data class OpenHardwareMonitorMetrics(
    val cpuLoadPercent: Int? = null,
    val memoryLoadPercent: Int? = null,
    val cpuTempC: Int? = null,
    val gpuLoadPercent: Int? = null,
    val gpuTempC: Int? = null
)

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

@Serializable
private data class OpenHardwareMonitorNodeDto(
    @SerialName("id")
    val id: Int? = null,
    @SerialName("Text")
    val text: String = "",
    @SerialName("Min")
    val min: String? = null,
    @SerialName("Value")
    val value: String? = null,
    @SerialName("Max")
    val max: String? = null,
    @SerialName("ImageURL")
    val imageUrl: String? = null,
    @SerialName("Children")
    val children: List<OpenHardwareMonitorNodeDto> = emptyList()
) {
    fun toModel(): OpenHardwareMonitorNode {
        return OpenHardwareMonitorNode(
            id = id,
            text = text,
            min = min,
            value = value,
            max = max,
            imageUrl = imageUrl,
            children = children.map { it.toModel() }
        )
    }
}
