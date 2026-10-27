package de.robnice.elly.epg

import de.robnice.elly.data.model.PairedTv
import de.robnice.elly.tv.ssh.RootAccessService
import de.robnice.elly.tv.ssh.TvLuna
import de.robnice.elly.tv.ssh.TvShellException
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

// Guide data from the TV's own guide service (luna://com.webos.service.iepg) over SSH.
class RootedEpgSource(
    private val tv: PairedTv,
    private val rootAccess: RootAccessService
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

    // The TV keeps many logos as files; missing ones it loads from LG's logo server for its own guide,
    // and here it does so on Elly's behalf, so the phone never contacts that server.
    override suspend fun logos(channels: List<EpgChannel>): Map<String, ByteArray> = rootAccess.withLuna(tv) { luna ->
        channels.mapNotNull { channel ->
            val bytes = channel.logoPath?.let { luna.readFile(it, LOGO_FOLDER) } ?: channel.logoUrl?.let { luna.download(it) }
            bytes?.let { channel.id to it }
        }.toMap()
    }

    override suspend fun reservations(channels: List<EpgChannel>, from: Long, to: Long): List<EpgReservation> {
        if (channels.isEmpty()) return emptyList()
        val zone = ZoneId.systemDefault()
        val payload = JSONObject()
            .put("channelId", JSONArray(channels.map { it.id }))
            .put("localStartTime", EpgParser.localTime(from, zone))
            .put("localEndTime", EpgParser.localTime(to, zone))
        return rootAccess.withLuna(tv) { luna -> EpgParser.reservations(luna.call(RESERVATIONS, payload).requireSuccess(), zone) }
    }

    override suspend fun canRecord(): Boolean = rootAccess.withLuna(tv) { storage(it) != null }

    // Same parameters as the TV's own scheduling app (com.webos.app.dvrpopup). As with recordings, programs
    // received over the air go by their broadcast ID; programs the TV only knows from LG's online data need the
    // network call with LG's channel code.
    override suspend fun remind(channel: EpgChannel, program: EpgProgram) {
        val payload = schedule(SCHEDULE_WATCHING, channel, program)
        val offlineId = program.offlineProgramId
        if (offlineId != null) {
            payload.put("programId", offlineId).put("programIdType", ID_OFFLINE)
            rootAccess.withLuna(tv) { luna -> luna.call(WATCH_ADD, payload).requireAction() }
        } else {
            val channelCode = channel.channelCode ?: throw EpgActionException(null)
            payload.put("programId", program.programId).networkProgram(channel, channelCode)
            rootAccess.withLuna(tv) { luna -> luna.call(WATCH_NETWORK_ADD, payload).requireAction() }
        }
    }

    // Programs received over the air are recorded by their broadcast ID. Programs the TV only knows from LG's
    // online data need the network call with LG's channel code; with their ID the first call answers
    // "Invalid program ID". Whether a tuner is free is left to the TV.
    override suspend fun record(channel: EpgChannel, program: EpgProgram) {
        val offlineId = program.offlineProgramId
        val channelCode = channel.channelCode
        if (offlineId == null && channelCode == null) throw EpgActionException(null)
        rootAccess.withLuna(tv) { luna ->
            val storage = storage(luna) ?: throw EpgActionException(null)
            val payload = schedule(SCHEDULE_RECORDING, channel, program)
                .put("quality", RECORD_QUALITY)
                .put("storagePath", storage.path)
                .put("storageNumber", storage.number)
                .put("storageId", storage.id)
                .put("bUSBStick", storage.usbStick)
            if (offlineId != null) {
                payload
                    .put("programId", offlineId)
                    .put("programIdType", ID_OFFLINE)
                    .put("includeCurrentRecording", true)
                    .put("dataType", 1)
                luna.call(RECORD_ADD, payload).requireAction()
            } else {
                payload
                    .put("programId", program.programId)
                    .networkProgram(channel, channelCode)
                luna.call(RECORD_NETWORK_ADD, payload).requireAction()
            }
        }
    }

    private fun JSONObject.networkProgram(channel: EpgChannel, channelCode: String?): JSONObject = this
        .put("channelCode", channelCode)
        .put("channelName", channel.name)
        .put("seriesGroupId", "")
        .put("seriesCRIDs", "")
        .put("posterImageUrl", "")
        .put("startPadding", 0)
        .put("endPadding", 0)

    override suspend fun cancel(reservation: EpgReservation) {
        val payload = JSONObject().put("scheduleIdList", JSONArray().put(reservation.id))
        rootAccess.withLuna(tv) { luna -> luna.call(RESERVATION_DELETE, payload).requireAction() }
    }

    // Recording IDs do not name the channel; the source list of each running recording does. With two tuners
    // several recordings may run at once.
    override suspend fun stopRecording(channel: EpgChannel) {
        rootAccess.withLuna(tv) { luna ->
            val ids = luna.call(RECORDING_IDS).optJSONArray("recordingIds") ?: throw EpgActionException(null)
            val recordingId = (0 until ids.length()).map { ids.optString(it) }.firstOrNull { id ->
                val sources = luna.call(RECORDING_SOURCES, JSONObject().put("recordingId", id)).optJSONArray("recordingSourceList")
                sources != null && (0 until sources.length()).any { sources.optJSONObject(it)?.optString("sourceId") == channel.id }
            } ?: throw EpgActionException(null)
            luna.call(RECORDING_STOP, JSONObject().put("recordingId", recordingId).put("needSave", true)).requireAction()
        }
    }

    // The banner's audio info would need the live TV app's own playback ID, so only the picture is read.
    override suspend fun videoInfo(channel: EpgChannel): EpgVideoInfo? = rootAccess.withLuna(tv) { luna ->
        val current = luna.call(CURRENT_CHANNEL).optString("channelId")
        if (current != channel.id) null else EpgParser.videoInfo(luna.call(VIDEO_INFO))
    }

    // Like the logos, the TV loads the picture from LG's server, so the phone never contacts it.
    override suspend fun programImage(url: String): ByteArray? = rootAccess.withLuna(tv) { it.download(url) }

    // The older broadcast service takes the channel on screen itself; the newer one needs the live TV app's playback ID.
    override suspend fun audioTracks(): AudioTracks = rootAccess.withLuna(tv) { luna ->
        val channelId = currentChannelId(luna)
        val setting = luna.call(SYSTEM_SETTINGS, JSONObject().put("category", "sound").put("keys", JSONArray().put(AUDIO_DESCRIPTION)))
        EpgParser.audioTracks(luna.call(AUDIO_TRACKS)).copy(
            channelId = channelId,
            descriptionEnabled = EpgParser.audioDescriptionEnabled(setting)
        )
    }

    // The TV applies the id to whatever channel runs, so it is only sent while that is still the listed one.
    override suspend fun selectAudioTrack(id: Int, channelId: String?): Boolean = rootAccess.withLuna(tv) { luna ->
        if (channelId == null || currentChannelId(luna) != channelId) return@withLuna false
        luna.call(AUDIO_TRACK_SELECT, JSONObject().put("broadcastId", "").put("audioLanguageId", id)).requireSuccess()
        true
    }

    override suspend fun setAudioDescription(enabled: Boolean) = rootAccess.withLuna(tv) { luna ->
        val settings = JSONObject().put(AUDIO_DESCRIPTION, if (enabled) "on" else "off")
        luna.call(SYSTEM_SETTINGS_SET, JSONObject().put("category", "sound").put("settings", settings)).requireSuccess()
        Unit
    }

    private suspend fun currentChannelId(luna: TvLuna): String? =
        luna.call(CURRENT_CHANNEL).optString("channelId").takeIf { it.isNotBlank() }

    private fun schedule(type: String, channel: EpgChannel, program: EpgProgram): JSONObject = JSONObject()
        .put("scheduleType", type)
        .put("channelId", channel.id)
        .put("startTime", timeObject(program.start))
        .put("endTime", timeObject(program.end))
        .put("repeatType", REPEAT_ONCE)
        .put("repeatedWeekday", 0)
        .put("title", program.title)

    private fun timeObject(millis: Long): JSONObject {
        val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        return JSONObject()
            .put("year", time.year)
            .put("month", time.monthValue)
            .put("day", time.dayOfMonth)
            .put("hour", time.hour)
            .put("minute", time.minute)
            .put("second", time.second)
    }

    private class RecordStorage(val path: String, val number: Int, val id: String, val usbStick: Boolean)

    // The storage chosen on the TV for recordings, if it is plugged in.
    private suspend fun storage(luna: TvLuna): RecordStorage? {
        val chosen = luna.call(SYSTEM_SETTINGS, JSONObject().put("category", "timemachine"))
            .optJSONObject("settings")?.optJSONObject("recordStorageValue") ?: return null
        val subDeviceId = chosen.optString("subDeviceId").takeIf { it.isNotBlank() } ?: return null
        val deviceId = chosen.optString("deviceId")
        val path = chosen.optString("devicePath").takeIf { it.isNotBlank() } ?: return null
        val devices = luna.call(LIST_DEVICES).optJSONArray("devices") ?: return null
        val device = (0 until devices.length()).mapNotNull { devices.optJSONObject(it) }
            .firstOrNull { it.optString("deviceId") == deviceId } ?: return null
        return RecordStorage(path, device.optInt("deviceNum"), subDeviceId, device.optString("physicalType") != PHYSICAL_HDD)
    }

    private fun JSONObject.requireAction() {
        if (!optBoolean(RETURN_VALUE)) throw EpgActionException(optString(ERROR_TEXT).takeIf { it.isNotBlank() })
    }

    private fun JSONObject.requireSuccess(): JSONObject {
        if (!optBoolean(RETURN_VALUE)) throw TvShellException.LunaUnavailable(optString(ERROR_TEXT))
        return this
    }

    private companion object {
        const val CHANNEL_GROUPS = "luna://com.webos.service.iepg/getChannelGroupList"
        const val CHANNEL_LIST = "luna://com.webos.service.iepg/getChannelList"
        const val EVENT_LIST = "luna://com.webos.service.iepg/getEventList"
        const val RESERVATIONS = "luna://com.webos.service.iepg/getReservationEventList"
        const val WATCH_ADD = "luna://com.webos.service.tvReservationAgent/watch/epgProgram/add"
        const val WATCH_NETWORK_ADD = "luna://com.webos.service.tvReservationAgent/watch/fullEpgProgram/add"
        const val RECORD_ADD = "luna://com.webos.service.tvReservationAgent/record/epgProgram/add"
        const val RECORD_NETWORK_ADD = "luna://com.webos.service.tvReservationAgent/record/fullEpgProgram/add"
        const val RESERVATION_DELETE = "luna://com.webos.service.tvReservationAgent/delete"
        const val RECORDING_IDS = "luna://com.webos.service.legacybroadcast.dvr/record/getRecordingIdList"
        const val RECORDING_SOURCES = "luna://com.webos.service.legacybroadcast.dvr/record/getRecordingSourceList"
        const val CURRENT_CHANNEL = "luna://com.webos.service.apiadapter/tv/getCurrentChannel"
        const val VIDEO_INFO = "luna://com.webos.service.utp.broadcast/getCurrentVideoInfo"
        const val AUDIO_TRACKS = "luna://com.webos.service.legacybroadcast.broadcast/getAudioLanguageList"
        const val AUDIO_TRACK_SELECT = "luna://com.webos.service.legacybroadcast.broadcast/setAudioLanguage"
        const val RECORDING_STOP ="luna://com.webos.service.legacybroadcast.dvr/record/stopRecord"
        const val SYSTEM_SETTINGS = "luna://com.lge.settingsservice/getSystemSettings"
        const val SYSTEM_SETTINGS_SET = "luna://com.webos.settingsservice/setSystemSettings"
        const val AUDIO_DESCRIPTION = "audioDescription"
        const val LIST_DEVICES = "luna://com.webos.service.attachedstoragemanager/listDevices"
        const val SCHEDULE_WATCHING = "watching"
        const val SCHEDULE_RECORDING = "recording"
        const val ID_OFFLINE = "offline"
        const val REPEAT_ONCE = "once"
        const val RECORD_QUALITY = "record_quality_high"
        const val PHYSICAL_HDD = "HDD"
        const val LOGO_FOLDER = "/mnt/lg/cmn_data/channel_logo/"
        const val RETURN_VALUE = "returnValue"
        const val ERROR_TEXT = "errorText"
        const val CHANNELS_PER_CALL = 26
        const val MAX_ROUNDS = 4
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
