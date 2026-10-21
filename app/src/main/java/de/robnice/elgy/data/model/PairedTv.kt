package de.robnice.elgy.data.model

data class PairedTv(
    val id: String,
    val ipAddress: String,
    val name: String,
    val modelName: String?,
    val clientKey: String,
    val macAddress: String?,
    val certificatePin: String?,
    val secure: Boolean
)
