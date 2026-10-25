package de.robnice.elly.epg

import android.util.Log
import de.robnice.elly.data.model.PairedTv
import de.robnice.elly.tv.ssh.RootAccessService
import de.robnice.elly.tv.ssh.TvLuna
import de.robnice.elly.tv.ssh.TvShellException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZoneId

// Guide data from the TV's own guide service (luna://com.webos.service.iepg) over SSH.
class RootedEpgSource(
    private val tv: PairedTv,
    private val rootAccess: RootAccessService,
    private val httpClient: OkHttpClient
) : EpgSource {

    // The TV always reports eight favorite groups; empty ones are left out.
    override suspend fun favoriteLists(): List<FavoriteList> = rootAccess.withLuna(tv) { luna ->
        EpgParser.favoriteLists(luna.call(CHANNEL_GROUPS).requireSuccess()).filter { list ->
            val payload = channelListPayload(list.id).put("requestField", JSONArray().put("channelId"))
            luna.callCompressed(CHANNEL_LIST, payload).optInt("channelListCount") > 0
        }
    }

    override suspend fun channels(listId: String): List<EpgChannel> = rootAccess.withLuna(tv) { luna ->
        EpgParser.channels(luna.callCompressed(CHANNEL_LIST, channelListPayload(listId)).requireSuccess(), listId)
    }

    private fun channelListPayload(listId: String): JSONObject =
        JSONObject()
            .put("dataType", 0)
            .put("sort", 0)
            .put(
                "attribute",
                JSONArray()
                    .put(JSONObject().put("filterType", "Invisible").put("value", false))
                    .put(JSONObject().put("filterType", "skipped").put("value", false))
            )
            .put("channelGroup", JSONArray().put(listId))
            .put("channelMode", JSONArray().put("Tuner").put("IP"))
            .put("format", 1)

    // Asked in pieces of a few channels and one day, so no single answer grows too large.
    override suspend fun programs(channels: List<EpgChannel>, from: Long, to: Long): List<EpgProgram> {
        if (channels.isEmpty() || to <= from) return emptyList()
        val zone = ZoneId.systemDefault()
        return rootAccess.withLuna(tv) { luna ->
            val found = LinkedHashMap<Triple<String, String, Long>, EpgProgram>()
            channels.chunked(CHANNELS_PER_CALL).forEach { group ->
                var start = from
                while (start < to) {
                    val end = minOf(start + DAY_MILLIS, to)
                    window(luna, group, start, end, zone).forEach { found[Triple(it.signalId, it.programId, it.start)] = it }
                    start = end
                }
            }
            found.values.toList()
        }
    }

    // Per channel the TV answers from one source only: if the window starts within the data received over the air,
    // the answer stops where that data ends, even when online data follows. Channels whose answer stops early are
    // asked again from that point.
    private suspend fun window(luna: TvLuna, channels: List<EpgChannel>, from: Long, to: Long, zone: ZoneId): List<EpgProgram> {
        val result = mutableListOf<EpgProgram>()
        var queue = mapOf(from to channels)
        repeat(MAX_ROUNDS) {
            if (queue.isEmpty()) return result
            val next = mutableMapOf<Long, MutableList<EpgChannel>>()
            queue.forEach { (start, group) ->
                val programs = eventList(luna, group, start, to, zone)
                result += programs
                val lastEnd = programs.groupBy { it.signalId }.mapValues { (_, list) -> list.maxOf { it.end } }
                group.forEach { channel ->
                    val end = lastEnd[channel.signalId]
                    if (end != null && end > start && end < to) next.getOrPut(end) { mutableListOf() } += channel
                }
            }
            queue = next
        }
        return result
    }

    private suspend fun eventList(luna: TvLuna, group: List<EpgChannel>, from: Long, to: Long, zone: ZoneId): List<EpgProgram> {
        val payload = JSONObject()
            .put("dataType", 0)
            .put("sort", 0)
            .put("channelId", JSONArray(group.map { it.id }))
            .put("localStartTime", EpgParser.localTime(from, zone))
            .put("localEndTime", EpgParser.localTime(to, zone))
        return EpgParser.programs(luna.callCompressed(EVENT_LIST, payload).requireSuccess())
    }

    // The TV keeps many logos as files; missing ones it loads from LG's logo server, and so does the app.
    override suspend fun logos(channels: List<EpgChannel>): Map<String, ByteArray> {
        val withFile = channels.filter { it.logoPath != null }
        val fromTv = if (withFile.isEmpty()) emptyMap() else runCatching {
            rootAccess.withLuna(tv) { luna ->
                withFile.mapNotNull { channel -> luna.readFile(channel.logoPath!!, LOGO_FOLDER)?.let { channel.id to it } }.toMap()
            }
        }.getOrDefault(emptyMap())
        val fromServer = channels.filter { it.id !in fromTv }.mapNotNull { channel -> download(channel)?.let { channel.id to it } }
        return fromTv + fromServer
    }

    private suspend fun download(channel: EpgChannel): ByteArray? {
        val url = channel.logoUrl ?: return null
        return withContext(Dispatchers.IO) {
            try {
                httpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    response.body?.bytes()?.takeIf { response.isSuccessful && it.isNotEmpty() }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Logo download failed: ${e.message}")
                null
            }
        }
    }

    private fun JSONObject.requireSuccess(): JSONObject {
        if (!optBoolean(RETURN_VALUE)) throw TvShellException.LunaUnavailable(optString(ERROR_TEXT))
        return this
    }

    private companion object {
        const val TAG = "EllyEpg"
        const val CHANNEL_GROUPS = "luna://com.webos.service.iepg/getChannelGroupList"
        const val CHANNEL_LIST = "luna://com.webos.service.iepg/getChannelList"
        const val EVENT_LIST = "luna://com.webos.service.iepg/getEventList"
        const val LOGO_FOLDER = "/mnt/lg/cmn_data/channel_logo/"
        const val RETURN_VALUE = "returnValue"
        const val ERROR_TEXT = "errorText"
        const val CHANNELS_PER_CALL = 26
        const val MAX_ROUNDS = 4
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
