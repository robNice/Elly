package de.robnice.elly.tv.ssh

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec

// One SSH key per TV in the Android Keystore; the private key never leaves it.
class SshKeyStore {

    private val keyStore: KeyStore by lazy { KeyStore.getInstance(PROVIDER).apply { load(null) } }

    @Synchronized
    fun identity(tvId: String): EcdsaIdentity {
        val alias = alias(tvId)
        if (!keyStore.containsAlias(alias)) generate(alias)
        val publicKey = keyStore.getCertificate(alias).publicKey as ECPublicKey
        return EcdsaIdentity(alias, publicKey) { data -> sign(alias, data) }
    }

    @Synchronized
    fun delete(tvId: String) {
        val alias = alias(tvId)
        if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
    }

    private fun generate(alias: String) {
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec(CURVE))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, PROVIDER).apply {
            initialize(spec)
            generateKeyPair()
        }
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
        const val CURVE = "secp256r1"
        const val SIGNATURE = "SHA256withECDSA"
        const val ALIAS_PREFIX = "elly_ssh_"
    }
}
