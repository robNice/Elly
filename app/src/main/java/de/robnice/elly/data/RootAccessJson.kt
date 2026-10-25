package de.robnice.elly.data

import de.robnice.elly.data.model.RootAccess
import org.json.JSONObject

object RootAccessJson {

    private const val PORT = "port"
    private const val USER = "user"
    private const val HOST_KEY = "hostKey"

    fun encode(access: RootAccess): String = JSONObject()
        .put(PORT, access.port)
        .put(USER, access.user)
        .put(HOST_KEY, access.hostKey)
        .toString()

    fun decode(raw: String?): RootAccess? {
        if (raw.isNullOrBlank()) return null
        return try {
            val json = JSONObject(raw)
            val port = json.getInt(PORT)
            val user = json.getString(USER)
            val hostKey = json.getString(HOST_KEY)
            if (port !in 1..65535 || user.isBlank() || hostKey.isBlank()) null else RootAccess(port, user, hostKey)
        } catch (_: Exception) {
            null
        }
    }
}
