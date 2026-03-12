package de.czoeller.openhardwaremonitor.client

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
