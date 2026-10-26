package de.robnice.elly.epg

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// Reads the answers of luna://com.webos.service.iepg.
object EpgParser {

    private val TV_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy,MM,dd,HH,mm,ss")

    // getChannelGroupList: the favorite groups carry their code (A to H) and the name the user gave them.
    fun favoriteLists(answer: JSONObject): List<FavoriteList> {
        val groups = answer.optJSONArray("channelGroupList") ?: return emptyList()
        return (0 until groups.length()).mapNotNull { index ->
            val group = groups.optJSONObject(index) ?: return@mapNotNull null
            if (!group.optBoolean("isFavoriteGroup")) return@mapNotNull null
            val id = group.optString("channelGroupName").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val name = group.optString("userEditedName").ifBlank { id }
            FavoriteList(id, name)
        }.sortedBy { it.id }
    }

    // getChannelList with "format":1 sends a table: "names" maps each field to its column in the rows of "values".
    fun channels(answer: JSONObject, listId: String): List<EpgChannel> {
        val names = answer.optJSONObject("names") ?: return emptyList()
        val rows = answer.optJSONArray("values") ?: return emptyList()
        val logoServer = answer.optString("channelLogoServerUrl").takeIf { it.startsWith("https://") }
        fun column(name: String) = names.optInt(name, -1)
        val id = column("channelId")
        val signalId = column("signalChannelId")
        val number = column("channelNumber")
        val name = column("channelName")
        val logo = column("imgUrl")
        val channelCode = column("chanCode")
        val satellite = column("satelliteName")
        val frequency = column("Frequency")
        val hd = column("HDTV")
        val position = column("favoriteIdx$listId")
        if (id < 0 || signalId < 0 || name < 0) return emptyList()
        return (0 until rows.length()).mapNotNull { index ->
            val row = rows.optJSONArray(index) ?: return@mapNotNull null
            val channelId = row.text(id) ?: return@mapNotNull null
            val logoPath = row.text(logo)
            val channel = EpgChannel(
                id = channelId,
                signalId = row.text(signalId) ?: return@mapNotNull null,
                number = row.text(number).orEmpty(),
                name = row.text(name).orEmpty(),
                logoPath = logoPath,
                logoUrl = if (logoPath != null && logoServer != null) "$logoServer/${logoPath.substringAfterLast('/')}" else null,
                channelCode = row.text(channelCode),
                satellite = row.text(satellite),
                frequency = if (frequency >= 0) row.optInt(frequency).takeIf { it > 0 } else null,
                hd = hd >= 0 && row.optBoolean(hd)
            )
            channel to (if (position >= 0) row.optInt(position, Int.MAX_VALUE) else index)
        }.sortedBy { it.second }.map { it.first }
    }

    // getEventList: start and end come in UTC as well as in local time; UTC is used.
    fun programs(answer: JSONObject): List<EpgProgram> {
        val list = answer.optJSONArray("programList") ?: return emptyList()
        return (0 until list.length()).mapNotNull { index ->
            val item = list.optJSONObject(index) ?: return@mapNotNull null
            val start = utc(item.optString("startTime")) ?: return@mapNotNull null
            val end = utc(item.optString("endTime")) ?: return@mapNotNull null
            if (end <= start) return@mapNotNull null
            EpgProgram(
                signalId = item.optString("signalChannelId").takeIf { it.isNotBlank() } ?: return@mapNotNull null,
                programId = item.optString("programId"),
                start = start,
                end = end,
                title = item.optString("programName").ifBlank { item.optString("programMainTitle") },
                subtitle = item.optString("programSubTitle"),
                description = item.optString("description"),
                details = extendedText(item.optJSONArray("extendedDescription")),
                genre = item.optString("genre"),
                offlineProgramId = item.optString("offlineProgramId").takeIf { it.isNotBlank() },
                online = item.optInt("epgDataType", 1) != 1,
                imageUrl = item.optString("imgUrl").ifBlank { item.optString("fepgImgUrl") }.takeIf { it.startsWith("http") },
                subtitles = item.optBoolean("isSubtitle"),
                audioDescription = item.optBoolean("isAD"),
                signLanguage = item.optBoolean("isSignLanguage")
            )
        }
    }

