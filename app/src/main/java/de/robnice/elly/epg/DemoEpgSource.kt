package de.robnice.elly.epg

import android.content.Context
import de.robnice.elly.R
import kotlin.random.Random

// Invented channels and programs for demo mode, so the guide can be tried without a rooted TV.
// The same channel and day always give the same programs.
class DemoEpgSource(private val context: Context) : EpgSource {

    override suspend fun favoriteLists(): List<FavoriteList> = listOf(
        FavoriteList(MAIN_LIST, context.getString(R.string.epg_demo_list_main)),
        FavoriteList(KIDS_LIST, context.getString(R.string.epg_demo_list_kids))
    )

    override suspend fun channels(listId: String): List<EpgChannel> {
        val (names, prefix) = when (listId) {
            KIDS_LIST -> context.resources.getStringArray(R.array.epg_demo_kids_channels) to KIDS_PREFIX
            else -> context.resources.getStringArray(R.array.epg_demo_channels) to MAIN_PREFIX
        }
        return names.mapIndexed { index, name ->
            val id = "$prefix${index + 1}"
            EpgChannel(id = id, signalId = id, number = (index + 1).toString(), name = name)
        }
    }

    override suspend fun programs(channels: List<EpgChannel>, from: Long, to: Long): List<EpgProgram> {
        val titles = context.resources.getStringArray(R.array.epg_demo_titles)
        val kidsTitles = context.resources.getStringArray(R.array.epg_demo_kids_titles)
        val description = context.getString(R.string.epg_demo_description)
        return channels.flatMap { channel ->
            val pool = if (channel.id.startsWith(KIDS_PREFIX)) kidsTitles else titles
            val firstDay = Math.floorDiv(from, DAY_MILLIS)
            val lastDay = Math.floorDiv(to - 1, DAY_MILLIS)
            (firstDay..lastDay).flatMap { day -> day(channel, day, pool, description) }
                .filter { it.end > from && it.start < to }
        }
    }

    private fun day(channel: EpgChannel, day: Long, titles: Array<String>, description: String): List<EpgProgram> {
        val random = Random(channel.id.hashCode() * 31L + day)
        val programs = mutableListOf<EpgProgram>()
        var start = day * DAY_MILLIS
        val end = start + DAY_MILLIS
        while (start < end) {
            val length = minOf(LENGTHS_MINUTES[random.nextInt(LENGTHS_MINUTES.size)] * MINUTE_MILLIS, end - start)
            programs += EpgProgram(
                signalId = channel.signalId,
                programId = "${channel.id}_$start",
                start = start,
                end = start + length,
                title = titles[random.nextInt(titles.size)],
                description = description,
                offlineProgramId = "${channel.id}_$start"
            )
            start += length
        }
        return programs
    }

    override suspend fun logos(channels: List<EpgChannel>): Map<String, ByteArray> = emptyMap()

    // Demo reservations live only while the app runs.
    private val planned = mutableListOf<EpgReservation>()

    override suspend fun reservations(channels: List<EpgChannel>, from: Long, to: Long): List<EpgReservation> =
        synchronized(planned) { planned.filter { it.start in from until to }.toList() }

    override suspend fun canRecord(): Boolean = true

    override suspend fun remind(channel: EpgChannel, program: EpgProgram) = plan(ReservationType.REMINDER, program)

    override suspend fun record(channel: EpgChannel, program: EpgProgram) = plan(ReservationType.RECORDING, program)

    override suspend fun cancel(reservation: EpgReservation) {
        synchronized(planned) { planned.removeAll { it.id == reservation.id } }
    }

    override suspend fun stopRecording(channel: EpgChannel) {
        val now = System.currentTimeMillis()
        synchronized(planned) {
            planned.removeAll { it.type == ReservationType.RECORDING && it.signalId == channel.signalId && it.start <= now }
        }
    }

    override suspend fun videoInfo(channel: EpgChannel): EpgVideoInfo? = null

    private fun plan(type: ReservationType, program: EpgProgram) {
        synchronized(planned) {
            planned += EpgReservation("${type.name}_${program.programId}", type, program.signalId, program.start, program.title)
        }
    }

    private companion object {
        const val MAIN_LIST = "A"
        const val KIDS_LIST = "B"
        const val MAIN_PREFIX = "demo_"
        const val KIDS_PREFIX = "demo_kids_"
        const val MINUTE_MILLIS = 60_000L
        const val DAY_MILLIS = 24 * 60 * MINUTE_MILLIS
        val LENGTHS_MINUTES = intArrayOf(15, 25, 30, 45, 45, 60, 60, 90, 105, 120)
    }
}
