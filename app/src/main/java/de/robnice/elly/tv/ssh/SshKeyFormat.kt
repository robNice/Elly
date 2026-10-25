package de.robnice.elly.tv.ssh

import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.interfaces.ECPublicKey
import java.util.Base64

// Wire format of ECDSA P-256 keys and signatures as used by SSH (RFC 5656).
object SshKeyFormat {

    const val ALGORITHM = "ecdsa-sha2-nistp256"
    private const val CURVE = "nistp256"
    private const val COORDINATE_BYTES = 32
    private const val UNCOMPRESSED_POINT: Byte = 0x04
    private const val DER_SEQUENCE = 0x30
    private const val DER_INTEGER = 0x02

    fun publicKeyBlob(key: ECPublicKey): ByteArray {
        val point = byteArrayOf(UNCOMPRESSED_POINT) +
            key.w.affineX.toFixedLength(COORDINATE_BYTES) +
            key.w.affineY.toFixedLength(COORDINATE_BYTES)
        return SshBuffer().string(ALGORITHM).string(CURVE).bytes(point).toByteArray()
    }

    // Java signs in DER; SSH expects r and s as mpints, which have the same layout as DER integers.
    fun signatureBlob(der: ByteArray): ByteArray {
        val reader = DerReader(der)
        reader.expect(DER_SEQUENCE)
        reader.length()
        val r = reader.integer()
        val s = reader.integer()
        val inner = SshBuffer().bytes(r).bytes(s).toByteArray()
        return SshBuffer().string(ALGORITHM).bytes(inner).toByteArray()
    }

    fun encode(blob: ByteArray): String = Base64.getEncoder().encodeToString(blob)

    fun authorizedKeysLine(blob: ByteArray, comment: String): String = "$ALGORITHM ${encode(blob)} $comment"

    private fun BigInteger.toFixedLength(size: Int): ByteArray {
        val raw = toByteArray().let { if (it.size > size && it[0] == 0.toByte()) it.copyOfRange(it.size - size, it.size) else it }
        require(raw.size <= size) { "Coordinate too long" }
        return ByteArray(size - raw.size) + raw
    }

    private class SshBuffer {
        private val out = ByteArrayOutputStream()

        fun string(value: String): SshBuffer = bytes(value.toByteArray(Charsets.US_ASCII))

        fun bytes(value: ByteArray): SshBuffer {
            val size = value.size
            out.write(byteArrayOf((size ushr 24).toByte(), (size ushr 16).toByte(), (size ushr 8).toByte(), size.toByte()))
            out.write(value)
            return this
        }

        fun toByteArray(): ByteArray = out.toByteArray()
    }

    private class DerReader(private val data: ByteArray) {
        private var position = 0

        fun expect(tag: Int) {
            require(position < data.size && data[position].toInt() and 0xFF == tag) { "Unexpected DER tag" }
            position++
        }

        fun length(): Int {
            val first = next()
            if (first < 0x80) return first
            var length = 0
            repeat(first and 0x7F) { length = (length shl 8) or next() }
            return length
        }

        fun integer(): ByteArray {
            expect(DER_INTEGER)
            val length = length()
            require(position + length <= data.size) { "DER integer too long" }
            return data.copyOfRange(position, position + length).also { position += length }
        }

        private fun next(): Int {
            require(position < data.size) { "DER data too short" }
            return data[position++].toInt() and 0xFF
        }
    }
}
