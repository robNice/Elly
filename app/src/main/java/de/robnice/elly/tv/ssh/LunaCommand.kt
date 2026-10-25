package de.robnice.elly.tv.ssh

import org.json.JSONObject
import org.json.JSONTokener

// Builds luna-send calls and reads their answer from the terminal output.
object LunaCommand {

    fun build(uri: String, payload: JSONObject = JSONObject()): String {
        require(uri.matches(URI_PATTERN)) { "Invalid luna URI" }
        return "luna-send -n 1 $uri '${payload.toString().replace("'", "'\\''")}'"
    }

    // The terminal may add text around the answer, so only the first JSON object is read.
    fun parse(output: String): JSONObject {
        val start = output.indexOf('{')
        if (start < 0) throw TvShellException.LunaUnavailable(output.trim().take(MAX_REASON_LENGTH))
        return try {
            JSONTokener(output.substring(start)).nextValue() as JSONObject
        } catch (e: Exception) {
            throw TvShellException.LunaUnavailable(e.message)
        }
    }

    private val URI_PATTERN = Regex("^luna://[A-Za-z0-9._-]+(/[A-Za-z0-9._-]+)+$")
    private const val MAX_REASON_LENGTH = 200
}
