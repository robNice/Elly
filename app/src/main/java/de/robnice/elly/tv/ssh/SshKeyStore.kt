package de.robnice.elly.tv.ssh

import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.interfaces.ECPublicKey

// SSH keys that older versions created per TV in the Android Keystore and stored on the TV.
// They are only used once more, to remove them from the TV again.
class SshKeyStore {

    private val keyStore: KeyStore by lazy { KeyStore.getInstance(PROVIDER).apply { load(null) } }

    @Synchronized
    fun existing(tvId: String): EcdsaIdentity? {
        val alias = alias(tvId)
        if (!keyStore.containsAlias(alias)) return null
        val publicKey = keyStore.getCertificate(alias).publicKey as ECPublicKey
        return EcdsaIdentity(alias, publicKey) { data -> sign(alias, data) }
    }

    @Synchronized
    fun delete(tvId: String) {
        val alias = alias(tvId)
        if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
    }

    private fun sign(alias: String, data: ByteArray): ByteArray {
        val key = keyStore.getKey(alias, null) as PrivateKey
        return Signature.getInstance(SIGNATURE).run {
            initSign(key)
            update(data)
            sign()
        }
    }

    private fun alias(tvId: String) = "$ALIAS_PREFIX$tvId"

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val SIGNATURE = "SHA256withECDSA"
        const val ALIAS_PREFIX = "elly_ssh_"
    }
}
