package de.robnice.elly.tv.discovery

object DeviceDescriptionParser {

    data class DeviceDescription(val friendlyName: String?, val modelName: String?)

    fun parse(xml: String): DeviceDescription = DeviceDescription(
        friendlyName = element(xml, "friendlyName"),
        modelName = element(xml, "modelName")
    )

    private fun element(xml: String, name: String): String? =
        Regex("<(?:\\w+:)?$name>\\s*([^<]*?)\\s*</(?:\\w+:)?$name>")
            .find(xml)
            ?.groupValues
            ?.get(1)
            ?.let(::unescape)
            ?.ifBlank { null }

    private fun unescape(value: String): String = value
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&")
}
