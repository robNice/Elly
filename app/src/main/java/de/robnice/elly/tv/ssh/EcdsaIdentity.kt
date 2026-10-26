package de.robnice.elly.tv.ssh

import com.jcraft.jsch.Identity
import java.security.interfaces.ECPublicKey

// SSH identity whose private key stays outside JSch; signing is delegated, e.g. to the Android Keystore.
class EcdsaIdentity(
    private val name: String,
    publicKey: ECPublicKey,
    private val signDer: (ByteArray) -> ByteArray
) : Identity {

    private val blob = SshKeyFormat.publicKeyBlob(publicKey)

    val encodedPublicKey: String = SshKeyFormat.encode(blob)

    override fun setPassphrase(passphrase: ByteArray?): Boolean = true

    override fun getPublicKeyBlob(): ByteArray = blob.copyOf()

    // JSch treats null as a failed signature.
    override fun getSignature(data: ByteArray): ByteArray? =
        runCatching { SshKeyFormat.signatureBlob(signDer(data)) }.getOrNull()

    override fun getSignature(data: ByteArray, alg: String?): ByteArray? = getSignature(data)

    override fun getAlgName(): String = SshKeyFormat.ALGORITHM

    override fun getName(): String = name

    override fun isEncrypted(): Boolean = false

    override fun clear() = Unit
}
