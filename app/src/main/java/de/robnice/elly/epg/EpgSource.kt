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

// Where the program guide gets its data from: the rooted TV or the demo.
interface EpgSource {
    suspend fun favoriteLists(): List<FavoriteList>

    suspend fun channels(listId: String): List<EpgChannel>

    suspend fun programs(channels: List<EpgChannel>, from: Long, to: Long): List<EpgProgram>

    // Logos by channel id; channels without a logo are left out.
    suspend fun logos(channels: List<EpgChannel>): Map<String, ByteArray>
}
