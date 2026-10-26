package de.robnice.elly.data.model

// SSH access to a rooted TV; hostKey pins the TV. Elly logs in either with the password or with the private key
// (and its passphrase) the user entered. Access set up by older versions has neither and logs in with the key
// those versions stored on the TV.
data class RootAccess(
    val port: Int,
    val user: String,
    val hostKey: String,
    val password: String? = null,
    val privateKey: String? = null,
    val passphrase: String? = null
) {
    val oldElly: Boolean get() = password == null && privateKey == null
}
