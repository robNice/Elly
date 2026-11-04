package de.robnice.elly.remote

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TvAppParserTest {

    @Test
    fun parseLaunchPoints_readsAppsSortedByTitle() {
        val payload = JSONObject(
            """
            {"launchPoints": [
              {"id": "youtube.leanback.v4", "title": "YouTube", "largeIcon": "http://tv/yt-large.png", "icon": "http://tv/yt.png"},
              {"id": "netflix", "title": "Netflix", "icon": "http://tv/nf.png"},
              {"id": "netflix", "title": "Netflix duplicate"},
              {"id": "amazon"}
            ]}
            """
        )

        assertEquals(
            listOf(
                TvApp("amazon", "amazon", null),
                TvApp("netflix", "Netflix", "http://tv/nf.png"),
                TvApp("youtube.leanback.v4", "YouTube", "http://tv/yt-large.png")
            ),
            TvAppParser.parseLaunchPoints(payload)
        )
    }

    @Test
    fun parseLaunchPoints_handlesMissingList() {
        assertTrue(TvAppParser.parseLaunchPoints(JSONObject("{}")).isEmpty())
    }
}
