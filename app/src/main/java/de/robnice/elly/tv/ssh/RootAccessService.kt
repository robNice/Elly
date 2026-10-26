package de.robnice.elly.tv.ssh

import de.robnice.elly.data.RootAccessRepository
import de.robnice.elly.data.model.PairedTv
import de.robnice.elly.data.model.RootAccess
import org.json.JSONArray
import org.json.JSONObject

// Sets up, checks and removes the SSH access of the app to a rooted TV. The app logs in the way the TV is set up,
// with the root password or with a private key the user enters, keeps it encrypted on the phone and changes
// nothing on the TV. The only exception is removing the key older versions stored there, when the user asks for it.
class RootAccessService(
    private val shell: TvShell,
    private val keys: SshKeyStore,
    private val repository: RootAccessRepository
) {

    // auth is either SshAuth.Password or SshAuth.PrivateKey.
    suspend fun setUp(tv: PairedTv, port: Int, user: String, auth: SshAuth): RootAccess {
        if (auth is SshAuth.PrivateKey) shell.checkKey(auth.key, auth.passphrase)
        val hostKey = shell.session(SshTarget(tv.ipAddress, port, user, null), auth) { session -> session.hostKey }
        val access = RootAccess(
            port = port,
            user = user,
            hostKey = hostKey,
            password = (auth as? SshAuth.Password)?.password,
            privateKey = (auth as? SshAuth.PrivateKey)?.key,
            passphrase = (auth as? SshAuth.PrivateKey)?.passphrase
        )
        verify(tv, access)
        repository.save(tv.id, access)
        keys.delete(tv.id)
        return access
    }

    suspend fun check(tv: PairedTv) {
        verify(tv, current(tv))
    }

    suspend fun <T> withLuna(tv: PairedTv, block: suspend (TvLuna) -> T): T {
        val access = current(tv)
        return shell.session(target(tv, access), auth(tv, access)) { session -> block(TvLuna(session)) }
    }

    // Deletes Elly's old key from the TV and forgets the access. Other keys stay; an authorized_keys left empty
    // is deleted, because the Homebrew Channel only sets its default root password while that file does not exist.
    suspend fun removeOldKey(tv: PairedTv) {
        val access = current(tv)
        val oldKey = keys.existing(tv.id)
        if (!access.oldElly || oldKey == null) throw TvShellException.AuthFailed()
        shell.session(target(tv, access), SshAuth.Key(oldKey)) { session ->
            session.run(REMOVE_KEY.format(oldKey.encodedPublicKey)).requireSuccess()
        }
        forget(tv.id)
    }

    suspend fun forget(tvId: String) {
        repository.remove(tvId)
        keys.delete(tvId)
    }

    private suspend fun current(tv: PairedTv): RootAccess = repository.get(tv.id) ?: throw TvShellException.AuthFailed()

    private suspend fun verify(tv: PairedTv, access: RootAccess) {
        val command = LunaCommand.build(SYSTEM_INFO, JSONObject().put(KEYS, JSONArray().put(MODEL_NAME)))
        val answer = shell.session(target(tv, access), auth(tv, access)) { session ->
            LunaCommand.parse(session.run(command).output)
        }
        if (!answer.optBoolean(RETURN_VALUE)) throw TvShellException.LunaUnavailable(answer.optString(ERROR_TEXT))
    }

    private fun auth(tv: PairedTv, access: RootAccess): SshAuth = when {
        access.password != null -> SshAuth.Password(access.password)
        access.privateKey != null -> SshAuth.PrivateKey(access.privateKey, access.passphrase)
        else -> SshAuth.Key(keys.existing(tv.id) ?: throw TvShellException.AuthFailed())
    }

    private fun target(tv: PairedTv, access: RootAccess) = SshTarget(tv.ipAddress, access.port, access.user, access.hostKey)

    private fun ShellResult.requireSuccess() {
        if (exitStatus != 0) throw TvShellException.CommandFailed(exitStatus, output.trim())
    }

    private companion object {
        const val SYSTEM_INFO = "luna://com.webos.service.tv.systemproperty/getSystemInfo"
        const val KEYS = "keys"
        const val MODEL_NAME = "modelName"
        const val RETURN_VALUE = "returnValue"
        const val ERROR_TEXT = "errorText"

        // %1$s is the Base64 key (letters, digits, +, / and =).
        const val REMOVE_KEY = "F=~/.ssh/authorized_keys; [ -f \$F ] || exit 0; grep -vF '%1\$s' \$F > \$F.elly; " +
            "if [ -s \$F.elly ]; then mv \$F.elly \$F && chmod 600 \$F; else rm -f \$F.elly \$F; fi"
    }
}
