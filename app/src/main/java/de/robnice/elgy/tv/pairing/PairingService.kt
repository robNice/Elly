package de.robnice.elgy.tv.pairing

import de.robnice.elgy.data.model.PairedTv
import de.robnice.elgy.tv.connection.WebOsConnector
import de.robnice.elgy.tv.discovery.DiscoveredTv
import java.util.UUID

class PairingService(
    private val manifest: PairingManifest,
    private val connector: WebOsConnector
) {

    suspend fun pair(target: DiscoveredTv, existing: PairedTv?, onPromptShown: () -> Unit): PairedTv {
        val connection = connector.connect(
            ipAddress = target.ipAddress,
            certificatePin = existing?.certificatePin,
            allowInsecure = existing?.secure != true
        )
        connection.use {
            val clientKey = it.register(existing?.clientKey, manifest.json, onPromptShown)
            val macAddress = runCatching { MacAddressSelector.select(it.request(GET_NETWORK_INFO)) }.getOrNull()
            val systemModel = runCatching { it.request(GET_SYSTEM_INFO).optString("modelName") }
                .getOrNull()
                ?.ifBlank { null }

            return PairedTv(
                id = existing?.id ?: UUID.randomUUID().toString(),
                ipAddress = target.ipAddress,
                name = target.name ?: existing?.name ?: systemModel ?: target.ipAddress,
                modelName = target.modelName ?: systemModel ?: existing?.modelName,
                clientKey = clientKey,
                macAddress = macAddress ?: existing?.macAddress,
                certificatePin = it.certificatePin,
                secure = it.secure
            )
        }
    }

    private companion object {
        const val GET_NETWORK_INFO = "ssap://com.webos.service.connectionmanager/getinfo"
        const val GET_SYSTEM_INFO = "ssap://system/getSystemInfo"
    }
}
