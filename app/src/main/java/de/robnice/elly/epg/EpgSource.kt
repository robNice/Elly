package de.robnice.elly.epg

data class FavoriteList(val id: String, val name: String)

// id is what the TV switches to, signalId links the channel to its programs.
data class EpgChannel(
    val id: String,
    val signalId: String,
    val number: String,
    val name: String,
    val logoPath: String? = null,
    val logoUrl: String? = null,
    val channelCode: String? = null,
    val satellite: String? = null,
    val frequency: Int? = null,
    val hd: Boolean = false
)

// Times in milliseconds since 1970 (UTC). offlineProgramId is only set for programs received over the air;
// the others are recorded via LG's channel code.
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
    val imageUrl: String? = null,
    val subtitles: Boolean = false,
    val audioDescription: Boolean = false,
    val signLanguage: Boolean = false
)

// Picture of the channel currently on the TV, from the received signal.
data class EpgVideoInfo(val resolution: String?, val aspectRatio: String?, val hdr: String?)

enum class ReservationType { REMINDER, RECORDING }

// A reminder or recording planned on the TV; it belongs to the program of that channel starting at start.
data class EpgReservation(
    val id: String,
    val type: ReservationType,
    val signalId: String,
    val start: Long,
    val title: String
) {
    // The TV keeps reservation times without seconds, while programs may start at e.g. 22:01:49.
    fun belongsTo(program: EpgProgram): Boolean {
        val minute = program.start - Math.floorMod(program.start, MINUTE_MILLIS)
        return signalId == program.signalId && start >= minute && start < minute + MINUTE_MILLIS
    }

    private companion object {
        const val MINUTE_MILLIS = 60_000L
    }
}

sealed class EpgAction {
    class Remind(val channel: EpgChannel, val program: EpgProgram) : EpgAction()
    class Record(val channel: EpgChannel, val program: EpgProgram) : EpgAction()
    class Cancel(val reservation: EpgReservation) : EpgAction()
    class StopRecording(val channel: EpgChannel, val reservation: EpgReservation) : EpgAction()
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

    // Ends a running recording of this channel and keeps what was recorded so far.
    suspend fun stopRecording(channel: EpgChannel)

    // Null unless this channel is the one on the TV right now.
    suspend fun videoInfo(channel: EpgChannel): EpgVideoInfo?

    suspend fun favoriteLists(): List<FavoriteList>

    suspend fun channels(listId: String): List<EpgChannel>

    suspend fun programs(channels: List<EpgChannel>, from: Long, to: Long): List<EpgProgram>

    // Logos by channel id; channels without a logo are left out.
    suspend fun logos(channels: List<EpgChannel>): Map<String, ByteArray>
}
