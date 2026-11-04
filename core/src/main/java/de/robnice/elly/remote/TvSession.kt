package de.robnice.elly.remote

import de.robnice.elly.data.model.PairedTv
import de.robnice.elly.tv.connection.PointerSocket
import de.robnice.elly.tv.connection.WebOsConnection
import java.io.Closeable

class TvSession(
    val tv: PairedTv,
    val connection: WebOsConnection,
    pointer: PointerSocket
) : Closeable {

    var pointer: PointerSocket = pointer
        private set

    fun replacePointer(newPointer: PointerSocket) {
        pointer.close()
        pointer = newPointer
    }

    fun belongsTo(other: PairedTv): Boolean =
        tv.id == other.id && tv.ipAddress == other.ipAddress && tv.clientKey == other.clientKey

    override fun close() {
        pointer.close()
        connection.close()
    }
}
