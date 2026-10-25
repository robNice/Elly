package de.robnice.elly.epg

data class FavoriteList(val id: String, val name: String)

// id is what the TV switches to, signalId links the channel to its programs.
data class EpgChannel(
    val id: String,
    val signalId: String,
    val number: String,
    val name: String,
    val logoPath: String? = null,
    val logoUrl: String? = null
)

// Times in milliseconds since 1970 (UTC). offlineProgramId is only set for programs received over the air,
// which are the only ones the TV can record.
data class EpgProgram(
    val signalId: String,
    val programId: String,
    val start: Long,
    val end: Long,
    val title: String,
    val subtitle: String = "",
    val description: String = "",
    val details: String = "",
    val genre: String = "",
    val offlineProgramId: String? = null,
    val online: Boolean = false,
    val imageUrl: String? = null
)

enum class ReservationType { REMINDER, RECORDING }

// A reminder or recording planned on the TV; it belongs to the program of that channel starting at start.
data class EpgReservation(
    val id: String,
    val type: ReservationType,
    val signalId: String,
    val start: Long,
    val title: String
)

sealed class EpgAction {
    class Remind(val channel: EpgChannel, val program: EpgProgram) : EpgAction()
    class Record(val channel: EpgChannel, val program: EpgProgram) : EpgAction()
    class Cancel(val reservation: EpgReservation) : EpgAction()
}

// Thrown when the TV refuses a reminder or recording; reason is the TV's own text.
class EpgActionException(val reason: String?) : Exception(reason)

// Where the program guide gets its data from: the rooted TV or the demo.
interface EpgSource {
    suspend fun reservations(channels: List<EpgChannel>, from: Long, to: Long): List<EpgReservation>

    suspend fun canRecord(): Boolean

    suspend fun remind(channel: EpgChannel, program: EpgProgram)

    suspend fun record(channel: EpgChannel, program: EpgProgram)

    suspend fun cancel(reservation: EpgReservation)

    suspend fun favoriteLists(): List<FavoriteList>

    suspend fun channels(listId: String): List<EpgChannel>

    suspend fun programs(channels: List<EpgChannel>, from: Long, to: Long): List<EpgProgram>

    // Logos by channel id; channels without a logo are left out.
    suspend fun logos(channels: List<EpgChannel>): Map<String, ByteArray>
}
