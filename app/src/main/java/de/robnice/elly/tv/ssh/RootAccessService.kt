package de.robnice.elly.tv.ssh

import de.robnice.elly.data.RootAccessRepository
import de.robnice.elly.data.model.PairedTv
import de.robnice.elly.data.model.RootAccess
import org.json.JSONArray
import org.json.JSONObject

// Sets up, checks and removes the SSH access of the app to a rooted TV.
class RootAccessService(
    private val shell: TvShell,
    private val keys: SshKeyStore,
    private val repository: RootAccessRepository
) {

    // Logs in once with the password, adds the app key to authorized_keys and checks that it works.
    // The password is not stored.
    suspend fun setUp(tv: PairedTv, port: Int, user: String, password: String): RootAccess {
        val identity = keys.identity(tv.id)
        val install = INSTALL_KEY.format(identity.encodedPublicKey, identity.authorizedKeysLine(KEY_COMMENT))
        val hostKey = shell.session(SshTarget(tv.ipAddress, port, user, null), SshAuth.Password(password)) { session ->
            session.run(install).requireSuccess()
            session.hostKey
        }
        val access = RootAccess(port, user, hostKey)
        verify(tv, access)
        repository.save(tv.id, access)
        return access
    }

    suspend fun check(tv: PairedTv) {
        val access = repository.get(tv.id) ?: throw TvShellException.AuthFailed()
        verify(tv, access)
    }

    // Removes the key from the TV if it can be reached; the local key and settings are removed in any case.
    suspend fun remove(tv: PairedTv): Boolean {
        val access = repository.get(tv.id)
        val removedOnTv = access != null && runCatching {
            val identity = keys.identity(tv.id)
            shell.session(target(tv, access), SshAuth.Key(identity)) { session ->
                session.run(REMOVE_KEY.format(identity.encodedPublicKey)).requireSuccess()
            }
        }.isSuccess
        forget(tv.id)
        return removedOnTv
    }

    suspend fun forget(tvId: String) {
        repository.remove(tvId)
        keys.delete(tvId)
    }

    private suspend fun verify(tv: PairedTv, access: RootAccess) {
        val command = LunaCommand.build(SYSTEM_INFO, JSONObject().put(KEYS, JSONArray().put(MODEL_NAME)))
        val answer = shell.session(target(tv, access), SshAuth.Key(keys.identity(tv.id))) { session ->
            LunaCommand.parse(session.run(command).output)
        }
        if (!answer.optBoolean(RETURN_VALUE)) throw TvShellException.LunaUnavailable(answer.optString(ERROR_TEXT))
    }

    private fun target(tv: PairedTv, access: RootAccess) = SshTarget(tv.ipAddress, access.port, access.user, access.hostKey)

    private fun ShellResult.requireSuccess() {
        if (exitStatus != 0) throw TvShellException.CommandFailed(exitStatus, output.trim())
    }

    private companion object {
        const val KEY_COMMENT = "elly-app"
        const val SYSTEM_INFO = "luna://com.webos.service.tv.systemproperty/getSystemInfo"
        const val KEYS = "keys"
        const val MODEL_NAME = "modelName"
        const val RETURN_VALUE = "returnValue"
        const val ERROR_TEXT = "errorText"

        // %1$s is the Base64 key (letters, digits, +, / and =), %2$s the authorized_keys line built from it.
        const val INSTALL_KEY = "mkdir -p ~/.ssh && chmod 700 ~/.ssh && touch ~/.ssh/authorized_keys && " +
            "chmod 600 ~/.ssh/authorized_keys && " +
            "(grep -qF '%1\$s' ~/.ssh/authorized_keys || echo '%2\$s' >> ~/.ssh/authorized_keys)"
        const val REMOVE_KEY = "F=~/.ssh/authorized_keys; grep -vF '%1\$s' \$F > \$F.elly; mv \$F.elly \$F && chmod 600 \$F"
    }
}
