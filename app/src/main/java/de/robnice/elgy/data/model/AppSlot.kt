package de.robnice.elgy.data.model

data class AppSlot(
    val enabled: Boolean,
    val appId: String?,
    val title: String?,
    val iconPath: String? = null,
    val showIcon: Boolean = true,
    val showText: Boolean = false,
    val customText: String? = null,
    val textColor: Long? = null
) {
    val isUsable: Boolean
        get() = enabled && !appId.isNullOrBlank()

    val label: String?
        get() = customText?.takeIf { it.isNotBlank() } ?: title
}

object AppSlots {
    const val DEMO_OWNER = "demo"

    val EMPTY = AppSlot(enabled = false, appId = null, title = null)
}
