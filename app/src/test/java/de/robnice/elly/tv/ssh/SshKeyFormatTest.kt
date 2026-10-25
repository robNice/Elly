package de.robnice.elly.tv.ssh

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec

class SshKeyFormatTest {

    private val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
    private val publicKey = keyPair.public as ECPublicKey

    @Test
    fun publicKeyBlobHasAlgorithmCurveAndUncompressedPoint() {
        val buffer = ByteBuffer.wrap(SshKeyFormat.publicKeyBlob(publicKey))

        assertEquals("ecdsa-sha2-nistp256", String(buffer.field()))
        assertEquals("nistp256", String(buffer.field()))
        val point = buffer.field()
        assertEquals(65, point.size)
        assertEquals(0x04.toByte(), point[0])
        assertEquals(publicKey.w.affineX, BigInteger(1, point.copyOfRange(1, 33)))
        assertEquals(publicKey.w.affineY, BigInteger(1, point.copyOfRange(33, 65)))
        assertEquals(0, buffer.remaining())
    }

    @Test
    fun signatureBlobCarriesTheSameSignatureAsDer() {
        val data = "session data".toByteArray()
        val der = Signature.getInstance("SHA256withECDSA").run {
            initSign(keyPair.private)
            update(data)
            sign()
        }

        val buffer = ByteBuffer.wrap(SshKeyFormat.signatureBlob(der))
        assertEquals("ecdsa-sha2-nistp256", String(buffer.field()))
        val inner = ByteBuffer.wrap(buffer.field())
        val r = BigInteger(inner.field())
        val s = BigInteger(inner.field())
        assertEquals(0, buffer.remaining())
        assertTrue(r.signum() > 0 && s.signum() > 0)

        val plain = r.toFixed() + s.toFixed()
        val valid = Signature.getInstance("SHA256withECDSAinP1363Format").run {
            initVerify(keyPair.public)
            update(data)
            verify(plain)
        }
        assertTrue(valid)
    }

    @Test
    fun authorizedKeysLineContainsBase64Blob() {
        val blob = SshKeyFormat.publicKeyBlob(publicKey)
        val parts = SshKeyFormat.authorizedKeysLine(blob, "elly-app").split(" ")

        assertEquals("ecdsa-sha2-nistp256", parts[0])
        assertArrayEquals(blob, java.util.Base64.getDecoder().decode(parts[1]))
        assertEquals("elly-app", parts[2])
    }

    @Test
    fun identitySignsWithDelegatedSigner() {
        val identity = EcdsaIdentity("test", publicKey) { data ->
            Signature.getInstance("SHA256withECDSA").run {
                initSign(keyPair.private)
                update(data)
                sign()
            }
        }

        assertEquals("ecdsa-sha2-nistp256", identity.algName)
        assertArrayEquals(SshKeyFormat.publicKeyBlob(publicKey), identity.publicKeyBlob)
        assertTrue(identity.getSignature("x".toByteArray())!!.isNotEmpty())
    }

    @Test
    fun identityReturnsNullWhenSigningFails() {
        val identity = EcdsaIdentity("test", publicKey) { throw IllegalStateException("locked") }

        assertEquals(null, identity.getSignature("x".toByteArray()))
    }

    private fun ByteBuffer.field(): ByteArray = ByteArray(int).also { get(it) }

    private fun BigInteger.toFixed(): ByteArray {
        val raw = toByteArray().dropWhile { it == 0.toByte() }.toByteArray()
        return ByteArray(32 - raw.size) + raw
    }
}
