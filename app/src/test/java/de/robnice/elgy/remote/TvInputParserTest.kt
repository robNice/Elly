package de.robnice.elgy.remote

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TvInputParserTest {

    @Test
    fun parse_readsInputsFromDeviceList() {
        val payload = JSONObject(
            """
            {"devices": [
              {"id": "HDMI_1", "label": "HDMI 1", "connected": true},
              {"id": "HDMI_2", "label": "PlayStation", "connected": false},
              {"id": "AV_1"}
            ]}
            """
        )

        assertEquals(
            listOf(
                TvInput("HDMI_1", "HDMI 1", true),
                TvInput("HDMI_2", "PlayStation", false),
                TvInput("AV_1", "AV_1", false)
            ),
            TvInputParser.parse(payload)
        )
    }

    @Test
    fun parse_skipsEntriesWithoutIdAndHandlesMissingList() {
        assertTrue(TvInputParser.parse(JSONObject("""{"devices": [{"label": "x"}]}""")).isEmpty())
        assertTrue(TvInputParser.parse(JSONObject("{}")).isEmpty())
    }
}
