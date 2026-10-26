package de.robnice.elly.data.model

// SSH access to a rooted TV; hostKey pins the TV. password is null for access set up by older versions,
// which logged in with a key stored on the TV.
data class RootAccess(
    val port: Int,
    val user: String,
    val hostKey: String,
    val password: String? = null
)
