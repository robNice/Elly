package de.robnice.elly.epg

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class EpgParserTest {

    // Shortened answers of luna://com.webos.service.iepg from an LG OLED55C27LA (webOS 10.3.1).
    private val channelGroups = JSONObject(
        """
        {"subscribed":false,"returnValue":true,"channelGroupList":[
         {"channelGroupName":"All","channelGroupId":0,"userEdited":false,"userEditedName":"All","isFavoriteGroup":false},
         {"channelGroupName":"DTV","channelGroupId":1,"userEdited":false,"userEditedName":"DTV","isFavoriteGroup":false},
         {"channelGroupName":"B","channelGroupId":201,"userEdited":true,"userEditedName":"AI-Favoriten","isFavoriteGroup":true},
         {"channelGroupName":"A","channelGroupId":200,"userEdited":true,"userEditedName":"TV","isFavoriteGroup":true},
         {"channelGroupName":"D","channelGroupId":203,"userEdited":false,"userEditedName":"","isFavoriteGroup":true},
         {"channelGroupName":"HD","channelGroupId":7,"userEdited":false,"userEditedName":"HD","isFavoriteGroup":false}
        ],"channelGroupListCount":6}
        """
    )

    private val channelList = JSONObject(
        """
        {"returnValue":true,"channelListCount":3,
         "channelLogoServerUrl":"https://eic-ngfts.tv.wiselg.com/gfts/v1/gftsDownload.lge?biz_code=IBS&func_code=GN_CHANNEL_IMG&file_path=/ibs/gn/channel_img",
         "names":{"channelId":0,"signalChannelId":2,"chanCode":3,"channelNumber":8,"channelName":11,"imgUrl":18,"favoriteIdxA":39},
         "values":[
          ["7_2_7_6_1057_61201_1",null,"1057_61201_1","251533373-342AEF452636DC6B26E129C090C74801",null,null,null,null,"7",null,null,"VOX HD",null,null,null,null,null,null,"/mnt/lg/cmn_data/channel_logo/major_logo_img/251533373-342aef452636dc6b26e129c090c74801_202307100811_h133x100.png",null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,8],
          ["7_16_1_1_1019_10301_1",null,"1019_10301_1",null,null,null,null,null,"1",null,null,"Das Erste HD",null,null,null,null,null,null,"",null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,0],
          ["7_3_3_1_1057_61200_1",null,"1057_61200_1",null,null,null,null,null,"3",null,null,"RTL HD",null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,2]
         ]}
        """
    )

    private val eventList = JSONObject(
        """
        {"returnValue":true,"programListCount":2,"programList":[
         {"signalChannelId":"1057_61201_1","programId":"83871/2026-10-2519:15","programName":"Grenzenlos Genial Gekocht",
          "programSubTitle":"","description":"Kochshow","genre":"Leisure hobbies","epgDataType":3,
          "offlineProgramId":"1_1057_61201_45336","imgUrl":"","fepgImgUrl":"https://img.example/1.jpg",
          "startTime":"2026,10,25,19,15,00","endTime":"2026,10,25,22,20,00",
          "localStartTime":"2026,10,25,20,15,00","localEndTime":"2026,10,25,23,20,00",
          "extendedDescription":[{"extendedDescriptionNumber":1,"extendedText":"Teil zwei."},
                                 {"extendedDescriptionNumber":0,"extendedText":"Teil eins, "}]},
         {"signalChannelId":"5_62_133","programId":"x","programName":"Kaputt",
          "startTime":"2026,10,25,19,15,00","endTime":"2026,10,25,19,15,00"},
         {"signalChannelId":"1057_61201_1","programId":"1_1057_61201_45324","programName":"Criminal Intent",
          "epgDataType":1,"offlineProgramId":"","startTime":"2026,10,25,07,10,00","endTime":"2026,10,25,08,05,00"}
        ]}
        """
    )

    @Test
    fun favoriteListsKeepOnlyFavoriteGroupsSortedByCode() {
        val lists = EpgParser.favoriteLists(channelGroups)

        assertEquals(listOf("A", "B", "D"), lists.map { it.id })
        assertEquals("TV", lists[0].name)
        assertEquals("AI-Favoriten", lists[1].name)
        assertEquals("D", lists[2].name)
    }

    @Test
    fun missingListGivesNoFavorites() {
        assertTrue(EpgParser.favoriteLists(JSONObject("{\"returnValue\":true}")).isEmpty())
    }

    @Test
    fun channelsAreSortedByFavoritePositionWithLogos() {
        val channels = EpgParser.channels(channelList, "A")

        assertEquals(listOf("Das Erste HD", "RTL HD", "VOX HD"), channels.map { it.name })
        val vox = channels.last()
        assertEquals("7_2_7_6_1057_61201_1", vox.id)
        assertEquals("1057_61201_1", vox.signalId)
        assertEquals("7", vox.number)
        assertEquals("251533373-342AEF452636DC6B26E129C090C74801", vox.channelCode)
        assertNull(channels.first().channelCode)
        assertTrue(vox.logoPath!!.startsWith("/mnt/lg/cmn_data/channel_logo/"))
        assertTrue(vox.logoUrl!!.endsWith("/channel_img/251533373-342aef452636dc6b26e129c090c74801_202307100811_h133x100.png"))
        assertNull(channels.first().logoPath)
        assertNull(channels[1].logoUrl)
    }

    @Test
    fun programsUseUtcAndJoinExtendedText() {
        val programs = EpgParser.programs(eventList)

        assertEquals(2, programs.size)
        val show = programs.first()
        assertEquals(Instant.parse("2026-10-25T19:15:00Z").toEpochMilli(), show.start)
        assertEquals(Instant.parse("2026-10-25T22:20:00Z").toEpochMilli(), show.end)
        assertEquals("Grenzenlos Genial Gekocht", show.title)
        assertEquals("Teil eins, Teil zwei.", show.details)
        assertEquals("1_1057_61201_45336", show.offlineProgramId)
        assertTrue(show.online)
        assertEquals("https://img.example/1.jpg", show.imageUrl)

        val broadcast = programs.last()
        assertFalse(broadcast.online)
        assertNull(broadcast.offlineProgramId)
    }

    @Test
    fun reservationsUseLocalStartAndType() {
        val answer = JSONObject(
            """
            {"returnValue":true,"count":2,"results":[
             {"_id":"OTfPFJYRgFR","reserveType":0,"signalChannelId":"1057_61201_1","title":"Grenzenlos Genial Gekocht",
              "startTime":"2026,10,25,20,15,00","endTime":"2026,10,25,23,20,00"},
             {"_id":"OTfPU2XMxS7","reserveType":1,"signalChannelId":"1019_10301_1","title":"Tagesthemen",
              "startTime":"2026,10,25,23,15,00","endTime":"2026,10,25,23,35,00"}
            ]}
            """
        )

        val reservations = EpgParser.reservations(answer, ZoneId.of("Europe/Berlin"))

        assertEquals(listOf(ReservationType.REMINDER, ReservationType.RECORDING), reservations.map { it.type })
        assertEquals(Instant.parse("2026-10-25T19:15:00Z").toEpochMilli(), reservations[0].start)
        assertEquals("1019_10301_1", reservations[1].signalId)
        assertEquals("OTfPU2XMxS7", reservations[1].id)
    }

    @Test
    fun reservationBelongsToProgramDespiteMissingSeconds() {
        val start = Instant.parse("2026-10-25T21:01:49Z").toEpochMilli()
        val program = EpgProgram("1017_61306_1", "p", start, start + 53 * 60_000L, "Criminal Minds")
        val reservation = EpgReservation("id", ReservationType.REMINDER, "1017_61306_1", Instant.parse("2026-10-25T21:01:00Z").toEpochMilli(), "Criminal Minds")

        assertTrue(reservation.belongsTo(program))
        assertFalse(reservation.copy(signalId = "other").belongsTo(program))
        assertFalse(reservation.copy(start = start - 120_000L).belongsTo(program))
        val previous = EpgProgram("1017_61306_1", "q", start - 52 * 60_000L, start, "Criminal Minds")
        assertFalse(reservation.belongsTo(previous))
    }

    @Test
    fun videoInfoGivesResolutionRatioAndHdr() {
        val answer = JSONObject(
            """
            {"appId":"com.webos.app.livetv","returnValue":true,"videoInfo":{"video":{"width":1920,"frameRate":25.0,
             "ratio":"DISPLAY_RATIO_16_9","IsBT2020":false,"scanType":"VIDEO_INTERLACED","height":1080,"hdrType":"none"},
             "context":"x","content":"dtv"}}
            """
        )

        assertEquals(EpgVideoInfo("1080i", "16:9", null), EpgParser.videoInfo(answer))
        val uhd = JSONObject(answer.toString().replace("1080", "2160").replace("VIDEO_INTERLACED", "VIDEO_PROGRESSIVE").replace("\"none\"", "\"hlg\""))
        assertEquals(EpgVideoInfo("2160p", "16:9", "HLG"), EpgParser.videoInfo(uhd))
        assertNull(EpgParser.videoInfo(JSONObject("{\"returnValue\":false}")))
    }

    @Test
    fun audioTracksFromLegacyBroadcast() {
        // BR Fernsehen Süd HD, shortened.
        val answer = JSONObject(
            """
            {"returnValue":true,"audioLanguageListCount":4,"currentAudioLanguageId":3,"audioLanguageList":[
             {"audioLanguageId":0,"audioLanguageName":"German","translatedAudioLanguageName":"Deutsch","audioCodecType":"AUDIO_CODEC_MPEG","hearingImpaired":false,"visualImpaired":false},
             {"audioLanguageId":1,"audioLanguageName":"German","translatedAudioLanguageName":"Deutsch","audioCodecType":"AUDIO_CODEC_MPEG","hearingImpaired":false,"visualImpaired":true,"mixType":true},
             {"audioLanguageId":2,"audioLanguageName":"German","translatedAudioLanguageName":"","audioCodecType":"AUDIO_CODEC_MPEG","hearingImpaired":true,"visualImpaired":false},
             {"audioLanguageId":3,"audioLanguageName":"German","translatedAudioLanguageName":"Deutsch","audioCodecType":"AUDIO_CODEC_AC3_DOLBY","hearingImpaired":false,"visualImpaired":false}]}
            """
        )

        val tracks = EpgParser.audioTracks(answer)

        assertEquals(3, tracks.currentId)
        assertEquals(
            listOf(
                AudioTrack(0, "Deutsch", description = false, hearingImpaired = false, dolby = false),
                AudioTrack(1, "Deutsch", description = true, hearingImpaired = false, dolby = false),
                AudioTrack(2, "German", description = false, hearingImpaired = true, dolby = false),
                AudioTrack(3, "Deutsch", description = false, hearingImpaired = false, dolby = true)
            ),
            tracks.tracks
        )
        assertEquals(AudioTracks(emptyList(), null), EpgParser.audioTracks(JSONObject("{\"returnValue\":false}")))
    }

    @Test
    fun audioDescriptionSetting() {
        assertEquals(true, EpgParser.audioDescriptionEnabled(JSONObject("{\"returnValue\":true,\"settings\":{\"audioDescription\":\"on\"}}")))
        assertEquals(false, EpgParser.audioDescriptionEnabled(JSONObject("{\"returnValue\":true,\"settings\":{\"audioDescription\":\"off\"}}")))
        assertNull(EpgParser.audioDescriptionEnabled(JSONObject("{\"returnValue\":false}")))
    }

    @Test
    fun subtitlesFromUtpSubtitle() {
        // ZDF HD, subtitles off.
        val answer = JSONObject(
            """
            {"subtitleCount":1,"returnValue":true,"broadcastId":"00000029-dd41-cd8c-b4b8-c9bda3f3a8f2","currentSubtitleId":null,
             "subtitle":[{"subtitleId":1,"subtitleLanguage":"German","subtitleType":1,"isTeletext":false,"isHearingImpaired":true,
             "subtitleRatio":"","subtitleIndex":0,"translatedSubtitleLanguage":"Deutsch"}]}
            """
        )

        val off = EpgParser.subtitles(answer, enabled = true)

        assertNull(off.currentId)
        assertEquals(listOf(SubtitleTrack(1, "Deutsch", hearingImpaired = true, teletext = false)), off.tracks)

        answer.put("currentSubtitleId", 1)
        assertEquals(1, EpgParser.subtitles(answer, enabled = true).currentId)
        assertNull(EpgParser.subtitles(answer, enabled = false).currentId)
        assertTrue(EpgParser.subtitles(JSONObject("{\"returnValue\":false,\"errorText\":\"no broadcast\"}"), enabled = true).tracks.isEmpty())
    }

    @Test
    fun subtitleSetting() {
        assertTrue(EpgParser.subtitlesEnabled(JSONObject("{\"returnValue\":true,\"settings\":{\"captionEnable\":\"on\"}}")))
        assertFalse(EpgParser.subtitlesEnabled(JSONObject("{\"returnValue\":true,\"settings\":{\"captionEnable\":\"off\"}}")))
    }

    @Test
    fun localTimeUsesTvFormat() {
        val millis = Instant.parse("2026-10-25T19:15:00Z").toEpochMilli()

        assertEquals("2026,10,25,20,15,00", EpgParser.localTime(millis, ZoneId.of("Europe/Berlin")))
    }
}
