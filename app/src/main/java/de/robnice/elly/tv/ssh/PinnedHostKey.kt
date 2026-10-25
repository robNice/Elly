package de.robnice.elly.tv.ssh

import com.jcraft.jsch.HostKey
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.UserInfo
import java.util.Base64

// Trust on first use: without a pin every key is accepted and remembered, afterwards only the pinned one.
internal class PinnedHostKey(private val pinned: String?) : HostKeyRepository {

    var seen: String? = null
        private set

    override fun check(host: String?, key: ByteArray): Int {
        val encoded = Base64.getEncoder().encodeToString(key)
        seen = encoded
        return if (pinned == null || pinned == encoded) HostKeyRepository.OK else HostKeyRepository.CHANGED
    }

    override fun add(hostkey: HostKey?, ui: UserInfo?) = Unit

    override fun remove(host: String?, type: String?) = Unit

    override fun remove(host: String?, type: String?, key: ByteArray?) = Unit

    override fun getKnownHostsRepositoryID(): String = ID

    override fun getHostKey(): Array<HostKey> = emptyArray()

    override fun getHostKey(host: String?, type: String?): Array<HostKey> = emptyArray()

    private companion object {
        const val ID = "elly"
    }
}
