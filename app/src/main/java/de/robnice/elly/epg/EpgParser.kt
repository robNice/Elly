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
                logoUrl = if (logoPath != null && logoServer != null) "$logoServer/${logoPath.substringAfterLast('/')}" else null
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
                imageUrl = item.optString("imgUrl").ifBlank { item.optString("fepgImgUrl") }.takeIf { it.startsWith("http") }
            )
        }
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
