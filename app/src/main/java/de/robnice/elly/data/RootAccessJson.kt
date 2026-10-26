package de.robnice.elly.data

import de.robnice.elly.data.model.RootAccess
import org.json.JSONObject

object RootAccessJson {

    private const val PORT = "port"
    private const val USER = "user"
    private const val HOST_KEY = "hostKey"
    private const val PASSWORD = "password"
    private const val PRIVATE_KEY = "privateKey"
    private const val PASSPHRASE = "passphrase"

    // Password, key and passphrase are only written encrypted.
    fun encode(access: RootAccess, encrypt: (String) -> String): String = JSONObject()
        .put(PORT, access.port)
        .put(USER, access.user)
        .put(HOST_KEY, access.hostKey)
        .apply {
            access.password?.let { put(PASSWORD, encrypt(it)) }
            access.privateKey?.let { put(PRIVATE_KEY, encrypt(it)) }
            access.passphrase?.let { put(PASSPHRASE, encrypt(it)) }
        }
        .toString()

    fun decode(raw: String?, decrypt: (String) -> String?): RootAccess? {
        if (raw.isNullOrBlank()) return null
        return try {
            val json = JSONObject(raw)
            val port = json.getInt(PORT)
            val user = json.getString(USER)
            val hostKey = json.getString(HOST_KEY)
            fun secret(name: String) = json.optString(name).takeIf { it.isNotEmpty() }?.let(decrypt)
            if (port !in 1..65535 || user.isBlank() || hostKey.isBlank()) {
                null
            } else {
                RootAccess(port, user, hostKey, secret(PASSWORD), secret(PRIVATE_KEY), secret(PASSPHRASE))
            }
        } catch (_: Exception) {
            null
        }
    }
}
