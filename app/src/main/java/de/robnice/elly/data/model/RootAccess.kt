package de.robnice.elly.data.model

// SSH access to a rooted TV. The key itself lives in the Android Keystore, hostKey pins the TV.
data class RootAccess(
    val port: Int,
    val user: String,
    val hostKey: String
)