    // getReservationEventList: start times are local; reserveType 0 is a reminder, 1 a recording.
    fun reservations(answer: JSONObject, zone: ZoneId): List<EpgReservation> {
        val results = answer.optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).mapNotNull { index ->
            val item = results.optJSONObject(index) ?: return@mapNotNull null
            val start = runCatching {
                LocalDateTime.parse(item.optString("startTime"), TV_TIME).atZone(zone).toInstant().toEpochMilli()
            }.getOrNull() ?: return@mapNotNull null
            EpgReservation(
                id = item.optString("_id").takeIf { it.isNotBlank() } ?: return@mapNotNull null,
                type = if (item.optInt("reserveType") == 1) ReservationType.RECORDING else ReservationType.REMINDER,
                signalId = item.optString("signalChannelId").takeIf { it.isNotBlank() } ?: return@mapNotNull null,
                start = start,
                title = item.optString("title")
            )
        }
    }

    // utp.broadcast/getCurrentVideoInfo: e.g. 1920 x 1080, VIDEO_INTERLACED, DISPLAY_RATIO_16_9, hdrType none.
    fun videoInfo(answer: JSONObject): EpgVideoInfo? {
        val video = answer.optJSONObject("videoInfo")?.optJSONObject("video") ?: return null
        val height = video.optInt("height")
        val scan = if (video.optString("scanType").contains("INTERLACED")) "i" else "p"
        val ratio = video.optString("ratio").removePrefix("DISPLAY_RATIO_").takeIf { it.matches(RATIO) }?.replace('_', ':')
        val hdr = video.optString("hdrType").takeIf { it.isNotBlank() && !it.equals("none", ignoreCase = true) }?.uppercase()
        return EpgVideoInfo(if (height > 0) "$height$scan" else null, ratio, hdr)
    }

    private val RATIO = Regex("^\\d+_\\d+$")

    // legacybroadcast.broadcast/getAudioLanguageList: visualImpaired marks the audio description,
    // codecs like AUDIO_CODEC_AC3_DOLBY the Dolby track. Without a TV channel on screen the list is missing.
    fun audioTracks(answer: JSONObject): AudioTracks {
        val list = answer.optJSONArray("audioLanguageList") ?: return AudioTracks(emptyList(), null)
        val tracks = (0 until list.length()).mapNotNull { index ->
            val track = list.optJSONObject(index) ?: return@mapNotNull null
            val codec = track.optString("audioCodecType")
            AudioTrack(
                id = track.optInt("audioLanguageId", -1).takeIf { it >= 0 } ?: return@mapNotNull null,
                language = track.optString("translatedAudioLanguageName").ifBlank { track.optString("audioLanguageName") },
                description = track.optBoolean("visualImpaired"),
                hearingImpaired = track.optBoolean("hearingImpaired"),
                dolby = DOLBY_CODECS.any { codec.contains(it) }
            )
        }
        return AudioTracks(tracks, if (answer.has("currentAudioLanguageId")) answer.optInt("currentAudioLanguageId") else null)
    }

    private val DOLBY_CODECS = listOf("DOLBY", "AC3", "AC4")

    // getSystemSettings, category sound: audioDescription is "on" or "off".
    fun audioDescriptionEnabled(answer: JSONObject): Boolean? =
        when (answer.optJSONObject("settings")?.optString("audioDescription")) {
            "on" -> true
            "off" -> false
            else -> null
        }

    fun localTime(millis: Long, zone: ZoneId): String =
        LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), zone).format(TV_TIME)

    private fun utc(text: String): Long? = runCatching {
        LocalDateTime.parse(text, TV_TIME).toInstant(ZoneOffset.UTC).toEpochMilli()
    }.getOrNull()

    // Long descriptions arrive in numbered pieces that are cut mid-word.
    private fun extendedText(parts: JSONArray?): String {
        if (parts == null) return ""
        return (0 until parts.length())
            .mapNotNull { parts.optJSONObject(it) }
            .sortedBy { it.optInt("extendedDescriptionNumber") }
            .joinToString("") { it.optString("extendedText") }
            .trim()
    }

    private fun JSONArray.text(column: Int): String? =
        if (column < 0) null else optString(column).trim().takeIf { it.isNotEmpty() && it != "null" }
}
