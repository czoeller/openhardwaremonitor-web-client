package de.czoeller.openhardwaremonitor.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal class OpenHardwareMonitorJsonParser(
    private val json: Json
) {
    fun parseTree(payload: String): OpenHardwareMonitorNode {
        return json.decodeFromString(OpenHardwareMonitorNodeDto.serializer(), payload).toModel()
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
