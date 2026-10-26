package de.robnice.elly.tv.ssh

import de.robnice.elly.data.RootAccessRepository
import de.robnice.elly.data.model.PairedTv
import de.robnice.elly.data.model.RootAccess
import org.json.JSONArray
import org.json.JSONObject

// Sets up, checks and removes the SSH access of the app to a rooted TV. The app logs in with the password,
// which it keeps encrypted on the phone, and changes nothing on the TV.
class RootAccessService(
    private val shell: TvShell,
    private val keys: SshKeyStore,
    private val repository: RootAccessRepository
) {

    // Removes a key an older version stored on the TV, if there is one.
    suspend fun setUp(tv: PairedTv, port: Int, user: String, password: String): RootAccess {
        val oldKey = keys.existing(tv.id)
        var oldKeyRemoved = oldKey == null
        val hostKey = shell.session(SshTarget(tv.ipAddress, port, user, null), SshAuth.Password(password)) { session ->
            if (oldKey != null) oldKeyRemoved = session.run(REMOVE_KEY.format(oldKey.encodedPublicKey)).exitStatus == 0
            session.hostKey
        }
        val access = RootAccess(port, user, hostKey, password)
        verify(tv, access)
        repository.save(tv.id, access)
        if (oldKeyRemoved) keys.delete(tv.id)
        return access
    }

    suspend fun check(tv: PairedTv) {
        verify(tv, current(tv))
    }

    // Only a key of an older version needs removing from the TV; the local settings are removed in any case.
    suspend fun remove(tv: PairedTv): Boolean {
        val access = repository.get(tv.id)
        val oldKey = keys.existing(tv.id)
        val removedOnTv = oldKey == null || access != null && runCatching {
            shell.session(target(tv, access), access.password?.let { SshAuth.Password(it) } ?: SshAuth.Key(oldKey)) { session ->
                session.run(REMOVE_KEY.format(oldKey.encodedPublicKey)).requireSuccess()
            }
        }.isSuccess
        forget(tv.id)
        return removedOnTv
    }

    suspend fun <T> withLuna(tv: PairedTv, block: suspend (TvLuna) -> T): T {
        val access = current(tv)
        return shell.session(target(tv, access), auth(access)) { session -> block(TvLuna(session)) }
    }

    suspend fun forget(tvId: String) {
        repository.remove(tvId)
        keys.delete(tvId)
    }

    // Access set up by an older version logs in with a key stored on the TV. That key is removed from the TV
    // and the access forgotten, so the user sets it up again with the password. If the TV cannot be reached,
    // this is tried again next time.
    private suspend fun current(tv: PairedTv): RootAccess {
        val access = repository.get(tv.id) ?: throw TvShellException.AuthFailed()
        if (access.password != null) return access
        val oldKey = keys.existing(tv.id)
        if (oldKey != null) {
            try {
                shell.session(target(tv, access), SshAuth.Key(oldKey)) { session ->
                    session.run(REMOVE_KEY.format(oldKey.encodedPublicKey)).requireSuccess()
                }
            } catch (_: TvShellException.AuthFailed) {
                // The key is no longer on the TV.
            }
        }
        forget(tv.id)
        throw TvShellException.AuthFailed()
    }

    private suspend fun verify(tv: PairedTv, access: RootAccess) {
        val command = LunaCommand.build(SYSTEM_INFO, JSONObject().put(KEYS, JSONArray().put(MODEL_NAME)))
        val answer = shell.session(target(tv, access), auth(access)) { session ->
            LunaCommand.parse(session.run(command).output)
        }
        if (!answer.optBoolean(RETURN_VALUE)) throw TvShellException.LunaUnavailable(answer.optString(ERROR_TEXT))
    }

    private fun auth(access: RootAccess): SshAuth = SshAuth.Password(access.password ?: throw TvShellException.AuthFailed())

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

        // %1$s is the Base64 key (letters, digits, +, / and =). An authorized_keys left empty is deleted, because
        // the Homebrew Channel only sets its default root password while that file does not exist.
        const val REMOVE_KEY = "F=~/.ssh/authorized_keys; [ -f \$F ] || exit 0; grep -vF '%1\$s' \$F > \$F.elly; " +
            "if [ -s \$F.elly ]; then mv \$F.elly \$F && chmod 600 \$F; else rm -f \$F.elly \$F; fi"
    }
}
