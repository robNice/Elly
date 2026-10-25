package de.robnice.elly.tv.ssh

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LunaCommandTest {

    @Test
    fun buildsCommandWithQuotedPayload() {
        val command = LunaCommand.build(
            "luna://com.webos.service.tv.systemproperty/getSystemInfo",
            JSONObject().put("name", "it's")
        )

        assertEquals(
            "luna-send -n 1 luna://com.webos.service.tv.systemproperty/getSystemInfo '{\"name\":\"it'\\''s\"}'",
            command
        )
    }

    @Test
    fun rejectsUriWithShellCharacters() {
        assertThrows(IllegalArgumentException::class.java) {
            LunaCommand.build("luna://com.webos.service.iepg/getEventList; rm -rf /")
        }
    }

    @Test
    fun parsesFirstObjectAndIgnoresTerminalText() {
        val output = "{\"returnValue\":true,\"modelName\":\"OLED55C27LA\"}\r\nConnection to 192.168.1.148 closed.\r\n"

        val answer = LunaCommand.parse(output)

        assertTrue(answer.getBoolean("returnValue"))
        assertEquals("OLED55C27LA", answer.getString("modelName"))
    }

    @Test
    fun parseFailsWithoutJson() {
        assertThrows(TvShellException.LunaUnavailable::class.java) {
            LunaCommand.parse("sh: luna-send: not found\r\n")
        }
    }
}
