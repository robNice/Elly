package de.robnice.elly.epg

import org.json.JSONArray
import org.json.JSONObject

// Short field names: a week of guide data with descriptions runs to several megabytes.
object EpgCacheJson {

    private const val VERSION = "v"
    // 2: caches before it missed online programs after the data received over the air.
    // 3: channels carry LG's channel code. 4: channels carry satellite and HD, programs their access services.
    // Older caches from version 2 on keep their programs but reload the channel lists.
    private const val CURRENT_VERSION = 4
    private const val OLDEST_KEPT_VERSION = 2
    private const val LISTS = "lists"
    private const val PROGRAMS = "programs"
    private const val CHANNELS_UPDATED = "channelsUpdated"
    private const val NEAR_UPDATED = "near"
    private const val FAR_UPDATED = "far"

    fun encode(cache: EpgCache): String {
        val lists = JSONObject()
        cache.lists.forEach { (id, channels) -> lists.put(id, JSONArray(channels.map(::channel))) }
        val programs = JSONObject()
        cache.programs.forEach { (signalId, list) -> programs.put(signalId, JSONArray(list.map(::program))) }
        val updated = JSONObject()
        cache.channelsUpdated.forEach { (id, time) -> updated.put(id, time) }
        return JSONObject()
            .put(VERSION, CURRENT_VERSION)
            .put(LISTS, lists)
            .put(PROGRAMS, programs)
            .put(CHANNELS_UPDATED, updated)
            .put(NEAR_UPDATED, cache.nearUpdated)
            .put(FAR_UPDATED, cache.farUpdated)
            .toString()
    }

    fun decode(raw: String?): EpgCache {
        if (raw.isNullOrBlank()) return EpgCache()
        return try {
            val json = JSONObject(raw)
            val version = json.optInt(VERSION)
            if (version !in OLDEST_KEPT_VERSION..CURRENT_VERSION) return EpgCache()
            val lists = json.optJSONObject(LISTS)
            val programs = json.optJSONObject(PROGRAMS)
            val updated = json.optJSONObject(CHANNELS_UPDATED)
            EpgCache(
                lists = lists?.keys()?.asSequence()?.associateWith { id -> lists.getJSONArray(id).objects().mapNotNull(::channel) }.orEmpty(),
                programs = programs?.keys()?.asSequence()?.associateWith { signalId ->
                    programs.getJSONArray(signalId).objects().mapNotNull { program(signalId, it) }
                }.orEmpty(),
                channelsUpdated = if (version < CURRENT_VERSION) {
                    emptyMap()
                } else {
                    updated?.keys()?.asSequence()?.associateWith { updated.getLong(it) }.orEmpty()
                },
                nearUpdated = json.optLong(NEAR_UPDATED),
                farUpdated = json.optLong(FAR_UPDATED)
            )
        } catch (_: Exception) {
            EpgCache()
        }
    }

    private fun channel(channel: EpgChannel) = JSONObject()
        .put("i", channel.id)
        .put("s", channel.signalId)
        .put("n", channel.number)
        .put("t", channel.name)
        .putOpt("lp", channel.logoPath)
        .putOpt("lu", channel.logoUrl)
        .putOpt("cc", channel.channelCode)
        .putOpt("sat", channel.satellite)
        .putOpt("fq", channel.frequency)
        .put("hd", channel.hd)

    private fun channel(json: JSONObject): EpgChannel? {
        val id = json.optString("i").takeIf { it.isNotEmpty() } ?: return null
        val signalId = json.optString("s").takeIf { it.isNotEmpty() } ?: return null
        return EpgChannel(
            id, signalId, json.optString("n"), json.optString("t"), json.text("lp"), json.text("lu"), json.text("cc"),
            json.text("sat"), json.optInt("fq").takeIf { it > 0 }, json.optBoolean("hd")
        )
    }

    private fun program(program: EpgProgram) = JSONObject()
        .put("p", program.programId)
        .put("b", program.start)
        .put("e", program.end)
        .put("t", program.title)
        .putIfNotEmpty("st", program.subtitle)
        .putIfNotEmpty("d", program.description)
        .putIfNotEmpty("x", program.details)
        .putIfNotEmpty("g", program.genre)
        .putOpt("o", program.offlineProgramId)
        .put("on", program.online)
        .putOpt("img", program.imageUrl)
        .putIfTrue("sub", program.subtitles)
        .putIfTrue("ad", program.audioDescription)
        .putIfTrue("sl", program.signLanguage)

    private fun program(signalId: String, json: JSONObject): EpgProgram? {
        val start = json.optLong("b")
        val end = json.optLong("e")
        if (end <= start) return null
        return EpgProgram(
            signalId = signalId,
            programId = json.optString("p"),
            start = start,
            end = end,
            title = json.optString("t"),
            subtitle = json.optString("st"),
            description = json.optString("d"),
            details = json.optString("x"),
            genre = json.optString("g"),
            offlineProgramId = json.text("o"),
            online = json.optBoolean("on"),
            imageUrl = json.text("img"),
            subtitles = json.optBoolean("sub"),
            audioDescription = json.optBoolean("ad"),
            signLanguage = json.optBoolean("sl")
        )
    }

    private fun JSONObject.putIfTrue(key: String, value: Boolean): JSONObject = if (value) put(key, true) else this

    private fun JSONObject.putIfNotEmpty(key: String, value: String): JSONObject = if (value.isEmpty()) this else put(key, value)

    private fun JSONObject.text(key: String): String? = optString(key).takeIf { it.isNotEmpty() }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
}
