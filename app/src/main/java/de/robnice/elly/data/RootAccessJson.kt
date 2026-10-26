package de.robnice.elly.data

import de.robnice.elly.data.model.RootAccess
import org.json.JSONObject

object RootAccessJson {

    private const val PORT = "port"
    private const val USER = "user"
    private const val HOST_KEY = "hostKey"
    private const val PASSWORD = "password"

    // The password is only written encrypted.
    fun encode(access: RootAccess, encrypt: (String) -> String): String = JSONObject()
        .put(PORT, access.port)
        .put(USER, access.user)
        .put(HOST_KEY, access.hostKey)
        .apply { access.password?.let { put(PASSWORD, encrypt(it)) } }
        .toString()

    fun decode(raw: String?, decrypt: (String) -> String?): RootAccess? {
        if (raw.isNullOrBlank()) return null
        return try {
            val json = JSONObject(raw)
            val port = json.getInt(PORT)
            val user = json.getString(USER)
            val hostKey = json.getString(HOST_KEY)
            val password = json.optString(PASSWORD).takeIf { it.isNotEmpty() }?.let(decrypt)
            if (port !in 1..65535 || user.isBlank() || hostKey.isBlank()) null else RootAccess(port, user, hostKey, password)
        } catch (_: Exception) {
            null
        }
    }
}
