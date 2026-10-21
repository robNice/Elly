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
    const val COUNT = 9
    const val DEMO_OWNER = "demo"

    val EMPTY = AppSlot(enabled = false, appId = null, title = null)

    val DEFAULT: List<AppSlot> = listOf(
        AppSlot(enabled = true, appId = "netflix", title = "Netflix"),
        AppSlot(enabled = true, appId = "amazon", title = "Prime Video"),
        AppSlot(enabled = true, appId = "ui30", title = "Rakuten TV")
    ) + List(COUNT - 3) { EMPTY }
}
